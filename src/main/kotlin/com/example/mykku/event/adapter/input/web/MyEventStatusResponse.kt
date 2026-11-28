package com.example.mykku.event.adapter.input.web

import com.example.mykku.event.application.dto.AttendanceStatusResult
import com.example.mykku.event.application.dto.EntryStatusResult
import com.example.mykku.event.application.dto.MyEventStatusResult
import com.example.mykku.event.domain.vo.EventType
import java.time.LocalDate

data class MyEventStatusResponse(
    val eventId: Long,
    val type: EventTypeResponse,
    val isInProgress: Boolean,
    val attendance: AttendanceStatusResponse?,
    val entry: EntryStatusResponse?
) {
    companion object {
        fun from(result: MyEventStatusResult): MyEventStatusResponse {
            return MyEventStatusResponse(
                eventId = result.eventId,
                type = EventTypeResponse.from(result.type),
                isInProgress = result.isInProgress,
                attendance = result.attendance?.let { AttendanceStatusResponse.from(it) },
                entry = result.entry?.let { EntryStatusResponse.from(it) }
            )
        }
    }
}

data class AttendanceStatusResponse(
    val today: LocalDate,
    val periodStartDate: LocalDate?,
    val periodEndDate: LocalDate?,
    val hasAttendedToday: Boolean,
    val isAttendable: Boolean,
    val totalAttendanceDays: Int,
    val consecutiveAttendanceDays: Int
) {
    companion object {
        fun from(result: AttendanceStatusResult): AttendanceStatusResponse {
            return AttendanceStatusResponse(
                today = result.today,
                periodStartDate = result.periodStartDate,
                periodEndDate = result.periodEndDate,
                hasAttendedToday = result.hasAttendedToday,
                isAttendable = result.isAttendable,
                totalAttendanceDays = result.totalAttendanceDays,
                consecutiveAttendanceDays = result.consecutiveAttendanceDays
            )
        }
    }
}

data class EntryStatusResponse(
    val hasEntered: Boolean,
    val isEnterable: Boolean
) {
    companion object {
        fun from(result: EntryStatusResult): EntryStatusResponse {
            return EntryStatusResponse(
                hasEntered = result.hasEntered,
                isEnterable = result.isEnterable
            )
        }
    }
}

enum class EventTypeResponse {
    GENERAL,
    ATTENDANCE,
    GOOGLE_FORM,
    ENTRY;

    companion object {
        fun from(type: EventType): EventTypeResponse {
            return when (type) {
                EventType.GENERAL -> GENERAL
                EventType.ATTENDANCE -> ATTENDANCE
                EventType.GOOGLE_FORM -> GOOGLE_FORM
                EventType.ENTRY -> ENTRY
            }
        }
    }
}
