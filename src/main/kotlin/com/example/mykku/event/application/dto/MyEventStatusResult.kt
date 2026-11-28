package com.example.mykku.event.application.dto

import com.example.mykku.event.domain.entity.Event
import com.example.mykku.event.domain.vo.AttendanceSummary
import com.example.mykku.event.domain.vo.EventType
import java.time.LocalDate
import java.time.LocalDateTime

data class MyEventStatusResult(
    val eventId: Long,
    val type: EventType,
    val isInProgress: Boolean,
    val attendance: AttendanceStatusResult?,
    val entry: EntryStatusResult?
) {
    companion object {
        fun of(
            event: Event,
            now: LocalDateTime,
            attendance: AttendanceStatusResult?,
            entry: EntryStatusResult?
        ): MyEventStatusResult {
            return MyEventStatusResult(
                eventId = event.id.value,
                type = event.type,
                isInProgress = event.isInProgress(now),
                attendance = attendance,
                entry = entry
            )
        }
    }
}

data class AttendanceStatusResult(
    val today: LocalDate,
    val periodStartDate: LocalDate?,
    val periodEndDate: LocalDate?,
    val hasAttendedToday: Boolean,
    val isAttendable: Boolean,
    val totalAttendanceDays: Int,
    val consecutiveAttendanceDays: Int
) {
    companion object {
        fun of(event: Event, now: LocalDateTime, attendedDates: Collection<LocalDate>): AttendanceStatusResult {
            val today = now.toLocalDate()
            val period = event.attendancePeriod()
            val summary = AttendanceSummary.of(attendedDates, today, period, event.phaseAt(now))
            return AttendanceStatusResult(
                today = today,
                periodStartDate = period.startDate,
                periodEndDate = period.endDate,
                hasAttendedToday = summary.hasAttendedToday,
                isAttendable = summary.isAttendable,
                totalAttendanceDays = summary.totalAttendanceDays,
                consecutiveAttendanceDays = summary.consecutiveAttendanceDays
            )
        }
    }
}

data class EntryStatusResult(
    val hasEntered: Boolean,
    val isEnterable: Boolean
)
