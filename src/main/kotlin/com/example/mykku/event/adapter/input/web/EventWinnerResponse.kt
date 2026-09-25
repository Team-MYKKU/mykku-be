package com.example.mykku.event.adapter.input.web

import com.example.mykku.event.application.dto.EventWinnerEntryResult
import com.example.mykku.event.application.dto.EventWinnerResult
import com.example.mykku.event.application.dto.EventWinnersResult
import com.example.mykku.event.application.dto.MyAwardEventResult
import com.example.mykku.event.application.dto.MyEventWinnerStatusResult
import com.example.mykku.event.application.dto.PagedMyAwardEventsResult
import com.example.mykku.event.application.dto.SetEventWinnersResult
import com.example.mykku.event.domain.vo.EventWinnerEntryStatus
import java.time.LocalDateTime

data class EventWinnersResponse(
    val eventId: Long,
    val eventTitle: String,
    val winners: List<EventWinnerResponse>
) {
    companion object {
        fun from(result: EventWinnersResult): EventWinnersResponse {
            return EventWinnersResponse(
                eventId = result.eventId,
                eventTitle = result.eventTitle,
                winners = result.winners.map { EventWinnerResponse.from(it) }
            )
        }
    }
}

data class EventWinnerResponse(
    val winnerId: Long,
    val memberId: String?,
    val nickname: String?,
    val profileImage: String
) {
    companion object {
        fun from(result: EventWinnerResult): EventWinnerResponse {
            return EventWinnerResponse(
                winnerId = result.winnerId,
                memberId = result.memberId,
                nickname = result.nickname,
                profileImage = result.profileImage
            )
        }
    }
}

data class MyEventWinnerStatusResponse(
    val isWinner: Boolean,
    val winnerId: Long?
) {
    companion object {
        fun from(result: MyEventWinnerStatusResult): MyEventWinnerStatusResponse {
            return MyEventWinnerStatusResponse(
                isWinner = result.isWinner,
                winnerId = result.winnerId
            )
        }
    }
}

data class MyAwardEventResponse(
    val eventId: Long,
    val eventTitle: String,
    val thumbnailUrl: String,
    val startedAt: LocalDateTime,
    val expiredAt: LocalDateTime
) {
    companion object {
        fun from(result: MyAwardEventResult): MyAwardEventResponse {
            return MyAwardEventResponse(
                eventId = result.eventId,
                eventTitle = result.eventTitle,
                thumbnailUrl = result.thumbnailUrl,
                startedAt = result.startedAt,
                expiredAt = result.expiredAt
            )
        }
    }
}

data class PagedMyAwardEventsResponse(
    val content: List<MyAwardEventResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val isLast: Boolean
) {
    companion object {
        fun from(result: PagedMyAwardEventsResult): PagedMyAwardEventsResponse {
            return PagedMyAwardEventsResponse(
                content = result.content.map { MyAwardEventResponse.from(it) },
                page = result.page,
                size = result.size,
                totalElements = result.totalElements,
                totalPages = result.totalPages,
                isLast = result.isLast
            )
        }
    }
}

data class SetEventWinnersResponse(
    val eventId: Long,
    val eventTitle: String,
    val dryRun: Boolean,
    val entries: List<EventWinnerEntryResponse>,
    val addedCount: Int,
    val keptCount: Int,
    val removedCount: Int,
    val withdrawnKeptCount: Int
) {
    companion object {
        fun from(result: SetEventWinnersResult): SetEventWinnersResponse {
            return SetEventWinnersResponse(
                eventId = result.eventId,
                eventTitle = result.eventTitle,
                dryRun = result.dryRun,
                entries = result.entries.map { EventWinnerEntryResponse.from(it) },
                addedCount = result.addedCount,
                keptCount = result.keptCount,
                removedCount = result.removedCount,
                withdrawnKeptCount = result.withdrawnKeptCount
            )
        }
    }
}

data class EventWinnerEntryResponse(
    val input: String,
    val memberId: String?,
    val nickname: String?,
    val result: EventWinnerEntryStatus
) {
    companion object {
        fun from(result: EventWinnerEntryResult): EventWinnerEntryResponse {
            return EventWinnerEntryResponse(
                input = result.input,
                memberId = result.memberId,
                nickname = result.nickname,
                result = result.result
            )
        }
    }
}
