package com.example.mykku.report.domain.entity

import com.example.mykku.report.domain.vo.ReportId
import com.example.mykku.report.domain.vo.ReportReason
import com.example.mykku.report.domain.vo.ReportStatus
import com.example.mykku.report.domain.vo.ReportTargetType
import com.example.mykku.report.exception.ReportException
import java.time.LocalDateTime

class Report private constructor(
    val id: ReportId?,
    val reporterId: Long?,
    val targetType: ReportTargetType,
    val targetId: Long,
    val targetMemberId: Long?,
    val reason: ReportReason,
    val detail: String?,
    private var _status: ReportStatus,
    private var _processedAt: LocalDateTime?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    val status: ReportStatus
        get() = _status

    val processedAt: LocalDateTime?
        get() = _processedAt

    fun process(newStatus: ReportStatus) {
        if (newStatus == ReportStatus.PENDING) {
            throw ReportException.statusNotProcessable()
        }
        if (_status != ReportStatus.PENDING) {
            throw ReportException.alreadyProcessed()
        }
        _status = newStatus
        _processedAt = LocalDateTime.now()
    }

    companion object {
        const val DETAIL_MAX_LENGTH = 500

        fun create(
            reporterId: Long,
            targetType: ReportTargetType,
            targetId: Long,
            targetMemberId: Long?,
            reason: ReportReason,
            detail: String?
        ): Report {
            val trimmedDetail = detail?.trim()?.takeIf { it.isNotEmpty() }
            validateDetail(reason, trimmedDetail)

            val now = LocalDateTime.now()
            return Report(
                id = null,
                reporterId = reporterId,
                targetType = targetType,
                targetId = targetId,
                targetMemberId = targetMemberId,
                reason = reason,
                detail = trimmedDetail,
                _status = ReportStatus.PENDING,
                _processedAt = null,
                createdAt = now,
                updatedAt = now
            )
        }

        fun reconstitute(
            id: ReportId,
            reporterId: Long?,
            targetType: ReportTargetType,
            targetId: Long,
            targetMemberId: Long?,
            reason: ReportReason,
            detail: String?,
            status: ReportStatus,
            processedAt: LocalDateTime?,
            createdAt: LocalDateTime,
            updatedAt: LocalDateTime
        ): Report {
            return Report(
                id = id,
                reporterId = reporterId,
                targetType = targetType,
                targetId = targetId,
                targetMemberId = targetMemberId,
                reason = reason,
                detail = detail,
                _status = status,
                _processedAt = processedAt,
                createdAt = createdAt,
                updatedAt = updatedAt
            )
        }

        private fun validateDetail(reason: ReportReason, detail: String?) {
            if (reason == ReportReason.ETC && detail == null) {
                throw ReportException.detailRequired()
            }
            if ((detail?.length ?: 0) > DETAIL_MAX_LENGTH) {
                throw ReportException.detailTooLong()
            }
        }
    }
}
