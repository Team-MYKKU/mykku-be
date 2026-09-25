package com.example.mykku.event.application.dto

import com.example.mykku.event.domain.vo.EventListFilter
import com.example.mykku.event.domain.vo.EventSortType
import org.springframework.data.domain.Pageable
import java.time.LocalDate
import java.time.LocalDateTime

data class CreateEventCommand(
    val title: String,
    val subTitle: String?,
    val description: String?,
    val startedAt: LocalDateTime,
    val expiredAt: LocalDateTime,
    val thumbnailUrl: String,
    val images: List<EventImageCommand>
)

data class EventImageCommand(
    val url: String,
    val orderIndex: Int
)

data class EventListQuery(
    val filter: EventListFilter,
    val sortType: EventSortType,
    val page: Int,
    val size: Int
)

data class SetEventWinnersCommand(
    val eventId: Long,
    val memberIds: List<String>,
    val dryRun: Boolean
)

data class GetMyEventWinnerStatusQuery(
    val eventId: Long,
    val id: Long
)

data class GetMyAwardEventsQuery(
    val id: Long,
    val pageable: Pageable
)

data class UpsertEventWinnerAnnouncementCommand(
    val eventId: Long,
    val title: String,
    val content: String,
    val announcedAt: LocalDate
)
