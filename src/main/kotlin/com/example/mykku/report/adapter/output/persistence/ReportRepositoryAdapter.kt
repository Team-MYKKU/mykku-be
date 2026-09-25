package com.example.mykku.report.adapter.output.persistence

import com.example.mykku.report.adapter.output.persistence.entity.ReportJpaEntity
import com.example.mykku.report.adapter.output.persistence.repository.ReportJpaRepository
import com.example.mykku.report.application.port.output.ReportRepository
import com.example.mykku.report.domain.entity.Report
import com.example.mykku.report.domain.vo.ReportId
import com.example.mykku.report.domain.vo.ReportStatus
import com.example.mykku.report.domain.vo.ReportTargetType
import com.example.mykku.report.exception.ReportException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

@Repository
class ReportRepositoryAdapter(
    private val reportJpaRepository: ReportJpaRepository
) : ReportRepository {

    override fun save(report: Report): Report {
        return reportJpaRepository.save(ReportJpaEntity.fromDomain(report)).toDomain()
    }

    override fun update(report: Report): Report {
        val reportId = report.id ?: throw ReportException.reportNotFound()
        val entity = reportJpaRepository.findByIdOrNull(reportId.value)
            ?: throw ReportException.reportNotFound()
        entity.updateFromDomain(report)
        return reportJpaRepository.save(entity).toDomain()
    }

    override fun findById(id: ReportId): Report? {
        return reportJpaRepository.findByIdOrNull(id.value)?.toDomain()
    }

    override fun findAllByStatus(status: ReportStatus?, pageable: Pageable): Page<Report> {
        val page = if (status == null) {
            reportJpaRepository.findAllByOrderByCreatedAtDesc(pageable)
        } else {
            reportJpaRepository.findAllByStatusOrderByCreatedAtDesc(status, pageable)
        }
        return page.map { it.toDomain() }
    }

    override fun existsByReporterIdAndTarget(
        reporterId: Long,
        targetType: ReportTargetType,
        targetId: Long
    ): Boolean {
        return reportJpaRepository.existsByReporterIdAndTargetTypeAndTargetId(reporterId, targetType, targetId)
    }

    override fun findAllByTarget(targetType: ReportTargetType, targetId: Long): List<Report> {
        return reportJpaRepository.findAllByTargetTypeAndTargetIdOrderByCreatedAtDescIdDesc(targetType, targetId)
            .map { it.toDomain() }
    }

    override fun countByTargetMemberId(targetMemberId: Long): Long {
        return reportJpaRepository.countByTargetMemberId(targetMemberId)
    }
}
