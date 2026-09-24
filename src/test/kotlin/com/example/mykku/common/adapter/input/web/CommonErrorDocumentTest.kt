package com.example.mykku.common.adapter.input.web

import com.example.mykku.BaseDocumentTest
import com.example.mykku.auth.exception.AuthErrorCode
import com.example.mykku.common.exception.CommonErrorCode
import com.example.mykku.docs.RestDocumentationResponse
import io.restassured.http.ContentType
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class CommonErrorDocumentTest : BaseDocumentTest() {

    @Nested
    @DisplayName("인증 실패")
    inner class Unauthorized {

        @Test
        fun `인증 필수 API에 토큰 없이 요청`() {
            val documentFilter = document("common", "UNAUTHORIZED")
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            given(documentFilter)
                .`when`()
                .get("/api/v1/members/me")
                .then()
                .statusCode(401)
                .body("code", equalTo(AuthErrorCode.UNAUTHORIZED.code))
        }
    }

    @Nested
    @DisplayName("요청 본문 오류")
    inner class InvalidInput {

        @Test
        fun `입력값 검증 실패`() {
            val documentFilter = document("common", "INVALID_INPUT")
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            given(documentFilter)
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(mapOf("memberId" to "invalid_id!")))
                .`when`()
                .post("/api/v1/members/check-id")
                .then()
                .statusCode(400)
                .body("code", equalTo(CommonErrorCode.INVALID_INPUT.code))
        }

        @Test
        fun `JSON 해석 실패`() {
            val documentFilter = document("common", "INVALID_INPUT_MALFORMED")
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            given(documentFilter)
                .contentType(ContentType.JSON)
                .body("{}")
                .`when`()
                .post("/api/v1/members/check-id")
                .then()
                .statusCode(400)
                .body("code", equalTo(CommonErrorCode.INVALID_INPUT.code))
        }
    }

    @Nested
    @DisplayName("페이지 파라미터 오류")
    inner class InvalidPageSize {

        @Test
        fun `페이지 크기 범위 초과`() {
            val documentFilter = document("common", "INVALID_PAGE_SIZE")
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            given(documentFilter)
                .queryParam("size", 1001)
                .`when`()
                .get("/api/v1/boards/{boardId}/feeds", 1L)
                .then()
                .statusCode(400)
                .body("code", equalTo(CommonErrorCode.INVALID_PAGE_SIZE.code))
        }
    }

    @Nested
    @DisplayName("파라미터 형식 오류")
    inner class InvalidParameterType {

        @Test
        fun `정의되지 않은 enum 값`() {
            val documentFilter = document("common", "INVALID_PARAMETER_TYPE")
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .queryParam("status", "active")
                .`when`()
                .get("/api/v1/contests")
                .then()
                .statusCode(400)
                .body("code", equalTo(CommonErrorCode.INVALID_PARAMETER_TYPE.code))
        }
    }
}
