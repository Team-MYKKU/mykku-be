package com.example.mykku.contract.jackson

import com.example.mykku.BaseControllerTest
import com.example.mykku.contract.dump.MvcJackson
import com.example.mykku.contract.dump.MvcBeans
import com.example.mykku.contract.support.ContractGolden
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.web.servlet.HandlerExceptionResolver
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter

@DisplayName("Jackson 설정 계약")
class JacksonConfigContractTest : BaseControllerTest() {

    @Autowired
    private lateinit var handlerAdapter: RequestMappingHandlerAdapter

    @Autowired
    @Qualifier("handlerExceptionResolver")
    private lateinit var exceptionResolver: HandlerExceptionResolver

    @Test
    fun `MVC JSON 컨버터의 ObjectMapper 설정은 골든과 같다`() {
        val dump = JacksonConfigDump(MvcJackson.objectMapper(handlerAdapter)).render()

        ContractGolden.DEFAULT.verify(GOLDEN, dump)
    }

    @Test
    fun `에러 응답을 쓰는 JSON 컨버터도 같은 ObjectMapper를 쓴다`() {
        val resolver = MvcBeans.exceptionHandlerResolver(exceptionResolver)

        assertThat(MvcJackson.converter(resolver.messageConverters).objectMapper)
            .isSameAs(MvcJackson.objectMapper(handlerAdapter))
    }

    companion object {
        private const val GOLDEN = "wire/jackson/config.txt"

        val GOLDENS: List<String> = listOf(GOLDEN)
    }
}
