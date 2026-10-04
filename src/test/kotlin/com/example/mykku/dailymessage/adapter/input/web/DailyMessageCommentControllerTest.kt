package com.example.mykku.dailymessage.adapter.input.web

import com.example.mykku.BaseControllerTest
import com.example.mykku.dailymessage.adapter.output.persistence.entity.DailyMessageJpaEntity
import com.example.mykku.dailymessage.adapter.output.persistence.entity.DailyMessageCommentJpaEntity
import com.example.mykku.dailymessage.adapter.input.web.CreateCommentRequest
import com.example.mykku.dailymessage.adapter.input.web.UpdateCommentRequest
import com.example.mykku.dailymessage.adapter.output.persistence.repository.DailyMessageCommentJpaRepository
import com.example.mykku.dailymessage.adapter.output.persistence.repository.DailyMessageJpaRepository
import com.example.mykku.dailymessage.exception.DailyMessageErrorCode
import com.example.mykku.like.adapter.output.persistence.LikeDailyMessageCommentJpaRepository
import com.example.mykku.like.adapter.output.persistence.entity.LikeDailyMessageCommentJpaEntity
import com.example.mykku.member.adapter.output.persistence.entity.MemberJpaEntity
import com.example.mykku.member.domain.vo.SocialProvider
import com.example.mykku.member.exception.MemberErrorCode
import com.example.mykku.role.adapter.output.persistence.entity.RoleJpaEntity
import com.example.mykku.util.TestTokenGenerator
import io.restassured.RestAssured
import io.restassured.http.ContentType
import java.time.LocalDate
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.notNullValue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@DisplayName("DailyMessageCommentController 통합 테스트")
class DailyMessageCommentControllerTest : BaseControllerTest() {

    @Autowired
    private lateinit var dailyMessageJpaRepository: DailyMessageJpaRepository

    @Autowired
    private lateinit var dailyMessageCommentJpaRepository: DailyMessageCommentJpaRepository

    @Autowired
    private lateinit var likeDailyMessageCommentJpaRepository: LikeDailyMessageCommentJpaRepository

    @Test
    @DisplayName("댓글 생성 - 정상 케이스")
    fun `createComment - 정상적으로 댓글을 생성한다`() {
        // given
        val member = memberJpaRepository.save(
            MemberJpaEntity(
                memberId = "testmemberid1",
                socialId = "member1",
                provider = SocialProvider.GOOGLE,
                email = "member1@example.com",
                nickname = "Member1",
                role = null,
                profileImage = ""
            )
        )
        val dailyMessage = dailyMessageJpaRepository.save(
            DailyMessageJpaEntity(
                title = "오늘의 덕담",
                content = "오늘도 좋은 하루!",
                date = LocalDate.now()
            )
        )
        val authHeader = TestTokenGenerator.getBearerToken(member.id)
        val request = CreateCommentRequest(content = "좋은 글이네요!")

        // when & then
        RestAssured.given()
            .header("Authorization", authHeader)
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .post("/api/v1/daily-messages/{dailyMessageId}/comments", dailyMessage.id)
            .then()
            .statusCode(200)
            .body("message", equalTo("댓글이 성공적으로 등록되었습니다."))
            .body("data", notNullValue())
            .body("data.author.memberId", equalTo("testmemberid1"))
            .body("data.author.nickname", equalTo("Member1"))
            .body("data.author.role", equalTo(null))
            .body("data.replies.size()", equalTo(0))
            .body("data.replyCount", equalTo(0))
            .body("data.likeCount", equalTo(0))
            .body("data.isLiked", equalTo(false))
            .body("data.updatedAt", notNullValue())
    }

    @Test
    @DisplayName("댓글 생성 - 인증되지 않은 사용자")
    fun `createComment - 인증되지 않은 사용자는 댓글을 생성할 수 없다`() {
        // given
        val dailyMessage = dailyMessageJpaRepository.save(
            DailyMessageJpaEntity(
                title = "오늘의 덕담",
                content = "오늘도 좋은 하루!",
                date = LocalDate.now()
            )
        )
        val request = CreateCommentRequest(content = "좋은 글이네요!")

        // when & then
        RestAssured.given()
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .post("/api/v1/daily-messages/{dailyMessageId}/comments", dailyMessage.id)
            .then()
            .statusCode(401)
    }

