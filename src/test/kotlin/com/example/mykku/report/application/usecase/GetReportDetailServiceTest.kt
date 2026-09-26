package com.example.mykku.report.application.usecase

import com.example.mykku.report.application.dto.ReportResult
import com.example.mykku.report.application.dto.ReportedDailyMessageCommentResult
import com.example.mykku.report.application.dto.ReportedFeedResult
import com.example.mykku.report.application.port.output.ReportRepository
import com.example.mykku.report.domain.entity.Report
import com.example.mykku.report.domain.vo.ReportId
import com.example.mykku.report.domain.vo.ReportReason
import com.example.mykku.report.domain.vo.ReportStatus
import com.example.mykku.report.domain.vo.ReportTargetType
import com.example.mykku.report.exception.ReportErrorCode
import com.example.mykku.report.exception.ReportException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("GetReportDetailService 테스트")
class GetReportDetailServiceTest {

    private val reportRepository: ReportRepository = mock()
    private val reportMemberIdResolver: ReportMemberIdResolver = mock()
    private val reportTargetDetailLoader: ReportTargetDetailLoader = mock()
    private val service = GetReportDetailService(reportRepository, reportMemberIdResolver, reportTargetDetailLoader)

    @Test
    @DisplayName("없는 신고는 RP001")
    fun `없는 신고`() {
        whenever(reportRepository.findById(ReportId.of(1L))).thenReturn(null)

        val exception = assertThrows<ReportException> { service.execute(1L) }

        assertThat(exception.errorCode).isEqualTo(ReportErrorCode.REPORT_NOT_FOUND)
    }

    @Test
    @DisplayName("대상이 사라졌으면 예외 없이 targetDeleted가 참이다")
    fun `대상 없음`() {
        val report = report(1L, targetMemberId = 7L)
        whenever(reportRepository.findById(ReportId.of(1L))).thenReturn(report)
        whenever(reportRepository.findAllByTarget(ReportTargetType.FEED, 10L)).thenReturn(listOf(report))
        whenever(reportMemberIdResolver.toResults(listOf(report))).thenReturn(listOf(result(1L)))
        whenever(reportTargetDetailLoader.loadFeed(10L)).thenReturn(null)

        val detail = service.execute(1L)

        assertThat(detail.targetDeleted).isTrue()
        verify(reportTargetDetailLoader, never()).loadComment(any())
        verify(reportTargetDetailLoader, never()).loadDailyMessageComment(any())
    }

    @Test
    @DisplayName("같은 대상의 다른 신고는 자기 자신을 빼고, 피신고자 누적 수를 함께 준다")
    fun `형제 신고와 누적 수`() {
        val report = report(1L, targetMemberId = 7L)
        val sibling = report(2L, targetMemberId = 7L)
        whenever(reportRepository.findById(ReportId.of(1L))).thenReturn(report)
        whenever(reportRepository.findAllByTarget(ReportTargetType.FEED, 10L)).thenReturn(listOf(sibling, report))
        whenever(reportMemberIdResolver.toResults(listOf(report, sibling))).thenReturn(listOf(result(1L), result(2L)))
        whenever(reportTargetDetailLoader.loadFeed(10L)).thenReturn(feed())
        whenever(reportRepository.countByTargetMemberId(7L)).thenReturn(5L)

        val detail = service.execute(1L)

        assertThat(detail.report.id).isEqualTo(1L)
        assertThat(detail.sameTargetReports.map { it.id }).containsExactly(2L)
        assertThat(detail.targetMemberReportCount).isEqualTo(5L)
        assertThat(detail.targetDeleted).isFalse()
    }

    @Test
    @DisplayName("피신고자가 없으면 누적 수를 조회하지 않고 0이다")
    fun `피신고자 없음`() {
        val report = report(1L, targetMemberId = null)
        whenever(reportRepository.findById(ReportId.of(1L))).thenReturn(report)
        whenever(reportRepository.findAllByTarget(ReportTargetType.FEED, 10L)).thenReturn(listOf(report))
        whenever(reportMemberIdResolver.toResults(listOf(report))).thenReturn(listOf(result(1L)))

        val detail = service.execute(1L)

        assertThat(detail.targetMemberReportCount).isZero()
        verify(reportRepository, never()).countByTargetMemberId(any())
    }

    @Test
    @DisplayName("하루덕담 댓글 신고는 하루덕담 댓글만 불러오고 피드·피드 댓글은 조회하지 않는다")
    fun `하루덕담 댓글 대상`() {
        val report = report(1L, targetMemberId = 7L, targetType = ReportTargetType.DAILY_MESSAGE_COMMENT)
        whenever(reportRepository.findById(ReportId.of(1L))).thenReturn(report)
        whenever(reportRepository.findAllByTarget(ReportTargetType.DAILY_MESSAGE_COMMENT, 10L))
            .thenReturn(listOf(report))
        whenever(reportMemberIdResolver.toResults(listOf(report))).thenReturn(listOf(result(1L)))
        whenever(reportTargetDetailLoader.loadDailyMessageComment(10L)).thenReturn(dailyMessageComment())

        val detail = service.execute(1L)

        assertThat(detail.dailyMessageComment?.content).isEqualTo("덕담 댓글")
        assertThat(detail.targetDeleted).isFalse()
        verify(reportTargetDetailLoader, never()).loadFeed(any())
        verify(reportTargetDetailLoader, never()).loadComment(any())
    }

    private fun report(
        id: Long,
        targetMemberId: Long?,
        targetType: ReportTargetType = ReportTargetType.FEED
    ): Report {
        return Report.reconstitute(
            id = ReportId.of(id),
            reporterId = 3L,
            targetType = targetType,
            targetId = 10L,
            targetMemberId = targetMemberId,
            reason = ReportReason.SPAM,
            detail = null,
            status = ReportStatus.PENDING,
            processedAt = null,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
    }

    private fun result(id: Long): ReportResult {
        return ReportResult.from(report(id, targetMemberId = 7L), "reporter", "author")
    }

    private fun dailyMessageComment(): ReportedDailyMessageCommentResult {
        return ReportedDailyMessageCommentResult(10L, "덕담 댓글", 5L, "오늘의 덕담", LocalDate.of(2026, 9, 26))
    }

    private fun feed(): ReportedFeedResult {
        return ReportedFeedResult(10L, "제목", "본문", emptyList(), "게시판", emptyList())
    }
}
