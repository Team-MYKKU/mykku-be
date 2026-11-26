package com.example.mykku.contract.snapshot

import com.example.mykku.contract.support.ContractJson
import com.example.mykku.contract.support.ContractViolationError
import com.fasterxml.jackson.databind.JsonNode
import java.time.Instant
import java.util.Base64
import kotlin.math.abs

object JwtClaims {

    private val VALUE_CLAIMS = listOf("sub", "tokenType", "email")

    fun render(label: String, token: String): String {
        val parts = token.split('.')
        if (parts.size != JWS_PARTS) {
            throw ContractViolationError("[API 계약 위반] $label 이 JWS 세 부분이 아닙니다")
        }
        val payload = decode(parts[1])
        return listOf(
            "### $label",
            "header: ${decode(parts[0])}",
            "claims: ${payload.fieldNames().asSequence().joinToString(", ")}"
        ).plus(valueLines(payload)).plus("exp-iat: ${lifetime(label, payload)}").joinToString("\n", postfix = "\n")
    }

    private fun valueLines(payload: JsonNode): List<String> =
        VALUE_CLAIMS.filter { payload.has(it) }.map { "$it: ${payload.get(it)}" }

    private fun lifetime(label: String, payload: JsonNode): Long {
        val issuedAt = payload.get("iat")
        val expiresAt = payload.get("exp")
        if (issuedAt == null || expiresAt == null || !issuedAt.isIntegralNumber || !expiresAt.isIntegralNumber) {
            throw ContractViolationError("[API 계약 위반] $label 에 정수 iat·exp가 없습니다: $payload")
        }
        if (abs(issuedAt.asLong() - Instant.now().epochSecond) > CLOCK_TOLERANCE_SECONDS) {
            throw ContractViolationError("[API 계약 위반] $label 의 iat가 JVM 시계에서 10분 넘게 떨어져 있습니다: $payload")
        }
        return expiresAt.asLong() - issuedAt.asLong()
    }

    private fun decode(part: String): JsonNode =
        ContractJson.parse(ContractJson.decodeUtf8(Base64.getUrlDecoder().decode(part)))

    private const val JWS_PARTS = 3
    private const val CLOCK_TOLERANCE_SECONDS = 600L
}
