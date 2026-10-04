package com.example.mykku.feed.adapter.input.web

import com.example.mykku.BaseControllerTest
import com.example.mykku.board.adapter.output.persistence.entity.BoardJpaEntity
import com.example.mykku.feed.adapter.input.web.dto.CreateFeedCommentRequest
import com.example.mykku.feed.adapter.input.web.dto.UpdateFeedCommentRequest
import com.example.mykku.feed.adapter.output.persistence.FeedCommentJpaRepository
import com.example.mykku.feed.adapter.output.persistence.FeedJpaRepository
import com.example.mykku.feed.adapter.output.persistence.entity.FeedCommentJpaEntity
import com.example.mykku.feed.adapter.output.persistence.entity.FeedJpaEntity
import com.example.mykku.member.adapter.output.persistence.entity.MemberJpaEntity
import com.example.mykku.member.domain.vo.SocialProvider
import com.example.mykku.member.exception.MemberErrorCode
import com.example.mykku.util.TestTokenGenerator
import com.fasterxml.jackson.databind.ObjectMapper
import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.notNullValue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@DisplayName("FeedCommentController 통합 테스트")
class FeedCommentControllerTest : BaseControllerTest() {

    @Autowired
    private lateinit var feedJpaRepository: FeedJpaRepository

    @Autowired
    private lateinit var feedCommentJpaRepository: FeedCommentJpaRepository

    private val objectMapper = ObjectMapper()

    @Test
    @DisplayName("댓글 생성 - 정상 케이스")
    fun `createComment - 정상적으로 댓글을 생성한다`() {
        val member = memberJpaRepository.save(
            MemberJpaEntity(
                memberId = "member1",
                socialId = "member1",
                provider = SocialProvider.GOOGLE,
                email = "member1@example.com",
                nickname = "Member1",
                role = null,
                profileImage = ""
            )
        )
        val board = boardJpaRepository.save(
            BoardJpaEntity(
                title = "테스트 게시판",
                logo = "test_logo.png"
            )
        )
        val feed = feedJpaRepository.save(
            FeedJpaEntity(
                title = "테스트 피드",
                content = "테스트 내용",
                member = member,
                board = board
            )
        )
        val authHeader = TestTokenGenerator.getBearerToken(member.id)
        val request = CreateFeedCommentRequest(
            content = "테스트 댓글",
            parentCommentId = null
        )
        val requestJson = objectMapper.writeValueAsString(request)

        RestAssured.given()
            .header("Authorization", authHeader)
            .contentType(ContentType.JSON)
            .body(requestJson)
        .`when`()
            .post("/api/v1/feeds/{feedId}/comments", feed.id)
        .then()
            .statusCode(200)
            .body("message", equalTo("댓글이 성공적으로 등록되었습니다."))
            .body("data.content", equalTo("테스트 댓글"))
            .body("data.author.memberId", equalTo("member1"))
            .body("data.author.nickname", equalTo("Member1"))
            .body("data.author.role", equalTo(null))
            .body("data.replies.size()", equalTo(0))
            .body("data.replyCount", equalTo(0))
            .body("data.likeCount", equalTo(0))
            .body("data.isLiked", equalTo(false))
            .body("data.updatedAt", notNullValue())
    }

    @Test
    @DisplayName("답글 생성 - 정상 케이스")
    fun `createComment - 정상적으로 답글을 생성한다`() {
        val member = memberJpaRepository.save(
            MemberJpaEntity(
                memberId = "testmemberid2",
                socialId = "member1",
                provider = SocialProvider.GOOGLE,
                email = "member1@example.com",
                nickname = "Member1",
                role = null,
                profileImage = ""
            )
        )
        val board = boardJpaRepository.save(
            BoardJpaEntity(
                title = "테스트 게시판",
                logo = "test_logo.png"
            )
        )
        val feed = feedJpaRepository.save(
            FeedJpaEntity(
                title = "테스트 피드",
                content = "테스트 내용",
                member = member,
                board = board
            )
        )
        val parentComment = feedCommentJpaRepository.save(
            FeedCommentJpaEntity(
                content = "부모 댓글",
                feed = feed,
                member = member
            )
        )
        val authHeader = TestTokenGenerator.getBearerToken(member.id)
        val request = CreateFeedCommentRequest(
            content = "답글입니다",
            parentCommentId = parentComment.id
        )
        val requestJson = objectMapper.writeValueAsString(request)

        RestAssured.given()
            .header("Authorization", authHeader)
            .contentType(ContentType.JSON)
            .body(requestJson)
        .`when`()
            .post("/api/v1/feeds/{feedId}/comments", feed.id)
        .then()
            .statusCode(200)
            .body("message", equalTo("댓글이 성공적으로 등록되었습니다."))
            .body("data.content", equalTo("답글입니다"))
    }

