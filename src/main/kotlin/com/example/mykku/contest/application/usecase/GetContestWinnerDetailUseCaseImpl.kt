package com.example.mykku.contest.application.usecase

import com.example.mykku.contest.application.dto.ContestWinnerDetailResult
import com.example.mykku.contest.application.dto.WinnerDetailResult
import com.example.mykku.contest.application.port.input.GetContestWinnerDetailUseCase
import com.example.mykku.contest.application.port.output.ContestParticipationRepository
import com.example.mykku.contest.application.port.output.ContestRepository
import com.example.mykku.contest.application.port.output.ContestWinnerRepository
import com.example.mykku.contest.domain.entity.ContestParticipation
import com.example.mykku.contest.domain.entity.ContestWinner
import com.example.mykku.contest.domain.vo.ContestId
import com.example.mykku.contest.exception.ContestException
import com.example.mykku.feed.application.port.output.FeedImageRepository
import com.example.mykku.feed.application.port.output.FeedRepository
import com.example.mykku.feed.domain.entity.Feed
import com.example.mykku.feed.domain.entity.FeedImage
import com.example.mykku.feed.domain.vo.FeedId
import com.example.mykku.member.application.port.output.MemberRepository
import com.example.mykku.member.domain.entity.Member
import com.example.mykku.member.domain.vo.MemberPk
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GetContestWinnerDetailUseCaseImpl(
    private val contestRepository: ContestRepository,
    private val contestWinnerRepository: ContestWinnerRepository,
    private val contestParticipationRepository: ContestParticipationRepository,
    private val feedRepository: FeedRepository,
    private val feedImageRepository: FeedImageRepository,
    private val memberRepository: MemberRepository
) : GetContestWinnerDetailUseCase {

    @Transactional(readOnly = true)
    override fun execute(contestId: Long): ContestWinnerDetailResult {
        val contest = contestRepository.findById(ContestId.of(contestId))
            ?: throw ContestException.contestNotFound()

        val winners = contestWinnerRepository.findByContestId(contest.id)
        if (winners.isEmpty()) {
            return ContestWinnerDetailResult(contest.id.value, contest.title, emptyList())
        }

        val context = buildContext(winners)
        val winnerDetails = winners.sortedBy { it.winnerRank }
            .mapNotNull { toWinnerDetail(it, context) }

        return ContestWinnerDetailResult(contest.id.value, contest.title, winnerDetails)
    }

    private fun buildContext(winners: List<ContestWinner>): WinnerContext {
        val participationsMap = contestParticipationRepository
            .findAllByIdIn(winners.map { it.participationId })
            .associateBy { it.id.value }
        val feedIds = participationsMap.values.map { FeedId(it.feedId) }
        val feedsMap = feedRepository.findAllByIds(feedIds).associateBy { it.id!!.value }
        val feedImagesMap = feedImageRepository.findByFeedIds(feedIds).groupBy { it.feedId.value }
        val membersMap = resolveMembers(participationsMap.values)
        return WinnerContext(participationsMap, feedsMap, feedImagesMap, membersMap)
    }

    private fun resolveMembers(participations: Collection<ContestParticipation>): Map<Long, Member> =
        memberRepository.findByIds(participations.mapNotNull { it.memberId }.map { MemberPk.of(it) })
            .associateBy { it.id.value }

    private fun toWinnerDetail(winner: ContestWinner, context: WinnerContext): WinnerDetailResult? {
        val participation = context.participationsMap[winner.participationId.value]
            ?: return null
        val feed = context.feedsMap[participation.feedId]
        val feedImageUrl = feed?.id?.value?.let { context.feedImagesMap[it]?.firstOrNull()?.url }
        val member = participation.memberId?.let { context.membersMap[it] }

        return WinnerDetailResult(
            winnerId = winner.id.value,
            winnerRank = winner.winnerRank,
            awardTitle = winner.awardTitle,
            feedId = participation.feedId,
            feedTitle = feed?.title ?: "",
            feedImageUrl = feedImageUrl ?: "",
            authorNickname = member?.nickname ?: "",
            authorProfileImage = member?.profileImage ?: "",
            description = winner.description,
            acceptanceSpeech = winner.acceptanceSpeech
        )
    }

    private data class WinnerContext(
        val participationsMap: Map<Long, ContestParticipation>,
        val feedsMap: Map<Long, Feed>,
        val feedImagesMap: Map<Long, List<FeedImage>>,
        val membersMap: Map<Long, Member>
    )
}
