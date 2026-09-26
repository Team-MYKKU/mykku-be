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
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import java.time.LocalDateTime

class ReportDocumentTest : BaseDocumentTest() {

    private val reportReasonValues = ReportReason.entries
        .joinToString { "${it.name}: ${it.description}" }

    private val reportStatusValues = ReportStatus.entries
        .joinToString { "${it.name}: ${it.description}" }

    private val reportTargetTypeLabels = ReportTargetType.entries
        .joinToString { "${it.name}: ${it.description}" }

    @Nested
    @DisplayName("콘텐츠 신고")
    inner class CreateReport {

        private val apiConfig = ApiRequestConfig(
            requestBodyFields = listOf(
                fieldWithPath("targetType").type(JsonFieldType.STRING)
                    .description(
                        "신고 대상 종류 (FEED: 피드 게시글(콘테스트 참여작 포함), FEED_COMMENT: 피드 댓글 또는 답글, " +
                            "DAILY_MESSAGE_COMMENT: 하루 덕담 댓글 또는 답글). " +
                            "대소문자 구분. 하루 덕담 본문, 덕질노트, 회원 등 다른 대상은 신고할 수 없음"
                    ),
                fieldWithPath("targetId").type(JsonFieldType.NUMBER)
                    .description(
                        "신고 대상 ID (1 이상의 정수). targetType이 FEED면 피드 ID, FEED_COMMENT면 피드 댓글/답글 ID, " +
                            "DAILY_MESSAGE_COMMENT면 하루 덕담 댓글/답글 ID"
                    ),
                fieldWithPath("reason").type(JsonFieldType.STRING)
                    .description(
                        "신고 사유 ($reportReasonValues). 대소문자 구분. ETC면 detail 필수. " +
                            "화면 표시용 목록과 라벨은 신고 사유 목록 조회 API(GET /api/v1/reports/reasons) 참고"
                    ),
                fieldWithPath("detail").type(JsonFieldType.STRING)
                    .description(
                        "상세 내용 (최대 500자, 앞뒤 공백 포함 원문 기준). 앞뒤 공백은 제거되어 저장되고 " +
                            "공백만 입력하면 미입력으로 처리. reason이 ETC면 필수, 그 외 사유에서는 선택"
                    ).optional()
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
                            fieldWithPath("data.reporterMemberId").type(JsonFieldType.STRING)
                                .description(
                                    "신고자 사용자 ID (문자열 memberId). " +
                                        "사용자 ID를 아직 설정하지 않은(프로필 미완성) 회원이 신고하면 null"
                                ).optional(),
                            fieldWithPath("data.targetType").type(JsonFieldType.STRING)
                                .description(
                                    "신고 대상 종류 코드 (FEED: 피드 게시글, FEED_COMMENT: 피드 댓글/답글, " +
                                        "DAILY_MESSAGE_COMMENT: 하루 덕담 댓글/답글). " +
                                        "요청의 targetType과 같은 값. 표시 문구는 targetTypeDescription"
                                ),
                            fieldWithPath("data.targetTypeDescription").type(JsonFieldType.STRING)
                                .description("신고 대상 종류의 화면 표시용 한글 라벨 ($reportTargetTypeLabels)"),
                            fieldWithPath("data.targetId").type(JsonFieldType.NUMBER)
                                .description(
                                    "신고 대상 ID (요청의 targetId와 같은 값. FEED면 피드 ID, " +
                                        "FEED_COMMENT면 피드 댓글/답글 ID, DAILY_MESSAGE_COMMENT면 하루 덕담 댓글/답글 ID)"
                                ),
                            fieldWithPath("data.targetMemberId").type(JsonFieldType.STRING)
                                .description(
                                    "신고 대상 작성자의 사용자 ID (문자열 memberId). " +
                                        "작성자가 탈퇴한 콘텐츠이거나 작성자에게 사용자 ID가 없으면 null"
                                ).optional(),
                            fieldWithPath("data.reason").type(JsonFieldType.STRING)
                                .description(
                                    "신고 사유 코드 (요청의 reason과 같은 값, 값 목록은 요청 필드 reason 참고). " +
                                        "표시 문구는 reasonDescription"
                                ),
                            fieldWithPath("data.reasonDescription").type(JsonFieldType.STRING)
                                .description("신고 사유의 화면 표시용 한글 라벨 (예: ${ReportReason.SPAM.description})"),
                            fieldWithPath("data.detail").type(JsonFieldType.STRING)
                                .description("상세 내용 (앞뒤 공백을 제거한 값. 입력하지 않았거나 공백만 입력했으면 null)")
                                .optional(),
                            fieldWithPath("data.status").type(JsonFieldType.STRING)
                                .description(
                                    "처리 상태 코드 ($reportStatusValues). 관리자가 검토 후 RESOLVED 또는 REJECTED로 " +
                                        "변경하며, 신고 생성 응답에서는 항상 PENDING. 표시 문구는 statusDescription"
                                ),
                            fieldWithPath("data.statusDescription").type(JsonFieldType.STRING)
                                .description(
                                    "처리 상태의 화면 표시용 한글 라벨 " +
                                        "(신고 생성 응답에서는 항상 ${ReportStatus.PENDING.description})"
                                ),
                            fieldWithPath("data.processedAt").type(JsonFieldType.STRING)
                                .description(
                                    "관리자 처리 일시 (KST, ISO-8601). 처리 전(PENDING)에는 null이므로 " +
                                        "신고 생성 응답에서는 항상 null"
                                ).optional(),
                            fieldWithPath("data.createdAt").type(JsonFieldType.STRING)
                                .description("신고 접수 일시 (KST, ISO-8601)")
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
        fun `신고 대상 없음 에러`() {
            val request = CreateReportRequest(
                targetType = ReportTargetType.FEED_COMMENT,
                targetId = 999L,
                reason = ReportReason.ABUSE,
                detail = null
            )

            `when`(createReportUseCase.createReport(any()))
                .thenThrow(ReportException(ReportErrorCode.REPORT_TARGET_NOT_FOUND))

            val documentFilter = document("report/create", "REPORT_TARGET_NOT_FOUND")
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
                .statusCode(404)
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
        fun `기타 사유 상세 내용 누락 에러`() {
            val request = CreateReportRequest(
                targetType = ReportTargetType.FEED,
                targetId = 1L,
                reason = ReportReason.ETC,
                detail = "   "
            )

            `when`(createReportUseCase.createReport(any()))
                .thenThrow(ReportException(ReportErrorCode.REPORT_DETAIL_REQUIRED))

            val documentFilter = document("report/create", "REPORT_DETAIL_REQUIRED")
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

        @Test
        fun `동시 중복 신고 에러`() {
            val request = CreateReportRequest(
                targetType = ReportTargetType.FEED,
                targetId = 1L,
                reason = ReportReason.SPAM,
                detail = null
            )

            `when`(createReportUseCase.createReport(any()))
                .thenThrow(DataIntegrityViolationException("uk_report_reporter_target"))

            val documentFilter = document("report/create", "DATA_INTEGRITY_VIOLATION")
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
                            fieldWithPath("data[]").type(JsonFieldType.ARRAY)
                                .description("신고 사유 목록 (항상 전체 사유를 고정 순서로 반환하며 마지막이 ETC)"),
                            fieldWithPath("data[].reason").type(JsonFieldType.STRING)
                                .description("신고 사유 코드. 콘텐츠 신고 API의 reason에 그대로 사용 ($reportReasonValues)"),
                            fieldWithPath("data[].description").type(JsonFieldType.STRING)
                                .description("신고 사유의 화면 표시용 한글 라벨")
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
