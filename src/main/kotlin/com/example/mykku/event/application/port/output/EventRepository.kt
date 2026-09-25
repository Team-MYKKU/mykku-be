package com.example.mykku.event.application.port.output

import com.example.mykku.event.domain.entity.Event
import com.example.mykku.event.domain.vo.EventId
import com.example.mykku.event.domain.vo.EventListFilter
import com.example.mykku.event.domain.vo.EventSortType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.LocalDateTime

interface EventRepository {
    fun save(event: Event): Event
    fun findById(id: EventId): Event?
    fun findAllByIds(ids: List<EventId>): List<Event>
    fun findByExpiredAtAfter(dateTime: LocalDateTime): List<Event>
    fun findWithPagination(
        filter: EventListFilter,
        sortType: EventSortType,
        pageable: Pageable,
        currentTime: LocalDateTime
    ): Page<Event>
    fun deleteById(id: EventId)
}
