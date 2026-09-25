package com.example.mykku.event.application.port.output

import com.example.mykku.event.domain.entity.Event
import com.example.mykku.event.domain.entity.EventParticipation
import com.example.mykku.event.domain.vo.EventId
import com.example.mykku.event.domain.vo.EventParticipationId
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface EventParticipationRepository {
    fun save(participation: EventParticipation): EventParticipation
    fun findByEventId(eventId: EventId, pageable: Pageable): Page<EventParticipation>
    fun findEventsByMemberId(memberId: Long, pageable: Pageable): Page<Event>
    fun findByMemberIdAndEventIds(memberId: Long, eventIds: List<EventId>): List<EventParticipation>
    fun findAllByIdIn(ids: List<EventParticipationId>): List<EventParticipation>
    fun findAllByEventId(eventId: EventId): List<EventParticipation>
    fun deleteAllByIdIn(ids: List<EventParticipationId>)
    fun existsByMemberIdAndEventId(memberId: Long, eventId: EventId): Boolean
    fun countByEventId(eventId: EventId): Long
}
