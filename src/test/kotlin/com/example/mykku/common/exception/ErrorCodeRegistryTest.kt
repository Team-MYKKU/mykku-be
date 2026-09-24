package com.example.mykku.common.exception

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("ErrorCodeRegistry 테스트")
class ErrorCodeRegistryTest {

    @Test
    @DisplayName("등록된 에러 코드 문자열은 서로 겹치지 않는다")
    fun `에러 코드 중복 없음`() {
        val codes = ErrorCodeRegistry.getAllErrorCodes().map { it.code }

        assertThat(codes).doesNotHaveDuplicates()
    }
}
