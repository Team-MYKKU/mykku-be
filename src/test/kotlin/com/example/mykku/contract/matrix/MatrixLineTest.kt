package com.example.mykku.contract.matrix

import com.example.mykku.contract.support.ContractViolationError
import com.example.mykku.contract.support.JsonRules
import io.restassured.builder.ResponseBuilder
import io.restassured.response.Response
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("매트릭스 줄")
class MatrixLineTest {

    @Test
    fun `JSON 본문은 compact 한 줄로 쓰고 선언한 경로의 키 순서만 이름순으로 바꾼다`() {
        val response = response(200, JSON, """{"message":"m","data":{"b":1,"a":[{"d":2,"c":3}]}}""")

        assertThat(MatrixLine.outcome(response, JsonRules(sortedKeyPaths = listOf("/data")), REQUEST))
            .isEqualTo("""200 application/json {"message":"m","data":{"a":[{"d":2,"c":3}],"b":1}}""")
    }

    @Test
    fun `본문과 Content-Type이 없으면 -로 쓰고 JSON이 아닌 본문은 역슬래시·CR·LF를 이스케이프한다`() {
        assertThat(MatrixLine.outcome(response(204, null, ""), JsonRules(), REQUEST)).isEqualTo("204 - -")
        assertThat(MatrixLine.outcome(response(200, "text/plain", "a\\b\r\nc"), JsonRules(), REQUEST))
            .isEqualTo("200 text/plain a\\\\b\\r\\nc")
    }

    @Test
    fun `JSON 규칙을 선언했는데 본문이 비었거나 JSON이 아니면 요청 설명을 담아 실패한다`() {
        val rules = JsonRules(sortedKeyPaths = listOf("/data"))

        assertThatThrownBy { MatrixLine.outcome(response(200, JSON, ""), rules, REQUEST) }
            .isInstanceOf(ContractViolationError::class.java).hasMessageContaining(REQUEST).hasMessageContaining("비어")
        assertThatThrownBy { MatrixLine.outcome(response(200, "text/plain", "x"), rules, REQUEST) }
            .isInstanceOf(ContractViolationError::class.java).hasMessageContaining("JSON이 아닙니다")
    }

    @Test
    fun `원문이 compact 정규형이 아니면 접두를 한 번만 붙이고 요청 설명을 담아 실패한다`() {
        assertThatThrownBy { MatrixLine.outcome(response(200, JSON, """{ "a":1}"""), JsonRules(), REQUEST) }
            .isInstanceOf(ContractViolationError::class.java)
            .hasMessageStartingWith("[API 계약 위반] $REQUEST → HTTP 200: 응답 원문이 compact 정규형과 다릅니다")
    }

    private fun response(status: Int, contentType: String?, body: String): Response {
        val builder = ResponseBuilder().setStatusCode(status).setBody(body)
        contentType?.let { builder.setContentType(it) }
        return builder.build()
    }

    private companion object {
        const val JSON = "application/json"
        const val REQUEST = "GET /api/v1/contract-probe"
    }
}
