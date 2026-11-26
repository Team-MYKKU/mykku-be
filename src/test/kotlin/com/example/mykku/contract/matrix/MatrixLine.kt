package com.example.mykku.contract.matrix

import com.example.mykku.contract.support.ContractJson
import com.example.mykku.contract.support.ContractViolationError
import com.example.mykku.contract.support.JsonRules
import com.fasterxml.jackson.databind.ObjectWriter
import com.fasterxml.jackson.databind.json.JsonMapper
import io.restassured.response.Response
import org.springframework.http.MediaType

object MatrixLine {

    private const val SEPARATOR = " | "
    private const val ABSENT = "-"
    private const val VIOLATION_PREFIX = "[API 계약 위반] "
    private val compactWriter: ObjectWriter = JsonMapper.builder().build().writer()

    fun of(vararg columns: String): String = columns.joinToString(SEPARATOR)

    fun outcome(response: Response, rules: JsonRules, request: String): String = try {
        listOf(response.statusCode.toString(), contentTypeOf(response).ifEmpty { ABSENT }, body(response, rules))
            .joinToString(" ")
    } catch (violation: ContractViolationError) {
        val detail = violation.message.orEmpty().removePrefix(VIOLATION_PREFIX)
        throw ContractViolationError("$VIOLATION_PREFIX$request → HTTP ${response.statusCode}: $detail", violation)
    }

    private fun body(response: Response, rules: JsonRules): String {
        val bytes = response.asByteArray()
        if (bytes.isNotEmpty() && isJson(contentTypeOf(response))) {
            return compactWriter.writeValueAsString(ContractJson.parse(ContractJson.render(bytes, rules)))
        }
        if (!rules.isEmpty) {
            throw ContractViolationError("JSON 규칙을 선언했지만 ${bodyProblem(bytes, contentTypeOf(response))}")
        }
        return if (bytes.isEmpty()) ABSENT else escape(ContractJson.decodeUtf8(bytes))
    }

    private fun contentTypeOf(response: Response): String = response.contentType.orEmpty()

    private fun bodyProblem(bytes: ByteArray, contentType: String): String =
        if (bytes.isEmpty()) "응답 본문이 비어 있습니다" else "응답이 JSON이 아닙니다: ${contentType.ifEmpty { ABSENT }}"

    private fun isJson(contentType: String): Boolean {
        if (contentType.isEmpty()) {
            return false
        }
        val mediaType = MediaType.parseMediaType(contentType)
        return MediaType.APPLICATION_JSON.isCompatibleWith(mediaType) || mediaType.subtype.endsWith("+json")
    }

    private fun escape(text: String): String =
        text.replace("\\", "\\\\").replace("\r", "\\r").replace("\n", "\\n")
}
