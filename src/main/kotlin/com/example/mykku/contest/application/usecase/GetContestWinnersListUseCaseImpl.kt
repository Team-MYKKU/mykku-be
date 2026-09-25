package com.example.mykku.contest.application.usecase

import com.example.mykku.contest.application.dto.ContestWinnerPreviewResult
import com.example.mykku.contest.application.dto.ContestWinnersListResult
import com.example.mykku.contest.application.dto.WinnerThumbnailResult
import com.example.mykku.contest.application.port.input.GetContestWinnersListUseCase
import com.example.mykku.contest.application.port.output.ContestParticipationRepository
import com.example.mykku.contest.application.port.output.ContestRepository
import com.example.mykku.contest.application.port.output.ContestWinnerRepository
import com.example.mykku.contest.domain.entity.Contest
import com.example.mykku.contest.domain.entity.ContestWinner
import com.example.mykku.contest.domain.vo.ContestStatusType
import com.example.mykku.feed.application.port.output.FeedImageRepository
import com.example.mykku.feed.domain.vo.FeedId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GetContestWinnersListUseCaseImpl(
    private val contestRepository: ContestRepository,
    private val contestWinnerRepository: ContestWinnerRepository,
    private val contestParticipationRepository: ContestParticipationRepository,
    private val feedImageRepository: FeedImageRepository
) : GetContestWinnersListUseCase {

    @Transactional(readOnly = true)
    override fun execute(): ContestWinnersListResult {
        val contests = contestRepository.findByStatus(ContestStatusType.WINNER_SELECTED)

        if (contests.isEmpty()) {
            return ContestWinnersListResult(emptyList())
        }

        val winnersByContestId = contestWinnerRepository.findByContestIds(contests.map { it.id })
            .groupBy { it.contestId.value }
        val feedImageByParticipationId = resolveFeedImages(winnersByContestId.values.flatten())

        val contestPreviews = contests.mapNotNull { contest ->
            val winners = winnersByContestId[contest.id.value] ?: return@mapNotNull null
            toPreview(contest, winners, feedImageByParticipationId)
        }

        return ContestWinnersListResult(contestPreviews)
    }

    private fun resolveFeedImages(winners: List<ContestWinner>): Map<Long, String?> {
        val participationIds = winners.map { it.participationId }
        val participations = contestParticipationRepository.findAllByIdIn(participationIds)
        val feedIds = participations.map { FeedId(it.feedId) }
        val feedImagesMap = feedImageRepository.findByFeedIds(feedIds)
            .groupBy { it.feedId.value }
        return participations.associate {
            it.id.value to feedImagesMap[it.feedId]?.firstOrNull()?.url
        }
    }

    private fun toPreview(
        contest: Contest,
        winners: List<ContestWinner>,
        feedImageByParticipationId: Map<Long, String?>
    ): ContestWinnerPreviewResult {
        return ContestWinnerPreviewResult(
            contestId = contest.id.value,
            contestTitle = contest.title,
            startedAt = contest.startedAt,
            expiredAt = contest.expiredAt,
            winners = winners.sortedBy { it.winnerRank }.map { winner ->
                WinnerThumbnailResult(
                    winnerId = winner.id.value,
                    winnerRank = winner.winnerRank,
                    feedImageUrl = feedImageByParticipationId[winner.participationId.value] ?: contest.thumbnailUrl
                )
            }
        )
    }
}
