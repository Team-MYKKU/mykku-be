package com.example.mykku.dailymessage.adapter.input.web

import com.example.mykku.dailymessage.application.dto.DailyMessageResult
import com.example.mykku.dailymessage.application.dto.DailyMessageSummaryResult
import java.time.LocalDate
import java.time.LocalDateTime

data class DailyMessageSummaryResponse(
    val id: Long,
    val title: String,
    val content: String,
    val date: LocalDate
) {
    companion object {
        fun from(result: DailyMessageSummaryResult): DailyMessageSummaryResponse {
            return DailyMessageSummaryResponse(
                id = result.id,
                title = result.title,
                content = result.content,
                date = result.date
            )
        }
    }
}

data class DailyMessageResponse(
    val id: Long,
    val title: String,
    val content: String,
    val date: LocalDate,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(result: DailyMessageResult): DailyMessageResponse {
            return DailyMessageResponse(
                id = result.id,
                title = result.title,
                content = result.content,
                date = result.date,
                createdAt = result.createdAt
            )
        }
    }
}
