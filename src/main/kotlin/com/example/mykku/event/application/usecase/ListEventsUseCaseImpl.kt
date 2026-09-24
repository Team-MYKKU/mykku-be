package com.example.mykku.event.application.usecase

import com.example.mykku.common.util.PageableValidator
import com.example.mykku.event.application.dto.EventListQuery
import com.example.mykku.event.application.dto.EventListResult
import com.example.mykku.event.application.dto.PagedEventsResult
import com.example.mykku.event.application.port.input.ListEventsUseCase
import com.example.mykku.event.application.port.output.EventRepository
import com.example.mykku.event.domain.entity.Event
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class ListEventsUseCaseImpl(
    private val eventRepository: EventRepository
) : ListEventsUseCase {

    @Transactional(readOnly = true)
    override fun execute(query: EventListQuery): PagedEventsResult {
        val pageable = PageableValidator.validateAndCreate(query.page, query.size)
        val now = LocalDateTime.now()

        val eventPage = eventRepository.findWithPagination(query.filter, query.sortType, pageable, now)
        val eventListResults = eventPage.content.map { toEventListResult(it, now) }
        return PagedEventsResult.of(eventPage, eventListResults)
    }

    private fun toEventListResult(event: Event, now: LocalDateTime): EventListResult {
        return EventListResult(
            id = event.id.value,
            title = event.title,
            subTitle = event.subTitle,
            description = event.description,
            startedAt = event.startedAt,
            expiredAt = event.expiredAt,
            status = event.resolveStatus(now),
            thumbnailUrl = event.thumbnailUrl
        )
    }
}
