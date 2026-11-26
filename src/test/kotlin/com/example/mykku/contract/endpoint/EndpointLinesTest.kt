package com.example.mykku.contract.endpoint

import com.example.mykku.BaseControllerTest
import com.example.mykku.contract.schema.RequestTargets
import com.example.mykku.contract.support.Endpoint
import com.example.mykku.contract.support.HandlerArgumentReader
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.support.StaticListableBeanFactory
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.http.HttpEntity
import org.springframework.http.MediaType
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.validation.BindingResult
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.WebDataBinder
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.InitBinder
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.method.ControllerAdviceBean
import org.springframework.web.method.HandlerMethod
import org.springframework.web.method.annotation.HandlerMethodValidationException
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter

@DisplayName("엔드포인트 줄 표기")
class EndpointLinesTest : BaseControllerTest() {

    @Autowired
    private lateinit var handlerAdapter: RequestMappingHandlerAdapter

    @Test
    fun `인자 제약·검증 그룹·Errors·바인딩 인자·HttpEntity 본문과 메서드 검증을 적고 서블릿 요청은 뺀다`() {
        assertThat(EndpointLines({ false }, tokens()).render(endpoint("probe"))).isEqualTo(
            "GET /probe auth=none (query keyword:String required @NotBlank(message=\"키워드\"), " +
                "body:json valid(groups=[TokenProbeGroup]), errors, " +
                "framework:RequestAttributeMethodArgumentResolver trace:String, body:http-entity valid(무시됨)) " +
                "method-validation"
        )
    }

    @Test
    fun `적용 범위 안의 어드바이스 @InitBinder만 토큰으로 적는다`() {
        val applicable = advice("applicable", arrayOf(TokenProbeController::class.java))
        val elsewhere = advice("elsewhere", arrayOf(String::class.java))
        val scoped = EndpointLines({ false }, tokens(), listOf(applicable)).render(endpoint("lists"))
        val unscoped = EndpointLines({ false }, tokens(), listOf(elsewhere)).render(endpoint("lists"))

        assertThat(scoped).endsWith(" init-binder")
        assertThat(unscoped).doesNotContain("init-binder")
    }

    @Test
    fun `HttpEntity와 @Validated 목록 본문은 검증하지 않고 @Valid 목록 본문만 검증한다`() {
        val probeTargets = RequestTargets(tokens()).of(endpoint("probe")).map { it.validated }
        val listTargets = RequestTargets(tokens()).of(endpoint("lists")).map { it.validated }

        assertThat(probeTargets).containsExactly(true, false)
        assertThat(listTargets).containsExactly(false, true)
    }

    @Test
    fun `@Valid Set 본문은 다른 인자가 메서드 검증을 켤 때만 원소를 검증하고 덤프도 그렇게 적는다`() {
        val alone = endpoint("sets")
        val constrained = endpoint("constrainedSets")

        assertThat(RequestTargets(tokens()).of(alone).map { it.validated }).containsExactly(false)
        assertThat(RequestTargets(tokens()).of(constrained).map { it.validated }).containsExactly(true)
        assertThatCode { invoke(alone) }.doesNotThrowAnyException()
        assertThatThrownBy { invoke(constrained) }.isInstanceOf(HandlerMethodValidationException::class.java)
    }

    @Test
    fun `핸들러 메서드에 붙은 @ModelAttribute는 빼고 모델 속성 메서드만 토큰으로 적는다`() {
        val model = advice("model", arrayOf(TokenProbeController::class.java), ModelProbeAdvice())
        val plain = EndpointLines({ false }, tokens()).render(endpoint("form"))
        val advised = EndpointLines({ false }, tokens(), listOf(model)).render(endpoint("form"))

        assertThat(plain).isEqualTo("GET /form auth=none ()")
        assertThat(advised).isEqualTo("GET /form auth=none () model-attribute-method")
    }

    private fun tokens(): ArgumentTokens = ArgumentTokens(handlerAdapter.argumentResolvers.orEmpty())

    private fun endpoint(name: String): Endpoint {
        val method = TokenProbeController::class.java.methods.single { it.name == name }
        val handler = HandlerMethod(TokenProbeController(), method).createWithValidateFlags()
        val arguments = HandlerArgumentReader(handlerAdapter.argumentResolvers.orEmpty()).read(handler)
        return Endpoint("GET", "/$name", emptyList(), emptyList(), emptyList(), emptyList(), arguments, handler)
    }

    private fun invoke(endpoint: Endpoint) {
        val request = MockHttpServletRequest("POST", "/probe").apply {
            contentType = MediaType.APPLICATION_JSON_VALUE
            setContent(BLANK_NAME_SET.toByteArray())
            addParameter("keyword", "k")
        }
        handlerAdapter.handle(request, MockHttpServletResponse(), endpoint.handlerMethod)
    }

    private fun advice(
        name: String,
        assignableTypes: Array<Class<*>>,
        bean: Any = TokenProbeAdvice()
    ): ControllerAdviceBean {
        val factory = StaticListableBeanFactory().apply { addBean(name, bean) }
        val annotation = AnnotationUtils.synthesizeAnnotation(
            mapOf("assignableTypes" to assignableTypes),
            ControllerAdvice::class.java,
            null
        )
        return ControllerAdviceBean(name, factory, annotation)
    }

    private companion object {
        const val BLANK_NAME_SET = """[{"name":""}]"""
    }
}

interface TokenProbeGroup

data class TokenProbeBody(val name: String)

data class TokenProbeConstrainedBody(@field:NotBlank(message = "이름") val name: String)

class TokenProbeAdvice {
    @InitBinder
    @Suppress("UNUSED_PARAMETER")
    fun initBinder(binder: WebDataBinder) = Unit
}

class ModelProbeAdvice {
    @ModelAttribute("common")
    fun common(): String = "common"
}

class TokenProbeController {
    @Suppress("UNUSED_PARAMETER")
    fun probe(
        @RequestParam @NotBlank(message = "키워드") keyword: String,
        @Validated(TokenProbeGroup::class) @RequestBody body: TokenProbeBody,
        errors: BindingResult,
        @RequestAttribute("trace") trace: String,
        request: HttpServletRequest,
        @Valid entity: HttpEntity<TokenProbeBody>
    ) = Unit

    @Suppress("UNUSED_PARAMETER")
    fun lists(
        @Validated @RequestBody validatedList: List<TokenProbeBody>,
        @Valid @RequestBody validList: List<TokenProbeBody>
    ) = Unit

    @Suppress("UNUSED_PARAMETER")
    fun sets(@Valid @RequestBody validSet: Set<TokenProbeConstrainedBody>) = Unit

    @Suppress("UNUSED_PARAMETER")
    fun constrainedSets(
        @Valid @RequestBody validSet: Set<TokenProbeConstrainedBody>,
        @RequestParam @NotBlank(message = "키워드") keyword: String
    ) = Unit

    @GetMapping("/form")
    @ModelAttribute("form")
    fun form(): TokenProbeBody = TokenProbeBody("form")
}
