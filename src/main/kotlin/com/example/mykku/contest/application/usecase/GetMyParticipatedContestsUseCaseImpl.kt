package com.example.mykku.contest.application.usecase

import com.example.mykku.common.util.PageableValidator
import com.example.mykku.contest.application.dto.ContestListResult
import com.example.mykku.contest.application.dto.PagedContestsResult
import com.example.mykku.contest.application.port.input.GetMyParticipatedContestsUseCase
import com.example.mykku.contest.application.port.output.ContestParticipationRepository
import com.example.mykku.contest.application.port.output.ContestTagRepository
import com.example.mykku.contest.application.port.output.ContestWinnerRepository
import com.example.mykku.contest.domain.entity.Contest
import com.example.mykku.contest.domain.entity.ContestTag
import com.example.mykku.contest.domain.entity.ContestWinner
import com.example.mykku.contest.domain.vo.ContestStatusType
import com.example.mykku.contest.domain.vo.ContestWinnerStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class GetMyParticipatedContestsUseCaseImpl(
    private val contestParticipationRepository: ContestParticipationRepository,
    private val contestTagRepository: ContestTagRepository,
    private val contestWinnerRepository: ContestWinnerRepository
) : GetMyParticipatedContestsUseCase {

    @Transactional(readOnly = true)
    override fun execute(memberId: Long, page: Int, size: Int): PagedContestsResult {
        val pageable = PageableValidator.validateAndCreate(page, size)
        val contestPage = contestParticipationRepository.findContestsByMemberId(memberId, pageable)
        val contestIds = contestPage.content.map { it.id }
        val tagsByContestId = contestTagRepository.findByContestIds(contestIds)
            .groupBy { it.contestId.value }
        val winnersByContestId = contestWinnerRepository.findByMemberIdAndContestIds(memberId, contestIds)
            .associateBy { it.contestId.value }
        val now = LocalDateTime.now()
        val contestListResults = contestPage.content.map { contest ->
            toContestListResult(contest, tagsByContestId, winnersByContestId, now)
        }
        return PagedContestsResult.of(contestPage, contestListResults)
    }

    private fun toContestListResult(
        contest: Contest,
        tagsByContestId: Map<Long, List<ContestTag>>,
        winnersByContestId: Map<Long, ContestWinner>,
        now: LocalDateTime
    ): ContestListResult {
        val tags = tagsByContestId[contest.id.value] ?: emptyList()
        val winner = winnersByContestId[contest.id.value]

        return ContestListResult(
            id = contest.id.value,
            title = contest.title,
            startedAt = contest.startedAt,
            expiredAt = contest.expiredAt,
            status = contest.resolveStatus(now),
            thumbnailUrl = contest.thumbnailUrl,
            tags = tags.map { it.title },
            winnerStatus = resolveWinnerStatus(contest, winner),
            winnerRank = winner?.winnerRank
        )
    }

    private fun resolveWinnerStatus(contest: Contest, winner: ContestWinner?): ContestWinnerStatus {
        return when {
            winner != null -> ContestWinnerStatus.WON
            contest.status != ContestStatusType.WINNER_SELECTED -> ContestWinnerStatus.PENDING
            else -> ContestWinnerStatus.LOST
        }
    }
}
