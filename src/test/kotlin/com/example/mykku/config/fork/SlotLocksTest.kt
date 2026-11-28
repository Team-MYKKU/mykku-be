package com.example.mykku.config.fork

import com.example.mykku.BaseRepositoryTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import java.sql.Connection
import java.sql.DriverManager
import java.time.Duration
import java.util.UUID

@DisplayName("테스트 fork 슬롯 잠금")
class SlotLocksTest : BaseRepositoryTest() {

    @Autowired
    private lateinit var environment: Environment

    private val prefix = "mykku-test-fork-selftest-${UUID.randomUUID().toString().take(UNIQUE_LENGTH)}-"

    @Test
    fun `다른 연결이 잡은 슬롯은 건너뛰고 비어 있는 첫 슬롯을 잡는다`() {
        connect().use { holder ->
            SlotLocks(holder, prefix, 1..1).lockFree()
            SlotLocks(holder, prefix, 2..2).lockFree()
            connect().use { assertThat(SlotLocks(it, prefix, 1..3).lockFree(Duration.ZERO)).isEqualTo(3) }
        }
    }

    @Test
    fun `슬롯을 잡은 연결이 닫히면 다른 연결이 그 슬롯을 잡는다`() {
        connect().use { contender ->
            connect().use { SlotLocks(it, prefix, 1..1).lockFree() }
            assertThat(SlotLocks(contender, prefix, 1..1).lockFree(SERVER_RELEASE_WAIT)).isEqualTo(1)
        }
    }

    @Test
    fun `슬롯 스키마를 쓰는 세션이 있으면 그 슬롯 잠금을 놓고 다음 슬롯을 잡는다`() {
        connect().use { finder ->
            assertThat(SlotLocks(finder, prefix, 1..2).lockFree(Duration.ZERO) { it == 1 }).isEqualTo(2)
            connect().use { assertThat(SlotLocks(it, prefix, 1..1).lockFree(SERVER_RELEASE_WAIT)).isEqualTo(1) }
        }
    }

    @Test
    fun `모든 슬롯이 사용 중이면 기다리는 시간이 지난 뒤 실패한다`() {
        connect().use { holder ->
            SlotLocks(holder, prefix, 1..1).lockFree()
            connect().use { contender ->
                assertThatThrownBy { SlotLocks(contender, prefix, 1..1).lockFree(Duration.ZERO) }
                    .isInstanceOf(IllegalStateException::class.java)
            }
        }
    }

    private fun connect(): Connection = DriverManager.getConnection(
        environment.getRequiredProperty("spring.datasource.url"),
        environment.getProperty("spring.datasource.username"),
        environment.getProperty("spring.datasource.password")
    )

    private companion object {
        const val UNIQUE_LENGTH = 13
        val SERVER_RELEASE_WAIT: Duration = Duration.ofSeconds(10)
    }
}
