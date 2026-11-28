package com.example.mykku.event.domain

import com.example.mykku.event.domain.entity.EventAttendance
import com.example.mykku.event.domain.vo.EventAttendanceId
import com.example.mykku.event.domain.vo.EventId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate

@DisplayName("EventAttendance 도메인 엔티티 테스트")
class EventAttendanceTest {

    @Test
    @DisplayName("새 출석은 id 0으로 이벤트·회원·출석 날짜를 담는다")
    fun `생성 - 새 출석`() {
        val attendance = EventAttendance.create(EventId(3L), memberId = 7L, attendanceDate = DATE)

        assertThat(attendance.id).isEqualTo(EventAttendanceId(0L))
        assertThat(attendance.eventId).isEqualTo(EventId(3L))
        assertThat(attendance.memberId).isEqualTo(7L)
        assertThat(attendance.attendanceDate).isEqualTo(DATE)
    }

    @Test
    @DisplayName("탈퇴한 회원의 출석은 회원 없이(null) 복원한다")
    fun `복원 - 탈퇴한 회원`() {
        val attendance = EventAttendance.reconstitute(EventAttendanceId(9L), EventId(3L), null, DATE)

        assertThat(attendance.id).isEqualTo(EventAttendanceId(9L))
        assertThat(attendance.memberId).isNull()
        assertThat(attendance.attendanceDate).isEqualTo(DATE)
    }

    companion object {
        private val DATE = LocalDate.of(2026, 12, 1)
    }
}
