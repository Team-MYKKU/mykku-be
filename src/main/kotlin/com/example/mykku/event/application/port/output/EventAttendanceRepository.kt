package com.example.mykku.event.application.port.output

import com.example.mykku.event.domain.entity.EventAttendance
import com.example.mykku.event.domain.vo.EventId
import java.time.LocalDate

interface EventAttendanceRepository {
    fun save(attendance: EventAttendance): EventAttendance
    fun existsByEventIdAndMemberIdAndDate(eventId: EventId, memberId: Long, date: LocalDate): Boolean
    fun findDates(eventId: EventId, memberId: Long, from: LocalDate, to: LocalDate): List<LocalDate>
    fun countByEventIds(eventIds: List<EventId>): Map<Long, Int>
}
