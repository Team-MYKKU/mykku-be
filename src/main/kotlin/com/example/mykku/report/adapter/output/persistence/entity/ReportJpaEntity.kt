package com.example.mykku.report.adapter.output.persistence.entity

import com.example.mykku.common.adapter.persistence.BaseJpaEntity
import com.example.mykku.report.domain.entity.Report
import com.example.mykku.report.domain.vo.ReportId
import com.example.mykku.report.domain.vo.ReportReason
import com.example.mykku.report.domain.vo.ReportStatus
import com.example.mykku.report.domain.vo.ReportTargetType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

@Entity
@Table(
    name = "report",
    uniqueConstraints = [UniqueConstraint(columnNames = ["reporter_id", "target_type", "target_id"])],
    indexes = [
        Index(name = "idx_report_status_created", columnList = "status, created_at"),
        Index(name = "idx_report_target", columnList = "target_type, target_id")
    ]
)
class ReportJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "reporter_id", updatable = false)
    val reporterId: Long?,

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 30)
    val targetType: ReportTargetType,

    @Column(name = "target_id", nullable = false)
    val targetId: Long,

    @Column(name = "target_member_id", updatable = false)
    val targetMemberId: Long? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 30)
    val reason: ReportReason,

    @Column(name = "detail", length = Report.DETAIL_MAX_LENGTH)
    val detail: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: ReportStatus,

    @Column(name = "processed_at")
    var processedAt: LocalDateTime? = null
) : BaseJpaEntity() {

    fun toDomain(): Report {
        return Report.reconstitute(
            id = ReportId.of(id!!),
            reporterId = reporterId,
            targetType = targetType,
            targetId = targetId,
            targetMemberId = targetMemberId,
            reason = reason,
            detail = detail,
            status = status,
            processedAt = processedAt,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    fun updateFromDomain(report: Report) {
        this.status = report.status
        this.processedAt = report.processedAt
    }

    companion object {
        fun fromDomain(report: Report): ReportJpaEntity {
            return ReportJpaEntity(
                id = report.id?.value,
                reporterId = report.reporterId,
                targetType = report.targetType,
                targetId = report.targetId,
                targetMemberId = report.targetMemberId,
                reason = report.reason,
                detail = report.detail,
                status = report.status,
                processedAt = report.processedAt
            )
        }
    }
}
