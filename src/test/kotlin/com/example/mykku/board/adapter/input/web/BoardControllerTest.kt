package com.example.mykku.board.adapter.input.web

import com.example.mykku.BaseControllerTest
import com.example.mykku.block.adapter.output.persistence.entity.MemberBlockJpaEntity
import com.example.mykku.block.adapter.output.persistence.repository.MemberBlockJpaRepository
import com.example.mykku.board.adapter.output.persistence.entity.BoardJpaEntity
import com.example.mykku.board.exception.BoardErrorCode
import com.example.mykku.feed.adapter.output.persistence.FeedCommentJpaRepository
import com.example.mykku.feed.adapter.output.persistence.FeedJpaRepository
import com.example.mykku.feed.adapter.output.persistence.entity.FeedCommentJpaEntity
import com.example.mykku.feed.adapter.output.persistence.entity.FeedJpaEntity
import com.example.mykku.like.adapter.output.persistence.LikeFeedJpaRepository
import com.example.mykku.like.adapter.output.persistence.entity.LikeFeedJpaEntity
import com.example.mykku.member.adapter.output.persistence.entity.MemberJpaEntity
import com.example.mykku.member.domain.vo.SocialProvider
import io.restassured.RestAssured
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.greaterThan
import org.hamcrest.Matchers.notNullValue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@DisplayName("BoardController 통합 테스트")
class BoardControllerTest : BaseControllerTest() {

    @Autowired
    private lateinit var feedJpaRepository: FeedJpaRepository

    @Autowired
    private lateinit var likeFeedJpaRepository: LikeFeedJpaRepository

    @Autowired
    private lateinit var feedCommentJpaRepository: FeedCommentJpaRepository

    @Autowired
    private lateinit var memberBlockJpaRepository: MemberBlockJpaRepository

    @Test
    @DisplayName("게시판 목록 조회 - 정상 케이스")
    fun `getBoards - 정상적으로 게시판 목록을 조회한다`() {
        // given
        boardJpaRepository.save(BoardJpaEntity(title = "자유게시판", logo = "logo1.png"))
        boardJpaRepository.save(BoardJpaEntity(title = "정보게시판", logo = "logo2.png"))

        // when & then
        RestAssured.given()
            .`when`()
            .get("/api/v1/boards")
            .then()
            .statusCode(200)
            .body("message", equalTo("게시판 목록을 성공적으로 조회했습니다."))
            .body("data.boards", notNullValue())
            .body("data.boards.size()", equalTo(2))
    }

    @Test
    @DisplayName("게시판 목록 조회 - 빈 목록")
    fun `getBoards - 게시판이 없으면 빈 목록을 반환한다`() {
        // when & then
        RestAssured.given()
            .`when`()
            .get("/api/v1/boards")
            .then()
            .statusCode(200)
            .body("message", equalTo("게시판 목록을 성공적으로 조회했습니다."))
            .body("data.boards.size()", equalTo(0))
    }

    @Test
    @DisplayName("보드별 피드 목록 조회 - 정상 케이스")
    fun `getFeedsByBoard - 정상적으로 보드별 피드 목록을 조회한다`() {
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
        val board = boardJpaRepository.save(
            BoardJpaEntity(
                title = "테스트 게시판",
                logo = "test_logo.png"
            )
        )
        feedJpaRepository.save(
            FeedJpaEntity(
                title = "보드 피드 1",
                content = "보드 피드 내용 1",
                member = member,
                board = board
            )
        )
        feedJpaRepository.save(
            FeedJpaEntity(
                title = "보드 피드 2",
                content = "보드 피드 내용 2",
                member = member,
                board = board
            )
        )

        RestAssured.given()
            .queryParam("page", 0)
            .queryParam("size", 10)
            .`when`()
            .get("/api/v1/boards/{boardId}/feeds", board.id)
            .then()
            .log().all()
            .statusCode(200)
            .body("message", equalTo("보드별 피드 목록을 성공적으로 조회했습니다."))
            .body("data.feeds", notNullValue())
            .body("data.currentPage", equalTo(0))
            .body("data.size", equalTo(10))
    }

