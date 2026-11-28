package com.example.mykku.event.adapter.input.web

import com.example.mykku.event.application.dto.AttendanceStatusResult
import com.example.mykku.event.application.dto.EntryStatusResult
import com.example.mykku.event.application.dto.MyEventStatusResult
import com.example.mykku.event.domain.vo.EventType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDate

@DisplayName("공통 상태 응답 변환 테스트")
class MyEventStatusResponseTest {

    @Test
    @DisplayName("출석 상태를 필드마다 그대로 옮기고 응모 상태는 null로 둔다")
    fun `출석 상태 변환`() {
        assertThat(MyEventStatusResponse.from(ATTENDANCE_RESULT)).isEqualTo(ATTENDANCE_RESPONSE)
    }

    @Test
    @DisplayName("응모 상태를 필드마다 그대로 옮기고 출석 상태는 null로 둔다")
    fun `응모 상태 변환`() {
        assertThat(MyEventStatusResponse.from(ENTRY_RESULT)).isEqualTo(ENTRY_RESPONSE)
    }

    @Test
    @DisplayName("응답 유형 enum은 도메인 유형과 상수 이름·선언 순서가 같다")
    fun `응답 유형 상수`() {
        val domainNames = EventType.entries.map { it.name }

        assertThat(EventTypeResponse.entries.map { it.name }).containsExactlyElementsOf(domainNames)
    }

    @ParameterizedTest(name = "{displayName} [{0}]")
    @EnumSource(EventType::class)
    @DisplayName("도메인 유형을 같은 이름의 응답 유형으로 옮긴다")
    fun `응답 유형 변환`(type: EventType) {
        assertThat(EventTypeResponse.from(type).name).isEqualTo(type.name)
    }

    companion object {
        private val TODAY: LocalDate = LocalDate.of(2026, 11, 28)

        private val ATTENDANCE_RESULT = MyEventStatusResult(
            eventId = 12L,
            type = EventType.ATTENDANCE,
            isInProgress = true,
            attendance = AttendanceStatusResult(
                today = TODAY,
                periodStartDate = TODAY.minusDays(3),
                periodEndDate = TODAY.plusDays(3),
                hasAttendedToday = true,
                isAttendable = false,
                totalAttendanceDays = 3,
                consecutiveAttendanceDays = 2
            ),
            entry = null
        )

        private val ATTENDANCE_RESPONSE = MyEventStatusResponse(
            eventId = 12L,
            type = EventTypeResponse.ATTENDANCE,
            isInProgress = true,
            attendance = AttendanceStatusResponse(
                today = TODAY,
                periodStartDate = TODAY.minusDays(3),
                periodEndDate = TODAY.plusDays(3),
                hasAttendedToday = true,
                isAttendable = false,
                totalAttendanceDays = 3,
                consecutiveAttendanceDays = 2
            ),
            entry = null
        )

        private val ENTRY_RESULT = MyEventStatusResult(
            eventId = 13L,
            type = EventType.ENTRY,
            isInProgress = false,
            attendance = null,
            entry = EntryStatusResult(hasEntered = true, isEnterable = false)
        )

        private val ENTRY_RESPONSE = MyEventStatusResponse(
            eventId = 13L,
            type = EventTypeResponse.ENTRY,
            isInProgress = false,
            attendance = null,
            entry = EntryStatusResponse(hasEntered = true, isEnterable = false)
        )
    }
}