    @Test
    @DisplayName("댓글 생성 - 아이디를 설정하지 않은 회원")
    fun `createComment - memberId가 없는 회원은 댓글을 생성할 수 없다`() {
        // given
        val member = memberJpaRepository.save(
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
        val dailyMessage = dailyMessageJpaRepository.save(
            DailyMessageJpaEntity(
                title = "오늘의 덕담",
                content = "오늘도 좋은 하루!",
                date = LocalDate.now()
            )
        )
        val authHeader = TestTokenGenerator.getBearerToken(member.id)
        val request = CreateCommentRequest(content = "좋은 글이네요!")

        // when & then
        RestAssured.given()
            .header("Authorization", authHeader)
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .post("/api/v1/daily-messages/{dailyMessageId}/comments", dailyMessage.id)
            .then()
            .statusCode(403)
            .body("code", equalTo(MemberErrorCode.PROFILE_NOT_COMPLETED.code))
    }

    @Test
    @DisplayName("댓글 생성 - 닉네임을 설정하지 않은 회원")
    fun `createComment - nickname이 없는 회원은 댓글을 생성할 수 없다`() {
        // given
        val member = memberJpaRepository.save(
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
        val dailyMessage = dailyMessageJpaRepository.save(
            DailyMessageJpaEntity(
                title = "오늘의 덕담",
                content = "오늘도 좋은 하루!",
                date = LocalDate.now()
            )
        )
        val authHeader = TestTokenGenerator.getBearerToken(member.id)
        val request = CreateCommentRequest(content = "좋은 글이네요!")

        // when & then
        RestAssured.given()
            .header("Authorization", authHeader)
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .post("/api/v1/daily-messages/{dailyMessageId}/comments", dailyMessage.id)
            .then()
            .statusCode(403)
            .body("code", equalTo(MemberErrorCode.PROFILE_NOT_COMPLETED.code))
    }

    @Test
    @DisplayName("댓글 수정 - 정상 케이스")
    fun `updateComment - 정상적으로 댓글을 수정한다`() {
        // given
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
        val dailyMessage = dailyMessageJpaRepository.save(
            DailyMessageJpaEntity(
                title = "오늘의 덕담",
                content = "오늘도 좋은 하루!",
                date = LocalDate.now()
            )
        )
        val comment = dailyMessageCommentJpaRepository.save(
            DailyMessageCommentJpaEntity(
                content = "원래 댓글",
                dailyMessage = dailyMessage,
                member = member
            )
        )
        val replier = memberJpaRepository.save(
            MemberJpaEntity(
                memberId = "updatereplier",
                socialId = "updatereplier",
                provider = SocialProvider.GOOGLE,
                email = "updatereplier@example.com",
                nickname = "답글러",
                role = null,
                profileImage = ""
            )
        )
        dailyMessageCommentJpaRepository.save(
            DailyMessageCommentJpaEntity(
                content = "기존 답글",
                dailyMessage = dailyMessage,
                member = replier,
                parentComment = comment
            )
        )
        val authHeader = TestTokenGenerator.getBearerToken(member.id)
        val request = UpdateCommentRequest(content = "수정된 댓글")

        // when & then
        RestAssured.given()
            .header("Authorization", authHeader)
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .put("/api/v1/daily-messages/comments/{commentId}", comment.id)
            .then()
            .statusCode(200)
            .body("message", equalTo("댓글이 성공적으로 수정되었습니다."))
            .body("data", notNullValue())
            .body("data.content", equalTo("수정된 댓글"))
            .body("data.author.memberId", equalTo("testmemberid2"))
            .body("data.replies.size()", equalTo(1))
            .body("data.replyCount", equalTo(1))
            .body("data.replies[0].author.memberId", equalTo("updatereplier"))
            .body("data.replies[0].author.nickname", equalTo("답글러"))
    }

    @Test
    @DisplayName("댓글 수정 - 인증되지 않은 사용자")
    fun `updateComment - 인증되지 않은 사용자는 댓글을 수정할 수 없다`() {
        // given
        val request = UpdateCommentRequest(content = "수정된 댓글")

        // when & then
        RestAssured.given()
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .put("/api/v1/daily-messages/comments/{commentId}", 1L)
            .then()
            .statusCode(401)
    }

    @Test
    @DisplayName("댓글 삭제 - 정상 케이스")
    fun `deleteComment - 정상적으로 댓글을 삭제한다`() {
        // given
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
        val dailyMessage = dailyMessageJpaRepository.save(
            DailyMessageJpaEntity(
                title = "오늘의 덕담",
                content = "오늘도 좋은 하루!",
                date = LocalDate.now()
            )
        )
        val comment = dailyMessageCommentJpaRepository.save(
            DailyMessageCommentJpaEntity(
                content = "삭제할 댓글",
                dailyMessage = dailyMessage,
                member = member
            )
        )
        val authHeader = TestTokenGenerator.getBearerToken(member.id)

        // when & then
        RestAssured.given()
            .header("Authorization", authHeader)
            .`when`()
            .delete("/api/v1/daily-messages/comments/{commentId}", comment.id)
            .then()
            .statusCode(200)
            .body("message", equalTo("댓글이 성공적으로 삭제되었습니다."))
    }

    @Test
    @DisplayName("댓글 삭제 - 권한 없는 사용자")
    fun `deleteComment - 다른 사용자의 댓글은 삭제할 수 없다`() {
        // given
        val member1 = memberJpaRepository.save(
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
        val member2 = memberJpaRepository.save(
            MemberJpaEntity(
                memberId = "testmemberid5",
                socialId = "member2",
                provider = SocialProvider.GOOGLE,
                email = "member2@example.com",
                nickname = "Member2",
                role = null,
                profileImage = ""
            )
        )
        val dailyMessage = dailyMessageJpaRepository.save(
            DailyMessageJpaEntity(
                title = "오늘의 덕담",
                content = "오늘도 좋은 하루!",
                date = LocalDate.now()
            )
        )
        val comment = dailyMessageCommentJpaRepository.save(
            DailyMessageCommentJpaEntity(
                content = "삭제할 댓글",
                dailyMessage = dailyMessage,
                member = member1
            )
        )
        val authHeader = TestTokenGenerator.getBearerToken(member2.id)

        // when & then
        RestAssured.given()
            .header("Authorization", authHeader)
            .`when`()
            .delete("/api/v1/daily-messages/comments/{commentId}", comment.id)
            .then()
            .statusCode(403)
    }

    @Test
    @DisplayName("댓글 삭제 - 인증되지 않은 사용자")
    fun `deleteComment - 인증되지 않은 사용자는 댓글을 삭제할 수 없다`() {
        // when & then
        RestAssured.given()
            .`when`()
            .delete("/api/v1/daily-messages/comments/{commentId}", 1L)
            .then()
            .statusCode(401)
    }

    @Test
    @DisplayName("댓글 목록 조회 - 로그인 멤버가 좋아요한 댓글/답글은 isLiked=true로 표시된다")
    fun `getComments - 내가 좋아요한 댓글과 답글은 isLiked가 true다`() {
        // given
        val member = memberJpaRepository.save(
            MemberJpaEntity(
                memberId = "likemember1",
                socialId = "member1",
                provider = SocialProvider.GOOGLE,
                email = "member1@example.com",
                nickname = "Member1",
                role = null,
                profileImage = ""
            )
        )
        val dailyMessage = dailyMessageJpaRepository.save(
            DailyMessageJpaEntity(title = "오늘의 덕담", content = "내용", date = LocalDate.now())
        )
        val comment = dailyMessageCommentJpaRepository.save(
            DailyMessageCommentJpaEntity(content = "좋아요한 댓글", dailyMessage = dailyMessage, member = member)
        )
        val reply = dailyMessageCommentJpaRepository.save(
            DailyMessageCommentJpaEntity(
                content = "좋아요한 답글",
                dailyMessage = dailyMessage,
                parentComment = comment,
                member = member
            )
        )
        likeDailyMessageCommentJpaRepository.save(
            LikeDailyMessageCommentJpaEntity(member = member, dailyMessageComment = comment)
        )
        likeDailyMessageCommentJpaRepository.save(
            LikeDailyMessageCommentJpaEntity(member = member, dailyMessageComment = reply)
        )
        val authHeader = TestTokenGenerator.getBearerToken(member.id)

        // when & then
        RestAssured.given()
            .header("Authorization", authHeader)
            .`when`()
            .get("/api/v1/daily-messages/{dailyMessageId}/comments", dailyMessage.id)
            .then()
            .statusCode(200)
            .body("data.comments[0].isLiked", equalTo(true))
            .body("data.comments[0].replies[0].isLiked", equalTo(true))
    }

    @Test
    @DisplayName("댓글 목록 조회 - 비로그인 사용자는 모든 isLiked가 false다")
    fun `getComments - 비로그인 사용자는 isLiked가 false다`() {
        // given
        val member = memberJpaRepository.save(
            MemberJpaEntity(
                memberId = "likemember2",
                socialId = "member2",
                provider = SocialProvider.GOOGLE,
                email = "member2@example.com",
                nickname = "Member2",
                role = null,
                profileImage = ""
            )
        )
        val dailyMessage = dailyMessageJpaRepository.save(
            DailyMessageJpaEntity(title = "오늘의 덕담", content = "내용", date = LocalDate.now())
        )
        val comment = dailyMessageCommentJpaRepository.save(
            DailyMessageCommentJpaEntity(content = "댓글", dailyMessage = dailyMessage, member = member)
        )
        likeDailyMessageCommentJpaRepository.save(
            LikeDailyMessageCommentJpaEntity(member = member, dailyMessageComment = comment)
        )

        // when & then
        RestAssured.given()
            .`when`()
            .get("/api/v1/daily-messages/{dailyMessageId}/comments", dailyMessage.id)
            .then()
            .statusCode(200)
            .body("data.comments[0].isLiked", equalTo(false))
    }

    @Test
    @DisplayName("댓글 목록 조회 - 작성자 아이디와 칭호를 포함한다")
    fun `getComments - 작성자 memberId와 칭호를 반환한다`() {
        val role = roleJpaRepository.save(RoleJpaEntity(name = "덕담왕", description = "덕담을 많이 남긴 사람"))
        val commentAuthor = memberJpaRepository.save(
            MemberJpaEntity(
                memberId = "commentauthor",
                socialId = "commentauthor",
                provider = SocialProvider.GOOGLE,
                email = "commentauthor@example.com",
                nickname = "댓글작성자",
                role = role,
                profileImage = ""
            )
        )
        val replyAuthor = memberJpaRepository.save(
            MemberJpaEntity(
                memberId = "replyauthor",
                socialId = "replyauthor",
                provider = SocialProvider.GOOGLE,
                email = "replyauthor@example.com",
                nickname = "답글작성자",
                role = null,
                profileImage = ""
            )
        )
        val dailyMessage = dailyMessageJpaRepository.save(
            DailyMessageJpaEntity(title = "오늘의 덕담", content = "좋은 하루!", date = LocalDate.now())
        )
        val comment = dailyMessageCommentJpaRepository.save(
            DailyMessageCommentJpaEntity(content = "댓글", dailyMessage = dailyMessage, member = commentAuthor)
        )
        dailyMessageCommentJpaRepository.save(
            DailyMessageCommentJpaEntity(
                content = "답글",
                dailyMessage = dailyMessage,
                member = replyAuthor,
                parentComment = comment
            )
        )

        RestAssured.given()
            .`when`()
            .get("/api/v1/daily-messages/{dailyMessageId}/comments", dailyMessage.id)
            .then()
            .statusCode(200)
            .body("data.comments[0].author.memberId", equalTo("commentauthor"))
            .body("data.comments[0].author.nickname", equalTo("댓글작성자"))
            .body("data.comments[0].author.role.name", equalTo("덕담왕"))
            .body("data.comments[0].author.role.description", equalTo("덕담을 많이 남긴 사람"))
            .body("data.comments[0].replies[0].author.memberId", equalTo("replyauthor"))
            .body("data.comments[0].replies[0].author.role", equalTo(null))
            .body("data.comments[0].replyCount", equalTo(1))
    }

    @Test
    @DisplayName("댓글 목록 조회 - 작성자가 없는 댓글도 조회된다")
    fun `getComments - 작성자가 삭제된 댓글도 목록에 포함된다`() {
        val dailyMessage = dailyMessageJpaRepository.save(
            DailyMessageJpaEntity(title = "오늘의 덕담", content = "좋은 하루!", date = LocalDate.now())
        )
        val author = memberJpaRepository.save(
            MemberJpaEntity(
                memberId = "aliveauthor",
                socialId = "aliveauthor",
                provider = SocialProvider.GOOGLE,
                email = "aliveauthor@example.com",
                nickname = "살아있는회원",
                role = null,
                profileImage = ""
            )
        )
        val aliveComment = dailyMessageCommentJpaRepository.save(
            DailyMessageCommentJpaEntity(content = "살아있는 댓글", dailyMessage = dailyMessage, member = author)
        )
        dailyMessageCommentJpaRepository.save(
            DailyMessageCommentJpaEntity(
                content = "탈퇴자 답글",
                dailyMessage = dailyMessage,
                member = null,
                parentComment = aliveComment
            )
        )
        val withdrawnComment = dailyMessageCommentJpaRepository.save(
            DailyMessageCommentJpaEntity(content = "탈퇴자 댓글", dailyMessage = dailyMessage, member = null)
        )

        RestAssured.given()
            .`when`()
            .get("/api/v1/daily-messages/{dailyMessageId}/comments", dailyMessage.id)
            .then()
            .statusCode(200)
            .body("data.comments.size()", equalTo(2))
            .body("data.totalElements", equalTo(2))
            .body("data.comments.find { it.id == ${withdrawnComment.id} }.author.memberId", equalTo(null))
            .body("data.comments.find { it.id == ${withdrawnComment.id} }.author.nickname", equalTo("탈퇴한 회원"))
            .body("data.comments.find { it.id == ${withdrawnComment.id} }.author.profileImage", equalTo(null))
            .body("data.comments.find { it.id == ${withdrawnComment.id} }.author.role", equalTo(null))
            .body("data.comments.find { it.id == ${aliveComment.id} }.replyCount", equalTo(1))
            .body(
                "data.comments.find { it.id == ${aliveComment.id} }.replies[0].author.nickname",
                equalTo("탈퇴한 회원")
            )
            .body("data.comments.find { it.id == ${aliveComment.id} }.replies[0].author.memberId", equalTo(null))
    }

    @Test
    @DisplayName("댓글 목록 조회 - 답글은 오래된 순으로 정렬된다")
    fun `getComments - 답글은 오래된 순으로 반환된다`() {
        val author = memberJpaRepository.save(
            MemberJpaEntity(
                memberId = "orderauthor",
                socialId = "orderauthor",
                provider = SocialProvider.GOOGLE,
                email = "orderauthor@example.com",
                nickname = "정렬회원",
                role = null,
                profileImage = ""
            )
        )
        val dailyMessage = dailyMessageJpaRepository.save(
            DailyMessageJpaEntity(title = "오늘의 덕담", content = "좋은 하루!", date = LocalDate.now())
        )
        val comment = dailyMessageCommentJpaRepository.save(
            DailyMessageCommentJpaEntity(content = "댓글", dailyMessage = dailyMessage, member = author)
        )
        val first = dailyMessageCommentJpaRepository.save(
            DailyMessageCommentJpaEntity(
                content = "첫 답글",
                dailyMessage = dailyMessage,
                member = author,
                parentComment = comment
            )
        )
        val second = dailyMessageCommentJpaRepository.save(
            DailyMessageCommentJpaEntity(
                content = "둘째 답글",
                dailyMessage = dailyMessage,
                member = author,
                parentComment = comment
            )
        )

        RestAssured.given()
            .`when`()
            .get("/api/v1/daily-messages/{dailyMessageId}/comments", dailyMessage.id)
            .then()
            .statusCode(200)
            .body("data.comments[0].replies.size()", equalTo(2))
            .body("data.comments[0].replies[0].id", equalTo(first.id!!.toInt()))
            .body("data.comments[0].replies[0].content", equalTo("첫 답글"))
            .body("data.comments[0].replies[1].id", equalTo(second.id!!.toInt()))
    }

    @Test
    @DisplayName("답글에 답글을 작성할 수 없다")
    fun `createComment - 답글에는 답글을 달 수 없다`() {
        val member = memberJpaRepository.save(
            MemberJpaEntity(
                memberId = "depthuser",
                socialId = "depthuser",
                provider = SocialProvider.GOOGLE,
                email = "depthuser@example.com",
                nickname = "깊이유저",
                role = null,
                profileImage = ""
            )
        )
        val dailyMessage = dailyMessageJpaRepository.save(
            DailyMessageJpaEntity(title = "오늘의 덕담", content = "좋은 하루!", date = LocalDate.now())
        )
        val comment = dailyMessageCommentJpaRepository.save(
            DailyMessageCommentJpaEntity(content = "댓글", dailyMessage = dailyMessage, member = member)
        )
        val reply = dailyMessageCommentJpaRepository.save(
            DailyMessageCommentJpaEntity(
                content = "답글",
                dailyMessage = dailyMessage,
                member = member,
                parentComment = comment
            )
        )
        val authHeader = TestTokenGenerator.getBearerToken(member.id)
        val request = CreateCommentRequest(content = "답글의 답글", parentCommentId = reply.id)

        RestAssured.given()
            .header("Authorization", authHeader)
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .post("/api/v1/daily-messages/{dailyMessageId}/comments", dailyMessage.id)
            .then()
            .statusCode(400)
            .body("code", equalTo(DailyMessageErrorCode.REPLY_DEPTH_EXCEEDED.code))
    }
}
