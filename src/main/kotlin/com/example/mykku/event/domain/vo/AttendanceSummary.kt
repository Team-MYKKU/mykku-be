package com.example.mykku.event.domain.vo

import java.time.LocalDate

data class AttendanceSummary(
    val hasAttendedToday: Boolean,
    val isAttendable: Boolean,
    val totalAttendanceDays: Int,
    val consecutiveAttendanceDays: Int
) {
    companion object {
        fun of(
            dates: Collection<LocalDate>,
            today: LocalDate,
            period: AttendancePeriod,
            phase: EventPhase
        ): AttendanceSummary {
            val attended = dates.filterTo(HashSet()) { it in period }
            val hasAttendedToday = today in attended
            return AttendanceSummary(
                hasAttendedToday = hasAttendedToday,
                isAttendable = phase == EventPhase.IN_PROGRESS && !hasAttendedToday,
                totalAttendanceDays = attended.size,
                consecutiveAttendanceDays = streakEndingAt(anchorOf(phase, today, hasAttendedToday, period), attended)
            )
        }

        private fun anchorOf(
            phase: EventPhase,
            today: LocalDate,
            hasAttendedToday: Boolean,
            period: AttendancePeriod
        ): LocalDate? {
            return when (phase) {
                EventPhase.BEFORE -> null
                EventPhase.IN_PROGRESS -> if (hasAttendedToday) today else today.minusDays(1)
                EventPhase.ENDED -> period.endDate
            }
        }

        private fun streakEndingAt(anchor: LocalDate?, attended: Set<LocalDate>): Int {
            return generateSequence(anchor) { it.minusDays(1) }.takeWhile { it in attended }.count()
        }
    }
}
