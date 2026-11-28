package com.example.mykku.event.adapter.output.persistence

import org.hibernate.exception.ConstraintViolationException

internal object EventAttendanceUniqueKey {
    const val NAME = "uk_event_attendance_event_member_date"

    fun isViolatedBy(exception: Throwable): Boolean {
        return generateSequence(exception) { it.cause }
            .filterIsInstance<ConstraintViolationException>()
            .any { it.constraintName?.substringAfterLast('.') == NAME }
    }
}
