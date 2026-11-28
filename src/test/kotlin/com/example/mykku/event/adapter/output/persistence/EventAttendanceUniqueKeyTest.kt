package com.example.mykku.event.adapter.output.persistence

import org.assertj.core.api.Assertions.assertThat
import org.hibernate.exception.ConstraintViolationException
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.dao.DataIntegrityViolationException
import java.sql.SQLException

@DisplayName("출석 유니크 제약 위반 판별 테스트")
class EventAttendanceUniqueKeyTest {

    @ParameterizedTest(name = "{displayName} [{0}]")
    @ValueSource(
        strings = ["uk_event_attendance_event_member_date", "event_attendance.uk_event_attendance_event_member_date"]
    )
    @DisplayName("제약 이름이 출석 유니크 제약이면 테이블 접두가 있어도 중복 출석이다")
    fun `판별 - 출석 유니크 제약`(constraintName: String) {
        assertThat(EventAttendanceUniqueKey.isViolatedBy(violation(constraintName))).isTrue()
    }

    @ParameterizedTest(name = "{displayName} [{0}]")
    @ValueSource(
        strings = [
            "fk_event_attendance_event",
            "fk_event_attendance_member",
            "event_attendance.uk_event_attendance_event_member",
            "event_attendance.xuk_event_attendance_event_member_date",
            "uk_event_attendance_event_member_date.other",
            "uk_event_participation_member_event"
        ]
    )
    @DisplayName("다른 제약 위반은 중복 출석이 아니다")
    fun `판별 - 다른 제약`(constraintName: String) {
        assertThat(EventAttendanceUniqueKey.isViolatedBy(violation(constraintName))).isFalse()
    }

    @Test
    @DisplayName("제약 이름을 알 수 없거나 Hibernate 원인이 없으면 중복 출석으로 보지 않는다")
    fun `판별 - 제약 이름 없음`() {
        assertThat(EventAttendanceUniqueKey.isViolatedBy(violation(null))).isFalse()
        assertThat(EventAttendanceUniqueKey.isViolatedBy(DataIntegrityViolationException("no cause"))).isFalse()
    }

    @Test
    @DisplayName("원인 사슬 깊이 있는 Hibernate 예외의 제약 이름도 본다")
    fun `판별 - 감싼 원인`() {
        val wrapped = RuntimeException("outer", violation("uk_event_attendance_event_member_date"))

        assertThat(EventAttendanceUniqueKey.isViolatedBy(wrapped)).isTrue()
    }

    private fun violation(constraintName: String?): DataIntegrityViolationException {
        val root = SQLException("Duplicate entry", "23000", 1062)
        val cause = ConstraintViolationException("Duplicate entry", root, constraintName)
        return DataIntegrityViolationException("could not execute statement", cause)
    }
}
