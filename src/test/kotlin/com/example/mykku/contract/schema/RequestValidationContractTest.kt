package com.example.mykku.contract.schema

import com.example.mykku.BaseControllerTest
import com.example.mykku.contract.dump.HintedEntry
import com.example.mykku.contract.dump.HintedGolden
import com.example.mykku.contract.dump.MvcJackson
import com.example.mykku.contract.endpoint.ArgumentTokens
import com.example.mykku.contract.support.HandlerCatalog
import jakarta.validation.Validator
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping

@DisplayName("요청 DTO·검증 규칙 계약")
class RequestValidationContractTest : BaseControllerTest() {

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private lateinit var handlerMapping: RequestMappingHandlerMapping

    @Autowired
    private lateinit var handlerAdapter: RequestMappingHandlerAdapter

    @Autowired
    private lateinit var validator: Validator

    @Test
    fun `요청 본문·JSON 파트·폼 DTO의 필드와 검증 규칙은 골든과 같다`() {
        val targets = RequestTargets(ArgumentTokens(handlerAdapter.argumentResolvers.orEmpty()))
        val writer = RequestSchemaWriter(MvcJackson.objectMapper(handlerAdapter), validator)
        val entries = HandlerCatalog(handlerMapping, handlerAdapter).endpoints.flatMap { endpoint ->
            targets.of(endpoint).map { HintedEntry(it.header, writer.render(it), endpoint.handlerName) }
        }

        HintedGolden.verify(GOLDEN, entries)
    }

    companion object {
        private const val GOLDEN = "wire/schema/request.txt"

        val GOLDENS: List<String> = listOf(GOLDEN)
    }
}
