package com.example.mykku.contract.mvc

import com.example.mykku.BaseControllerTest
import com.example.mykku.contract.support.ContractGolden
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext

@DisplayName("MVC·서블릿 설정 계약")
class MvcConfigContractTest : BaseControllerTest() {

    @Autowired
    private lateinit var context: ApplicationContext

    @Test
    fun `test 프로필 컨텍스트의 MVC·서블릿 설정은 골든과 같다`() {
        ContractGolden.DEFAULT.verify(GOLDEN, MvcConfigDump(context).render())
    }

    companion object {
        private const val GOLDEN = "wire/mvc/config.txt"

        val GOLDENS: List<String> = listOf(GOLDEN)
    }
}
