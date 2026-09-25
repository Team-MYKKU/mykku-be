package com.example.mykku.event.adapter.output.persistence.repository

import com.example.mykku.event.adapter.output.persistence.entity.EventJpaEntity
import com.example.mykku.event.adapter.output.persistence.entity.EventParticipationJpaEntity
import com.example.mykku.event.adapter.output.persistence.entity.EventWinnerJpaEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface EventWinnerJpaRepository : JpaRepository<EventWinnerJpaEntity, Long> {
    fun findByEvent(event: EventJpaEntity): List<EventWinnerJpaEntity>
    fun deleteAllByEvent(event: EventJpaEntity)
    fun deleteAllByParticipationIn(participations: List<EventParticipationJpaEntity>)

    @Query(
        "SELECT w FROM EventWinnerJpaEntity w " +
            "JOIN w.participation p " +
            "WHERE w.event = :event AND p.member.id = :memberId"
    )
    fun findByEventAndMemberId(
        @Param("event") event: EventJpaEntity,
        @Param("memberId") memberId: Long
    ): EventWinnerJpaEntity?

    @Query(
        "SELECT w FROM EventWinnerJpaEntity w " +
            "JOIN w.participation p " +
            "WHERE p.member.id = :memberId " +
            "ORDER BY w.createdAt DESC"
    )
    fun findByMemberId(
        @Param("memberId") memberId: Long,
        pageable: Pageable
    ): Page<EventWinnerJpaEntity>

    @Query(
        "SELECT w FROM EventWinnerJpaEntity w " +
            "JOIN w.participation p " +
            "WHERE p.member.id = :memberId AND w.event IN :events"
    )
    fun findByMemberIdAndEventIn(
        @Param("memberId") memberId: Long,
        @Param("events") events: List<EventJpaEntity>
    ): List<EventWinnerJpaEntity>
}
