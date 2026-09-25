package com.example.mykku.contest.adapter.output.persistence

import com.example.mykku.common.adapter.persistence.toCountMap
import com.example.mykku.contest.adapter.output.persistence.entity.ContestParticipationJpaEntity
import com.example.mykku.contest.adapter.output.persistence.repository.ContestJpaRepository
import com.example.mykku.contest.adapter.output.persistence.repository.ContestParticipationJpaRepository
import com.example.mykku.contest.application.port.output.ContestParticipationRepository
import com.example.mykku.contest.domain.entity.Contest
import com.example.mykku.contest.domain.entity.ContestParticipation
import com.example.mykku.contest.domain.vo.ContestId
import com.example.mykku.contest.domain.vo.ContestParticipationId
import com.example.mykku.contest.exception.ContestException
import com.example.mykku.feed.adapter.output.persistence.FeedJpaRepository
import com.example.mykku.feed.exception.FeedException
import com.example.mykku.member.adapter.output.persistence.MemberJpaRepository
import com.example.mykku.member.exception.MemberException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class ContestParticipationRepositoryAdapter(
    private val contestParticipationJpaRepository: ContestParticipationJpaRepository,
    private val contestJpaRepository: ContestJpaRepository,
    private val memberJpaRepository: MemberJpaRepository,
    private val feedJpaRepository: FeedJpaRepository
) : ContestParticipationRepository {

    override fun save(participation: ContestParticipation): ContestParticipation {
        val contestJpaEntity = contestJpaRepository.findById(participation.contestId.value)
            .orElseThrow { ContestException.contestNotFound() }
        val memberJpaEntity = memberJpaRepository.findById(participation.memberId!!)
            .orElseThrow { MemberException.memberNotFound() }
        val feedJpaEntity = feedJpaRepository.findById(participation.feedId)
            .orElseThrow { FeedException.feedNotFound() }

        val jpaEntity = ContestParticipationJpaEntity.fromDomain(
            participation,
            contestJpaEntity,
            memberJpaEntity,
            feedJpaEntity
        )
        return contestParticipationJpaRepository.save(jpaEntity).toDomain()
    }

    override fun findById(id: ContestParticipationId): ContestParticipation? {
        return contestParticipationJpaRepository.findById(id.value)
            .map { it.toDomain() }
            .orElse(null)
    }

    override fun findAllByIdIn(ids: List<ContestParticipationId>): List<ContestParticipation> {
        if (ids.isEmpty()) return emptyList()

        return contestParticipationJpaRepository.findAllByIdIn(ids.map { it.value })
            .map { it.toDomain() }
    }

    override fun findByContestId(contestId: ContestId, pageable: Pageable): Page<ContestParticipation> {
        val contestJpaEntity = contestJpaRepository.findById(contestId.value)
            .orElseThrow { ContestException.contestNotFound() }

        return contestParticipationJpaRepository.findByContest(contestJpaEntity, pageable)
            .map { it.toDomain() }
    }

    override fun findByFeedId(feedId: Long): List<ContestParticipation> {
        val feedJpaEntity = feedJpaRepository.findById(feedId).orElse(null)
            ?: return emptyList()

        return contestParticipationJpaRepository.findByFeed(feedJpaEntity)
            .map { it.toDomain() }
    }

    override fun findContestsByMemberId(memberId: Long, pageable: Pageable): Page<Contest> {
        val memberJpaEntity = memberJpaRepository.findById(memberId).orElse(null)
            ?: return Page.empty(pageable)

        return contestParticipationJpaRepository.findContestsByMember(memberJpaEntity, pageable)
            .map { it.toDomain() }
    }

    override fun findByMemberIdAndContestIds(memberId: Long, contestIds: List<ContestId>): List<ContestParticipation> {
        if (contestIds.isEmpty()) return emptyList()

        val memberJpaEntity = memberJpaRepository.findById(memberId).orElse(null)
            ?: return emptyList()

        val contestJpaEntities = contestJpaRepository.findAllById(contestIds.map { it.value })
        if (contestJpaEntities.isEmpty()) return emptyList()

        return contestParticipationJpaRepository.findByMemberAndContestIn(memberJpaEntity, contestJpaEntities)
            .map { it.toDomain() }
    }

    override fun existsByMemberIdAndContestId(memberId: Long, contestId: ContestId): Boolean {
        val memberJpaEntity = memberJpaRepository.findById(memberId).orElse(null)
            ?: return false

        val contestJpaEntity = contestJpaRepository.findById(contestId.value).orElse(null)
            ?: return false

        return contestParticipationJpaRepository.existsByMemberAndContest(memberJpaEntity, contestJpaEntity)
    }

    override fun existsByMemberIdAndContestIdAndFeedId(memberId: Long, contestId: ContestId, feedId: Long): Boolean {
        val memberJpaEntity = memberJpaRepository.findById(memberId).orElse(null)
            ?: return false

        val contestJpaEntity = contestJpaRepository.findById(contestId.value).orElse(null)
            ?: return false

        val feedJpaEntity = feedJpaRepository.findById(feedId).orElse(null)
            ?: return false

        return contestParticipationJpaRepository.existsByMemberAndContestAndFeed(
            memberJpaEntity,
            contestJpaEntity,
            feedJpaEntity
        )
    }

    override fun countByContestId(contestId: ContestId): Long {
        val contestJpaEntity = contestJpaRepository.findById(contestId.value).orElse(null)
            ?: return 0L

        return contestParticipationJpaRepository.countByContest(contestJpaEntity)
    }

    override fun deleteAll(participations: List<ContestParticipation>) {
        if (participations.isEmpty()) return

        val ids = participations.map { it.id.value }
        val jpaEntities = contestParticipationJpaRepository.findAllByIdIn(ids)
        contestParticipationJpaRepository.deleteAll(jpaEntities)
    }

    override fun countByContestIds(contestIds: List<ContestId>): Map<Long, Int> {
        if (contestIds.isEmpty()) return emptyMap()
        return contestParticipationJpaRepository.countGroupedByContestIdIn(contestIds.map { it.value }).toCountMap()
    }
}
