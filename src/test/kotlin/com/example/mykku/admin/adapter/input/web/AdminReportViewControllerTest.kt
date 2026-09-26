package com.example.mykku.admin.adapter.input.web

import com.example.mykku.BaseControllerTest
import com.example.mykku.contest.adapter.output.persistence.entity.ContestJpaEntity
import com.example.mykku.contest.adapter.output.persistence.entity.ContestParticipationJpaEntity
import com.example.mykku.contest.adapter.output.persistence.repository.ContestJpaRepository
import com.example.mykku.contest.adapter.output.persistence.repository.ContestParticipationJpaRepository
import com.example.mykku.contest.domain.vo.ContestStatusType
import com.example.mykku.dailymessage.adapter.output.persistence.entity.DailyMessageCommentJpaEntity
import com.example.mykku.dailymessage.adapter.output.persistence.entity.DailyMessageJpaEntity
import com.example.mykku.dailymessage.adapter.output.persistence.repository.DailyMessageCommentJpaRepository
import com.example.mykku.dailymessage.adapter.output.persistence.repository.DailyMessageJpaRepository
import com.example.mykku.feed.adapter.output.persistence.FeedCommentJpaRepository
import com.example.mykku.feed.adapter.output.persistence.FeedJpaRepository
import com.example.mykku.feed.adapter.output.persistence.entity.FeedCommentJpaEntity
import com.example.mykku.feed.adapter.output.persistence.entity.FeedJpaEntity
import com.example.mykku.member.adapter.output.persistence.entity.MemberJpaEntity
import com.example.mykku.report.adapter.output.persistence.entity.ReportJpaEntity
import com.example.mykku.report.adapter.output.persistence.repository.ReportJpaRepository
import com.example.mykku.report.domain.vo.ReportReason
import com.example.mykku.report.domain.vo.ReportStatus
import com.example.mykku.report.domain.vo.ReportTargetType
import com.example.mykku.report.exception.ReportErrorCode
import io.restassured.RestAssured
import io.restassured.response.ValidatableResponse
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.endsWith
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("AdminReportViewController 통합 테스트")
class AdminReportViewControllerTest : BaseControllerTest() {

    @Autowired
    private lateinit var reportJpaRepository: ReportJpaRepository

    @Autowired
    private lateinit var feedJpaRepository: FeedJpaRepository

    @Autowired
    private lateinit var feedCommentJpaRepository: FeedCommentJpaRepository

    @Autowired
    private lateinit var contestJpaRepository: ContestJpaRepository

    @Autowired
    private lateinit var contestParticipationJpaRepository: ContestParticipationJpaRepository

    @Autowired
    private lateinit var dailyMessageJpaRepository: DailyMessageJpaRepository

    @Autowired
    private lateinit var dailyMessageCommentJpaRepository: DailyMessageCommentJpaRepository

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Test
    @DisplayName("신고 목록은 신고자·피신고자 memberId와 닉네임을 보여 준다")
    fun `listPage - 닉네임 포함`() {
        val (reporter, author) = createMembers()
        val feed = saveFeed(author, "신고된 피드")
        val report = saveReport(reporter, author, ReportTargetType.FEED, feed.id!!)

        val body = getPage("/admin/report")
            .statusCode(200)
            .body(containsString("class=\"nav-link active\" href=\"/admin/report\""))
            .extract().asString()

        assertThat(rowOf(body, report.id!!))
            .contains("reporter1", "신고자", "author1", "작성자", ReportReason.SPAM.description, "접수")
    }

    @Test
    @DisplayName("상태 탭은 해당 상태의 신고만 보여 준다")
    fun `listPage - 상태 탭`() {
        val (reporter, author) = createMembers()
        val feed = saveFeed(author, "피드")
        val other = createAndSaveMember(memberId = "other1", nickname = "다른신고자", email = "o@e.com", socialId = "o1")
        val pending = saveReport(reporter, author, ReportTargetType.FEED, feed.id!!)
        val resolved = saveReport(other, author, ReportTargetType.FEED, feed.id!!, status = ReportStatus.RESOLVED)

        getPage("/admin/report?status=RESOLVED")
            .statusCode(200)
            .body(containsString("data-report-id=\"${resolved.id}\""))
            .body(not(containsString("data-report-id=\"${pending.id}\"")))
            .body(containsString("class=\"nav-link active\" href=\"/admin/report?status=RESOLVED\""))
    }