    @Test
    @DisplayName("댓글 생성 - 인증되지 않은 사용자")
    fun `createComment - 인증되지 않은 사용자는 댓글을 생성할 수 없다`() {
        val member = memberJpaRepository.save(
            MemberJpaEntity(
                memberId = "testmemberid3",
                socialId = "member1",
                provider = SocialProvider.GOOGLE,
                email = "member1@example.com",
                nickname = "Member1",
                role = null,
                profileImage = ""
            )
        )
        val board = boardJpaRepository.save(
            BoardJpaEntity(
                title = "테스트 게시판",
                logo = "test_logo.png"
            )
        )
        val feed = feedJpaRepository.save(
            FeedJpaEntity(
                title = "테스트 피드",
                content = "테스트 내용",
                member = member,
                board = board
            )
        )
        val request = CreateFeedCommentRequest(
            content = "테스트 댓글",
            parentCommentId = null
        )
        val requestJson = objectMapper.writeValueAsString(request)

        RestAssured.given()
            .contentType(ContentType.JSON)
            .body(requestJson)
        .`when`()
            .post("/api/v1/feeds/{feedId}/comments", feed.id)
        .then()
            .statusCode(401)
    }

    @Test
    @DisplayName("댓글 생성 - 아이디를 설정하지 않은 회원")
    fun `createComment - memberId가 없는 회원은 댓글을 생성할 수 없다`() {
        val feedAuthor = memberJpaRepository.save(
            MemberJpaEntity(
                memberId = "feedauthor",
                socialId = "feedauthor",
                provider = SocialProvider.GOOGLE,
                email = "feedauthor@example.com",
                nickname = "피드작성자",
                role = null,
                profileImage = ""
            )
        )
        val incompleteMember = memberJpaRepository.save(
            MemberJpaEntity(
                memberId = null,
                socialId = "incomplete-member-id",
                provider = SocialProvider.GOOGLE,
                email = "incomplete-member-id@example.com",
                nickname = "미완성유저",
                role = null,
                profileImage = ""
            )
        )
        val board = boardJpaRepository.save(
            BoardJpaEntity(
                title = "테스트 게시판",
                logo = "test_logo.png"
            )
        )
        val feed = feedJpaRepository.save(
            FeedJpaEntity(
                title = "테스트 피드",
                content = "테스트 내용",
                member = feedAuthor,
                board = board
            )
        )
        val authHeader = TestTokenGenerator.getBearerToken(incompleteMember.id)
        val request = CreateFeedCommentRequest(
            content = "테스트 댓글",
            parentCommentId = null
        )
        val requestJson = objectMapper.writeValueAsString(request)

        RestAssured.given()
            .header("Authorization", authHeader)
            .contentType(ContentType.JSON)
            .body(requestJson)
        .`when`()
            .post("/api/v1/feeds/{feedId}/comments", feed.id)
        .then()
            .statusCode(403)
            .body("code", equalTo(MemberErrorCode.PROFILE_NOT_COMPLETED.code))
    }

