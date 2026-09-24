package com.example.mykku.admin.adapter.input.web

import com.example.mykku.BaseControllerTest
import com.example.mykku.admin.exception.AdminErrorCode
import com.example.mykku.member.adapter.output.persistence.entity.MemberJpaEntity
import com.example.mykku.report.adapter.input.web.ProcessReportRequest
import com.example.mykku.report.adapter.output.persistence.entity.ReportJpaEntity
import com.example.mykku.report.adapter.output.persistence.repository.ReportJpaRepository
import com.example.mykku.report.domain.vo.ReportReason
import com.example.mykku.report.domain.vo.ReportStatus
import com.example.mykku.report.domain.vo.ReportTargetType
import com.example.mykku.report.exception.ReportErrorCode
import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.notNullValue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime

@DisplayName("AdminReportApiController 통합 테스트")
class AdminReportApiControllerTest : BaseControllerTest() {

    @Autowired
    private lateinit var reportJpaRepository: ReportJpaRepository

    private lateinit var reporter: MemberJpaEntity
    private lateinit var author: MemberJpaEntity
    private lateinit var adminSessionId: String

    @BeforeEach
    fun setUp() {
        reporter = createAndSaveMember(
            memberId = "reporter",
            nickname = "신고자",
            email = "reporter@example.com",
            socialId = "reporter1"
        )
        author = createAndSaveMember(
            memberId = "author",
            nickname = "작성자",
            email = "author@example.com",
            socialId = "author1"
        )
        adminSessionId = getAdminSessionId()
    }

    private fun createReport(
        targetId: Long = 1L,
        reason: ReportReason = ReportReason.SPAM,
        status: ReportStatus = ReportStatus.PENDING,
        processedAt: LocalDateTime? = null
    ): ReportJpaEntity {
        return reportJpaRepository.save(
            ReportJpaEntity(
                reporterId = reporter.id,
                targetType = ReportTargetType.FEED,
                targetId = targetId,
                targetMemberId = author.id,
                reason = reason,
                detail = "광고 도배입니다",
                status = status,
                processedAt = processedAt
            )
        )
    }

    @Test
    @DisplayName("신고 목록을 조회할 수 있다")
    fun `getReports - 신고 목록을 조회한다`() {
        createReport(targetId = 1L)
        createReport(targetId = 2L, status = ReportStatus.RESOLVED, processedAt = LocalDateTime.now())

        RestAssured
            .given()
            .sessionId(adminSessionId)
            .contentType(ContentType.JSON)
            .`when`()
            .get("/admin/api/v1/reports")
            .then()
            .statusCode(200)
            .body("message", equalTo("신고 목록 조회 성공"))
            .body("data.totalElements", equalTo(2))
            .body("data.currentPage", equalTo(0))
            .body("data.reports.size()", equalTo(2))
    }

    @Test
    @DisplayName("처리 상태로 신고 목록을 필터링할 수 있다")
    fun `getReports - status로 필터링한다`() {
        createReport(targetId = 1L)
        createReport(targetId = 2L, status = ReportStatus.RESOLVED, processedAt = LocalDateTime.now())

        RestAssured
            .given()
            .sessionId(adminSessionId)
            .contentType(ContentType.JSON)
            .queryParam("status", ReportStatus.PENDING.name)
            .`when`()
            .get("/admin/api/v1/reports")
            .then()
            .statusCode(200)
            .body("data.totalElements", equalTo(1))
            .body("data.reports[0].status", equalTo(ReportStatus.PENDING.name))
            .body("data.reports[0].targetId", equalTo(1))
    }

    @Test
    @DisplayName("신고를 처리 완료로 변경할 수 있다")
    fun `processReport - 신고를 RESOLVED로 처리한다`() {
        val report = createReport()
        val request = ProcessReportRequest(status = ReportStatus.RESOLVED)

        RestAssured
            .given()
            .sessionId(adminSessionId)
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .patch("/admin/api/v1/reports/${report.id}")
            .then()
            .statusCode(200)
            .body("message", equalTo("신고 처리 완료"))
            .body("data.status", equalTo(ReportStatus.RESOLVED.name))
            .body("data.statusDescription", equalTo(ReportStatus.RESOLVED.description))
            .body("data.processedAt", notNullValue())
    }

    @Test
    @DisplayName("이미 처리된 신고는 다시 처리할 수 없다")
    fun `processReport - 이미 처리된 신고는 실패한다`() {
        val report = createReport(status = ReportStatus.RESOLVED, processedAt = LocalDateTime.now())
        val request = ProcessReportRequest(status = ReportStatus.REJECTED)

        RestAssured
            .given()
            .sessionId(adminSessionId)
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .patch("/admin/api/v1/reports/${report.id}")
            .then()
            .statusCode(409)
            .body("code", equalTo(ReportErrorCode.REPORT_ALREADY_PROCESSED.code))
    }

    @Test
    @DisplayName("존재하지 않는 신고는 처리할 수 없다")
    fun `processReport - 존재하지 않는 신고는 실패한다`() {
        val request = ProcessReportRequest(status = ReportStatus.RESOLVED)

        RestAssured
            .given()
            .sessionId(adminSessionId)
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .patch("/admin/api/v1/reports/999999")
            .then()
            .statusCode(404)
            .body("code", equalTo(ReportErrorCode.REPORT_NOT_FOUND.code))
    }

    @Test
    @DisplayName("PENDING 상태로는 처리할 수 없다")
    fun `processReport - PENDING 상태로 처리하면 실패한다`() {
        val report = createReport()
        val request = ProcessReportRequest(status = ReportStatus.PENDING)

        RestAssured
            .given()
            .sessionId(adminSessionId)
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .patch("/admin/api/v1/reports/${report.id}")
            .then()
            .statusCode(400)
            .body("code", equalTo(ReportErrorCode.REPORT_STATUS_NOT_PROCESSABLE.code))
    }

    @Test
    @DisplayName("관리자 세션 없이 접근하면 로그인 페이지로 리다이렉트된다")
    fun `getReports - 세션이 없으면 리다이렉트된다`() {
        RestAssured
            .given()
            .contentType(ContentType.JSON)
            .redirects().follow(false)
            .`when`()
            .get("/admin/api/v1/reports")
            .then()
            .statusCode(401)
            .body("code", equalTo(AdminErrorCode.UNAUTHORIZED.code))
    }
}
