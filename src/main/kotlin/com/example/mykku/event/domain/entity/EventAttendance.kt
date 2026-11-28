package com.example.mykku.event.domain.entity

import com.example.mykku.event.domain.vo.EventAttendanceId
import com.example.mykku.event.domain.vo.EventId
import java.time.LocalDate

class EventAttendance private constructor(
    val id: EventAttendanceId,
    val eventId: EventId,
    val memberId: Long?,
    val attendanceDate: LocalDate
) {
    companion object {
        fun create(eventId: EventId, memberId: Long, attendanceDate: LocalDate): EventAttendance {
            return EventAttendance(EventAttendanceId(0L), eventId, memberId, attendanceDate)
        }

        fun reconstitute(
            id: EventAttendanceId,
            eventId: EventId,
            memberId: Long?,
            attendanceDate: LocalDate
        ): EventAttendance {
            return EventAttendance(id, eventId, memberId, attendanceDate)
        }
    }
}
