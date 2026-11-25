package com.example.mykku.contract.support

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.restassured.RestAssured
import io.restassured.http.ContentType

data class IssuedTokens(val accessToken: String, val refreshToken: String)

object LoginTokens {

    private const val EMAIL_LOGIN_PATH = "/api/v1/email-auth/login"

    private val objectMapper = jacksonObjectMapper()

    fun emailLogin(email: String, password: String): IssuedTokens {
        val response = RestAssured.given()
            .contentType(ContentType.JSON)
            .body(objectMapper.writeValueAsString(mapOf("email" to email, "password" to password)))
            .post(EMAIL_LOGIN_PATH)
        if (response.statusCode != 200) {
            throw AssertionError("이메일 로그인 실패: ${response.statusCode} ${response.asString()}")
        }
        val data = objectMapper.readTree(response.asByteArray()).path("data")
        return IssuedTokens(
            accessToken = requiredText(data, "accessToken"),
            refreshToken = requiredText(data, "refreshToken")
        )
    }

    private fun requiredText(data: JsonNode, field: String): String {
        val value = data.path(field)
        if (!value.isTextual || value.asText().isBlank()) {
            val fields = data.fieldNames().asSequence().toList()
            throw AssertionError("이메일 로그인 응답에 data.$field 문자열이 없습니다. data 필드: $fields")
        }
        return value.asText()
    }
}
