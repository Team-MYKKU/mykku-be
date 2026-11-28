package com.example.mykku.config.fork

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files
import java.nio.file.Path

@DisplayName("테스트 fork 격리: Redis 서버 전체를 지우는 호출 금지")
class RedisFlushGuardTest {

    @Test
    fun `테스트 코드는 다른 fork의 Redis DB까지 지우는 FLUSHALL을 부르지 않는다`() {
        val offenders = kotlinSources()
            .filter { callsFlushAll(Files.readString(it, UTF_8)) }
            .map { SOURCE_ROOT.relativize(it).joinToString("/") }

        assertThat(offenders).`as`(GUIDE).isEmpty()
    }

    private fun kotlinSources(): List<Path> = Files.walk(SOURCE_ROOT).use { paths ->
        paths.filter { Files.isRegularFile(it) && isScanned(it.fileName.toString()) }.toList()
    }

    private fun isScanned(name: String): Boolean = name.endsWith(".kt") && name != SELF

    private fun callsFlushAll(source: String): Boolean = FORBIDDEN.any { source.contains(it, ignoreCase = true) }

    private companion object {
        val SOURCE_ROOT: Path = Path.of("src/test/kotlin")
        const val SELF = "RedisFlushGuardTest.kt"
        val FORBIDDEN = listOf("deleteReservationLock", "flushall")
        const val GUIDE = "fork마다 같은 Redis 서버의 DB를 하나씩 쓴다. FLUSHALL(운영 deleteReservationLock 포함)은 " +
            "모든 fork의 DB를 지운다. 인증 키는 deleteByPattern(\"email:verification:*\")로 지운다"
    }
}
