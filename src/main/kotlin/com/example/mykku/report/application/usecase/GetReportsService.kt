package com.example.mykku.report.application.usecase

import com.example.mykku.report.application.dto.ListReportsQuery
import com.example.mykku.report.application.dto.PagedReportsResult
import com.example.mykku.report.application.dto.ReportResult
import com.example.mykku.report.application.port.input.GetReportsUseCase
import com.example.mykku.report.application.port.output.ReportRepository
import com.example.mykku.report.domain.entity.Report
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GetReportsService(
    private val reportRepository: ReportRepository,
    private val reportMemberIdResolver: ReportMemberIdResolver
) : GetReportsUseCase {

    override fun getReports(query: ListReportsQuery): PagedReportsResult {
        val page = reportRepository.findAllByStatus(query.status, query.pageable)
        val memberIds = resolveMemberIds(page.content)

        return PagedReportsResult(
            reports = page.content.map { toResult(it, memberIds) },
            currentPage = page.number,
            totalPages = page.totalPages,
            totalElements = page.totalElements,
            size = page.size,
            hasNext = page.hasNext(),
            hasPrevious = page.hasPrevious()
        )
    }

    private fun resolveMemberIds(reports: List<Report>): Map<Long, String?> {
        val memberPks = reports.mapNotNull { it.reporterId } + reports.mapNotNull { it.targetMemberId }
        return reportMemberIdResolver.resolve(memberPks)
    }

    private fun toResult(report: Report, memberIds: Map<Long, String?>): ReportResult {
        return ReportResult.from(
            report = report,
            reporterMemberId = report.reporterId?.let { memberIds[it] },
            targetMemberId = report.targetMemberId?.let { memberIds[it] }
        )
    }
}
