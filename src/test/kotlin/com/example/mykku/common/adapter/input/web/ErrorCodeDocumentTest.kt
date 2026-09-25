package com.example.mykku.common.adapter.input.web

import com.example.mykku.BaseDocumentTest
import com.example.mykku.common.exception.ErrorCodeRegistry
import com.example.mykku.docs.ApiRequestConfig
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.restdocs.payload.FieldDescriptor
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName

class ErrorCodeDocumentTest : BaseDocumentTest() {

    private val messageField: FieldDescriptor =
        fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지 (항상 \"success\")")

    private val errorCodeFields: List<FieldDescriptor> = listOf(
        messageField,
        fieldWithPath("data").type(JsonFieldType.ARRAY).description("에러 코드 목록"),
        fieldWithPath("data[].code").type(JsonFieldType.STRING)
            .description("에러 코드. 에러 응답 본문의 code와 같은 값 (예: FD001)"),
        fieldWithPath("data[].domain").type(JsonFieldType.STRING)
            .description("에러가 속한 도메인 키 (에러 코드 도메인 목록 조회 응답 값 중 하나, 예: feed)"),
        fieldWithPath("data[].name").type(JsonFieldType.STRING)
            .description("에러 이름. 각 API 문서 에러 응답 제목의 에러 이름과 같음 (예: FEED_NOT_FOUND)"),
        fieldWithPath("data[].status").type(JsonFieldType.NUMBER)
            .description("HTTP 상태 코드 숫자 (예: 404)"),
        fieldWithPath("data[].message").type(JsonFieldType.STRING)
            .description("기본 에러 메시지. 실제 응답 message와 다를 수 있으므로(C101 검증 실패 등) 표시용으로만 사용")
    )

    @Nested
    @DisplayName("에러 코드 전체 조회")
    inner class GetAllErrorCodes {

        @Test
        fun `성공`() {
            val documentFilter = document("error-code/list", 200)
                .response(response().responseBodyField(*errorCodeFields.toTypedArray()))
                .build()

            given(documentFilter)
                .`when`()
                .get("/api/error-codes")
                .then()
                .statusCode(200)
        }
    }

    @Nested
    @DisplayName("에러 코드 도메인 목록 조회")
    inner class GetDomains {

        @Test
        fun `성공`() {
            val documentFilter = document("error-code/domains", 200)
                .response(
                    response().responseBodyField(
                        messageField,
                        fieldWithPath("data").type(JsonFieldType.ARRAY)
                            .description(
                                "도메인 키 목록, 사전순 (${ErrorCodeRegistry.getDomains().joinToString()}). " +
                                    "대소문자를 구분하며 도메인별 에러 코드 조회의 경로 값으로 사용"
                            )
                    )
                )
                .build()

            given(documentFilter)
                .`when`()
                .get("/api/error-codes/domains")
                .then()
                .statusCode(200)
        }
    }

    @Nested
    @DisplayName("도메인별 에러 코드 조회")
    inner class GetErrorCodesByDomain {

        private val apiConfig = ApiRequestConfig(
            pathParameters = listOf(
                parameterWithName("domain")
                    .description("도메인 키 (대소문자 구분, 예: fanNote). 에러 코드 도메인 목록 조회 응답 값 중 하나")
            )
        )

        @Test
        fun `성공`() {
            val documentFilter = document("error-code/by-domain", 200)
                .request(request().applyConfig(apiConfig))
                .response(response().responseBodyField(*errorCodeFields.toTypedArray()))
                .build()

            given(documentFilter)
                .`when`()
                .get("/api/error-codes/{domain}", "fanNote")
                .then()
                .statusCode(200)
        }

        @Test
        fun `없는 도메인 키`() {
            val documentFilter = document("error-code/by-domain", "empty")
                .request(request().applyConfig(apiConfig))
                .response(
                    response().responseBodyField(
                        messageField,
                        fieldWithPath("data").type(JsonFieldType.ARRAY).description("빈 배열")
                    )
                )
                .build()

            given(documentFilter)
                .`when`()
                .get("/api/error-codes/{domain}", "unknown")
                .then()
                .statusCode(200)
        }
    }
}
