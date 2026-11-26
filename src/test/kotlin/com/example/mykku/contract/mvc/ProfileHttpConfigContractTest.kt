package com.example.mykku.contract.mvc

import com.example.mykku.contract.dump.StaticDumpGoldens
import com.example.mykku.contract.support.ContractGolden
import com.example.mykku.contract.support.ContractManifestExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@DisplayName("dev 프로필 HTTP 설정 계약")
@ExtendWith(ContractManifestExtension::class)
class ProfileHttpConfigContractTest {

    private val config = DevProfileHttpConfig()

    @Test
    fun `공통·dev 설정 파일을 병합한 HTTP 관련 키는 골든과 같다`() {
        ContractGolden.DEFAULT.verify(StaticDumpGoldens.DEV_PROFILE_HTTP, config.render())
    }

    @Test
    fun `읽는 파일은 application-dev yml을 포함하고 비밀 설정 파일은 읽지 않는다`() {
        val sources = config.existingSources().map { it.fileName.toString() }

        assertThat(sources).contains("application-dev.yml")
        assertThat(sources).noneMatch { it.contains("secret") }
        assertThat(config.unsupportedSources()).isEmpty()
    }

    @Test
    fun `덤프에는 접두 화이트리스트 키만 남는다`() {
        val prefixes = DevProfileHttpConfig.PREFIXES

        assertThat(config.properties().keys).allMatch { key -> prefixes.any { key.startsWith(it) } }
    }
}
