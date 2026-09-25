package com.example.mykku.event.application.port.output

import com.example.mykku.event.domain.entity.EventWinner
import com.example.mykku.event.domain.vo.EventId
import com.example.mykku.event.domain.vo.EventParticipationId
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface EventWinnerRepository {
    fun save(winner: EventWinner): EventWinner
    fun saveAll(winners: List<EventWinner>): List<EventWinner>
    fun findByEventId(eventId: EventId): List<EventWinner>
    fun findByEventIdAndMemberId(eventId: EventId, memberId: Long): EventWinner?
    fun findByMemberId(memberId: Long, pageable: Pageable): Page<EventWinner>
    fun findByMemberIdAndEventIds(memberId: Long, eventIds: List<EventId>): List<EventWinner>
    fun deleteAllByEventId(eventId: EventId)
    fun deleteAllByParticipationIds(participationIds: List<EventParticipationId>)
}
