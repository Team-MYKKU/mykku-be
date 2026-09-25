package com.example.mykku.event.adapter.output.persistence

import com.example.mykku.event.adapter.output.persistence.entity.EventJpaEntity
import com.example.mykku.event.adapter.output.persistence.repository.EventJpaRepository
import com.example.mykku.event.application.port.output.EventRepository
import com.example.mykku.event.domain.entity.Event
import com.example.mykku.event.domain.vo.EventId
import com.example.mykku.event.domain.vo.EventListFilter
import com.example.mykku.event.domain.vo.EventSortType
import com.example.mykku.event.domain.vo.EventStatusType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class EventRepositoryAdapter(
    private val eventJpaRepository: EventJpaRepository
) : EventRepository {

    override fun save(event: Event): Event {
        val entity = if (event.id.value == 0L) {
            EventJpaEntity.fromDomain(event)
        } else {
            eventJpaRepository.findById(event.id.value)
                .map { it.apply { updateFromDomain(event) } }
                .orElseGet { EventJpaEntity.fromDomain(event) }
        }
        return eventJpaRepository.save(entity).toDomain()
    }

    override fun findById(id: EventId): Event? {
        return eventJpaRepository.findById(id.value).orElse(null)?.toDomain()
    }

    override fun findAllByIds(ids: List<EventId>): List<Event> {
        if (ids.isEmpty()) {
            return emptyList()
        }
        return eventJpaRepository.findAllById(ids.map { it.value }).map { it.toDomain() }
    }

    override fun findByExpiredAtAfter(dateTime: LocalDateTime): List<Event> {
        return eventJpaRepository.findByExpiredAtAfter(dateTime).map { it.toDomain() }
    }

    override fun findWithPagination(
        filter: EventListFilter,
        sortType: EventSortType,
        pageable: Pageable,
        currentTime: LocalDateTime
    ): Page<Event> {
        val page = when (filter) {
            EventListFilter.ALL -> eventJpaRepository.findAllByOrderByCreatedAtDesc(pageable)
            EventListFilter.ACTIVE -> findActiveEvents(sortType, currentTime, pageable)
            EventListFilter.EXPIRED ->
                eventJpaRepository.findByExpiredAtLessThanEqualOrderByCreatedAtDesc(currentTime, pageable)
            EventListFilter.PENDING_SELECTION -> eventJpaRepository
                .findByExpiredAtLessThanEqualAndStatusNotOrderByCreatedAtDesc(currentTime, WINNER_SELECTED, pageable)
            EventListFilter.WINNER_SELECTED ->
                eventJpaRepository.findByStatusOrderByCreatedAtDesc(WINNER_SELECTED, pageable)
        }
        return page.map { it.toDomain() }
    }

    private fun findActiveEvents(
        sortType: EventSortType,
        currentTime: LocalDateTime,
        pageable: Pageable
    ): Page<EventJpaEntity> {
        return when (sortType) {
            EventSortType.LATEST -> eventJpaRepository.findByExpiredAtAfterOrderByCreatedAtDesc(currentTime, pageable)
            EventSortType.OLDEST -> eventJpaRepository.findByExpiredAtAfterOrderByCreatedAtAsc(currentTime, pageable)
            EventSortType.POPULAR -> eventJpaRepository.findActiveEventsByPopular(currentTime, pageable)
        }
    }

    override fun deleteById(id: EventId) {
        eventJpaRepository.deleteEventById(id.value)
    }

    companion object {
        private val WINNER_SELECTED = EventStatusType.WINNER_SELECTED
    }
}
