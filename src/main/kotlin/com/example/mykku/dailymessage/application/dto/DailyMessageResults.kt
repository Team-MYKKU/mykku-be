package com.example.mykku.dailymessage.application.dto

import com.example.mykku.dailymessage.domain.entity.DailyMessage
import java.time.LocalDate
import java.time.LocalDateTime

data class DailyMessageSummaryResult(
    val id: Long,
    val title: String,
    val content: String,
    val date: LocalDate,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(dailyMessage: DailyMessage): DailyMessageSummaryResult {
            return DailyMessageSummaryResult(
                id = dailyMessage.id.value,
                title = dailyMessage.title,
                content = dailyMessage.content,
                date = dailyMessage.date,
                createdAt = dailyMessage.createdAt
            )
        }
    }
}

data class DailyMessageResult(
    val id: Long,
    val title: String,
    val content: String,
    val date: LocalDate,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(dailyMessage: DailyMessage): DailyMessageResult {
            return DailyMessageResult(
                id = dailyMessage.id.value,
                title = dailyMessage.title,
                content = dailyMessage.content,
                date = dailyMessage.date,
                createdAt = dailyMessage.createdAt
            )
        }
    }
}
