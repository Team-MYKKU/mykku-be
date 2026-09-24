package com.example.mykku.contest.adapter.output.persistence

import com.example.mykku.contest.adapter.output.persistence.entity.ContestJpaEntity
import com.example.mykku.contest.adapter.output.persistence.repository.ContestJpaRepository
import com.example.mykku.contest.application.port.output.ContestRepository
import com.example.mykku.contest.domain.entity.Contest
import com.example.mykku.contest.domain.vo.ContestId
import com.example.mykku.contest.domain.vo.ContestListFilter
import com.example.mykku.contest.domain.vo.ContestSortType
import com.example.mykku.contest.domain.vo.ContestStatusType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class ContestRepositoryAdapter(
    private val contestJpaRepository: ContestJpaRepository
) : ContestRepository {

    override fun save(contest: Contest): Contest {
        val jpaEntity = if (contest.id.value == 0L) {
            ContestJpaEntity.fromDomain(contest)
        } else {
            contestJpaRepository.findById(contest.id.value)
                .map { it.apply { updateFromDomain(contest) } }
                .orElseGet { ContestJpaEntity.fromDomain(contest) }
        }
        return contestJpaRepository.save(jpaEntity).toDomain()
    }

    override fun findById(id: ContestId): Contest? {
        return contestJpaRepository.findById(id.value)
            .map { it.toDomain() }
            .orElse(null)
    }

    override fun findAllByIds(ids: List<ContestId>): List<Contest> {
        if (ids.isEmpty()) return emptyList()
        return contestJpaRepository.findAllById(ids.map { it.value })
            .map { it.toDomain() }
    }

    override fun findByStatus(status: ContestStatusType): List<Contest> {
        return contestJpaRepository.findByStatus(status)
            .map { it.toDomain() }
    }

    override fun findByExpiredAtAfter(dateTime: LocalDateTime): List<Contest> {
        return contestJpaRepository.findByExpiredAtAfter(dateTime)
            .map { it.toDomain() }
    }

    override fun findByStatusAndExpiredAtAfter(status: ContestStatusType, dateTime: LocalDateTime): List<Contest> {
        return contestJpaRepository.findByStatusAndExpiredAtAfter(status, dateTime)
            .map { it.toDomain() }
    }

    override fun findWithPagination(
        filter: ContestListFilter,
        sortType: ContestSortType,
        pageable: Pageable,
        currentTime: LocalDateTime
    ): Page<Contest> {
        val page = when (filter) {
            ContestListFilter.ALL -> contestJpaRepository.findAllByOrderByCreatedAtDesc(pageable)
            ContestListFilter.ACTIVE -> findActiveContests(sortType, currentTime, pageable)
            ContestListFilter.EXPIRED ->
                contestJpaRepository.findByExpiredAtLessThanEqualOrderByCreatedAtDesc(currentTime, pageable)
            ContestListFilter.PENDING_SELECTION -> contestJpaRepository
                .findByExpiredAtLessThanEqualAndStatusNotOrderByCreatedAtDesc(currentTime, WINNER_SELECTED, pageable)
            ContestListFilter.WINNER_SELECTED ->
                contestJpaRepository.findByStatusOrderByCreatedAtDesc(WINNER_SELECTED, pageable)
        }
        return page.map { it.toDomain() }
    }

    private fun findActiveContests(
        sortType: ContestSortType,
        currentTime: LocalDateTime,
        pageable: Pageable
    ): Page<ContestJpaEntity> {
        return when (sortType) {
            ContestSortType.LATEST ->
                contestJpaRepository.findByExpiredAtAfterOrderByCreatedAtDesc(currentTime, pageable)
            ContestSortType.OLDEST ->
                contestJpaRepository.findByExpiredAtAfterOrderByCreatedAtAsc(currentTime, pageable)
            ContestSortType.POPULAR -> contestJpaRepository.findActiveContestsByPopular(currentTime, pageable)
        }
    }

    companion object {
        private val WINNER_SELECTED = ContestStatusType.WINNER_SELECTED
    }
}
