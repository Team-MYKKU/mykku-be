package com.example.mykku.event.domain.vo

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

@DisplayName("AttendancePeriod 값 객체 테스트")
class AttendancePeriodTest {

    @Test
    @DisplayName("날짜 구간이 같으면 같은 값이고, 문자열로 구간을 읽을 수 있다")
    fun `값 동등성과 문자열`() {
        val period = AttendancePeriod.of(at("2026-12-01T00:00"), at("2026-12-08T00:00"))
        val sameDates = AttendancePeriod.of(at("2026-12-01T10:00"), at("2026-12-07T18:00"))

        assertThat(period).isEqualTo(sameDates).hasSameHashCodeAs(sameDates)
        assertThat(period).isNotEqualTo(AttendancePeriod.of(at("2026-12-01T00:00"), at("2026-12-09T00:00")))
        assertThat(period).isNotEqualTo(AttendancePeriod.of(at("2026-11-30T00:00"), at("2026-12-08T00:00")))
        assertThat(period.toString()).isEqualTo("AttendancePeriod(startDate=2026-12-01, endDate=2026-12-07)")
    }

    @Test
    @DisplayName("빈 구간끼리는 같은 값이고 비어 있지 않은 구간과는 다르다")
    fun `빈 구간 동등성`() {
        val empty = AttendancePeriod.of(at("2026-12-01T10:00"), at("2026-12-01T10:00"))

        assertThat(empty).isEqualTo(AttendancePeriod.of(at("2026-12-05T00:00"), at("2026-12-03T00:00")))
        assertThat(empty).isNotEqualTo(AttendancePeriod.of(at("2026-12-01T10:00"), at("2026-12-01T10:00:00.000000001")))
        assertThat(empty.toString()).isEqualTo("AttendancePeriod(startDate=null, endDate=null)")
    }

    private fun at(value: String): LocalDateTime = LocalDateTime.parse(value)
}
