package com.example.mykku.contract.header

import com.example.mykku.contract.support.ContractViolationError
import io.restassured.builder.ResponseBuilder
import io.restassured.http.Header
import io.restassured.http.Headers
import io.restassured.response.Response
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("응답 헤더 요약")
class HeaderSummaryTest {

    @Test
    fun `보내지 않은 요청 ID가 8자리 16진수면 생성값으로 적고 전송 계층 헤더는 빼고 이름순으로 중복까지 적는다`() {
        val response = response(404, REQUEST_ID to "0a1b2c3d", "Vary" to "Origin", "Date" to "x", "Vary" to "Accept")

        assertThat(HeaderSummary.of(response, null)).isEqualTo(
            "HTTP 404 | 헤더 [Vary, Vary, X-Request-ID] | Content-Type 없음 | X-Request-ID $GENERATED"
        )
    }

    @Test
    fun `보낸 요청 ID에 따라 그대로·생성값으로 바뀜·바뀐 값을 구분해 적는다`() {
        assertThat(requestIdRule(sent = "abc-123", actual = "abc-123")).isEqualTo("X-Request-ID 보낸 값 그대로")
        assertThat(requestIdRule(sent = "abc-123", actual = "0a1b2c3d")).isEqualTo("X-Request-ID 보낸 값 대신 $GENERATED")
        assertThat(requestIdRule(sent = "abc def", actual = "abc")).isEqualTo("X-Request-ID 보낸 값을 바꾼 값 'abc'")
        assertThat(requestIdRule(sent = null, actual = null)).isEqualTo("X-Request-ID 없음")
    }

    @Test
    fun `보내지 않은 요청 ID가 8자리 소문자 16진수가 아니면 값을 쓰지 않고 실패한다`() {
        assertThatThrownBy { HeaderSummary.of(response(200, REQUEST_ID to "0A1B2C3D"), null) }
            .isInstanceOf(ContractViolationError::class.java)
            .hasMessageNotContaining("0A1B2C3D")
    }

    @Test
    fun `세션 쿠키는 세션 ID만 가리고 속성은 그대로 적으며 세션 ID 형식이 다르면 실패한다`() {
        val cookie = "JSESSIONID=0123456789ABCDEF0123456789ABCDEF; Path=/; HttpOnly"

        assertThat(HeaderSummary.of(response(302, SET_COOKIE to cookie), null))
            .endsWith("| Set-Cookie JSESSIONID=<SESSION:32HEX>; Path=/; HttpOnly")
        assertThatThrownBy { HeaderSummary.of(response(302, SET_COOKIE to "JSESSIONID=short; Path=/"), null) }
            .isInstanceOf(ContractViolationError::class.java)
    }

    private fun requestIdRule(sent: String?, actual: String?): String {
        val headers = listOfNotNull(actual?.let { REQUEST_ID to it }).toTypedArray()
        return HeaderSummary.of(response(200, *headers), sent).substringAfterLast(" | ")
    }

    private fun response(status: Int, vararg headers: Pair<String, String>): Response = ResponseBuilder()
        .setStatusCode(status)
        .setHeaders(Headers(headers.map { (name, value) -> Header(name, value) }))
        .setBody("")
        .build()

    private companion object {
        const val REQUEST_ID = "X-Request-ID"
        const val SET_COOKIE = "Set-Cookie"
        const val GENERATED = "생성값(^[0-9a-f]{8}$)"
    }
}
