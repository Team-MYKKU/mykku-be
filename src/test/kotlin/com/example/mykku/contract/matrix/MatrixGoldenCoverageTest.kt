package com.example.mykku.contract.matrix

import com.example.mykku.contract.support.ContractGolden
import com.example.mykku.contract.support.ContractUpdateMode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assumptions.assumeFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("매트릭스 골든 완전성")
class MatrixGoldenCoverageTest {

    @BeforeEach
    fun skipWhileUpdating() {
        assumeFalse(ContractUpdateMode.fromEnvironment().enabled, "갱신 모드에서는 골든을 쓰는 중이라 건너뛴다")
    }

    @Test
    fun `매트릭스 골든 디렉터리에는 지금 비교하는 파일만 있다`() {
        assertThat(ContractGolden.DEFAULT.goldenFiles(MatrixGoldens.DIRECTORY))
            .containsExactlyInAnyOrderElementsOf(MatrixGoldens.FILES)
    }

    @Test
    fun `매트릭스는 409·500 관측이 없어 gray-zone에 matrix- 접두 골든이 없다`() {
        val matrixGrayZone = ContractGolden.DEFAULT.goldenFiles(MatrixGoldens.GRAY_ZONE_DIRECTORY)
            .filter { it.startsWith(MatrixGoldens.GRAY_ZONE_PREFIX) }

        assertThat(matrixGrayZone).isEmpty()
    }
}