    @Test
    @DisplayName("보드별 인기 피드 목록 조회 - 정상 케이스")
    fun `getPopularFeedsByBoard - 실제 좋아요 개수 순으로 인기 피드 목록을 조회한다`() {
        val author = createFeedMember(memberId = "testmemberid2", socialId = "member1", email = "member1@example.com")
        val liker = createFeedMember(memberId = "testmemberid3", socialId = "member2", email = "member2@example.com")
        val board = boardJpaRepository.save(
            BoardJpaEntity(
                title = "테스트 게시판",
                logo = "test_logo.png"
            )
        )
        val popularFeed1 = feedJpaRepository.save(
            FeedJpaEntity(
                title = "인기 피드 1",
                content = "인기 피드 내용 1",
                member = author,
                board = board
            )
        )
        val popularFeed2 = feedJpaRepository.save(
            FeedJpaEntity(
                title = "인기 피드 2",
                content = "인기 피드 내용 2",
                member = author,
                board = board
            )
        )
        likeFeedJpaRepository.save(LikeFeedJpaEntity(member = author, feed = popularFeed1))
        likeFeedJpaRepository.save(LikeFeedJpaEntity(member = liker, feed = popularFeed1))
        likeFeedJpaRepository.save(LikeFeedJpaEntity(member = author, feed = popularFeed2))

        RestAssured.given()
            .`when`()
            .get("/api/v1/boards/{boardId}/feeds/popular", board.id)
            .then()
            .log().all()
            .statusCode(200)
            .body("message", equalTo("인기 피드 목록을 성공적으로 조회했습니다."))
            .body("data.feeds.size()", equalTo(2))
            .body("data.feeds[0].rank", equalTo(1))
            .body("data.feeds[0].title", equalTo("인기 피드 1"))
            .body("data.feeds[0].content", equalTo("인기 피드 내용 1"))
            .body("data.feeds[1].rank", equalTo(2))
            .body("data.feeds[1].title", equalTo("인기 피드 2"))
            .body("data.feeds[1].content", equalTo("인기 피드 내용 2"))

        RestAssured.given()
            .queryParam("page", 0)
            .queryParam("size", 10)
            .`when`()
            .get("/api/v1/boards/{boardId}/feeds", board.id)
            .then()
            .statusCode(200)
            .body("data.feeds.find { it.title == '인기 피드 1' }.likeCount", equalTo(2))
            .body("data.feeds.find { it.title == '인기 피드 2' }.likeCount", equalTo(1))
    }

    @Test
    @DisplayName("보드별 피드 목록 조회 - 좋아요한 피드는 isLiked=true이면서 likeCount가 0보다 크다")
    fun `getFeedsByBoard - 좋아요를 누른 피드는 isLiked와 likeCount가 함께 반영된다`() {
        val author = createFeedMember(memberId = "feedauthor", socialId = "author1", email = "author1@example.com")
        val liker = createFeedMember(memberId = "feedliker", socialId = "liker1", email = "liker1@example.com")
        val board = boardJpaRepository.save(
            BoardJpaEntity(
                title = "좋아요 게시판",
                logo = "test_logo.png"
            )
        )
        val feed = feedJpaRepository.save(
            FeedJpaEntity(
                title = "좋아요 피드",
                content = "좋아요 피드 내용",
                member = author,
                board = board
            )
        )
        likeFeedJpaRepository.save(LikeFeedJpaEntity(member = author, feed = feed))
        likeFeedJpaRepository.save(LikeFeedJpaEntity(member = liker, feed = feed))

        RestAssured.given()
            .headers(createAuthHeaders(liker.id))
            .queryParam("page", 0)
            .queryParam("size", 10)
            .`when`()
            .get("/api/v1/boards/{boardId}/feeds", board.id)
            .then()
            .log().all()
            .statusCode(200)
            .body("data.feeds.size()", equalTo(1))
            .body("data.feeds[0].isLiked", equalTo(true))
            .body("data.feeds[0].likeCount", greaterThan(0))
            .body("data.feeds[0].likeCount", equalTo(2))
    }

