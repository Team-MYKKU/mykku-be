package com.example.mykku.contest.adapter.output.persistence

import com.example.mykku.common.adapter.persistence.toCountMap
import com.example.mykku.contest.adapter.output.persistence.entity.ContestWinnerJpaEntity
import com.example.mykku.contest.adapter.output.persistence.repository.ContestJpaRepository
import com.example.mykku.contest.adapter.output.persistence.repository.ContestParticipationJpaRepository
import com.example.mykku.contest.adapter.output.persistence.repository.ContestWinnerJpaRepository
import com.example.mykku.contest.application.port.output.ContestWinnerRepository
import com.example.mykku.contest.domain.entity.ContestWinner
import com.example.mykku.contest.domain.vo.ContestId
import com.example.mykku.contest.domain.vo.ContestParticipationId
import com.example.mykku.contest.domain.vo.ContestWinnerId
import com.example.mykku.contest.exception.ContestException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class ContestWinnerRepositoryAdapter(
    private val contestWinnerJpaRepository: ContestWinnerJpaRepository,
    private val contestJpaRepository: ContestJpaRepository,
    private val contestParticipationJpaRepository: ContestParticipationJpaRepository
) : ContestWinnerRepository {

    override fun save(winner: ContestWinner): ContestWinner {
        val contestJpaEntity = contestJpaRepository.findById(winner.contestId.value)
            .orElseThrow { ContestException.contestNotFound() }
        val participationJpaEntity = contestParticipationJpaRepository.findById(winner.participationId.value)
            .orElseThrow { ContestException.participationNotFound() }

        val jpaEntity = ContestWinnerJpaEntity.fromDomain(winner, contestJpaEntity, participationJpaEntity)
        return contestWinnerJpaRepository.save(jpaEntity).toDomain()
    }

    override fun saveAll(winners: List<ContestWinner>): List<ContestWinner> {
        if (winners.isEmpty()) return emptyList()

        val contestIds = winners.map { it.contestId.value }.distinct()
        val participationIds = winners.map { it.participationId.value }.distinct()

        val contestJpaEntities = contestJpaRepository.findAllById(contestIds)
        val contestMap = contestJpaEntities.associateBy { it.id }

        val participationJpaEntities = contestParticipationJpaRepository.findAllByIdIn(participationIds)
        val participationMap = participationJpaEntities.associateBy { it.id }

        val jpaEntities = winners.map { winner ->
            val contestJpaEntity = contestMap[winner.contestId.value]
                ?: throw ContestException.contestNotFound()
            val participationJpaEntity = participationMap[winner.participationId.value]
                ?: throw ContestException.participationNotFound()
            ContestWinnerJpaEntity.fromDomain(winner, contestJpaEntity, participationJpaEntity)
        }

        return contestWinnerJpaRepository.saveAll(jpaEntities).map { it.toDomain() }
    }

    override fun findById(id: ContestWinnerId): ContestWinner? {
        return contestWinnerJpaRepository.findById(id.value)
            .map { it.toDomain() }
            .orElse(null)
    }

    override fun findByContestId(contestId: ContestId): List<ContestWinner> {
        val contestJpaEntity = contestJpaRepository.findById(contestId.value).orElse(null)
            ?: return emptyList()

        return contestWinnerJpaRepository.findByContest(contestJpaEntity)
            .map { it.toDomain() }
    }

    override fun findByContestIdAndMemberId(contestId: ContestId, memberId: Long): ContestWinner? {
        val contestJpaEntity = contestJpaRepository.findById(contestId.value).orElse(null)
            ?: return null
        return contestWinnerJpaRepository.findByContestAndMemberId(contestJpaEntity, memberId)
            ?.toDomain()
    }

    override fun findByContestIds(contestIds: List<ContestId>): List<ContestWinner> {
        if (contestIds.isEmpty()) return emptyList()

        val contestJpaEntities = contestJpaRepository.findAllById(contestIds.map { it.value })
        if (contestJpaEntities.isEmpty()) return emptyList()

        return contestWinnerJpaRepository.findByContestIn(contestJpaEntities)
            .map { it.toDomain() }
    }

    override fun findByMemberId(memberId: Long, pageable: Pageable): Page<ContestWinner> {
        return contestWinnerJpaRepository.findByMemberId(memberId, pageable)
            .map { it.toDomain() }
    }

    override fun findByMemberId(memberId: Long): List<ContestWinner> {
        return contestWinnerJpaRepository.findAllByMemberId(memberId)
            .map { it.toDomain() }
    }

    override fun findByMemberIdAndContestIds(memberId: Long, contestIds: List<ContestId>): List<ContestWinner> {
        if (contestIds.isEmpty()) return emptyList()

        val contestJpaEntities = contestJpaRepository.findAllById(contestIds.map { it.value })
        if (contestJpaEntities.isEmpty()) return emptyList()

        return contestWinnerJpaRepository.findByMemberIdAndContestIn(memberId, contestJpaEntities)
            .map { it.toDomain() }
    }

    override fun existsByContestId(contestId: ContestId): Boolean {
        val contestJpaEntity = contestJpaRepository.findById(contestId.value).orElse(null)
            ?: return false

        return contestWinnerJpaRepository.existsByContest(contestJpaEntity)
    }

    override fun deleteAllByContestId(contestId: ContestId) {
        val contestJpaEntity = contestJpaRepository.findById(contestId.value).orElse(null)
            ?: return

        contestWinnerJpaRepository.deleteAllByContest(contestJpaEntity)
    }

    override fun deleteAllByParticipationIds(participationIds: List<ContestParticipationId>) {
        if (participationIds.isEmpty()) return

        val participationJpaEntities = contestParticipationJpaRepository.findAllByIdIn(participationIds.map { it.value })
        if (participationJpaEntities.isEmpty()) return

        contestWinnerJpaRepository.deleteAllByParticipationIn(participationJpaEntities)
    }

    override fun countByContestIds(contestIds: List<ContestId>): Map<Long, Int> {
        if (contestIds.isEmpty()) return emptyMap()
        return contestWinnerJpaRepository.countGroupedByContestIdIn(contestIds.map { it.value }).toCountMap()
    }
}
