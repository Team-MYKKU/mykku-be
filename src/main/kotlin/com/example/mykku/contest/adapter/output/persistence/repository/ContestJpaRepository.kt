package com.example.mykku.contest.adapter.output.persistence.repository

import com.example.mykku.contest.adapter.output.persistence.entity.ContestJpaEntity
import com.example.mykku.contest.domain.vo.ContestStatusType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface ContestJpaRepository : JpaRepository<ContestJpaEntity, Long> {
    fun findByExpiredAtAfter(dateTime: LocalDateTime): List<ContestJpaEntity>

    fun findByStatusAndExpiredAtAfter(status: ContestStatusType, dateTime: LocalDateTime): List<ContestJpaEntity>

    fun findByExpiredAtAfterOrderByCreatedAtDesc(dateTime: LocalDateTime, pageable: Pageable): Page<ContestJpaEntity>

    fun findByExpiredAtAfterOrderByCreatedAtAsc(dateTime: LocalDateTime, pageable: Pageable): Page<ContestJpaEntity>

    @Query(
        """
            SELECT c
            FROM ContestJpaEntity c
            WHERE c.expiredAt > :dateTime
            ORDER BY c.createdAt DESC
        """
    )
    fun findActiveContestsByPopular(dateTime: LocalDateTime, pageable: Pageable): Page<ContestJpaEntity>

    fun findByExpiredAtLessThanEqualOrderByCreatedAtDesc(
        dateTime: LocalDateTime,
        pageable: Pageable
    ): Page<ContestJpaEntity>

    fun findAllByOrderByCreatedAtDesc(pageable: Pageable): Page<ContestJpaEntity>

    fun findByStatus(status: ContestStatusType): List<ContestJpaEntity>

    fun findByStatusOrderByCreatedAtDesc(status: ContestStatusType, pageable: Pageable): Page<ContestJpaEntity>

    fun findByExpiredAtLessThanEqualAndStatusNotOrderByCreatedAtDesc(
        dateTime: LocalDateTime,
        status: ContestStatusType,
        pageable: Pageable
    ): Page<ContestJpaEntity>

    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM ContestJpaEntity c WHERE c.id = :id")
    fun deleteContestById(@Param("id") id: Long): Int
}
