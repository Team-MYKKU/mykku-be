package com.example.mykku.config.fork

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files
import java.nio.file.Path

@DisplayName("테스트 fork 슬롯 수: Gradle 설정과 슬롯 잠금이 같은 수를 쓴다")
class ForkSlotCountTest {

    @Test
    fun `build_gradle_kts의 testForkSlots는 SlotLocks의 MAX_SLOTS와 같다`() {
        val buildScript = Files.readString(Path.of("build.gradle.kts"), UTF_8)

        assertThat(buildScript).`as`(GUIDE).contains("val testForkSlots = ${SlotLocks.MAX_SLOTS}\n")
    }

    private companion object {
        const val GUIDE = "fork 슬롯 수는 Redis DB 1~N과 같다. build.gradle.kts의 testForkSlots와 SlotLocks.MAX_SLOTS를 함께 바꾼다"
    }
}