    @Test
    @DisplayName("보드별 피드 목록 조회 - 좋아요를 취소하면 likeCount가 즉시 줄어든다")
    fun `getFeedsByBoard - 좋아요 삭제 후 likeCount가 실시간으로 반영된다`() {
        val author = createFeedMember(memberId = "cancelauthor", socialId = "cancel1", email = "cancel1@example.com")
        val liker = createFeedMember(memberId = "cancelliker", socialId = "cancel2", email = "cancel2@example.com")
        val board = boardJpaRepository.save(
            BoardJpaEntity(
                title = "좋아요 취소 게시판",
                logo = "test_logo.png"
            )
        )
        val feed = feedJpaRepository.save(
            FeedJpaEntity(
                title = "좋아요 취소 피드",
                content = "좋아요 취소 피드 내용",
                member = author,
                board = board
            )
        )
        likeFeedJpaRepository.save(LikeFeedJpaEntity(member = author, feed = feed))
        val likerLike = likeFeedJpaRepository.save(LikeFeedJpaEntity(member = liker, feed = feed))
        likeFeedJpaRepository.deleteById(likerLike.id!!)

        RestAssured.given()
            .headers(createAuthHeaders(liker.id))
            .queryParam("page", 0)
            .queryParam("size", 10)
            .`when`()
            .get("/api/v1/boards/{boardId}/feeds", board.id)
            .then()
            .statusCode(200)
            .body("data.feeds[0].likeCount", equalTo(1))
            .body("data.feeds[0].isLiked", equalTo(false))
    }

    @Test
    @DisplayName("보드별 피드 목록 조회 - commentCount는 대댓글까지 포함해 실시간 집계된다")
    fun `getFeedsByBoard - commentCount가 대댓글을 포함해 실시간으로 반영된다`() {
        val author = createFeedMember(memberId = "cmtauthor", socialId = "cmt1", email = "cmt1@example.com")
        val board = boardJpaRepository.save(
            BoardJpaEntity(
                title = "댓글 수 게시판",
                logo = "test_logo.png"
            )
        )
        val feed = feedJpaRepository.save(
            FeedJpaEntity(
                title = "댓글 수 피드",
                content = "댓글 수 피드 내용",
                member = author,
                board = board
            )
        )
        val parentComment = feedCommentJpaRepository.save(
            FeedCommentJpaEntity(content = "부모 댓글", feed = feed, member = author)
        )
        feedCommentJpaRepository.save(
            FeedCommentJpaEntity(content = "대댓글 1", feed = feed, parentComment = parentComment, member = author)
        )
        feedCommentJpaRepository.save(
            FeedCommentJpaEntity(content = "대댓글 2", feed = feed, parentComment = parentComment, member = author)
        )

        RestAssured.given()
            .queryParam("page", 0)
            .queryParam("size", 10)
            .`when`()
            .get("/api/v1/boards/{boardId}/feeds", board.id)
            .then()
            .statusCode(200)
            .body("data.feeds.size()", equalTo(1))
            .body("data.feeds[0].commentCount", equalTo(3))
            .body("data.feeds[0].comment.content", equalTo("부모 댓글"))
    }