    @Test
    @DisplayName("탈퇴한 신고자와 피신고자는 '탈퇴 회원'으로 보인다")
    fun `listPage - 탈퇴 회원`() {
        val report = reportJpaRepository.save(
            ReportJpaEntity(
                reporterId = null,
                targetType = ReportTargetType.FEED,
                targetId = 999999L,
                targetMemberId = null,
                reason = ReportReason.SPAM,
                status = ReportStatus.PENDING
            )
        )

        val body = getPage("/admin/report").statusCode(200).extract().asString()

        assertThat(rowOf(body, report.id!!).split("탈퇴 회원").size - 1).isEqualTo(2)
    }

    @Test
    @DisplayName("피드 신고 상세는 대상 내용·콘테스트 수상·같은 대상 신고·누적 수를 보여 준다")
    fun `detailPage - 피드 대상`() {
        val (reporter, author) = createMembers()
        val other = createAndSaveMember(memberId = "other1", nickname = "다른신고자", email = "o@e.com", socialId = "o1")
        val feed = saveFeed(author, "수상 피드")
        saveWinner(feed, author)
        val report = saveReport(reporter, author, ReportTargetType.FEED, feed.id!!)
        val sibling = saveReport(other, author, ReportTargetType.FEED, feed.id!!)

        getPage("/admin/report/${report.id}")
            .statusCode(200)
            .body(containsString("id=\"target-feed-title\">수상 피드<"))
            .body(containsString("1위 수상"))
            .body(containsString("data-winner-rank=\"1\""))
            .body(containsString("href=\"/admin/report/${sibling.id}\""))
            .body(containsString("(누적 신고 2건)"))
            .body(containsString("data-report-ids=\"${report.id},${sibling.id}\""))
            .body(not(containsString("id=\"target-deleted\"")))
    }

    @Test
    @DisplayName("댓글 신고 상세는 댓글 본문과 상위 피드 제목을 보여 준다")
    fun `detailPage - 댓글 대상`() {
        val (reporter, author) = createMembers()
        val feed = saveFeed(author, "상위 피드")
        val comment = feedCommentJpaRepository.save(
            FeedCommentJpaEntity(content = "나쁜 댓글", feed = feed, member = author)
        )
        val report = saveReport(reporter, author, ReportTargetType.FEED_COMMENT, comment.id!!)

        getPage("/admin/report/${report.id}")
            .statusCode(200)
            .body(containsString("id=\"target-comment-content\">나쁜 댓글<"))
            .body(containsString("상위 피드: 상위 피드"))
            .body(containsString("data-target-type=\"FEED_COMMENT\""))
    }

    @Test
    @DisplayName("하루덕담 댓글 신고 상세는 댓글 본문과 상위 하루 덕담 제목을 보여 준다")
    fun `detailPage - 하루덕담 댓글 대상`() {
        val (reporter, author) = createMembers()
        val comment = saveDailyMessageComment(author, "나쁜 덕담 댓글")
        val report = saveReport(reporter, author, ReportTargetType.DAILY_MESSAGE_COMMENT, comment.id!!)

        getPage("/admin/report/${report.id}")
            .statusCode(200)
            .body(containsString("id=\"target-daily-message-comment-content\">나쁜 덕담 댓글<"))
            .body(containsString("상위 하루 덕담: 오늘의 덕담"))
            .body(containsString("data-target-type=\"DAILY_MESSAGE_COMMENT\""))
            .body(not(containsString("id=\"target-deleted\"")))
    }

    @Test
    @DisplayName("대상이 삭제된 신고 상세는 200과 '삭제됨'을 보여 준다")
    fun `detailPage - 삭제된 대상`() {
        val (reporter, author) = createMembers()
        val report = saveReport(reporter, author, ReportTargetType.FEED, 999999L)

        getPage("/admin/report/${report.id}")
            .statusCode(200)
            .body(containsString("id=\"target-deleted\""))
            .body(not(containsString("id=\"delete-content\"")))
    }

    @Test
    @DisplayName("신고 사유와 대상 피드·댓글에 들어간 스크립트는 이스케이프한다")
    fun `detailPage - XSS 이스케이프`() {
        val (reporter, author) = createMembers()
        val feed = saveFeed(author, "<script>alert(1)</script>", content = "<script>alert(2)</script>")
        val comment = feedCommentJpaRepository.save(
            FeedCommentJpaEntity(content = "<script>alert(3)</script>", feed = feed, member = author)
        )
        val feedReport = saveReport(
            reporter,
            author,
            ReportTargetType.FEED,
            feed.id!!,
            detail = "<script>alert(4)</script>"
        )
        val commentReport = saveReport(reporter, author, ReportTargetType.FEED_COMMENT, comment.id!!)
        val dailyMessageComment = saveDailyMessageComment(author, "<script>alert(5)</script>")
        val dailyMessageCommentReport =
            saveReport(reporter, author, ReportTargetType.DAILY_MESSAGE_COMMENT, dailyMessageComment.id!!)

        val feedBody = getPage("/admin/report/${feedReport.id}").statusCode(200).extract().asString()
        val commentBody = getPage("/admin/report/${commentReport.id}").statusCode(200).extract().asString()
        val dailyMessageBody =
            getPage("/admin/report/${dailyMessageCommentReport.id}").statusCode(200).extract().asString()

        assertThat(feedBody).contains("&lt;script&gt;alert(1)", "&lt;script&gt;alert(2)", "&lt;script&gt;alert(4)")
        assertThat(commentBody).contains("&lt;script&gt;alert(3)")
        assertThat(dailyMessageBody).contains("&lt;script&gt;alert(5)")
        assertThat(feedBody + commentBody + dailyMessageBody).doesNotContain("<script>alert(")
    }

