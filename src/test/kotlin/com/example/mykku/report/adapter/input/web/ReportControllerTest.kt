package com.example.mykku.report.adapter.input.web

import com.example.mykku.BaseControllerTest
import com.example.mykku.board.adapter.output.persistence.entity.BoardJpaEntity
import com.example.mykku.dailymessage.adapter.output.persistence.entity.DailyMessageCommentJpaEntity
import com.example.mykku.dailymessage.adapter.output.persistence.entity.DailyMessageJpaEntity
import com.example.mykku.dailymessage.adapter.output.persistence.repository.DailyMessageCommentJpaRepository
import com.example.mykku.dailymessage.adapter.output.persistence.repository.DailyMessageJpaRepository
import com.example.mykku.feed.adapter.output.persistence.FeedCommentJpaRepository
import com.example.mykku.feed.adapter.output.persistence.FeedJpaRepository
import com.example.mykku.feed.adapter.output.persistence.entity.FeedCommentJpaEntity
import com.example.mykku.feed.adapter.output.persistence.entity.FeedJpaEntity
import com.example.mykku.member.adapter.output.persistence.entity.MemberJpaEntity
import com.example.mykku.report.domain.vo.ReportReason
import com.example.mykku.report.domain.vo.ReportTargetType
import com.example.mykku.report.exception.ReportErrorCode
import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasItems
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

@DisplayName("ReportController 통합 테스트")
class ReportControllerTest : BaseControllerTest() {

    @Autowired
    private lateinit var feedJpaRepository: FeedJpaRepository

    @Autowired
    private lateinit var feedCommentJpaRepository: FeedCommentJpaRepository

    @Autowired
    private lateinit var dailyMessageJpaRepository: DailyMessageJpaRepository

    @Autowired
    private lateinit var dailyMessageCommentJpaRepository: DailyMessageCommentJpaRepository

    private fun createReporter(): MemberJpaEntity = createAndSaveMember(
        memberId = "reporter",
        nickname = "신고자",
        email = "reporter@example.com",
        socialId = "reporter1"
    )

    private fun createAuthor(): MemberJpaEntity = createAndSaveMember(
        memberId = "author",
        nickname = "작성자",
        email = "author@example.com",
        socialId = "author1"
    )

    private fun createFeed(author: MemberJpaEntity, board: BoardJpaEntity = createAndSaveBoard()): FeedJpaEntity {
        return feedJpaRepository.save(
            FeedJpaEntity(
                title = "신고 대상 게시글",
                content = "신고 대상 내용",
                board = board,
                member = author
            )
        )
    }

    private fun createComment(feed: FeedJpaEntity, author: MemberJpaEntity): FeedCommentJpaEntity {
        return feedCommentJpaRepository.save(
            FeedCommentJpaEntity(
                content = "신고 대상 댓글",
                feed = feed,
                member = author
            )
        )
    }

    private fun createDailyMessageComment(author: MemberJpaEntity): DailyMessageCommentJpaEntity {
        val dailyMessage = dailyMessageJpaRepository.save(
            DailyMessageJpaEntity(title = "오늘의 덕담", content = "오늘도 좋은 하루!", date = LocalDate.now())
        )
        return dailyMessageCommentJpaRepository.save(
            DailyMessageCommentJpaEntity(content = "신고 대상 덕담 댓글", dailyMessage = dailyMessage, member = author)
        )
    }

    @Test
    @DisplayName("게시글 신고 - 정상 케이스")
    fun `createReport - 게시글을 신고한다`() {
        val reporter = createReporter()
        val author = createAuthor()
        val feed = createFeed(author)
        val request = CreateReportRequest(
            targetType = ReportTargetType.FEED,
            targetId = feed.id!!,
            reason = ReportReason.SPAM,
            detail = "광고 도배입니다"
        )

        RestAssured.given()
            .header("Authorization", getBearerToken(reporter.id))
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .post("/api/v1/reports")
            .then()
            .statusCode(201)
            .body("message", equalTo("신고가 접수되었습니다"))
            .body("data.targetType", equalTo("FEED"))
            .body("data.targetId", equalTo(feed.id!!.toInt()))
            .body("data.reason", equalTo("SPAM"))
            .body("data.reasonDescription", equalTo(ReportReason.SPAM.description))
            .body("data.detail", equalTo("광고 도배입니다"))
            .body("data.status", equalTo("PENDING"))
            .body("data.reporterMemberId", equalTo(reporter.memberId))
            .body("data.targetMemberId", equalTo(author.memberId))
    }

