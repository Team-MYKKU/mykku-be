package com.example.mykku.contract.mvc

import com.example.mykku.BaseControllerTest
import com.example.mykku.contract.support.ContractGolden
import com.example.mykku.contract.support.HandlerCatalog
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.ApplicationContext
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping

@DisplayName("MVC·서블릿 설정 계약")
class MvcConfigContractTest : BaseControllerTest() {

    @Autowired
    private lateinit var context: ApplicationContext

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private lateinit var handlerMapping: RequestMappingHandlerMapping

    @Autowired
    private lateinit var handlerAdapter: RequestMappingHandlerAdapter

    @Test
    fun `test 프로필 컨텍스트의 MVC·서블릿 설정은 골든과 같다`() {
        ContractGolden.DEFAULT.verify(GOLDEN, MvcConfigDump(context).render())
    }

    @Test
    fun `변환기 대상과 견주는 핸들러 인자 타입은 변환 서비스처럼 원시 타입을 박싱해 모은다`() {
        val bound = BoundTypes.of(HandlerCatalog(handlerMapping, handlerAdapter))

        assertThat(bound).contains(Int::class.javaObjectType, Long::class.javaObjectType)
        assertThat(bound.filter { it.isPrimitive }).isEmpty()
    }

    companion object {
        private const val GOLDEN = "wire/mvc/config.txt"

        val GOLDENS: List<String> = listOf(GOLDEN)
    }
}
