package com.example.mykku.docs

import com.example.mykku.BaseDocumentTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("문서 테스트 요청·응답 로그 버퍼 연결")
class RequestResponseLogWiringTest : BaseDocumentTest() {

    @Test
    fun `문서 테스트 기반 클래스로 보낸 요청과 받은 응답이 실패 시 출력할 버퍼에 쌓인다`() {
        given()
            .`when`()
            .get("/api/error-codes/domains")
            .then()
            .statusCode(200)

        assertThat(requestResponseLog())
            .contains("Request method:\tGET")
            .contains("/api/error-codes/domains")
            .contains("HTTP/1.1 200")
    }
}
