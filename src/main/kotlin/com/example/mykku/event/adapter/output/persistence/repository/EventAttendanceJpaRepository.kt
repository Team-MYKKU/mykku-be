package com.example.mykku.event.adapter.output.persistence.repository

import com.example.mykku.common.adapter.persistence.IdCountRow
import com.example.mykku.event.adapter.output.persistence.entity.EventAttendanceJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface EventAttendanceJpaRepository : JpaRepository<EventAttendanceJpaEntity, Long> {
    fun existsByEventIdAndMemberIdAndAttendanceDate(eventId: Long, memberId: Long, attendanceDate: LocalDate): Boolean

    @Query(
        "SELECT a.attendanceDate FROM EventAttendanceJpaEntity a " +
            "WHERE a.eventId = :eventId AND a.memberId = :memberId AND a.attendanceDate BETWEEN :from AND :to " +
            "ORDER BY a.attendanceDate"
    )
    fun findAttendanceDates(
        @Param("eventId") eventId: Long,
        @Param("memberId") memberId: Long,
        @Param("from") from: LocalDate,
        @Param("to") to: LocalDate
    ): List<LocalDate>

    @Query(
        "SELECT a.eventId AS entityId, COUNT(a) AS countValue FROM EventAttendanceJpaEntity a " +
            "WHERE a.eventId IN :eventIds GROUP BY a.eventId"
    )
    fun countGroupedByEventIdIn(@Param("eventIds") eventIds: List<Long>): List<IdCountRow>
}
