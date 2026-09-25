package com.example.mykku.event.adapter.output.persistence

import com.example.mykku.event.adapter.output.persistence.entity.EventWinnerJpaEntity
import com.example.mykku.event.adapter.output.persistence.repository.EventJpaRepository
import com.example.mykku.event.adapter.output.persistence.repository.EventParticipationJpaRepository
import com.example.mykku.event.adapter.output.persistence.repository.EventWinnerJpaRepository
import com.example.mykku.event.application.port.output.EventWinnerRepository
import com.example.mykku.event.domain.entity.EventWinner
import com.example.mykku.event.domain.vo.EventId
import com.example.mykku.event.domain.vo.EventParticipationId
import com.example.mykku.event.exception.EventException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class EventWinnerRepositoryAdapter(
    private val eventWinnerJpaRepository: EventWinnerJpaRepository,
    private val eventJpaRepository: EventJpaRepository,
    private val eventParticipationJpaRepository: EventParticipationJpaRepository
) : EventWinnerRepository {

    override fun save(winner: EventWinner): EventWinner {
        val eventJpaEntity = eventJpaRepository.findById(winner.eventId.value)
            .orElseThrow { EventException.eventNotFound() }
        val participationJpaEntity = eventParticipationJpaRepository.findById(winner.participationId.value)
            .orElseThrow { EventException.eventParticipationNotFound() }

        val jpaEntity = EventWinnerJpaEntity.fromDomain(winner, eventJpaEntity, participationJpaEntity)
        return eventWinnerJpaRepository.save(jpaEntity).toDomain()
    }

    override fun saveAll(winners: List<EventWinner>): List<EventWinner> {
        if (winners.isEmpty()) return emptyList()

        val eventMap = eventJpaRepository.findAllById(winners.map { it.eventId.value }.distinct())
            .associateBy { it.id }
        val participationMap = eventParticipationJpaRepository
            .findAllByIdIn(winners.map { it.participationId.value }.distinct())
            .associateBy { it.id }

        val jpaEntities = winners.map { winner ->
            val eventJpaEntity = eventMap[winner.eventId.value]
                ?: throw EventException.eventNotFound()
            val participationJpaEntity = participationMap[winner.participationId.value]
                ?: throw EventException.eventParticipationNotFound()
            EventWinnerJpaEntity.fromDomain(winner, eventJpaEntity, participationJpaEntity)
        }

        return eventWinnerJpaRepository.saveAll(jpaEntities).map { it.toDomain() }
    }

    override fun findByEventId(eventId: EventId): List<EventWinner> {
        val eventJpaEntity = eventJpaRepository.findById(eventId.value).orElse(null)
            ?: return emptyList()
        return eventWinnerJpaRepository.findByEvent(eventJpaEntity).map { it.toDomain() }
    }

    override fun findByEventIdAndMemberId(eventId: EventId, memberId: Long): EventWinner? {
        val eventJpaEntity = eventJpaRepository.findById(eventId.value).orElse(null)
            ?: return null
        return eventWinnerJpaRepository.findByEventAndMemberId(eventJpaEntity, memberId)?.toDomain()
    }

    override fun findByMemberId(memberId: Long, pageable: Pageable): Page<EventWinner> {
        return eventWinnerJpaRepository.findByMemberId(memberId, pageable).map { it.toDomain() }
    }

    override fun findByMemberIdAndEventIds(memberId: Long, eventIds: List<EventId>): List<EventWinner> {
        if (eventIds.isEmpty()) return emptyList()

        val eventJpaEntities = eventJpaRepository.findAllById(eventIds.map { it.value })
        if (eventJpaEntities.isEmpty()) return emptyList()

        return eventWinnerJpaRepository.findByMemberIdAndEventIn(memberId, eventJpaEntities).map { it.toDomain() }
    }

    override fun deleteAllByEventId(eventId: EventId) {
        val eventJpaEntity = eventJpaRepository.findById(eventId.value).orElse(null)
            ?: return
        eventWinnerJpaRepository.deleteAllByEvent(eventJpaEntity)
        eventWinnerJpaRepository.flush()
    }

    override fun deleteAllByParticipationIds(participationIds: List<EventParticipationId>) {
        if (participationIds.isEmpty()) return
        val participations = eventParticipationJpaRepository.findAllByIdIn(participationIds.map { it.value })
        if (participations.isEmpty()) return
        eventWinnerJpaRepository.deleteAllByParticipationIn(participations)
        eventWinnerJpaRepository.flush()
    }
}
