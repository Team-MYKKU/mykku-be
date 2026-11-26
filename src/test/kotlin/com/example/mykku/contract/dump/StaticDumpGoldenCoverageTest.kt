package com.example.mykku.contract.dump

import com.example.mykku.contract.support.ContractGolden
import com.example.mykku.contract.support.ContractUpdateMode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assumptions.assumeFalse
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

@DisplayName("정적 덤프 골든 완전성")
class StaticDumpGoldenCoverageTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("directories")
    fun `정적 덤프 골든 디렉터리에는 지금 비교하는 파일만 있다`(directory: String, files: Set<String>) {
        assumeFalse(ContractUpdateMode.fromEnvironment().enabled, "갱신 모드에서는 골든을 쓰는 중이라 건너뛴다")

        assertThat(ContractGolden.DEFAULT.goldenFiles(directory)).containsExactlyInAnyOrderElementsOf(files)
    }

    companion object {
        @JvmStatic
        fun directories(): List<Arguments> =
            StaticDumpGoldens.filesByDirectory().map { (directory, files) -> Arguments.of(directory, files) }
    }
}
