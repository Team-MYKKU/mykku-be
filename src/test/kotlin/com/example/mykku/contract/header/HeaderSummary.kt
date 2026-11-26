package com.example.mykku.contract.header

import com.example.mykku.contract.support.ContractViolationError
import io.restassured.response.Response

object HeaderSummary {

    private const val REQUEST_ID_HEADER = "X-Request-ID"
    private const val CONTENT_TYPE_HEADER = "Content-Type"
    private const val SET_COOKIE_HEADER = "Set-Cookie"
    private const val ABSENT = "없음"
    private const val GENERATED = "생성값(^[0-9a-f]{8}$)"
    private val GENERATED_REQUEST_ID = Regex("[0-9a-f]{8}")
    private val SESSION_ID = Regex("[0-9A-F]{32}")
    private val TRANSPORT_HEADERS = setOf("date", "keep-alive", "connection", "transfer-encoding", "content-length")

    fun of(response: Response, sentRequestId: String?): String = listOf(
        "HTTP ${response.statusCode}",
        "헤더 ${names(response)}",
        "$CONTENT_TYPE_HEADER ${response.header(CONTENT_TYPE_HEADER) ?: ABSENT}",
        "$REQUEST_ID_HEADER ${requestIdRule(response.header(REQUEST_ID_HEADER), sentRequestId)}"
    ).plus(cookies(response)).joinToString(" | ")

    private fun names(response: Response): String = response.headers.asList().map { it.name }
        .filterNot { it.lowercase() in TRANSPORT_HEADERS }
        .sortedBy { it.lowercase() }
        .joinToString(", ", prefix = "[", postfix = "]")

    private fun requestIdRule(actual: String?, sent: String?): String = when {
        actual == null -> ABSENT
        sent != null && actual == sent -> "보낸 값 그대로"
        GENERATED_REQUEST_ID.matches(actual) -> if (sent == null) GENERATED else "보낸 값 대신 $GENERATED"
        sent != null -> "보낸 값을 바꾼 값 '$actual'"
        else -> throw ContractViolationError(
            "[API 계약 위반] $REQUEST_ID_HEADER 생성값이 8자리 소문자 16진수가 아닙니다(길이 ${actual.length})"
        )
    }

    private fun cookies(response: Response): List<String> =
        response.headers.getList(SET_COOKIE_HEADER).map { "$SET_COOKIE_HEADER ${maskedCookie(it.value)}" }

    private fun maskedCookie(value: String): String {
        val pair = value.substringBefore(";")
        val sessionId = pair.substringAfter("=")
        if (!SESSION_ID.matches(sessionId)) {
            throw ContractViolationError("[API 계약 위반] 세션 쿠키 값이 32자리 대문자 16진수가 아닙니다: '$pair'")
        }
        return value.replace(pair, pair.substringBefore("=") + "=<SESSION:32HEX>")
    }
}
