package com.example.mykku.contract.error

import com.example.mykku.contract.support.ContractGolden
import com.example.mykku.contract.support.ContractUpdateMode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assumptions.assumeFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("프레임워크 에러 골든 완전성")
class ErrorGoldenCoverageTest {

    @BeforeEach
    fun skipWhileUpdating() {
        assumeFalse(ContractUpdateMode.fromEnvironment().enabled, "갱신 모드에서는 골든을 쓰는 중이라 건너뛴다")
    }

    @Test
    fun `프레임워크 에러 골든 디렉터리에는 지금 비교하는 파일만 있다`() {
        assertThat(ContractGolden.DEFAULT.goldenFiles(ErrorGoldens.DIRECTORY))
            .containsExactlyInAnyOrderElementsOf(ErrorGoldens.FILES)
    }

    @Test
    fun `gray-zone의 framework- 접두 골든은 지금 비교하는 파일만 있다`() {
        val frameworkGrayZone = ContractGolden.DEFAULT.goldenFiles(ErrorGoldens.GRAY_ZONE_DIRECTORY)
            .filter { it.startsWith(ErrorGoldens.GRAY_ZONE_PREFIX) }

        assertThat(frameworkGrayZone).containsExactlyInAnyOrderElementsOf(ErrorGoldens.GRAY_ZONE_FILES)
    }
}
