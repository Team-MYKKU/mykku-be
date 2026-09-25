package com.example.mykku.event.adapter.output.persistence

import com.example.mykku.common.adapter.persistence.toCountMap
import com.example.mykku.event.adapter.output.persistence.entity.EventParticipationJpaEntity
import com.example.mykku.event.adapter.output.persistence.repository.EventJpaRepository
import com.example.mykku.event.adapter.output.persistence.repository.EventParticipationJpaRepository
import com.example.mykku.event.application.port.output.EventParticipationRepository
import com.example.mykku.event.domain.entity.Event
import com.example.mykku.event.domain.entity.EventParticipation
import com.example.mykku.event.domain.vo.EventId
import com.example.mykku.event.domain.vo.EventParticipationId
import com.example.mykku.event.exception.EventException
import com.example.mykku.member.adapter.output.persistence.MemberJpaRepository
import com.example.mykku.member.exception.MemberException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class EventParticipationRepositoryAdapter(
    private val eventParticipationJpaRepository: EventParticipationJpaRepository,
    private val eventJpaRepository: EventJpaRepository,
    private val memberJpaRepository: MemberJpaRepository
) : EventParticipationRepository {

    override fun save(participation: EventParticipation): EventParticipation {
        val eventJpaEntity = eventJpaRepository.findById(participation.eventId.value)
            .orElseThrow { EventException.eventNotFound() }
        val memberJpaEntity = memberJpaRepository.findById(participation.memberId!!)
            .orElseThrow { MemberException.memberNotFound() }

        val entity = EventParticipationJpaEntity.fromDomain(participation, eventJpaEntity, memberJpaEntity)
        return eventParticipationJpaRepository.save(entity).toDomain()
    }

    override fun findByEventId(eventId: EventId, pageable: Pageable): Page<EventParticipation> {
        val eventJpaEntity = eventJpaRepository.findById(eventId.value)
            .orElseThrow { EventException.eventNotFound() }
        return eventParticipationJpaRepository.findByEvent(eventJpaEntity, pageable)
            .map { it.toDomain() }
    }

    override fun findEventsByMemberId(memberId: Long, pageable: Pageable): Page<Event> {
        val memberJpaEntity = memberJpaRepository.findById(memberId)
            .orElseThrow { MemberException.memberNotFound() }
        return eventParticipationJpaRepository.findEventsByMember(memberJpaEntity, pageable)
            .map { it.toDomain() }
    }

    override fun findByMemberIdAndEventIds(memberId: Long, eventIds: List<EventId>): List<EventParticipation> {
        if (eventIds.isEmpty()) {
            return emptyList()
        }

        val memberJpaEntity = memberJpaRepository.findById(memberId).orElse(null)
            ?: return emptyList()

        val eventEntities = eventJpaRepository.findAllById(eventIds.map { it.value })
        if (eventEntities.isEmpty()) {
            return emptyList()
        }

        return eventParticipationJpaRepository.findByMemberAndEventIn(memberJpaEntity, eventEntities)
            .map { it.toDomain() }
    }

    override fun findAllByIdIn(ids: List<EventParticipationId>): List<EventParticipation> {
        if (ids.isEmpty()) {
            return emptyList()
        }
        return eventParticipationJpaRepository.findAllByIdIn(ids.map { it.value })
            .map { it.toDomain() }
    }

    override fun findAllByEventId(eventId: EventId): List<EventParticipation> {
        val eventJpaEntity = eventJpaRepository.findById(eventId.value).orElse(null)
            ?: return emptyList()
        return eventParticipationJpaRepository.findAllByEventOrderByIdAsc(eventJpaEntity)
            .map { it.toDomain() }
    }

    override fun deleteAllByIdIn(ids: List<EventParticipationId>) {
        if (ids.isEmpty()) return
        eventParticipationJpaRepository.deleteAllByIdIn(ids.map { it.value })
        eventParticipationJpaRepository.flush()
    }

    override fun existsByMemberIdAndEventId(memberId: Long, eventId: EventId): Boolean {
        val memberJpaEntity = memberJpaRepository.findById(memberId).orElse(null)
            ?: return false
        val eventJpaEntity = eventJpaRepository.findById(eventId.value).orElse(null)
            ?: return false
        return eventParticipationJpaRepository.existsByMemberAndEvent(memberJpaEntity, eventJpaEntity)
    }

    override fun countByEventId(eventId: EventId): Long {
        val eventJpaEntity = eventJpaRepository.findById(eventId.value).orElse(null)
            ?: return 0L
        return eventParticipationJpaRepository.countByEvent(eventJpaEntity)
    }

    override fun countByEventIds(eventIds: List<EventId>): Map<Long, Int> {
        if (eventIds.isEmpty()) return emptyMap()
        return eventParticipationJpaRepository.countGroupedByEventIdIn(eventIds.map { it.value }).toCountMap()
    }
}
