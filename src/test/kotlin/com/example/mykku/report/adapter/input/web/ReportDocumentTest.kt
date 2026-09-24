package com.example.mykku.report.adapter.input.web

import com.example.mykku.BaseDocumentTest
import com.example.mykku.docs.ApiRequestConfig
import com.example.mykku.docs.RestDocumentationResponse
import com.example.mykku.report.application.dto.ReportResult
import com.example.mykku.report.domain.vo.ReportReason
import com.example.mykku.report.domain.vo.ReportStatus
import com.example.mykku.report.domain.vo.ReportTargetType
import com.example.mykku.report.exception.ReportErrorCode
import com.example.mykku.report.exception.ReportException
import io.restassured.http.ContentType
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import java.time.LocalDateTime

class ReportDocumentTest : BaseDocumentTest() {

    @Nested
    @DisplayName("콘텐츠 신고")
    inner class CreateReport {

        private val apiConfig = ApiRequestConfig(
            requestBodyFields = listOf(
                fieldWithPath("targetType").type(JsonFieldType.STRING)
                    .description("신고 대상 종류 (FEED: 게시글, FEED_COMMENT: 댓글)"),
                fieldWithPath("targetId").type(JsonFieldType.NUMBER).description("신고 대상 ID"),
                fieldWithPath("reason").type(JsonFieldType.STRING)
                    .description("신고 사유 (SPAM, ABUSE, OBSCENE, PERSONAL_INFO, COPYRIGHT, FRAUD, OFF_TOPIC, ETC)"),
                fieldWithPath("detail").type(JsonFieldType.STRING)
                    .description("상세 내용 (최대 500자, 사유가 ETC인 경우 필수)").optional()
            ),
            headerDescriptors = AUTH_HEADER_DESCRIPTOR
        )

        @Test
        fun `성공`() {
            val request = CreateReportRequest(
                targetType = ReportTargetType.FEED,
                targetId = 1L,
                reason = ReportReason.SPAM,
                detail = "광고 도배입니다"
            )
            val result = ReportResult(
                id = 1L,
                reporterMemberId = testMember.memberId,
                targetType = ReportTargetType.FEED.name,
                targetTypeDescription = ReportTargetType.FEED.description,
                targetId = 1L,
                targetMemberId = "authorid",
                reason = ReportReason.SPAM.name,
                reasonDescription = ReportReason.SPAM.description,
                detail = "광고 도배입니다",
                status = ReportStatus.PENDING.name,
                statusDescription = ReportStatus.PENDING.description,
                processedAt = null,
                createdAt = LocalDateTime.now()
            )

            `when`(createReportUseCase.createReport(any())).thenReturn(result)

            val documentFilter = document("report/create", 201)
                .request(request().applyConfig(apiConfig))
                .response(
                    response()
                        .responseBodyField(
                            fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                            fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터"),
                            fieldWithPath("data.id").type(JsonFieldType.NUMBER).description("신고 ID"),
                            fieldWithPath("data.reporterMemberId").type(JsonFieldType.STRING).description("신고자 사용자 ID"),
                            fieldWithPath("data.targetType").type(JsonFieldType.STRING).description("신고 대상 종류"),
                            fieldWithPath("data.targetTypeDescription").type(JsonFieldType.STRING).description("신고 대상 종류 설명"),
                            fieldWithPath("data.targetId").type(JsonFieldType.NUMBER).description("신고 대상 ID"),
                            fieldWithPath("data.targetMemberId").type(JsonFieldType.STRING).description("신고 대상 작성자 사용자 ID").optional(),
                            fieldWithPath("data.reason").type(JsonFieldType.STRING).description("신고 사유"),
                            fieldWithPath("data.reasonDescription").type(JsonFieldType.STRING).description("신고 사유 설명"),
                            fieldWithPath("data.detail").type(JsonFieldType.STRING).description("상세 내용").optional(),
                            fieldWithPath("data.status").type(JsonFieldType.STRING).description("처리 상태 (PENDING, RESOLVED, REJECTED)"),
                            fieldWithPath("data.statusDescription").type(JsonFieldType.STRING).description("처리 상태 설명"),
                            fieldWithPath("data.processedAt").type(JsonFieldType.STRING).description("처리 시간").optional(),
                            fieldWithPath("data.createdAt").type(JsonFieldType.STRING).description("신고 접수 시간")
                        )
                )
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
                .`when`()
                .post("/api/v1/reports")
                .then()
                .statusCode(201)
        }

        @Test
        fun `자신의 콘텐츠 신고 에러`() {
            val request = CreateReportRequest(
                targetType = ReportTargetType.FEED,
                targetId = 1L,
                reason = ReportReason.SPAM,
                detail = null
            )

            `when`(createReportUseCase.createReport(any()))
                .thenThrow(ReportException(ReportErrorCode.CANNOT_REPORT_OWN_CONTENT))

            val documentFilter = document("report/create", "CANNOT_REPORT_OWN_CONTENT")
                .request(request().applyConfig(apiConfig))
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
                .`when`()
                .post("/api/v1/reports")
                .then()
                .statusCode(400)
        }

        @Test
        fun `이미 신고한 콘텐츠 에러`() {
            val request = CreateReportRequest(
                targetType = ReportTargetType.FEED,
                targetId = 1L,
                reason = ReportReason.SPAM,
                detail = null
            )

            `when`(createReportUseCase.createReport(any()))
                .thenThrow(ReportException(ReportErrorCode.ALREADY_REPORTED))

            val documentFilter = document("report/create", "ALREADY_REPORTED")
                .request(request().applyConfig(apiConfig))
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
                .`when`()
                .post("/api/v1/reports")
                .then()
                .statusCode(409)
        }
    }

    @Nested
    @DisplayName("신고 사유 목록 조회")
    inner class GetReportReasons {

        private val apiConfig = ApiRequestConfig()

        @Test
        fun `성공`() {
            val documentFilter = document("report/reasons", 200)
                .request(request().applyConfig(apiConfig))
                .response(
                    response()
                        .responseBodyField(
                            fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                            fieldWithPath("data[]").type(JsonFieldType.ARRAY).description("신고 사유 목록"),
                            fieldWithPath("data[].reason").type(JsonFieldType.STRING).description("신고 사유 코드"),
                            fieldWithPath("data[].description").type(JsonFieldType.STRING).description("신고 사유 설명")
                        )
                )
                .build()

            given(documentFilter)
                .`when`()
                .get("/api/v1/reports/reasons")
                .then()
                .statusCode(200)
        }
    }
}
