package com.example.mykku.contract.jackson

import com.example.mykku.BaseControllerTest
import com.example.mykku.contract.dump.MvcJackson
import com.example.mykku.contract.support.ContractGolden
import com.example.mykku.contract.support.ContractJson
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter

@DisplayName("Jackson 직렬화·역직렬화 프로브 계약")
class JacksonProbeContractTest : BaseControllerTest() {

    @Autowired
    private lateinit var handlerAdapter: RequestMappingHandlerAdapter

    @Test
    fun `프로브 DTO를 MVC JSON 컨버터로 쓴 결과는 골든과 같다`() {
        val converter = MvcJackson.converter(handlerAdapter)
        val tree = JsonNodeFactory.instance.objectNode()
        JacksonProbes.serialization().forEach { tree.replace(it.name, it.serialize(converter)) }

        ContractGolden.DEFAULT.verify(SERIALIZE, ContractJson.pretty(tree) + "\n")
    }

    @Test
    fun `프로브 JSON을 MVC JSON 컨버터로 읽은 결과는 골든과 같다`() {
        val converter = MvcJackson.converter(handlerAdapter)
        val lines = JacksonProbes.deserialization().map { it.describe(converter) }

        ContractGolden.DEFAULT.verify(DESERIALIZE, lines.joinToString("\n", postfix = "\n"))
    }

    companion object {
        private const val SERIALIZE = "wire/jackson/serialize.json"
        private const val DESERIALIZE = "wire/jackson/deserialize.txt"

        val GOLDENS: List<String> = listOf(SERIALIZE, DESERIALIZE)
    }
}
