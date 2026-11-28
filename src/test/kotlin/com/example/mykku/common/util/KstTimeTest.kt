package com.example.mykku.common.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

@DisplayName("KstTime 테스트")
class KstTimeTest {

    @ParameterizedTest(name = "{displayName} [{0} → {1}]")
    @CsvSource(
        "2026-11-28T14:59:59.999999Z, 2026-11-28T23:59:59.999999",
        "2026-11-28T15:00:00Z, 2026-11-29T00:00",
        "2026-12-31T15:00:00Z, 2027-01-01T00:00"
    )
    @DisplayName("UTC 존 고정 시계의 순간을 KST 벽시계 시각으로 바꾼다(KST 자정 = UTC 전날 15:00)")
    fun `UTC 시계를 KST로 변환`(instant: String, kst: String) {
        val clock = Clock.fixed(Instant.parse(instant), ZoneOffset.UTC)

        assertThat(KstTime.now(clock)).isEqualTo(LocalDateTime.parse(kst))
    }

    @ParameterizedTest(name = "{displayName} [{0}]")
    @ValueSource(strings = ["UTC", "Asia/Seoul", "America/Los_Angeles", "Pacific/Kiritimati"])
    @DisplayName("시계의 존과 관계없이 같은 순간이면 같은 KST 시각이다")
    fun `시계 존 무시`(zone: String) {
        val clock = Clock.fixed(Instant.parse("2026-11-28T15:00:00Z"), ZoneId.of(zone))

        assertThat(KstTime.now(clock)).isEqualTo(LocalDateTime.parse("2026-11-29T00:00"))
    }

    @Test
    @DisplayName("KST는 Asia/Seoul이다")
    fun `KST 존`() {
        assertThat(KstTime.KST).isEqualTo(ZoneId.of("Asia/Seoul"))
    }
}
