package com.example.mykku.contest.adapter.output.persistence.repository

import com.example.mykku.common.adapter.persistence.IdCountRow
import com.example.mykku.contest.adapter.output.persistence.entity.ContestJpaEntity
import com.example.mykku.contest.adapter.output.persistence.entity.ContestParticipationJpaEntity
import com.example.mykku.contest.adapter.output.persistence.entity.ContestWinnerJpaEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface ContestWinnerJpaRepository : JpaRepository<ContestWinnerJpaEntity, Long> {
    fun findByContest(contest: ContestJpaEntity): List<ContestWinnerJpaEntity>
    fun findByContestIn(contests: List<ContestJpaEntity>): List<ContestWinnerJpaEntity>
    fun existsByContest(contest: ContestJpaEntity): Boolean
    fun deleteAllByContest(contest: ContestJpaEntity)
    fun deleteAllByParticipationIn(participations: List<ContestParticipationJpaEntity>)

    @Query(
        "SELECT w FROM ContestWinnerJpaEntity w " +
        "JOIN w.participation p " +
        "WHERE w.contest = :contest AND p.member.id = :memberId"
    )
    fun findByContestAndMemberId(
        @Param("contest") contest: ContestJpaEntity,
        @Param("memberId") memberId: Long
    ): ContestWinnerJpaEntity?

    @Query(
        "SELECT w FROM ContestWinnerJpaEntity w " +
        "JOIN w.participation p " +
        "WHERE p.member.id = :memberId " +
        "ORDER BY w.createdAt DESC"
    )
    fun findByMemberId(
        @Param("memberId") memberId: Long,
        pageable: Pageable
    ): Page<ContestWinnerJpaEntity>

    @Query(
        "SELECT w FROM ContestWinnerJpaEntity w " +
        "JOIN w.participation p " +
        "WHERE p.member.id = :memberId " +
        "ORDER BY w.createdAt DESC"
    )
    fun findAllByMemberId(
        @Param("memberId") memberId: Long
    ): List<ContestWinnerJpaEntity>

    @Query(
        "SELECT w FROM ContestWinnerJpaEntity w " +
        "JOIN w.participation p " +
        "WHERE p.member.id = :memberId AND w.contest IN :contests"
    )
    fun findByMemberIdAndContestIn(
        @Param("memberId") memberId: Long,
        @Param("contests") contests: List<ContestJpaEntity>
    ): List<ContestWinnerJpaEntity>

    @Query(
        "SELECT w.contest.id AS entityId, COUNT(w) AS countValue FROM ContestWinnerJpaEntity w " +
            "WHERE w.contest.id IN :contestIds GROUP BY w.contest.id"
    )
    fun countGroupedByContestIdIn(@Param("contestIds") contestIds: List<Long>): List<IdCountRow>
}
