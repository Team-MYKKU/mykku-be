package com.example.mykku.contract.errorcode

import com.example.mykku.BaseControllerTest
import com.example.mykku.common.exception.DomainErrorCode
import com.example.mykku.common.exception.ErrorCodeRegistry
import com.example.mykku.contract.support.ContractGolden
import com.example.mykku.contract.support.HandlerCatalog
import com.example.mykku.event.exception.EventErrorCode
import com.example.mykku.event.exception.InvalidWinnerMemberIdsException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerExceptionResolver
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import java.nio.charset.StandardCharsets.UTF_8

@DisplayName("에러 코드별 에러 응답 계약")
class ErrorCodeResponseContractTest : BaseControllerTest() {

    @Autowired
    @Qualifier("handlerExceptionResolver")
    private lateinit var exceptionResolver: HandlerExceptionResolver

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private lateinit var handlerMapping: RequestMappingHandlerMapping

    @Autowired
    private lateinit var handlerAdapter: RequestMappingHandlerAdapter

    @Test
    fun `에러 코드마다 도메인 예외를 지금 핸들러로 해석한 응답은 골든과 같다`() {
        val catalog = HandlerCatalog(handlerMapping, handlerAdapter)
        val errorCodes = DomainExceptions.errorCodes()

        val lines = ResolutionContext.entries.flatMap { context ->
            val handler = catalog.find(context.method, context.pattern).handlerMethod.createWithResolvedBean()
            errorCodes.map { "${context.label} ${it.code} ${resolve(context, handler, exceptionOf(context, it))}" }
        }

        val golden = ErrorCodeGoldens.path(ErrorCodeGoldens.RESPONSES)
        ContractGolden.DEFAULT.verify(golden, lines.joinToString("\n", postfix = "\n"))
    }

    @Test
    fun `스캔한 에러 코드는 에러 코드 레지스트리 목록과 같다`() {
        assertThat(DomainExceptions.errorCodes().map { it.code })
            .containsExactlyElementsOf(ErrorCodeRegistry.getAllErrorCodes().map { it.code })
    }

    private fun exceptionOf(context: ResolutionContext, errorCode: DomainErrorCode): Exception {
        if (context == ResolutionContext.ADMIN_EVENT && errorCode == EventErrorCode.INVALID_WINNER_MEMBER_IDS) {
            return InvalidWinnerMemberIdsException(INVALID_MEMBER_IDS)
        }
        return DomainExceptions.create(errorCode)
    }

    private fun resolve(context: ResolutionContext, handler: HandlerMethod, exception: Exception): String {
        val request = MockHttpServletRequest(context.method, context.samplePath)
        val response = MockHttpServletResponse()
        exceptionResolver.resolveException(request, response, handler, exception) ?: return UNHANDLED
        return "${response.status} ${response.contentType} ${String(response.contentAsByteArray, UTF_8)}"
    }

    private enum class ResolutionContext(
        val label: String,
        val method: String,
        val pattern: String,
        val samplePath: String
    ) {
        APP("app", "GET", "/api/error-codes", "/api/error-codes"),
        ADMIN_EVENT("admin-event", "PUT", "/admin/api/v1/events/{eventId}/winners", "/admin/api/v1/events/1/winners")
    }

    private companion object {
        const val UNHANDLED = "UNHANDLED"
        val INVALID_MEMBER_IDS = listOf("contract-probe-1", "contract-probe-2")
    }
}
