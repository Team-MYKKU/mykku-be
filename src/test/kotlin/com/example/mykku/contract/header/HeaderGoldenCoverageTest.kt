package com.example.mykku.contract.header

import com.example.mykku.contract.support.ContractGolden
import com.example.mykku.contract.support.ContractUpdateMode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assumptions.assumeFalse
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("응답 헤더 골든 완전성")
class HeaderGoldenCoverageTest {

    @Test
    fun `응답 헤더 골든 디렉터리에는 지금 비교하는 파일만 있다`() {
        assumeFalse(ContractUpdateMode.fromEnvironment().enabled, "갱신 모드에서는 골든을 쓰는 중이라 건너뛴다")

        assertThat(ContractGolden.DEFAULT.goldenFiles(HeaderGoldens.DIRECTORY))
            .containsExactlyInAnyOrderElementsOf(HeaderGoldens.FILES)
    }
}
