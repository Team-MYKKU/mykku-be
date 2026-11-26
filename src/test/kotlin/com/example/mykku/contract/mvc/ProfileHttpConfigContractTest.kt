package com.example.mykku.contract.mvc

import com.example.mykku.contract.support.ContractGolden
import com.example.mykku.contract.support.ContractManifestExtension
import com.example.mykku.contract.support.ContractViolationError
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

@DisplayName("dev 프로필 HTTP 설정 계약")
@ExtendWith(ContractManifestExtension::class)
class ProfileHttpConfigContractTest {

    private val config = DevProfileHttpConfig()

    @Test
    fun `git이 추적하는 공통·dev 설정 파일을 병합한 HTTP 관련 키는 골든과 같다`() {
        val violation = ContractGolden.DEFAULT.violationOf(GOLDEN, config.render()) ?: return

        throw ContractViolationError("$violation\n- 병합한 파일: ${config.mergedSources()}")
    }

    @Test
    fun `덤프는 병합 대상 파일만 읽고 같은 디렉터리의 다른 설정 파일에 영향받지 않는다`(@TempDir copy: Path) {
        config.mergedSources().forEach { Files.copy(it, copy.resolve(it.fileName)) }

        assertThat(DevProfileHttpConfig(copy) { true }.properties()).isEqualTo(config.properties())
    }

    @Test
    fun `application-dev yml을 병합하고 지원하지 않는 설정 파일은 없다`() {
        assertThat(config.mergedSources().map { it.fileName.toString() }).contains("application-dev.yml")
        assertThat(config.unsupportedSources()).isEmpty()
    }

    companion object {
        private const val GOLDEN = "wire/mvc/application-dev-http.properties"

        val GOLDENS: List<String> = listOf(GOLDEN)
    }
}
