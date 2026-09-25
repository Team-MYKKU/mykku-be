package com.example.mykku.report.application.port.output

import com.example.mykku.report.domain.entity.Report
import com.example.mykku.report.domain.vo.ReportId
import com.example.mykku.report.domain.vo.ReportStatus
import com.example.mykku.report.domain.vo.ReportTargetType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface ReportRepository {
    fun save(report: Report): Report
    fun update(report: Report): Report
    fun findById(id: ReportId): Report?
    fun findAllByStatus(status: ReportStatus?, pageable: Pageable): Page<Report>
    fun existsByReporterIdAndTarget(
        reporterId: Long,
        targetType: ReportTargetType,
        targetId: Long
    ): Boolean
    fun findAllByTarget(targetType: ReportTargetType, targetId: Long): List<Report>
    fun countByTargetMemberId(targetMemberId: Long): Long
}