    @Test
    @DisplayName("댓글 생성 - 닉네임을 설정하지 않은 회원")
    fun `createComment - nickname이 없는 회원은 댓글을 생성할 수 없다`() {
        val feedAuthor = memberJpaRepository.save(
            MemberJpaEntity(
                memberId = "feedauthor2",
                socialId = "feedauthor2",
                provider = SocialProvider.GOOGLE,
                email = "feedauthor2@example.com",
                nickname = "피드작성자",
                role = null,
                profileImage = ""
            )
        )
        val incompleteMember = memberJpaRepository.save(
            MemberJpaEntity(
                memberId = "incompletenick",
                socialId = "incomplete-nickname",
                provider = SocialProvider.GOOGLE,
                email = "incomplete-nickname@example.com",
                nickname = null,
                role = null,
                profileImage = ""
            )
        )
        val board = boardJpaRepository.save(
            BoardJpaEntity(
                title = "테스트 게시판",
                logo = "test_logo.png"
            )
        )
        val feed = feedJpaRepository.save(
            FeedJpaEntity(
                title = "테스트 피드",
                content = "테스트 내용",
                member = feedAuthor,
                board = board
            )
        )
        val authHeader = TestTokenGenerator.getBearerToken(incompleteMember.id)
        val request = CreateFeedCommentRequest(
            content = "테스트 댓글",
            parentCommentId = null
        )
        val requestJson = objectMapper.writeValueAsString(request)

        RestAssured.given()
            .header("Authorization", authHeader)
            .contentType(ContentType.JSON)
            .body(requestJson)
        .`when`()
            .post("/api/v1/feeds/{feedId}/comments", feed.id)
        .then()
            .statusCode(403)
            .body("code", equalTo(MemberErrorCode.PROFILE_NOT_COMPLETED.code))
    }

    @Test
    @DisplayName("댓글 수정 - 정상 케이스")
    fun `updateComment - 정상적으로 댓글을 수정한다`() {
        val member = memberJpaRepository.save(
            MemberJpaEntity(
                memberId = "testmemberid4",
                socialId = "member1",
                provider = SocialProvider.GOOGLE,
                email = "member1@example.com",
                nickname = "Member1",
                role = null,
                profileImage = ""
            )
        )
        val board = boardJpaRepository.save(
            BoardJpaEntity(
                title = "테스트 게시판",
                logo = "test_logo.png"
            )
        )
        val feed = feedJpaRepository.save(
            FeedJpaEntity(
                title = "테스트 피드",
                content = "테스트 내용",
                member = member,
                board = board
            )
        )
        val comment = feedCommentJpaRepository.save(
            FeedCommentJpaEntity(
                content = "원본 댓글",
                feed = feed,
                member = member
            )
        )
        feedCommentJpaRepository.save(
            FeedCommentJpaEntity(
                content = "기존 답글",
                feed = feed,
                member = member,
                parentComment = comment
            )
        )
        val authHeader = TestTokenGenerator.getBearerToken(member.id)
        val request = UpdateFeedCommentRequest(content = "수정된 댓글")
        val requestJson = objectMapper.writeValueAsString(request)

        RestAssured.given()
            .header("Authorization", authHeader)
            .contentType(ContentType.JSON)
            .body(requestJson)
        .`when`()
            .put("/api/v1/feeds/comments/{commentId}", comment.id)
        .then()
            .statusCode(200)
            .body("message", equalTo("댓글이 성공적으로 수정되었습니다."))
            .body("data.content", equalTo("수정된 댓글"))
            .body("data.author.memberId", equalTo("testmemberid4"))
            .body("data.replies.size()", equalTo(1))
            .body("data.replyCount", equalTo(1))
            .body("data.replies[0].content", equalTo("기존 답글"))
            .body("data.isLiked", equalTo(false))
    }

    @Test
    @DisplayName("댓글 수정 - 권한 없는 사용자")
    fun `updateComment - 다른 사용자의 댓글은 수정할 수 없다`() {
        val member1 = memberJpaRepository.save(
            MemberJpaEntity(
                memberId = "testmemberid5",
                socialId = "member1",
                provider = SocialProvider.GOOGLE,
                email = "member1@example.com",
                nickname = "Member1",
                role = null,
                profileImage = ""
            )
        )
        val member2 = memberJpaRepository.save(
            MemberJpaEntity(
                memberId = "testmemberid6",
                socialId = "member2",
                provider = SocialProvider.GOOGLE,
                email = "member2@example.com",
                nickname = "Member2",
                role = null,
                profileImage = ""
            )
        )
        val board = boardJpaRepository.save(
            BoardJpaEntity(
                title = "테스트 게시판",
                logo = "test_logo.png"
            )
        )
        val feed = feedJpaRepository.save(
            FeedJpaEntity(
                title = "테스트 피드",
                content = "테스트 내용",
                member = member1,
                board = board
            )
        )
        val comment = feedCommentJpaRepository.save(
            FeedCommentJpaEntity(
                content = "원본 댓글",
                feed = feed,
                member = member1
            )
        )
        val authHeader = TestTokenGenerator.getBearerToken(member2.id)
        val request = UpdateFeedCommentRequest(content = "수정된 댓글")
        val requestJson = objectMapper.writeValueAsString(request)

        RestAssured.given()
            .header("Authorization", authHeader)
            .contentType(ContentType.JSON)
            .body(requestJson)
        .`when`()
            .put("/api/v1/feeds/comments/{commentId}", comment.id)
        .then()
            .statusCode(403)
    }

