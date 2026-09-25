package com.example.mykku.contest.application.usecase

import com.example.mykku.contest.application.dto.ContestDeletionSummaryResult
import com.example.mykku.contest.application.port.input.GetContestDeletionSummariesUseCase
import com.example.mykku.contest.application.port.output.ContestParticipationRepository
import com.example.mykku.contest.application.port.output.ContestWinnerRepository
import com.example.mykku.contest.domain.vo.ContestId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GetContestDeletionSummariesUseCaseImpl(
    private val contestParticipationRepository: ContestParticipationRepository,
    private val contestWinnerRepository: ContestWinnerRepository
) : GetContestDeletionSummariesUseCase {

    @Transactional(readOnly = true)
    override fun execute(contestIds: List<Long>): Map<Long, ContestDeletionSummaryResult> {
        val ids = contestIds.map { ContestId.of(it) }
        val participationCounts = contestParticipationRepository.countByContestIds(ids)
        val winnerCounts = contestWinnerRepository.countByContestIds(ids)
        return contestIds.associateWith {
            ContestDeletionSummaryResult(participationCounts[it] ?: 0, winnerCounts[it] ?: 0)
        }
    }
}
