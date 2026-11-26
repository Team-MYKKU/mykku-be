package com.example.mykku.contract.endpoint

import com.example.mykku.BaseControllerTest
import com.example.mykku.contract.dump.HintedEntry
import com.example.mykku.contract.dump.HintedGolden
import com.example.mykku.contract.support.EndpointArea
import com.example.mykku.contract.support.HandlerCatalog
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.ApplicationContext
import org.springframework.web.method.ControllerAdviceBean
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping

@DisplayName("엔드포인트 목록 계약")
class EndpointContractTest : BaseControllerTest() {

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private lateinit var handlerMapping: RequestMappingHandlerMapping

    @Autowired
    private lateinit var handlerAdapter: RequestMappingHandlerAdapter

    @Autowired
    private lateinit var context: ApplicationContext

    @ParameterizedTest(name = "{0}")
    @EnumSource(EndpointArea::class)
    fun `영역별 엔드포인트 목록은 골든과 같다`(area: EndpointArea) {
        val tokens = ArgumentTokens(handlerAdapter.argumentResolvers.orEmpty())
        val advices = ControllerAdviceBean.findAnnotatedBeans(context)
        val lines = EndpointLines(AdminSessionProbe(handlerMapping)::requires, tokens, advices)
        val entries = HandlerCatalog(handlerMapping, handlerAdapter).inArea(area).map {
            HintedEntry(EndpointKeys.of(it), lines.render(it) + "\n", it.handlerName)
        }

        HintedGolden.verify(golden(area), entries)
    }

    companion object {
        private const val DIRECTORY = "wire/endpoints"

        fun golden(area: EndpointArea): String = "$DIRECTORY/${area.name.lowercase()}.txt"

        val GOLDENS: List<String> = EndpointArea.entries.map { golden(it) }
    }
}
