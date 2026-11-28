package com.example.mykku.event.application.port.output

import com.example.mykku.event.domain.vo.AttendancePeriod
import com.example.mykku.event.domain.vo.EventId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
@DisplayName("기간 날짜 안의 출석 날짜 조회")
class EventAttendanceRepositoryFindDatesInTest {

    @Mock
    private lateinit var repository: EventAttendanceRepository

    @Test
    @DisplayName("기간 날짜의 첫날·마지막 날로 그 이벤트·회원의 출석 날짜를 조회한다")
    fun `기간 양 끝으로 조회`() {
        val period = periodOf("2026-12-01T10:00", "2026-12-08T00:00")
        val dates = listOf(LocalDate.parse("2026-12-03"))
        val (first, last) = LocalDate.parse("2026-12-01") to LocalDate.parse("2026-12-07")
        whenever(repository.findDates(EVENT_ID, MEMBER_PK, first, last)).thenReturn(dates)

        assertThat(repository.findDatesIn(EVENT_ID, MEMBER_PK, period)).isEqualTo(dates)
    }

    @ParameterizedTest(name = "{displayName} [{0} ~ {1}]")
    @CsvSource("2026-12-01T10:00, 2026-12-01T10:00", "2026-12-05T00:00, 2026-12-03T00:00")
    @DisplayName("기간 날짜가 비면(시작 ≥ 종료) 조회하지 않고 빈 목록이다")
    fun `빈 기간은 조회하지 않음`(startedAt: String, expiredAt: String) {
        assertThat(repository.findDatesIn(EVENT_ID, MEMBER_PK, periodOf(startedAt, expiredAt))).isEmpty()

        verifyNoInteractions(repository)
    }

    private fun periodOf(startedAt: String, expiredAt: String): AttendancePeriod {
        return AttendancePeriod.of(LocalDateTime.parse(startedAt), LocalDateTime.parse(expiredAt))
    }

    companion object {
        private val EVENT_ID = EventId(12L)
        private const val MEMBER_PK = 7L
    }
}