    @Test
    @DisplayName("없는 신고 상세는 RP001")
    fun `detailPage - 없는 신고`() {
        getPage("/admin/report/999999")
            .statusCode(404)
            .body("code", equalTo(ReportErrorCode.REPORT_NOT_FOUND.code))
    }

    @Test
    @DisplayName("세션 없이 신고 화면에 접근하면 로그인으로 리다이렉트된다")
    fun `listPage - 미인증이면 302`() {
        RestAssured.given()
            .redirects().follow(false)
            .`when`()
            .get("/admin/report")
            .then()
            .statusCode(302)
            .header("Location", endsWith("/admin/login"))
    }

    private fun getPage(path: String): ValidatableResponse {
        return RestAssured.given()
            .sessionId(getAdminSessionId())
            .urlEncodingEnabled(false)
            .`when`()
            .get(path)
            .then()
    }

    private fun createMembers(): Pair<MemberJpaEntity, MemberJpaEntity> {
        return Pair(
            createAndSaveMember(memberId = "reporter1", nickname = "신고자", email = "r@e.com", socialId = "r1"),
            createAndSaveMember(memberId = "author1", nickname = "작성자", email = "a@e.com", socialId = "a1")
        )
    }

    private fun saveFeed(author: MemberJpaEntity, title: String, content: String = "본문"): FeedJpaEntity {
        return feedJpaRepository.save(
            FeedJpaEntity(title = title, content = content, member = author, board = createAndSaveBoard())
        )
    }

    private fun saveDailyMessageComment(author: MemberJpaEntity, content: String): DailyMessageCommentJpaEntity {
        val dailyMessage = dailyMessageJpaRepository.save(
            DailyMessageJpaEntity(title = "오늘의 덕담", content = "오늘도 좋은 하루!", date = LocalDate.now())
        )
        return dailyMessageCommentJpaRepository.save(
            DailyMessageCommentJpaEntity(content = content, dailyMessage = dailyMessage, member = author)
        )
    }

    private fun saveReport(
        reporter: MemberJpaEntity,
        author: MemberJpaEntity,
        targetType: ReportTargetType,
        targetId: Long,
        status: ReportStatus = ReportStatus.PENDING,
        detail: String? = null
    ): ReportJpaEntity {
        return reportJpaRepository.save(
            ReportJpaEntity(
                reporterId = reporter.id,
                targetType = targetType,
                targetId = targetId,
                targetMemberId = author.id,
                reason = ReportReason.SPAM,
                detail = detail,
                status = status,
                processedAt = if (status == ReportStatus.PENDING) null else LocalDateTime.now()
            )
        )
    }

    private fun saveWinner(feed: FeedJpaEntity, member: MemberJpaEntity) {
        val contest = contestJpaRepository.save(
            ContestJpaEntity(
                title = "수상 콘테스트",
                startedAt = LocalDateTime.now().minusDays(10),
                expiredAt = LocalDateTime.now().minusDays(1),
                status = ContestStatusType.WINNER_SELECTED,
                thumbnailUrl = "https://example.com/thumbnail.jpg"
            )
        )
        val participation = contestParticipationJpaRepository.save(
            ContestParticipationJpaEntity(member = member, contest = contest, feed = feed)
        )
        jdbcTemplate.update(
            "INSERT INTO contest_winner (winner_rank, description, acceptance_speech, contest_id, participation_id, " +
                "created_at, updated_at) VALUES (1, '대상', '', ?, ?, NOW(), NOW())",
            contest.id,
            participation.id
        )
    }

    private fun rowOf(body: String, reportId: Long): String {
        val idIndex = body.indexOf("data-report-id=\"$reportId\"")
        return body.substring(idIndex, body.indexOf("</tr>", idIndex))
    }
}
