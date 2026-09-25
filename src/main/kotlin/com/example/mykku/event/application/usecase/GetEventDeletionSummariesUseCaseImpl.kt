package com.example.mykku.event.application.usecase

import com.example.mykku.event.application.dto.EventDeletionSummaryResult
import com.example.mykku.event.application.port.input.GetEventDeletionSummariesUseCase
import com.example.mykku.event.application.port.output.EventParticipationRepository
import com.example.mykku.event.application.port.output.EventWinnerRepository
import com.example.mykku.event.domain.vo.EventId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GetEventDeletionSummariesUseCaseImpl(
    private val eventParticipationRepository: EventParticipationRepository,
    private val eventWinnerRepository: EventWinnerRepository
) : GetEventDeletionSummariesUseCase {

    @Transactional(readOnly = true)
    override fun execute(eventIds: List<Long>): Map<Long, EventDeletionSummaryResult> {
        val ids = eventIds.map { EventId.of(it) }
        val participationCounts = eventParticipationRepository.countByEventIds(ids)
        val winnerCounts = eventWinnerRepository.countByEventIds(ids)
        return eventIds.associateWith {
            EventDeletionSummaryResult(participationCounts[it] ?: 0, winnerCounts[it] ?: 0)
        }
    }
}
