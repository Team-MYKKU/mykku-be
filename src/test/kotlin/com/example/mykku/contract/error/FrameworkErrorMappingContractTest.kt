package com.example.mykku.contract.error

import com.example.mykku.BaseControllerTest
import com.example.mykku.contract.support.ContractGolden
import com.example.mykku.contract.support.HandlerCatalog
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

@DisplayName("프레임워크 예외 매핑 계약")
class FrameworkErrorMappingContractTest : BaseControllerTest() {

    @Autowired
    @Qualifier("handlerExceptionResolver")
    private lateinit var exceptionResolver: HandlerExceptionResolver

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private lateinit var handlerMapping: RequestMappingHandlerMapping

    @Autowired
    private lateinit var handlerAdapter: RequestMappingHandlerAdapter

    @Test
    fun `프레임워크·인프라 예외를 지금 핸들러로 해석한 응답은 골든과 같다`() {
        val catalog = HandlerCatalog(handlerMapping, handlerAdapter)

        val lines = ResolutionContext.entries.flatMap { context ->
            val handler = handlerOf(catalog, context)
            FrameworkExceptions.cases().map { (label, exception) -> line(context, handler, label, exception) }
        }

        val golden = ErrorGoldens.path(ErrorGoldens.FRAMEWORK_MAPPING)
        ContractGolden.DEFAULT.verify(golden, lines.joinToString("\n", postfix = "\n"))
    }

    private fun handlerOf(catalog: HandlerCatalog, context: ResolutionContext): HandlerMethod? =
        context.pattern?.let { catalog.find(context.method, it).handlerMethod.createWithResolvedBean() }

    private fun line(context: ResolutionContext, handler: HandlerMethod?, label: String, exception: Exception): String =
        "${context.label} $label ${resolve(context, handler, exception)}"

    private fun resolve(context: ResolutionContext, handler: HandlerMethod?, exception: Exception): String {
        val request = MockHttpServletRequest(context.method, context.samplePath)
        val response = MockHttpServletResponse()
        exceptionResolver.resolveException(request, response, handler, exception) ?: return UNHANDLED
        return "${response.status} ${response.contentType} ${String(response.contentAsByteArray, UTF_8)}"
    }

    private enum class ResolutionContext(
        val label: String,
        val method: String,
        val pattern: String?,
        val samplePath: String
    ) {
        APP("app", "GET", "/api/error-codes", "/api/error-codes"),
        ADMIN_EVENT("admin-event", "PUT", "/admin/api/v1/events/{eventId}/winners", "/admin/api/v1/events/1/winners"),
        NO_HANDLER("no-handler", "GET", null, "/api/v1/contract-probe")
    }

    private companion object {
        const val UNHANDLED = "UNHANDLED"
    }
}
