package com.example.mykku.common.exception

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.type.filter.AssignableTypeFilter

@DisplayName("ErrorCodeRegistry 테스트")
class ErrorCodeRegistryTest {

    @Test
    @DisplayName("등록된 에러 코드 문자열은 서로 겹치지 않는다")
    fun `에러 코드 중복 없음`() {
        val codes = ErrorCodeRegistry.getAllErrorCodes().map { it.code }

        assertThat(codes).doesNotHaveDuplicates()
    }

    @Test
    @DisplayName("DomainErrorCode를 구현한 모든 enum의 코드가 레지스트리에 들어 있다")
    fun `모든 에러 코드 enum 등록`() {
        val registered = ErrorCodeRegistry.getAllErrorCodes().map { it.code }.toSet()

        val declared = domainErrorCodeEnums().flatMap { enumClass ->
            enumClass.enumConstants.map { (it as DomainErrorCode).code }
        }

        assertThat(registered).containsAll(declared)
    }

    private fun domainErrorCodeEnums(): List<Class<*>> {
        val scanner = ClassPathScanningCandidateComponentProvider(false)
        scanner.addIncludeFilter(AssignableTypeFilter(DomainErrorCode::class.java))
        return scanner.findCandidateComponents("com.example.mykku")
            .map { Class.forName(it.beanClassName) }
            .filter { it.isEnum }
    }
}
