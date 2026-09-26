package com.example.mykku.report.application.usecase

import com.example.mykku.report.application.dto.ReportDetailResult
import com.example.mykku.report.application.dto.ReportedCommentResult
import com.example.mykku.report.application.dto.ReportedDailyMessageCommentResult
import com.example.mykku.report.application.dto.ReportedFeedResult
import com.example.mykku.report.application.port.input.GetReportDetailUseCase
import com.example.mykku.report.application.port.output.ReportRepository
import com.example.mykku.report.domain.entity.Report
import com.example.mykku.report.domain.vo.ReportId
import com.example.mykku.report.domain.vo.ReportTargetType
import com.example.mykku.report.exception.ReportException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GetReportDetailService(
    private val reportRepository: ReportRepository,
    private val reportMemberIdResolver: ReportMemberIdResolver,
    private val reportTargetDetailLoader: ReportTargetDetailLoader
) : GetReportDetailUseCase {

    @Transactional(readOnly = true)
    override fun execute(reportId: Long): ReportDetailResult {
        val report = reportRepository.findById(ReportId.of(reportId))
            ?: throw ReportException.reportNotFound()
        val siblings = reportRepository.findAllByTarget(report.targetType, report.targetId)
            .filter { it.id != report.id }
        val results = reportMemberIdResolver.toResults(listOf(report) + siblings)
        return ReportDetailResult(
            report = results.first(),
            feed = loadFeed(report),
            comment = loadComment(report),
            dailyMessageComment = loadDailyMessageComment(report),
            sameTargetReports = results.drop(1),
            targetMemberReportCount = report.targetMemberId?.let { reportRepository.countByTargetMemberId(it) } ?: 0
        )
    }

    private fun loadFeed(report: Report): ReportedFeedResult? {
        if (report.targetType != ReportTargetType.FEED) return null
        return reportTargetDetailLoader.loadFeed(report.targetId)
    }

    private fun loadComment(report: Report): ReportedCommentResult? {
        if (report.targetType != ReportTargetType.FEED_COMMENT) return null
        return reportTargetDetailLoader.loadComment(report.targetId)
    }

    private fun loadDailyMessageComment(report: Report): ReportedDailyMessageCommentResult? {
        if (report.targetType != ReportTargetType.DAILY_MESSAGE_COMMENT) return null
        return reportTargetDetailLoader.loadDailyMessageComment(report.targetId)
    }
}
