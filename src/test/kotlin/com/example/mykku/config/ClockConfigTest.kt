package com.example.mykku.config

import com.example.mykku.BaseRepositoryTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import java.time.Clock

@DisplayName("Clock 빈")
class ClockConfigTest : BaseRepositoryTest() {

    @Autowired
    private lateinit var applicationContext: ApplicationContext

    @Test
    @DisplayName("Clock 빈은 clock 하나뿐이고 JVM 기본 시간대의 시스템 시계다")
    fun `Clock 빈은 하나뿐인 시스템 시계다`() {
        val clocks = applicationContext.getBeansOfType(Clock::class.java)

        assertThat(clocks.keys).containsExactly("clock")
        assertThat(clocks.getValue("clock")).isEqualTo(Clock.systemDefaultZone())
    }
}
