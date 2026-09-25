package com.example.mykku.admin.controller

import com.example.mykku.common.util.PageableValidator
import com.example.mykku.report.application.dto.ListReportsQuery
import com.example.mykku.report.application.port.input.GetReportDetailUseCase
import com.example.mykku.report.application.port.input.GetReportsUseCase
import com.example.mykku.report.domain.vo.ReportStatus
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
@RequestMapping("/admin/report")
class AdminReportViewController(
    private val getReportsUseCase: GetReportsUseCase,
    private val getReportDetailUseCase: GetReportDetailUseCase
) {

    @GetMapping
    fun listPage(
        @RequestParam(required = false) status: ReportStatus?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        model: Model
    ): String {
        val query = ListReportsQuery(status = status, pageable = PageableValidator.validateAndCreate(page, size))
        model.addAttribute("reports", getReportsUseCase.getReports(query))
        model.addAttribute("currentStatus", status?.name ?: ALL_STATUS)
        return "admin/report/list"
    }

    @GetMapping("/{reportId}")
    fun detailPage(@PathVariable reportId: Long, model: Model): String {
        val detail = getReportDetailUseCase.execute(reportId)
        val pendingReportIds = (listOf(detail.report) + detail.sameTargetReports)
            .filter { it.status == ReportStatus.PENDING.name }
            .map { it.id }
        model.addAttribute("detail", detail)
        model.addAttribute("pendingReportIds", pendingReportIds.joinToString(","))
        model.addAttribute("pendingReportCount", pendingReportIds.size)
        return "admin/report/detail"
    }

    companion object {
        private const val ALL_STATUS = "ALL"
    }
}
