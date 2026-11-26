package com.example.mykku.contract.support

import io.restassured.response.Response
import org.springframework.http.MediaType

data class HttpHeader(val name: String, val value: String)

class HttpSnapshot(
    private val title: String,
    private val status: Int,
    private val headers: List<HttpHeader>,
    private val body: ByteArray,
    private val rules: JsonRules = JsonRules()
) {

    fun render(): String = buildString {
        append("### ").append(title).append('\n')
        append("HTTP ").append(status).append('\n')
        renderedHeaders().forEach { append(it).append('\n') }
        renderedBody()?.let { append('\n').append(it).append('\n') }
    }

    fun verify(goldenPath: String, golden: ContractGolden = ContractGolden.DEFAULT) {
        golden.verify(goldenPath, render())
    }

    private fun renderedHeaders(): List<String> = headers
        .filterNot { it.name.lowercase() in TRANSPORT_HEADERS }
        .sortedBy { it.name.lowercase() }
        .map { "${it.name}: ${renderedHeaderValue(it)}" }

    private fun renderedHeaderValue(header: HttpHeader): String {
        if (!header.name.equals(REQUEST_ID_HEADER, ignoreCase = true)) {
            return header.value
        }
        if (!GENERATED_REQUEST_ID.matches(header.value)) {
            throw ContractViolationError(
                "[API 계약 위반] $REQUEST_ID_HEADER 생성값이 8자리 소문자 16진수가 아닙니다: '${header.value}'"
            )
        }
        return REQUEST_ID_TOKEN
    }

    private fun renderedBody(): String? {
        if (body.isNotEmpty() && isJson()) {
            return ContractJson.render(body, rules)
        }
        if (!rules.isEmpty) {
            throw ContractViolationError("[API 계약 위반] $title: JSON 규칙을 선언했지만 ${bodyProblem()}")
        }
        return ContractJson.decodeUtf8(body).ifEmpty { null }
    }

    private fun bodyProblem(): String =
        if (body.isEmpty()) "응답 본문이 비어 있습니다(HTTP $status)" else "응답이 JSON이 아닙니다: ${contentType()}"

    private fun isJson(): Boolean {
        val mediaType = contentType()?.let { MediaType.parseMediaType(it) } ?: return false
        return MediaType.APPLICATION_JSON.isCompatibleWith(mediaType) || mediaType.subtype.endsWith("+json")
    }

    private fun contentType(): String? =
        headers.firstOrNull { it.name.equals(CONTENT_TYPE_HEADER, ignoreCase = true) }?.value

    companion object {
        private const val CONTENT_TYPE_HEADER = "Content-Type"
        private const val REQUEST_ID_HEADER = "X-Request-ID"
        private const val REQUEST_ID_TOKEN = "<REQ-ID:8hex>"
        private val GENERATED_REQUEST_ID = Regex("[0-9a-f]{8}")
        private val TRANSPORT_HEADERS = setOf("date", "keep-alive", "connection", "transfer-encoding", "content-length")

        fun of(title: String, response: Response, rules: JsonRules = JsonRules()): HttpSnapshot = HttpSnapshot(
            title = title,
            status = response.statusCode,
            headers = response.headers.asList().map { HttpHeader(it.name, it.value) },
            body = response.asByteArray(),
            rules = rules
        )
    }
}
