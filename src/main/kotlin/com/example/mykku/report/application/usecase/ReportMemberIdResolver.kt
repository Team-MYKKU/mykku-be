package com.example.mykku.report.application.usecase

import com.example.mykku.member.application.port.output.MemberRepository
import com.example.mykku.member.domain.vo.MemberPk
import com.example.mykku.report.application.dto.ReportMemberSummary
import com.example.mykku.report.application.dto.ReportResult
import com.example.mykku.report.domain.entity.Report
import org.springframework.stereotype.Component

@Component
class ReportMemberIdResolver(
    private val memberRepository: MemberRepository
) {

    fun resolve(memberPks: List<Long>): Map<Long, String?> {
        return resolveSummaries(memberPks).mapValues { it.value.memberId }
    }

    fun resolveSummaries(memberPks: List<Long>): Map<Long, ReportMemberSummary> {
        val distinctPks = memberPks.distinct().map { MemberPk.of(it) }
        if (distinctPks.isEmpty()) return emptyMap()
        return memberRepository.findByIds(distinctPks)
            .associate { it.id.value to ReportMemberSummary(it.memberId, it.nickname) }
    }

    fun resolveOne(memberPk: Long?): String? {
        if (memberPk == null) return null
        return resolve(listOf(memberPk))[memberPk]
    }

    fun toResults(reports: List<Report>): List<ReportResult> {
        val members = resolveSummaries(reports.mapNotNull { it.reporterId } + reports.mapNotNull { it.targetMemberId })
        return reports.map { toResult(it, members) }
    }

    private fun toResult(report: Report, members: Map<Long, ReportMemberSummary>): ReportResult {
        val reporter = report.reporterId?.let { members[it] }
        val target = report.targetMemberId?.let { members[it] }
        return ReportResult.from(
            report = report,
            reporterMemberId = reporter?.memberId,
            targetMemberId = target?.memberId,
            reporterNickname = reporter?.nickname,
            targetNickname = target?.nickname
        )
    }
}
