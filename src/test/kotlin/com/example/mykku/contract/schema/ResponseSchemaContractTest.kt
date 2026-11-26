package com.example.mykku.contract.schema

import com.example.mykku.BaseControllerTest
import com.example.mykku.contract.dump.HintedEntry
import com.example.mykku.contract.dump.HintedGolden
import com.example.mykku.contract.dump.MvcJackson
import com.example.mykku.contract.dump.StaticDumpGoldens
import com.example.mykku.contract.endpoint.EndpointKeys
import com.example.mykku.contract.support.HandlerCatalog
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping

@DisplayName("응답 스키마 계약")
class ResponseSchemaContractTest : BaseControllerTest() {

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private lateinit var handlerMapping: RequestMappingHandlerMapping

    @Autowired
    private lateinit var handlerAdapter: RequestMappingHandlerAdapter

    @Test
    fun `엔드포인트별 응답 스키마는 골든과 같다`() {
        val mapper = MvcJackson.objectMapper(handlerAdapter)
        val bodies = ResponseBodies(mapper, handlerAdapter.returnValueHandlers.orEmpty())
        val entries = HandlerCatalog(handlerMapping, handlerAdapter).endpoints.map {
            val key = EndpointKeys.of(it)
            HintedEntry(key, SchemaBlocks.render(key, bodies.describe(it)), it.handlerName)
        }

        HintedGolden.verify(StaticDumpGoldens.RESPONSE_SCHEMA, entries)
    }
}
