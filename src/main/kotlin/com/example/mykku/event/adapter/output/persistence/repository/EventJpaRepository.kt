package com.example.mykku.event.adapter.output.persistence.repository

import com.example.mykku.event.adapter.output.persistence.entity.EventJpaEntity
import com.example.mykku.event.domain.vo.EventStatusType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface EventJpaRepository : JpaRepository<EventJpaEntity, Long> {
    fun findByExpiredAtAfter(dateTime: LocalDateTime): List<EventJpaEntity>

    fun findByExpiredAtAfterOrderByCreatedAtDesc(dateTime: LocalDateTime, pageable: Pageable): Page<EventJpaEntity>

    fun findByExpiredAtAfterOrderByCreatedAtAsc(dateTime: LocalDateTime, pageable: Pageable): Page<EventJpaEntity>

    @Query(
        """
            SELECT e
            FROM EventJpaEntity e
            WHERE e.expiredAt > :dateTime
            ORDER BY e.createdAt DESC
        """
    )
    fun findActiveEventsByPopular(dateTime: LocalDateTime, pageable: Pageable): Page<EventJpaEntity>

    fun findByExpiredAtLessThanEqualOrderByCreatedAtDesc(
        dateTime: LocalDateTime,
        pageable: Pageable
    ): Page<EventJpaEntity>

    fun findAllByOrderByCreatedAtDesc(pageable: Pageable): Page<EventJpaEntity>

    fun findByStatusOrderByCreatedAtDesc(status: EventStatusType, pageable: Pageable): Page<EventJpaEntity>

    fun findByExpiredAtLessThanEqualAndStatusNotOrderByCreatedAtDesc(
        dateTime: LocalDateTime,
        status: EventStatusType,
        pageable: Pageable
    ): Page<EventJpaEntity>

    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM EventJpaEntity e WHERE e.id = :id")
    fun deleteEventById(@Param("id") id: Long): Int
}
