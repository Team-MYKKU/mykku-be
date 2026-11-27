package com.example.mykku.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

@TestConfiguration
class TestImageUploadConfig {

    @Bean
    @Primary
    fun testImageUploadService(): TestImageUploadService {
        return TestImageUploadService()
    }
}
