package com.example.mykku.config

import com.example.mykku.util.DefaultTimeZone
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

@DisplayName("Clock 빈 정의의 시간대")
class ClockConfigZoneTest {

    @ParameterizedTest(name = "{displayName} [{0}]")
    @ValueSource(strings = ["UTC", "America/Los_Angeles"])
    @DisplayName("운영 Clock은 만들 때의 JVM 기본 시간대를 쓰는 시스템 시계다(KST로 고정하지 않는다, D23)")
    fun `JVM 기본 시간대를 따르는 시스템 시계`(jvmZone: String) {
        val clock = DefaultTimeZone.with(jvmZone) { ClockConfig().clock() }

        assertThat(clock.zone).isEqualTo(ZoneId.of(jvmZone))
        assertThat(Duration.between(clock.instant(), Instant.now()).abs()).isLessThan(MAX_GAP)
    }

    companion object {
        private val MAX_GAP: Duration = Duration.ofSeconds(5)
    }
}
