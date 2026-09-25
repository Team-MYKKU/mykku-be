package com.example.mykku.report.application.port.input

import com.example.mykku.report.application.dto.ReportDetailResult

interface GetReportDetailUseCase {
    fun execute(reportId: Long): ReportDetailResult
}
