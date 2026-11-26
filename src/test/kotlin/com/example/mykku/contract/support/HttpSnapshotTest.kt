package com.example.mykku.contract.support

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.readText

@DisplayName("HttpSnapshot 상태·헤더·본문 표현")
class HttpSnapshotTest {

    @Test
    fun `상태와 전송 계층을 뺀 헤더와 pretty JSON 본문을 한 파일로 만든다`() {
        val body = """{"message":"ok","data":{"id":1}}""".toByteArray()

        val snapshot = HttpSnapshot("GET /api/example", 200, SAMPLE_HEADERS, body)

        assertThat(snapshot.render()).isEqualTo(SAMPLE_RENDERED)
    }

    @Test
    fun `본문이 없으면 빈 줄과 본문을 쓰지 않는다`() {
        val snapshot = HttpSnapshot("DELETE /api/example", 204, emptyList(), ByteArray(0))

        assertThat(snapshot.render()).isEqualTo("### DELETE /api/example\nHTTP 204\n")
    }

    @Test
    fun `요청 ID 생성값이 8자리 16진수가 아니면 실패한다`() {
        val snapshot = HttpSnapshot("GET /", 200, listOf(HttpHeader("X-Request-ID", "abc-123")), ByteArray(0))

        assertThatThrownBy { snapshot.render() }
            .isInstanceOf(AssertionError::class.java)
            .hasMessageContaining("X-Request-ID")
    }

    @Test
    fun `JSON이 아닌 본문은 그대로 쓰고 JSON 규칙을 선언하면 실패한다`() {
        val htmlHeaders = listOf(HttpHeader("Content-Type", "text/html;charset=UTF-8"))
        val html = "<html></html>".toByteArray()

        assertThat(HttpSnapshot("GET /admin", 200, htmlHeaders, html).render()).endsWith("\n<html></html>\n")
        assertThatThrownBy { HttpSnapshot("GET /admin", 200, htmlHeaders, html, JsonRules.pageImpl()).render() }
            .isInstanceOf(AssertionError::class.java)
    }

    @Test
    fun `JSON 본문에는 선언한 규칙을 적용하고 본문이 없는데 규칙을 선언하면 실패한다`() {
        val headers = listOf(HttpHeader("Content-Type", "application/json"))
        val rules = JsonRules(masks = listOf(JsonMask("/data/token") { "<JWT>" }))
        val body = """{"data":{"token":"a.b.c"}}""".toByteArray()

        assertThat(HttpSnapshot("POST /login", 200, headers, body, rules).render()).contains("\"token\" : \"<JWT>\"")
        assertThatThrownBy { HttpSnapshot("POST /login", 204, headers, ByteArray(0), rules).render() }
            .isInstanceOf(ContractViolationError::class.java)
    }

    @Test
    fun `verify는 렌더링 결과를 골든 경로에 넘긴다(갱신 모드)`(@TempDir tempDir: Path) {
        val mode = ContractUpdateMode(enabled = true, ci = false)
        val golden = ContractGolden(tempDir.resolve("golden"), tempDir.resolve("actual"), mode)

        HttpSnapshot("DELETE /api/example", 204, emptyList(), ByteArray(0)).verify("wire/x.http", golden)

        assertThat(tempDir.resolve("golden/wire/x.http").readText()).isEqualTo("### DELETE /api/example\nHTTP 204\n")
    }

    private companion object {
        val SAMPLE_HEADERS = listOf(
            HttpHeader("X-Request-ID", "0a1b2c3d"),
            HttpHeader("Transfer-Encoding", "chunked"),
            HttpHeader("Content-Type", "application/json"),
            HttpHeader("Date", "Wed, 25 Nov 2026 00:00:00 GMT"),
            HttpHeader("Vary", "Origin"),
            HttpHeader("Vary", "Access-Control-Request-Method")
        )

        val SAMPLE_RENDERED = """
            ### GET /api/example
            HTTP 200
            Content-Type: application/json
            Vary: Origin
            Vary: Access-Control-Request-Method
            X-Request-ID: <REQ-ID:8hex>

            {
              "message" : "ok",
              "data" : {
                "id" : 1
              }
            }

        """.trimIndent()
    }
}
