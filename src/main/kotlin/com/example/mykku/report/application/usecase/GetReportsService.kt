package com.example.mykku.report.application.usecase

import com.example.mykku.report.application.dto.ListReportsQuery
import com.example.mykku.report.application.dto.PagedReportsResult
import com.example.mykku.report.application.port.input.GetReportsUseCase
import com.example.mykku.report.application.port.output.ReportRepository
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

        return PagedReportsResult(
            reports = reportMemberIdResolver.toResults(page.content),
            currentPage = page.number,
            totalPages = page.totalPages,
            totalElements = page.totalElements,
            size = page.size,
            hasNext = page.hasNext(),
            hasPrevious = page.hasPrevious()
        )
    }
}
