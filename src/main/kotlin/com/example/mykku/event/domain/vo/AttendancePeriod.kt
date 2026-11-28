package com.example.mykku.event.domain.vo

import java.time.LocalDate
import java.time.LocalDateTime

class AttendancePeriod private constructor(
    val startDate: LocalDate?,
    val endDate: LocalDate?
) {
    val isEmpty: Boolean
        get() = startDate == null || endDate == null

    operator fun contains(date: LocalDate): Boolean {
        if (startDate == null || endDate == null) return false
        return !date.isBefore(startDate) && !date.isAfter(endDate)
    }

    companion object {
        private val EMPTY = AttendancePeriod(null, null)

        fun of(startedAt: LocalDateTime, expiredAt: LocalDateTime): AttendancePeriod {
            if (!startedAt.isBefore(expiredAt)) return EMPTY
            return AttendancePeriod(startedAt.toLocalDate(), expiredAt.minusNanos(1).toLocalDate())
        }
    }
}