    @Test
    @DisplayName("보드별 피드 목록 조회 - 첫 댓글 작성자가 탈퇴해도 미리보기 내용이 나간다")
    fun `getFeedsByBoard - 탈퇴 회원의 첫 댓글도 미리보기에 포함된다`() {
        val author = createFeedMember(memberId = "prevauthor", socialId = "prev1", email = "prev1@example.com")
        val board = boardJpaRepository.save(BoardJpaEntity(title = "미리보기 게시판", logo = "test_logo.png"))
        val feed = feedJpaRepository.save(
            FeedJpaEntity(title = "미리보기 피드", content = "미리보기 피드 내용", member = author, board = board)
        )
        feedCommentJpaRepository.save(FeedCommentJpaEntity(content = "탈퇴자 첫 댓글", feed = feed, member = null))
        feedCommentJpaRepository.save(FeedCommentJpaEntity(content = "두번째 댓글", feed = feed, member = author))

        RestAssured.given()
            .`when`()
            .get("/api/v1/boards/{boardId}/feeds", board.id)
            .then()
            .statusCode(200)
            .body("data.feeds[0].commentCount", equalTo(2))
            .body("data.feeds[0].comment.content", equalTo("탈퇴자 첫 댓글"))
            .body("data.feeds[0].comment.profileImage", equalTo(null))
    }

    @Test
    @DisplayName("보드별 피드 목록 조회 - 내가 차단한 회원의 첫 댓글은 미리보기에서 건너뛴다")
    fun `getFeedsByBoard - 내가 차단한 회원의 첫 댓글은 미리보기에서 건너뛴다`() {
        val viewer = createFeedMember(memberId = "pvviewer", socialId = "pvviewer", email = "pvviewer@example.com")
        val author = createFeedMember(memberId = "pvauthor", socialId = "pvauthor", email = "pvauthor@example.com")
        val blocked = createFeedMember(memberId = "pvblocked", socialId = "pvblocked", email = "pvblocked@example.com")
        val board = boardJpaRepository.save(BoardJpaEntity(title = "미리보기 게시판", logo = "test_logo.png"))
        val feed = feedJpaRepository.save(
            FeedJpaEntity(title = "미리보기 피드", content = "미리보기 피드 내용", member = author, board = board)
        )
        feedCommentJpaRepository.save(FeedCommentJpaEntity(content = "차단 회원 첫 댓글", feed = feed, member = blocked))
        feedCommentJpaRepository.save(FeedCommentJpaEntity(content = "두번째 댓글", feed = feed, member = author))
        memberBlockJpaRepository.save(MemberBlockJpaEntity(blocker = viewer, blocked = blocked))

        RestAssured.given()
            .headers(createAuthHeaders(viewer.id))
            .`when`()
            .get("/api/v1/boards/{boardId}/feeds", board.id)
            .then()
            .statusCode(200)
            .body("data.feeds.size()", equalTo(1))
            .body("data.feeds[0].comment.content", equalTo("두번째 댓글"))
    }

    @Test
    @DisplayName("보드별 피드 목록 조회 - 나를 차단한 회원의 댓글만 있으면 미리보기가 비어 있다")
    fun `getFeedsByBoard - 나를 차단한 회원의 댓글만 있으면 미리보기가 비어 있다`() {
        val viewer = createFeedMember(memberId = "pvblockedme", socialId = "pvblockedme", email = "pvme@example.com")
        val author = createFeedMember(memberId = "pvauthor2", socialId = "pvauthor2", email = "pvauthor2@example.com")
        val blocker = createFeedMember(memberId = "pvblocker", socialId = "pvblocker", email = "pvblocker@example.com")
        val board = boardJpaRepository.save(BoardJpaEntity(title = "미리보기 게시판", logo = "test_logo.png"))
        val feed = feedJpaRepository.save(
            FeedJpaEntity(title = "미리보기 피드", content = "미리보기 피드 내용", member = author, board = board)
        )
        feedCommentJpaRepository.save(FeedCommentJpaEntity(content = "차단자 댓글", feed = feed, member = blocker))
        memberBlockJpaRepository.save(MemberBlockJpaEntity(blocker = blocker, blocked = viewer))

        RestAssured.given()
            .headers(createAuthHeaders(viewer.id))
            .`when`()
            .get("/api/v1/boards/{boardId}/feeds", board.id)
            .then()
            .statusCode(200)
            .body("data.feeds.size()", equalTo(1))
            .body("data.feeds[0].comment.content", equalTo(""))
            .body("data.feeds[0].comment.profileImage", equalTo(null))
    }

