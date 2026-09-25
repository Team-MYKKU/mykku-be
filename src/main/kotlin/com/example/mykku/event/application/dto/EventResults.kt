package com.example.mykku.event.application.dto

import com.example.mykku.event.domain.vo.EventStatusType
import com.example.mykku.event.domain.vo.EventWinnerEntryStatus
import com.example.mykku.event.domain.vo.EventWinnerStatus
import org.springframework.data.domain.Page
import java.time.LocalDate
import java.time.LocalDateTime

data class CreateEventResult(
    val id: Long,
    val title: String,
    val subTitle: String?,
    val description: String?,
    val startedAt: LocalDateTime,
    val expiredAt: LocalDateTime,
    val thumbnailUrl: String,
    val images: List<EventImageResult>,
    val createdAt: LocalDateTime
)

data class EventImageResult(
    val url: String,
    val orderIndex: Int
)

data class EventListResult(
    val id: Long,
    val title: String,
    val subTitle: String?,
    val description: String?,
    val startedAt: LocalDateTime,
    val expiredAt: LocalDateTime,
    val status: EventStatusType,
    val thumbnailUrl: String
)

data class PagedEventsResult(
    val content: List<EventListResult>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val isLast: Boolean
) {
    companion object {
        fun <T> of(page: Page<T>, content: List<EventListResult>): PagedEventsResult {
            return PagedEventsResult(
                content = content,
                page = page.number,
                size = page.size,
                totalElements = page.totalElements,
                totalPages = page.totalPages,
                isLast = page.isLast
            )
        }
    }
}

data class EventDetailResult(
    val id: Long,
    val title: String,
    val subTitle: String?,
    val description: String?,
    val startedAt: LocalDateTime,
    val expiredAt: LocalDateTime,
    val status: EventStatusType,
    val thumbnailUrl: String,
    val images: List<EventImageResult>,
    val createdAt: LocalDateTime,
    val isWinner: Boolean
)

data class EventPreviewResult(
    val id: Long,
    val thumbnailUrl: String,
    val images: List<String>
)

data class MyParticipatedEventResult(
    val id: Long,
    val title: String,
    val startedAt: LocalDateTime,
    val expiredAt: LocalDateTime,
    val status: EventStatusType,
    val thumbnailUrl: String,
    val winnerStatus: EventWinnerStatus
)

data class PagedMyParticipatedEventsResult(
    val content: List<MyParticipatedEventResult>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val isLast: Boolean
)

data class SetEventWinnersResult(
    val eventId: Long,
    val eventTitle: String,
    val dryRun: Boolean,
    val entries: List<EventWinnerEntryResult>,
    val addedCount: Int,
    val keptCount: Int,
    val removedCount: Int,
    val withdrawnKeptCount: Int
)

data class EventWinnerEntryResult(
    val input: String,
    val memberId: String?,
    val nickname: String?,
    val result: EventWinnerEntryStatus
)

data class EventWinnerSelectionResult(
    val eventId: Long,
    val title: String,
    val startedAt: LocalDateTime,
    val expiredAt: LocalDateTime,
    val status: EventStatusType,
    val winnerSelectable: Boolean,
    val winnerMemberIds: List<String>,
    val withdrawnWinnerCount: Int
)

data class EventWinnersResult(
    val eventId: Long,
    val eventTitle: String,
    val winners: List<EventWinnerResult>
)

data class EventWinnerResult(
    val winnerId: Long,
    val memberId: String?,
    val nickname: String?,
    val profileImage: String
)

data class MyEventWinnerStatusResult(
    val isWinner: Boolean,
    val winnerId: Long?
)

data class EventWinnerAnnouncementResult(
    val eventId: Long,
    val eventTitle: String,
    val title: String,
    val content: String,
    val announcedAt: LocalDate
)

data class MyAwardEventResult(
    val eventId: Long,
    val eventTitle: String,
    val thumbnailUrl: String,
    val startedAt: LocalDateTime,
    val expiredAt: LocalDateTime
)

data class PagedMyAwardEventsResult(
    val content: List<MyAwardEventResult>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val isLast: Boolean
)
