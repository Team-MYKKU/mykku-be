package com.example.mykku.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.boot.test.context.runner.ApplicationContextRunner

@DisplayName("FirebaseConfig 배선 테스트")
class FirebaseConfigTest {

    private val contextRunner = ApplicationContextRunner()
        .withUserConfiguration(FirebaseConfig::class.java)
        .withPropertyValues("firebase.service-account-key-path=")

    @Test
    @DisplayName("배포 프로필 dev에서는 빈 키 경로로 FirebaseConfig가 올라온다")
    fun registersFirebaseConfigInDevProfile() {
        contextRunner.withPropertyValues("spring.profiles.active=dev").run { context ->
            assertThat(context).hasNotFailed()
            assertThat(context).hasSingleBean(FirebaseConfig::class.java)
        }
    }

    @ParameterizedTest(name = "{0} 프로필에서는 FirebaseConfig가 올라오지 않는다")
    @ValueSource(strings = ["test", "local"])
    @DisplayName("test·local 프로필에서는 FirebaseConfig가 올라오지 않는다")
    fun skipsFirebaseConfigInTestAndLocalProfiles(profile: String) {
        contextRunner.withPropertyValues("spring.profiles.active=$profile").run { context ->
            assertThat(context).hasNotFailed()
            assertThat(context).doesNotHaveBean(FirebaseConfig::class.java)
        }
    }
}
