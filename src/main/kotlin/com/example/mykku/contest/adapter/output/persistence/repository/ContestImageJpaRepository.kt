package com.example.mykku.contest.adapter.output.persistence.repository

import com.example.mykku.contest.adapter.output.persistence.entity.ContestImageJpaEntity
import com.example.mykku.contest.adapter.output.persistence.entity.ContestJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ContestImageJpaRepository : JpaRepository<ContestImageJpaEntity, Long> {
    fun findByContestIn(contests: List<ContestJpaEntity>): List<ContestImageJpaEntity>
    fun deleteAllByContest(contest: ContestJpaEntity)
}
