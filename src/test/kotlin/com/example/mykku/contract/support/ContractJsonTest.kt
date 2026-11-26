package com.example.mykku.contract.support

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("ContractJson 정규형 검사·마스킹·키 순서 정규화")
class ContractJsonTest {

    @Test
    fun `키 순서를 지킨 채 REST Docs와 같은 형식으로 pretty 출력한다`() {
        val rendered = ContractJson.render("""{"b":1,"a":[{"y":null,"x":"가"}],"e":{},"f":[]}""".toByteArray())

        assertThat(rendered).isEqualTo(
            """
            {
              "b" : 1,
              "a" : [ {
                "y" : null,
                "x" : "가"
              } ],
              "e" : { },
              "f" : [ ]
            }
            """.trimIndent()
        )
    }

    @Test
    fun `원문이 compact 정규형이 아니면 실패한다`() {
        listOf("""{"a": 1}""", """{"a":"\u00e9"}""", """{"a":1.0e1}""").forEach { body ->
            assertThatThrownBy { ContractJson.render(body.toByteArray()) }
                .`as`(body)
                .isInstanceOf(AssertionError::class.java)
                .hasMessageContaining("compact 정규형")
        }
    }

    @Test
    fun `소수의 끝자리 0을 지우지 않는다`() {
        assertThat(ContractJson.render("""{"a":1.50}""".toByteArray())).contains("\"a\" : 1.50")
    }

    @Test
    fun `중복 키와 뒤따르는 토큰은 파서 단계에서 실패한다`() {
        assertThatThrownBy { ContractJson.parse("""{"a":1,"a":2}""") }
            .isInstanceOf(ContractViolationError::class.java)
            .hasMessageContaining("Duplicate field 'a'")
        assertThatThrownBy { ContractJson.parse("""{"a":1}{}""") }
            .isInstanceOf(ContractViolationError::class.java)
            .hasMessageContaining("Trailing token")
    }

    @Test
    fun `UTF-8이 아닌 바이트는 디코딩 단계에서 실패한다`() {
        val body = "{\"a\":\"".toByteArray() + byteArrayOf(0xC3.toByte()) + "\"}".toByteArray()

        assertThatThrownBy { ContractJson.decodeUtf8(body) }
            .isInstanceOf(ContractViolationError::class.java)
            .hasMessageContaining("UTF-8")
    }

    @Test
    fun `선언한 경로만 마스킹한다`() {
        val rules = JsonRules(masks = listOf(JsonMask("/data/*/at") { "<T:${it.asText().length}>" }))

        val rendered = ContractJson.render("""{"data":[{"at":"2026","n":1},{"at":"20","n":2}]}""".toByteArray(), rules)

        assertThat(rendered).contains("\"at\" : \"<T:4>\"", "\"at\" : \"<T:2>\"", "\"n\" : 1", "\"n\" : 2")
    }

    @Test
    fun `숫자 색인과 객체 와일드카드 경로도 마스킹한다`() {
        val rules = JsonRules(masks = listOf(JsonMask("/list/1/t") { "<B>" }, JsonMask("/map/*") { "<V>" }))
        val body = """{"list":[{"t":"a"},{"t":"b"}],"map":{"x":"1","y":"2"}}"""

        assertThat(compact(ContractJson.render(body.toByteArray(), rules)))
            .isEqualTo("""{"list":[{"t":"a"},{"t":"<B>"}],"map":{"x":"<V>","y":"<V>"}}""")
    }

    @Test
    fun `와일드카드로 고른 원소 중 하나라도 키가 없으면 실패한다`() {
        val rules = JsonRules(masks = listOf(JsonMask("/list/*/t") { "<T>" }))

        assertThatThrownBy { ContractJson.render("""{"list":[{"t":"a"},{"u":"b"}]}""".toByteArray(), rules) }
            .isInstanceOf(ContractViolationError::class.java)
            .hasMessageContaining("/list/*/t")
    }

    @Test
    fun `마스크 경로가 응답에 없으면 실패한다`() {
        listOf("/data/missing", "/data/*/at").forEach { path ->
            val rules = JsonRules(masks = listOf(JsonMask(path) { "<X>" }))

            assertThatThrownBy { ContractJson.render("""{"data":[]}""".toByteArray(), rules) }
                .`as`(path)
                .isInstanceOf(AssertionError::class.java)
                .hasMessageContaining(path)
        }
    }

    @Test
    fun `PageImpl 경로의 객체만 키를 이름순으로 다시 놓는다`() {
        val body = """{"message":"ok","data":{"content":[{"z":1,"a":2}],"pageable":{"sort":{"unsorted":false,""" +
            """"empty":true},"offset":0},"last":true,"sort":{"sorted":false,"empty":true}}}"""

        val rendered = ContractJson.render(body.toByteArray(), JsonRules.pageImpl())

        assertThat(compact(rendered)).isEqualTo(
            """{"message":"ok","data":{"content":[{"z":1,"a":2}],"last":true,"pageable":{"offset":0,""" +
                """"sort":{"empty":true,"unsorted":false}},"sort":{"empty":true,"sorted":false}}}"""
        )
    }

    @Test
    fun `키 순서 정규화 경로가 없거나 객체가 아니면 실패한다`() {
        val missing = """{"message":"ok","data":{"content":[],"sort":{}}}"""
        val notObject = """{"message":"ok","data":[]}"""

        listOf(missing, notObject).forEach { body ->
            assertThatThrownBy { ContractJson.render(body.toByteArray(), JsonRules.pageImpl()) }
                .`as`(body)
                .isInstanceOf(AssertionError::class.java)
        }
    }

    @Test
    fun `규칙을 더하면 마스킹과 키 순서 정규화를 함께 한다`() {
        val rules = JsonRules.pageImpl("") + JsonRules(masks = listOf(JsonMask("/content/*") { "<URL>" }))
        val body = """{"sort":{"sorted":true,"empty":false},"pageable":{"sort":{"b":1,"a":2}},"content":["u1"]}"""

        val expected = """{"content":["<URL>"],"pageable":{"sort":{"a":2,"b":1}},""" +
            """"sort":{"empty":false,"sorted":true}}"""

        assertThat(compact(ContractJson.render(body.toByteArray(), rules))).isEqualTo(expected)
    }

    private fun compact(pretty: String): String = ContractJson.parse(pretty).toString()
}
