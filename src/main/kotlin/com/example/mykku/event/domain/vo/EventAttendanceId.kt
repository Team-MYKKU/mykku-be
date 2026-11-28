package com.example.mykku.event.domain.vo

@JvmInline
value class EventAttendanceId(val value: Long) {
    companion object {
        fun of(value: Long): EventAttendanceId = EventAttendanceId(value)
    }
}
