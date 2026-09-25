package com.example.mykku.event.application.usecase

import com.example.mykku.event.application.dto.EventWinnerSelectionResult
import com.example.mykku.event.application.port.input.GetEventWinnerSelectionUseCase
import com.example.mykku.event.application.port.output.EventParticipationRepository
import com.example.mykku.event.application.port.output.EventRepository
import com.example.mykku.event.application.port.output.EventWinnerRepository
import com.example.mykku.event.domain.entity.EventWinner
import com.example.mykku.event.domain.vo.EventId
import com.example.mykku.event.exception.EventException
import com.example.mykku.member.application.port.output.MemberRepository
import com.example.mykku.member.domain.vo.MemberPk
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class GetEventWinnerSelectionUseCaseImpl(
    private val eventRepository: EventRepository,
    private val eventWinnerRepository: EventWinnerRepository,
    private val eventParticipationRepository: EventParticipationRepository,
    private val memberRepository: MemberRepository
) : GetEventWinnerSelectionUseCase {

    @Transactional(readOnly = true)
    override fun execute(eventId: Long): EventWinnerSelectionResult {
        val event = eventRepository.findById(EventId.of(eventId))
            ?: throw EventException.eventNotFound()
        val winners = eventWinnerRepository.findByEventId(event.id).sortedBy { it.id.value }
        val winnerMemberIds = findWinnerMemberIds(winners)
        val now = LocalDateTime.now()
        return EventWinnerSelectionResult(
            eventId = event.id.value,
            title = event.title,
            startedAt = event.startedAt,
            expiredAt = event.expiredAt,
            status = event.resolveStatus(now),
            winnerSelectable = !event.expiredAt.isAfter(now),
            winnerMemberIds = winnerMemberIds,
            withdrawnWinnerCount = winners.size - winnerMemberIds.size
        )
    }

    private fun findWinnerMemberIds(winners: List<EventWinner>): List<String> {
        val participationsById = eventParticipationRepository.findAllByIdIn(winners.map { it.participationId })
            .associateBy { it.id }
        val memberPks = winners.mapNotNull { participationsById[it.participationId]?.memberId }
        val membersByPk = memberRepository.findByIds(memberPks.map { MemberPk.of(it) })
            .associateBy { it.id.value }
        return memberPks.mapNotNull { membersByPk[it]?.memberId }
    }
}