    @Test
    @DisplayName("댓글 신고 - 정상 케이스")
    fun `createReport - 댓글을 신고한다`() {
        val reporter = createReporter()
        val author = createAuthor()
        val comment = createComment(createFeed(author), author)
        val request = CreateReportRequest(
            targetType = ReportTargetType.FEED_COMMENT,
            targetId = comment.id!!,
            reason = ReportReason.ABUSE,
            detail = null
        )

        RestAssured.given()
            .header("Authorization", getBearerToken(reporter.id))
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .post("/api/v1/reports")
            .then()
            .statusCode(201)
            .body("data.targetType", equalTo("FEED_COMMENT"))
            .body("data.targetId", equalTo(comment.id!!.toInt()))
            .body("data.reason", equalTo("ABUSE"))
            .body("data.status", equalTo("PENDING"))
    }

    @Test
    @DisplayName("하루덕담 댓글 신고 - 정상 케이스")
    fun `createReport - 하루덕담 댓글을 신고한다`() {
        val reporter = createReporter()
        val author = createAuthor()
        val comment = createDailyMessageComment(author)
        val request = CreateReportRequest(
            targetType = ReportTargetType.DAILY_MESSAGE_COMMENT,
            targetId = comment.id!!,
            reason = ReportReason.ABUSE,
            detail = null
        )

        RestAssured.given()
            .header("Authorization", getBearerToken(reporter.id))
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .post("/api/v1/reports")
            .then()
            .statusCode(201)
            .body("data.targetType", equalTo("DAILY_MESSAGE_COMMENT"))
            .body("data.targetTypeDescription", equalTo("하루 덕담 댓글"))
            .body("data.targetId", equalTo(comment.id!!.toInt()))
            .body("data.targetMemberId", equalTo(author.memberId))
            .body("data.status", equalTo("PENDING"))
    }

    @Test
    @DisplayName("하루덕담 답글 신고 - 정상 케이스")
    fun `createReport - 하루덕담 답글을 신고한다`() {
        val reporter = createReporter()
        val author = createAuthor()
        val parent = createDailyMessageComment(author)
        val reply = dailyMessageCommentJpaRepository.save(
            DailyMessageCommentJpaEntity(
                content = "신고 대상 덕담 답글",
                dailyMessage = parent.dailyMessage,
                member = author,
                parentComment = parent
            )
        )
        val request = CreateReportRequest(
            targetType = ReportTargetType.DAILY_MESSAGE_COMMENT,
            targetId = reply.id!!,
            reason = ReportReason.SPAM,
            detail = null
        )

        RestAssured.given()
            .header("Authorization", getBearerToken(reporter.id))
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .post("/api/v1/reports")
            .then()
            .statusCode(201)
            .body("data.targetType", equalTo("DAILY_MESSAGE_COMMENT"))
            .body("data.targetId", equalTo(reply.id!!.toInt()))
            .body("data.targetMemberId", equalTo(author.memberId))
    }

    @Test
    @DisplayName("하루덕담 댓글 신고 - 없는 댓글이면 RP002")
    fun `createReport - 없는 하루덕담 댓글은 실패한다`() {
        val reporter = createReporter()
        val request = CreateReportRequest(
            targetType = ReportTargetType.DAILY_MESSAGE_COMMENT,
            targetId = 999999L,
            reason = ReportReason.ABUSE,
            detail = null
        )

        RestAssured.given()
            .header("Authorization", getBearerToken(reporter.id))
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .post("/api/v1/reports")
            .then()
            .statusCode(404)
            .body("code", equalTo(ReportErrorCode.REPORT_TARGET_NOT_FOUND.code))
    }

    @Test
    @DisplayName("하루덕담 댓글 신고 - 자신의 댓글이면 RP101")
    fun `createReport - 자신의 하루덕담 댓글은 신고할 수 없다`() {
        val author = createAuthor()
        val comment = createDailyMessageComment(author)
        val request = CreateReportRequest(
            targetType = ReportTargetType.DAILY_MESSAGE_COMMENT,
            targetId = comment.id!!,
            reason = ReportReason.ABUSE,
            detail = null
        )

        RestAssured.given()
            .header("Authorization", getBearerToken(author.id))
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .post("/api/v1/reports")
            .then()
            .statusCode(400)
            .body("code", equalTo(ReportErrorCode.CANNOT_REPORT_OWN_CONTENT.code))
    }

