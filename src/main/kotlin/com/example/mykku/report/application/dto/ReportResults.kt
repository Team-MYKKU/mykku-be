package com.example.mykku.report.application.dto

import com.example.mykku.report.domain.entity.Report
import java.time.LocalDateTime

data class ReportResult(
    val id: Long,
    val reporterMemberId: String?,
    val targetType: String,
    val targetTypeDescription: String,
    val targetId: Long,
    val targetMemberId: String?,
    val reason: String,
    val reasonDescription: String,
    val detail: String?,
    val status: String,
    val statusDescription: String,
    val processedAt: LocalDateTime?,
    val createdAt: LocalDateTime,
    val reporterNickname: String? = null,
    val targetNickname: String? = null,
    val reporterWithdrawn: Boolean = false,
    val targetWithdrawn: Boolean = false
) {
    companion object {
        fun from(
            report: Report,
            reporterMemberId: String?,
            targetMemberId: String?,
            reporterNickname: String? = null,
            targetNickname: String? = null
        ): ReportResult {
            return ReportResult(
                id = report.id!!.value,
                reporterMemberId = reporterMemberId,
                targetType = report.targetType.name,
                targetTypeDescription = report.targetType.description,
                targetId = report.targetId,
                targetMemberId = targetMemberId,
                reason = report.reason.name,
                reasonDescription = report.reason.description,
                detail = report.detail,
                status = report.status.name,
                statusDescription = report.status.description,
                processedAt = report.processedAt,
                createdAt = report.createdAt,
                reporterNickname = reporterNickname,
                targetNickname = targetNickname,
                reporterWithdrawn = report.reporterId == null,
                targetWithdrawn = report.targetMemberId == null
            )
        }
    }
}

data class PagedReportsResult(
    val reports: List<ReportResult>,
    val currentPage: Int,
    val totalPages: Int,
    val totalElements: Long,
    val size: Int,
    val hasNext: Boolean,
    val hasPrevious: Boolean
)

data class ReportMemberSummary(
    val memberId: String?,
    val nickname: String?
)

data class ReportDetailResult(
    val report: ReportResult,
    val feed: ReportedFeedResult?,
    val comment: ReportedCommentResult?,
    val sameTargetReports: List<ReportResult>,
    val targetMemberReportCount: Long
) {
    val targetDeleted: Boolean
        get() = feed == null && comment == null
}

data class ReportedFeedResult(
    val feedId: Long,
    val title: String,
    val content: String,
    val imageUrls: List<String>,
    val boardTitle: String?,
    val contests: List<ReportedContestResult>
) {
    val bestWinnerRank: Int?
        get() = contests.mapNotNull { it.winnerRank }.minOrNull()
}

data class ReportedContestResult(
    val contestId: Long,
    val contestTitle: String,
    val winnerRank: Int?
)

data class ReportedCommentResult(
    val commentId: Long,
    val content: String,
    val feedId: Long,
    val feedTitle: String?
)