    @Test
    @DisplayName("보드별 피드 목록 조회 - 댓글 수에서 차단 관계 회원의 댓글·답글과 그 댓글의 답글은 빠진다")
    fun `getFeedsByBoard - 댓글 수에서 차단 관계 회원의 댓글과 그 답글은 빠진다`() {
        val viewer = createFeedMember(memberId = "cntviewer", socialId = "cntviewer", email = "cntviewer@example.com")
        val blocked = createFeedMember(memberId = "cntblocked", socialId = "cntblocked", email = "cntb@example.com")
        val board = boardJpaRepository.save(BoardJpaEntity(title = "댓글 수 게시판", logo = "test_logo.png"))
        val feed = feedJpaRepository.save(
            FeedJpaEntity(title = "댓글 수 피드", content = "댓글 수 피드 내용", member = viewer, board = board)
        )
        val blockedTop = saveComment(feed, blocked, null)
        saveComment(feed, viewer, blockedTop)
        val viewerTop = saveComment(feed, viewer, null)
        saveComment(feed, blocked, viewerTop)
        val withdrawnTop = saveComment(feed, null, null)
        saveComment(feed, viewer, withdrawnTop)
        memberBlockJpaRepository.save(MemberBlockJpaEntity(blocker = blocked, blocked = viewer))

        RestAssured.given()
            .headers(createAuthHeaders(viewer.id))
            .`when`()
            .get("/api/v1/boards/{boardId}/feeds", board.id)
            .then()
            .statusCode(200)
            .body("data.feeds.size()", equalTo(1))
            .body("data.feeds[0].commentCount", equalTo(3))
    }

    private fun saveComment(
        feed: FeedJpaEntity,
        member: MemberJpaEntity?,
        parent: FeedCommentJpaEntity?
    ): FeedCommentJpaEntity =
        feedCommentJpaRepository.save(
            FeedCommentJpaEntity(content = "댓글", feed = feed, member = member, parentComment = parent)
        )

    @Test
    @DisplayName("보드별 인기 피드 목록 조회 - 빈 목록")
    fun `getPopularFeedsByBoard - 인기 피드가 없으면 빈 목록을 반환한다`() {
        val board = boardJpaRepository.save(
            BoardJpaEntity(
                title = "테스트 게시판",
                logo = "test_logo.png"
            )
        )

        RestAssured.given()
            .`when`()
            .get("/api/v1/boards/{boardId}/feeds/popular", board.id)
            .then()
            .statusCode(200)
            .body("message", equalTo("인기 피드 목록을 성공적으로 조회했습니다."))
            .body("data.feeds.size()", equalTo(0))
    }

    private fun createFeedMember(
        memberId: String,
        socialId: String,
        email: String,
        nickname: String = "테스터"
    ): MemberJpaEntity {
        return memberJpaRepository.save(
            MemberJpaEntity(
                memberId = memberId,
                socialId = socialId,
                provider = SocialProvider.GOOGLE,
                email = email,
                nickname = nickname,
                role = null,
                profileImage = ""
            )
        )
    }

    @Test
    @DisplayName("보드별 피드 목록 조회 - 존재하지 않는 게시판이면 404")
    fun `getFeedsByBoard - 존재하지 않는 게시판이면 404를 반환한다`() {
        RestAssured.given()
            .`when`()
            .get("/api/v1/boards/{boardId}/feeds", 999999L)
            .then()
            .statusCode(404)
            .body("code", equalTo(BoardErrorCode.BOARD_NOT_FOUND.code))
    }

    @Test
    @DisplayName("보드별 인기 피드 목록 조회 - 존재하지 않는 게시판이면 404")
    fun `getPopularFeedsByBoard - 존재하지 않는 게시판이면 404를 반환한다`() {
        RestAssured.given()
            .`when`()
            .get("/api/v1/boards/{boardId}/feeds/popular", 999999L)
            .then()
            .statusCode(404)
            .body("code", equalTo(BoardErrorCode.BOARD_NOT_FOUND.code))
    }
}
