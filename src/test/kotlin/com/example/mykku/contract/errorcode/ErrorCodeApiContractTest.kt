package com.example.mykku.contract.errorcode

import com.example.mykku.BaseControllerTest
import com.example.mykku.contract.support.ContractManifestExtension
import com.example.mykku.contract.support.HttpSnapshot
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.restassured.RestAssured
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

@DisplayName("에러 코드 조회 API 계약")
@ExtendWith(ContractManifestExtension::class)
class ErrorCodeApiContractTest : BaseControllerTest() {

    private val objectMapper = jacksonObjectMapper()

    @ParameterizedTest(name = "{1}")
    @MethodSource("requests")
    fun `에러 코드 조회 응답은 골든과 같다`(golden: String, path: String) {
        val response = RestAssured.given().get(path)

        HttpSnapshot.of("GET $path", response).verify("wire/error-codes/$golden.http")
    }

    @Test
    fun `전체 목록은 code 오름차순이고 도메인별 목록을 모두 합친 것과 같다`() {
        val all = codesOf("/api/error-codes")
        val byDomain = DOMAINS.flatMap { codesOf("/api/error-codes/$it") }

        assertThat(all).isSorted()
        assertThat(all).containsExactlyInAnyOrderElementsOf(byDomain)
        assertThat(codesOf("/api/error-codes/domains")).containsExactlyElementsOf(DOMAINS.sorted())
    }

    private fun codesOf(path: String): List<String> {
        val data = objectMapper.readTree(RestAssured.given().get(path).asByteArray()).path("data")
        return data.map { if (it.isTextual) it.asText() else it.path("code").asText() }
    }

    companion object {
        private val DOMAINS = listOf(
            "common", "admin", "auth", "block", "board", "contest", "dailyMessage", "email", "event",
            "fanNote", "feed", "image", "like", "member", "notification", "preference", "report", "role"
        )

        @JvmStatic
        fun requests(): List<Arguments> = listOf(
            Arguments.of("list", "/api/error-codes"),
            Arguments.of("domains", "/api/error-codes/domains")
        ) + DOMAINS.map { Arguments.of("by-domain-$it", "/api/error-codes/$it") } + listOf(
            Arguments.of("by-domain-unknown", "/api/error-codes/unknown"),
            Arguments.of("by-domain-case-FEED", "/api/error-codes/FEED"),
            Arguments.of("by-domain-case-dailymessage", "/api/error-codes/dailymessage")
        )
    }
}