    @Test
    @DisplayName("게시글 신고 - 같은 대상을 두 번 신고할 수 없다")
    fun `createReport - 중복 신고는 실패한다`() {
        val reporter = createReporter()
        val feed = createFeed(createAuthor())
        val request = CreateReportRequest(
            targetType = ReportTargetType.FEED,
            targetId = feed.id!!,
            reason = ReportReason.SPAM,
            detail = null
        )

        RestAssured.given()
            .header("Authorization", getBearerToken(reporter.id))
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .post("/api/v1/reports")
            .then()
            .statusCode(201)

        RestAssured.given()
            .header("Authorization", getBearerToken(reporter.id))
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .post("/api/v1/reports")
            .then()
            .statusCode(409)
            .body("code", equalTo(ReportErrorCode.ALREADY_REPORTED.code))
    }

    @Test
    @DisplayName("게시글 신고 - 자신의 콘텐츠는 신고할 수 없다")
    fun `createReport - 자신의 게시글은 신고할 수 없다`() {
        val reporter = createReporter()
        val feed = createFeed(reporter)
        val request = CreateReportRequest(
            targetType = ReportTargetType.FEED,
            targetId = feed.id!!,
            reason = ReportReason.SPAM,
            detail = null
        )

        RestAssured.given()
            .header("Authorization", getBearerToken(reporter.id))
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .post("/api/v1/reports")
            .then()
            .statusCode(400)
            .body("code", equalTo(ReportErrorCode.CANNOT_REPORT_OWN_CONTENT.code))
    }

    @Test
    @DisplayName("게시글 신고 - 존재하지 않는 대상은 신고할 수 없다")
    fun `createReport - 존재하지 않는 대상은 실패한다`() {
        val reporter = createReporter()
        val request = CreateReportRequest(
            targetType = ReportTargetType.FEED,
            targetId = 999999L,
            reason = ReportReason.SPAM,
            detail = null
        )

        RestAssured.given()
            .header("Authorization", getBearerToken(reporter.id))
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .post("/api/v1/reports")
            .then()
            .statusCode(404)
            .body("code", equalTo(ReportErrorCode.REPORT_TARGET_NOT_FOUND.code))
    }

    @Test
    @DisplayName("게시글 신고 - 기타 사유는 상세 내용이 필수다")
    fun `createReport - 기타 사유에 상세 내용이 없으면 실패한다`() {
        val reporter = createReporter()
        val feed = createFeed(createAuthor())
        val request = CreateReportRequest(
            targetType = ReportTargetType.FEED,
            targetId = feed.id!!,
            reason = ReportReason.ETC,
            detail = null
        )

        RestAssured.given()
            .header("Authorization", getBearerToken(reporter.id))
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .post("/api/v1/reports")
            .then()
            .statusCode(400)
            .body("code", equalTo(ReportErrorCode.REPORT_DETAIL_REQUIRED.code))
    }

    @Test
    @DisplayName("게시글 신고 - 인증되지 않은 사용자")
    fun `createReport - 인증되지 않은 사용자는 신고할 수 없다`() {
        val request = CreateReportRequest(
            targetType = ReportTargetType.FEED,
            targetId = 1L,
            reason = ReportReason.SPAM,
            detail = null
        )

        RestAssured.given()
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .post("/api/v1/reports")
            .then()
            .statusCode(401)
    }

    @Test
    @DisplayName("게시글 신고 - 잘못된 targetType은 실패한다")
    fun `createReport - 잘못된 targetType은 실패한다`() {
        val reporter = createReporter()

        RestAssured.given()
            .header("Authorization", getBearerToken(reporter.id))
            .contentType(ContentType.JSON)
            .body("""{"targetType":"UNKNOWN","targetId":1,"reason":"SPAM"}""")
            .`when`()
            .post("/api/v1/reports")
            .then()
            .statusCode(400)
    }

    @Test
    @DisplayName("신고 사유 목록 조회 - 인증 없이 조회할 수 있다")
    fun `getReportReasons - 신고 사유 목록을 조회한다`() {
        RestAssured.given()
            .`when`()
            .get("/api/v1/reports/reasons")
            .then()
            .statusCode(200)
            .body("message", equalTo("신고 사유 목록 조회 성공"))
            .body("data", hasSize<Any>(ReportReason.entries.size))
            .body("data.reason", equalTo(ReportReason.entries.map { it.name }))
            .body("data[-1].reason", equalTo(ReportReason.ETC.name))
            .body("data.description", hasItems(ReportReason.ETC.description))
    }
}
