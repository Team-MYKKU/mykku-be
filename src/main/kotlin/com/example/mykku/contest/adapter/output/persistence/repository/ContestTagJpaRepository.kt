package com.example.mykku.contest.adapter.output.persistence.repository

import com.example.mykku.contest.adapter.output.persistence.entity.ContestJpaEntity
import com.example.mykku.contest.adapter.output.persistence.entity.ContestTagJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ContestTagJpaRepository : JpaRepository<ContestTagJpaEntity, Long> {
    fun findAllByTitleIn(titles: Collection<String>): List<ContestTagJpaEntity>
    fun findByContestIn(contests: List<ContestJpaEntity>): List<ContestTagJpaEntity>
    fun deleteAllByContest(contest: ContestJpaEntity)
}
