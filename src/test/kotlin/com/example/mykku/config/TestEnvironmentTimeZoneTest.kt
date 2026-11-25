package com.example.mykku.config

import com.example.mykku.BaseRepositoryTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

@DisplayName("테스트 환경 시간대 정렬")
class TestEnvironmentTimeZoneTest : BaseRepositoryTest() {

    companion object {
        private val SEOUL: ZoneId = ZoneId.of("Asia/Seoul")
        private val MAX_CLOCK_GAP: Duration = Duration.ofSeconds(5)
        private const val JVM_ZONE_HINT = "테스트 JVM은 Gradle Test 태스크의 user.timezone=Asia/Seoul로 실행해야 한다"
        private const val SESSION_ZONE_HINT = "test 프로필 JDBC URL의 sessionVariables=time_zone이 세션에 적용돼야 한다"
        private const val ALIGNMENT_HINT = "JVM(user.timezone)과 DB 세션(sessionVariables)이 모두 KST여야 한다"
    }

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Test
    @DisplayName("테스트 JVM 기본 시간대는 Asia/Seoul이다")
    fun `테스트 JVM 기본 시간대는 Asia Seoul이다`() {
        assertThat(ZoneId.systemDefault()).describedAs(JVM_ZONE_HINT).isEqualTo(SEOUL)
    }

    @Test
    @DisplayName("DB 세션 시간대는 +09:00이고 JVM 기본 시간대와 오프셋이 같다")
    fun `DB 세션 시간대는 JVM 기본 시간대와 오프셋이 같다`() {
        val sessionTimeZone = jdbcTemplate.queryForObject("SELECT @@session.time_zone", String::class.java)
        val sessionOffsetSeconds = jdbcTemplate.queryForObject(
            "SELECT TIMESTAMPDIFF(SECOND, UTC_TIMESTAMP(), NOW())",
            Int::class.java
        )
        val jvmOffsetSeconds = ZoneId.systemDefault().rules.getOffset(Instant.now()).totalSeconds

        assertThat(sessionTimeZone).describedAs(SESSION_ZONE_HINT).isEqualTo("+09:00")
        assertThat(sessionOffsetSeconds).describedAs(ALIGNMENT_HINT).isEqualTo(jvmOffsetSeconds)
    }

    @Test
    @DisplayName("DB NOW()와 JVM LocalDateTime.now()는 수 초 안에서 같은 시각이다")
    fun `DB NOW와 JVM LocalDateTime now는 같은 시각이다`() {
        val dbNow = jdbcTemplate.queryForObject("SELECT NOW(6)", LocalDateTime::class.java)
        val jvmNow = LocalDateTime.now()

        assertThat(Duration.between(dbNow, jvmNow).abs()).describedAs(ALIGNMENT_HINT).isLessThan(MAX_CLOCK_GAP)
    }
}