    @Test
    @DisplayName("댓글 삭제 - 정상 케이스")
    fun `deleteComment - 정상적으로 댓글을 삭제한다`() {
        val member = memberJpaRepository.save(
            MemberJpaEntity(
                memberId = "testmemberid7",
                socialId = "member1",
                provider = SocialProvider.GOOGLE,
                email = "member1@example.com",
                nickname = "Member1",
                role = null,
                profileImage = ""
            )
        )
        val board = boardJpaRepository.save(
            BoardJpaEntity(
                title = "테스트 게시판",
                logo = "test_logo.png"
            )
        )
        val feed = feedJpaRepository.save(
            FeedJpaEntity(
                title = "테스트 피드",
                content = "테스트 내용",
                member = member,
                board = board
            )
        )
        val comment = feedCommentJpaRepository.save(
            FeedCommentJpaEntity(
                content = "삭제할 댓글",
                feed = feed,
                member = member
            )
        )
        val authHeader = TestTokenGenerator.getBearerToken(member.id)

        RestAssured.given()
            .header("Authorization", authHeader)
        .`when`()
            .delete("/api/v1/feeds/comments/{commentId}", comment.id)
        .then()
            .statusCode(200)
            .body("message", equalTo("댓글이 성공적으로 삭제되었습니다."))
    }

    @Test
    @DisplayName("댓글 삭제 - 권한 없는 사용자")
    fun `deleteComment - 다른 사용자의 댓글은 삭제할 수 없다`() {
        val member1 = memberJpaRepository.save(
            MemberJpaEntity(
                memberId = "testmemberid8",
                socialId = "member1",
                provider = SocialProvider.GOOGLE,
                email = "member1@example.com",
                nickname = "Member1",
                role = null,
                profileImage = ""
            )
        )
        val member2 = memberJpaRepository.save(
            MemberJpaEntity(
                memberId = "testmemberid9",
                socialId = "member2",
                provider = SocialProvider.GOOGLE,
                email = "member2@example.com",
                nickname = "Member2",
                role = null,
                profileImage = ""
            )
        )
        val board = boardJpaRepository.save(
            BoardJpaEntity(
                title = "테스트 게시판",
                logo = "test_logo.png"
            )
        )
        val feed = feedJpaRepository.save(
            FeedJpaEntity(
                title = "테스트 피드",
                content = "테스트 내용",
                member = member1,
                board = board
            )
        )
        val comment = feedCommentJpaRepository.save(
            FeedCommentJpaEntity(
                content = "삭제할 댓글",
                feed = feed,
                member = member1
            )
        )
        val authHeader = TestTokenGenerator.getBearerToken(member2.id)

        RestAssured.given()
            .header("Authorization", authHeader)
        .`when`()
            .delete("/api/v1/feeds/comments/{commentId}", comment.id)
        .then()
            .statusCode(403)
    }

    @Test
    @DisplayName("댓글 삭제 - 인증되지 않은 사용자")
    fun `deleteComment - 인증되지 않은 사용자는 삭제할 수 없다`() {
        val member = memberJpaRepository.save(
            MemberJpaEntity(
                memberId = "testmemberid10",
                socialId = "member1",
                provider = SocialProvider.GOOGLE,
                email = "member1@example.com",
                nickname = "Member1",
                role = null,
                profileImage = ""
            )
        )
        val board = boardJpaRepository.save(
            BoardJpaEntity(
                title = "테스트 게시판",
                logo = "test_logo.png"
            )
        )
        val feed = feedJpaRepository.save(
            FeedJpaEntity(
                title = "테스트 피드",
                content = "테스트 내용",
                member = member,
                board = board
            )
        )
        val comment = feedCommentJpaRepository.save(
            FeedCommentJpaEntity(
                content = "삭제할 댓글",
                feed = feed,
                member = member
            )
        )

        RestAssured.given()
        .`when`()
            .delete("/api/v1/feeds/comments/{commentId}", comment.id)
        .then()
            .statusCode(401)
    }
}
