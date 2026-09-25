package com.example.mykku.contest.application.usecase

import com.example.mykku.common.util.PageableValidator
import com.example.mykku.contest.application.dto.ContestParticipantResult
import com.example.mykku.contest.application.dto.ContestParticipantsHeaderResult
import com.example.mykku.contest.application.dto.ContestParticipantsResult
import com.example.mykku.contest.application.dto.CurrentContestWinnerResult
import com.example.mykku.contest.application.dto.GetContestParticipantsQuery
import com.example.mykku.contest.application.dto.PagedContestParticipantsResult
import com.example.mykku.contest.application.port.input.GetContestParticipantsUseCase
import com.example.mykku.contest.application.port.output.ContestParticipationRepository
import com.example.mykku.contest.application.port.output.ContestRepository
import com.example.mykku.contest.application.port.output.ContestTagRepository
import com.example.mykku.contest.application.port.output.ContestWinnerRepository
import com.example.mykku.contest.domain.entity.Contest
import com.example.mykku.contest.domain.entity.ContestParticipation
import com.example.mykku.contest.domain.entity.ContestWinner
import com.example.mykku.contest.domain.vo.ContestId
import com.example.mykku.contest.exception.ContestException
import com.example.mykku.feed.application.port.output.FeedImageRepository
import com.example.mykku.feed.application.port.output.FeedRepository
import com.example.mykku.feed.domain.vo.FeedId
import com.example.mykku.like.application.port.output.LikeFeedPort
import com.example.mykku.member.application.port.output.MemberRepository
import com.example.mykku.member.domain.entity.Member
import com.example.mykku.member.domain.vo.MemberPk
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class GetContestParticipantsUseCaseImpl(
    private val contestRepository: ContestRepository,
    private val contestTagRepository: ContestTagRepository,
    private val contestParticipationRepository: ContestParticipationRepository,
    private val contestWinnerRepository: ContestWinnerRepository,
    private val feedRepository: FeedRepository,
    private val feedImageRepository: FeedImageRepository,
    private val memberRepository: MemberRepository,
    private val likeFeedPort: LikeFeedPort
) : GetContestParticipantsUseCase {

    @Transactional(readOnly = true)
    override fun execute(query: GetContestParticipantsQuery): ContestParticipantsResult {
        val contest = contestRepository.findById(ContestId.of(query.contestId))
            ?: throw ContestException.contestNotFound()
        val pageable = PageableValidator.validateAndCreate(query.page, query.size, PARTICIPATION_SORT)
        val participationPage = contestParticipationRepository.findByContestId(contest.id, pageable)
        val winners = contestWinnerRepository.findByContestId(contest.id).sortedBy { it.winnerRank }
        val winnerParticipations = contestParticipationRepository.findAllByIdIn(winners.map { it.participationId })
        val details = loadDetails(participationPage.content + winnerParticipations)
        val participants = participationPage.content.map { toParticipantResult(it, contest, details) }
        return ContestParticipantsResult(
            contest = toHeaderResult(contest),
            currentWinners = toCurrentWinnerResults(winners, winnerParticipations, details),
            participants = PagedContestParticipantsResult.of(participationPage, participants)
        )
    }

    private fun loadDetails(participations: List<ContestParticipation>): ParticipationDetails {
        val feedIds = participations.map { it.feedId }.distinct()
        val memberPks = participations.mapNotNull { it.memberId }.distinct().map { MemberPk.of(it) }
        return ParticipationDetails(
            feedTitles = feedRepository.findAllByIds(feedIds.map { FeedId.of(it) })
                .associate { it.id!!.value to it.title },
            imageUrls = feedImageRepository.findThumbnailUrlsByFeedIds(feedIds.map { FeedId.of(it) })
                .mapKeys { it.key.value },
            members = memberRepository.findByIds(memberPks).associateBy { it.id.value },
            likeCounts = likeFeedPort.countByFeedIdIn(feedIds)
        )
    }

    private fun toHeaderResult(contest: Contest): ContestParticipantsHeaderResult {
        val now = LocalDateTime.now()
        return ContestParticipantsHeaderResult(
            id = contest.id.value,
            title = contest.title,
            startedAt = contest.startedAt,
            expiredAt = contest.expiredAt,
            status = contest.resolveStatus(now),
            tags = contestTagRepository.findByContestIds(listOf(contest.id)).map { it.title },
            winnerSelectable = !contest.expiredAt.isAfter(now)
        )
    }

    private fun toCurrentWinnerResults(
        winners: List<ContestWinner>,
        participations: List<ContestParticipation>,
        details: ParticipationDetails
    ): List<CurrentContestWinnerResult> {
        val participationsById = participations.associateBy { it.id.value }
        return winners.map { winner ->
            val participation = participationsById[winner.participationId.value]
            CurrentContestWinnerResult(
                participationId = winner.participationId.value,
                winnerRank = winner.winnerRank,
                awardTitle = winner.awardTitle,
                description = winner.description,
                feedTitle = participation?.let { details.feedTitles[it.feedId] } ?: "",
                authorMemberId = participation?.memberId?.let { details.members[it]?.memberId }
            )
        }
    }

    private fun toParticipantResult(
        participation: ContestParticipation,
        contest: Contest,
        details: ParticipationDetails
    ): ContestParticipantResult {
        val author = participation.memberId?.let { details.members[it] }
        return ContestParticipantResult(
            participationId = participation.id.value,
            feedId = participation.feedId,
            feedTitle = details.feedTitles[participation.feedId] ?: "",
            imageUrl = details.imageUrls[participation.feedId],
            authorMemberId = author?.memberId,
            authorNickname = author?.nickname,
            authorWithdrawn = author == null,
            likeCount = details.likeCounts[participation.feedId] ?: 0,
            participatedAt = participation.createdAt,
            participatedBeforeStart = participation.createdAt.isBefore(contest.startedAt)
        )
    }

    companion object {
        private val PARTICIPATION_SORT = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))
    }
}

private data class ParticipationDetails(
    val feedTitles: Map<Long, String>,
    val imageUrls: Map<Long, String>,
    val members: Map<Long, Member>,
    val likeCounts: Map<Long, Int>
)
