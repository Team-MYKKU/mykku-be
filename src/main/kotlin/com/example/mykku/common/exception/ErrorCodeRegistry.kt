package com.example.mykku.common.exception

import com.example.mykku.admin.exception.AdminErrorCode
import com.example.mykku.auth.exception.AuthErrorCode
import com.example.mykku.block.exception.BlockErrorCode
import com.example.mykku.board.exception.BoardErrorCode
import com.example.mykku.contest.exception.ContestErrorCode
import com.example.mykku.dailymessage.exception.DailyMessageErrorCode
import com.example.mykku.email.exception.EmailAuthErrorCode
import com.example.mykku.event.exception.EventErrorCode
import com.example.mykku.fannote.exception.FanNoteErrorCode
import com.example.mykku.feed.exception.FeedErrorCode
import com.example.mykku.image.exception.ImageErrorCode
import com.example.mykku.like.exception.LikeErrorCode
import com.example.mykku.member.exception.MemberErrorCode
import com.example.mykku.notification.exception.NotificationErrorCode
import com.example.mykku.preference.exception.PreferenceErrorCode
import com.example.mykku.report.exception.ReportErrorCode
import com.example.mykku.role.exception.RoleErrorCode

object ErrorCodeRegistry {

    private val domainErrorCodes: Map<String, List<DomainErrorCode>> = mapOf(
        "common" to CommonErrorCode.entries,
        "admin" to AdminErrorCode.entries,
        "auth" to AuthErrorCode.entries,
        "block" to BlockErrorCode.entries,
        "board" to BoardErrorCode.entries,
        "contest" to ContestErrorCode.entries,
        "dailyMessage" to DailyMessageErrorCode.entries,
        "email" to EmailAuthErrorCode.entries,
        "event" to EventErrorCode.entries,
        "fanNote" to FanNoteErrorCode.entries,
        "feed" to FeedErrorCode.entries,
        "image" to ImageErrorCode.entries,
        "like" to LikeErrorCode.entries,
        "member" to MemberErrorCode.entries,
        "notification" to NotificationErrorCode.entries,
        "preference" to PreferenceErrorCode.entries,
        "report" to ReportErrorCode.entries,
        "role" to RoleErrorCode.entries
    )

    fun getAllErrorCodes(): List<ErrorCodeInfo> {
        return domainErrorCodes.flatMap { (domain, codes) ->
            codes.map { it.toErrorCodeInfo(domain) }
        }.sortedBy { it.code }
    }

    fun getErrorCodesByDomain(domain: String): List<ErrorCodeInfo> {
        return domainErrorCodes[domain]?.map { it.toErrorCodeInfo(domain) } ?: emptyList()
    }

    fun getDomains(): List<String> {
        return domainErrorCodes.keys.sorted()
    }

    private fun DomainErrorCode.toErrorCodeInfo(domain: String) = ErrorCodeInfo(
        code = this.code,
        domain = domain,
        name = this.name,
        status = this.status.value(),
        message = this.message
    )
}
