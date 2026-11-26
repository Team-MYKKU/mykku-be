package com.example.mykku.contract.endpoint

import com.example.mykku.BaseControllerTest
import com.example.mykku.contract.support.Endpoint
import com.example.mykku.contract.support.HandlerArgumentReader
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.constraints.NotBlank
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpEntity
import org.springframework.validation.BindingResult
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter

@DisplayName("엔드포인트 줄 표기")
class EndpointLinesTest : BaseControllerTest() {

    @Autowired
    private lateinit var handlerAdapter: RequestMappingHandlerAdapter

    @Test
    fun `인자 제약·검증 그룹·Errors·바인딩 인자·HttpEntity 본문과 메서드 검증을 적고 서블릿 요청은 뺀다`() {
        val tokens = ArgumentTokens(handlerAdapter.argumentResolvers.orEmpty())

        assertThat(EndpointLines({ false }, tokens).render(probeEndpoint())).isEqualTo(
            "GET /probe auth=none (query keyword:String required @NotBlank(message=\"키워드\"), " +
                "body:json valid(groups=[TokenProbeGroup]), errors, " +
                "framework:RequestAttributeMethodArgumentResolver trace:String, body:http-entity) method-validation"
        )
    }

    private fun probeEndpoint(): Endpoint {
        val method = TokenProbeController::class.java.methods.single { it.name == "probe" }
        val handler = HandlerMethod(TokenProbeController(), method).createWithValidateFlags()
        val arguments = HandlerArgumentReader(handlerAdapter.argumentResolvers.orEmpty()).read(handler)
        return Endpoint("GET", "/probe", emptyList(), emptyList(), emptyList(), emptyList(), arguments, handler)
    }
}

interface TokenProbeGroup

data class TokenProbeBody(val name: String)

class TokenProbeController {
    @Suppress("UNUSED_PARAMETER")
    fun probe(
        @RequestParam @NotBlank(message = "키워드") keyword: String,
        @Validated(TokenProbeGroup::class) @RequestBody body: TokenProbeBody,
        errors: BindingResult,
        @RequestAttribute("trace") trace: String,
        request: HttpServletRequest,
        entity: HttpEntity<TokenProbeBody>
    ) = Unit
}
