package com.example.mykku.report.adapter.output.persistence.repository

import com.example.mykku.report.adapter.output.persistence.entity.ReportJpaEntity
import com.example.mykku.report.domain.vo.ReportStatus
import com.example.mykku.report.domain.vo.ReportTargetType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ReportJpaRepository : JpaRepository<ReportJpaEntity, Long> {
    fun existsByReporterIdAndTargetTypeAndTargetId(
        reporterId: Long,
        targetType: ReportTargetType,
        targetId: Long
    ): Boolean

    fun findAllByStatusOrderByCreatedAtDesc(status: ReportStatus, pageable: Pageable): Page<ReportJpaEntity>

    fun findAllByOrderByCreatedAtDesc(pageable: Pageable): Page<ReportJpaEntity>

    fun findAllByTargetTypeAndTargetIdOrderByCreatedAtDescIdDesc(
        targetType: ReportTargetType,
        targetId: Long
    ): List<ReportJpaEntity>

    fun countByTargetMemberId(targetMemberId: Long): Long
}
