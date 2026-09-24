package com.example.mykku.dailymessage.adapter.input.web

import com.example.mykku.BaseDocumentTest
import com.example.mykku.docs.ApiRequestConfig
import com.example.mykku.docs.RestDocumentationResponse
import com.example.mykku.dailymessage.application.dto.DailyMessageResult
import com.example.mykku.dailymessage.application.dto.DailyMessageSummaryResult
import com.example.mykku.dailymessage.domain.entity.DailyMessage
import com.example.mykku.dailymessage.exception.DailyMessageErrorCode
import com.example.mykku.dailymessage.exception.DailyMessageException
import io.restassured.http.ContentType
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpHeaders
import org.springframework.restdocs.headers.HeaderDocumentation.headerWithName
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import java.time.LocalDate
import java.time.LocalDateTime

class DailyMessageDocumentTest : BaseDocumentTest() {

    @Nested
    @DisplayName("하루 덕담 목록 조회")
    inner class GetDailyMessages {

        private val apiConfig = ApiRequestConfig(
            queryParameters = listOf(
                parameterWithName("date")
                    .description("기준 날짜 (yyyy-MM-dd, ISO-8601). 이 날짜를 포함해 그 이전 날짜의 덕담을 조회"),
                parameterWithName("page")
                    .description("페이지 번호 (0부터 시작, 0 이상, 기본값: 0). 마지막 페이지를 넘으면 빈 목록 반환")
                    .optional(),
                parameterWithName("size").description("페이지 크기 (1~1000, 기본값: 20)").optional()
            )
        )

        @Test
        fun `성공`() {
            val date = LocalDate.now()
            val dailyMessages = listOf(
                DailyMessageSummaryResult(
                    id = 2L,
                    title = "오늘의 덕담",
                    content = "좋은 하루 되세요!",
                    date = date,
                    createdAt = LocalDateTime.now()
                ),
                DailyMessageSummaryResult(
                    id = 1L,
                    title = "희망찬 하루",
                    content = "모든 소망이 이루어지길!",
                    date = date.minusDays(1),
                    createdAt = LocalDateTime.now()
                )
            )
            val pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "date"))
            val page = PageImpl(dailyMessages, pageable, 2)

            `when`(getDailyMessagesUseCase.execute(any(), any())).thenReturn(page)

            val documentFilter = document("daily-message/list", 200)
                .request(request().applyConfig(apiConfig))
                .response(
                    response()
                        .responseBodyField(
                            fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                            fieldWithPath("data.content[]").type(JsonFieldType.ARRAY)
                                .description("하루 덕담 목록 (date 내림차순)"),
                            fieldWithPath("data.content[].id").type(JsonFieldType.NUMBER)
                                .description("덕담 ID (상세 조회 path의 `id` 로 사용)"),
                            fieldWithPath("data.content[].title").type(JsonFieldType.STRING)
                                .description("덕담 제목 (최대 255자)"),
                            fieldWithPath("data.content[].content").type(JsonFieldType.STRING)
                                .description("덕담 내용 (최대 ${DailyMessage.CONTENT_MAX_LENGTH}자)"),
                            fieldWithPath("data.content[].date").type(JsonFieldType.STRING)
                                .description("덕담이 배정된 게시 날짜 (yyyy-MM-dd, 날짜별 최대 1개)"),
                            fieldWithPath("data.pageable").type(JsonFieldType.OBJECT)
                                .description("페이지 요청 정보 (Spring 내부 메타, data.number/data.size와 중복)"),
                            fieldWithPath("data.pageable.pageNumber").type(JsonFieldType.NUMBER)
                                .description("현재 페이지 번호 (data.number와 동일)"),
                            fieldWithPath("data.pageable.pageSize").type(JsonFieldType.NUMBER)
                                .description("페이지 크기 (data.size와 동일)"),
                            fieldWithPath("data.pageable.sort").type(JsonFieldType.OBJECT)
                                .description("정렬 정보 (data.sort와 동일)"),
                            fieldWithPath("data.pageable.sort.empty").type(JsonFieldType.BOOLEAN)
                                .description("정렬 조건이 없으면 true (이 API는 항상 date 정렬이므로 false)"),
                            fieldWithPath("data.pageable.sort.sorted").type(JsonFieldType.BOOLEAN)
                                .description("정렬 적용 여부 (항상 true)"),
                            fieldWithPath("data.pageable.sort.unsorted").type(JsonFieldType.BOOLEAN)
                                .description("정렬 미적용 여부 (항상 false)"),
                            fieldWithPath("data.pageable.offset").type(JsonFieldType.NUMBER)
                                .description("건너뛴 요소 수 (page × size)"),
                            fieldWithPath("data.pageable.paged").type(JsonFieldType.BOOLEAN)
                                .description("페이지 처리 여부 (항상 true)"),
                            fieldWithPath("data.pageable.unpaged").type(JsonFieldType.BOOLEAN)
                                .description("페이지 미처리 여부 (항상 false)"),
                            fieldWithPath("data.totalElements").type(JsonFieldType.NUMBER)
                                .description("기준 날짜 이하 덕담 전체 개수"),
                            fieldWithPath("data.totalPages").type(JsonFieldType.NUMBER).description("전체 페이지 수"),
                            fieldWithPath("data.size").type(JsonFieldType.NUMBER).description("요청한 페이지 크기"),
                            fieldWithPath("data.number").type(JsonFieldType.NUMBER)
                                .description("현재 페이지 번호 (0부터 시작)"),
                            fieldWithPath("data.sort").type(JsonFieldType.OBJECT)
                                .description("정렬 정보 (항상 date 내림차순으로 정렬됨)"),
                            fieldWithPath("data.sort.empty").type(JsonFieldType.BOOLEAN)
                                .description("정렬 조건이 없으면 true (이 API는 항상 date 정렬이므로 false)"),
                            fieldWithPath("data.sort.sorted").type(JsonFieldType.BOOLEAN)
                                .description("정렬 적용 여부 (항상 true)"),
                            fieldWithPath("data.sort.unsorted").type(JsonFieldType.BOOLEAN)
                                .description("정렬 미적용 여부 (항상 false)"),
                            fieldWithPath("data.first").type(JsonFieldType.BOOLEAN).description("첫 페이지 여부"),
                            fieldWithPath("data.last").type(JsonFieldType.BOOLEAN)
                                .description("마지막 페이지 여부 (true이면 더 불러올 페이지 없음)"),
                            fieldWithPath("data.numberOfElements").type(JsonFieldType.NUMBER)
                                .description("현재 페이지에 담긴 덕담 수"),
                            fieldWithPath("data.empty").type(JsonFieldType.BOOLEAN)
                                .description("현재 페이지가 비었는지 여부 (조회된 덕담이 없거나 마지막 페이지를 넘는 page 요청 시 true)")
                        )
                )
                .build()

            given(documentFilter)
                .contentType(ContentType.JSON)
                .param("date", date.toString())
                .param("page", "0")
                .param("size", "20")
                .`when`()
                .get("/api/v1/daily-messages")
                .then()
                .statusCode(200)
        }
    }

    @Nested
    @DisplayName("하루 덕담 상세 조회")
    inner class GetDailyMessageDetail {

        private val apiConfig = ApiRequestConfig(
            pathParameters = listOf(
                parameterWithName("id").description("조회할 하루 덕담 ID (숫자, 목록 조회 응답의 `data.content[].id`)")
            ),
            headerDescriptors = listOf(
                headerWithName(HttpHeaders.AUTHORIZATION)
                    .description(
                        "Bearer {JWT 액세스 토큰} (선택). 보내면 하루 덕담 조회 횟수가 칭호 집계에 반영되고(응답 내용은 동일), " +
                            "없거나 유효하지 않은 토큰은 401 없이 비로그인으로 처리됨"
                    )
                    .optional()
            )
        )

        @Test
        fun `성공`() {
            val dailyMessageId = 1L
            val dailyMessage = DailyMessageResult(
                id = dailyMessageId,
                title = "오늘의 덕담",
                content = "좋은 하루 되세요! 올 한해도 건강하시길 바랍니다.",
                date = LocalDate.of(2026, 8, 14),
                createdAt = LocalDateTime.now()
            )

            `when`(getDailyMessageUseCase.execute(eq(dailyMessageId), anyOrNull())).thenReturn(dailyMessage)

            val documentFilter = document("daily-message/detail", 200)
                .request(request().applyConfig(apiConfig))
                .response(
                    response()
                        .responseBodyField(
                            fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                            fieldWithPath("data").type(JsonFieldType.OBJECT).description("하루 덕담 상세 정보"),
                            fieldWithPath("data.id").type(JsonFieldType.NUMBER).description("덕담 ID"),
                            fieldWithPath("data.title").type(JsonFieldType.STRING).description("덕담 제목 (최대 255자)"),
                            fieldWithPath("data.content").type(JsonFieldType.STRING)
                                .description("덕담 내용 (최대 ${DailyMessage.CONTENT_MAX_LENGTH}자)"),
                            fieldWithPath("data.date").type(JsonFieldType.STRING)
                                .description("덕담이 배정된 게시 날짜 (yyyy-MM-dd, 날짜별 최대 1개)"),
                            fieldWithPath("data.createdAt").type(JsonFieldType.STRING)
                                .description(
                                    "덕담 등록 일시 (KST, ISO-8601, 오프셋 없음). 관리자가 덕담을 등록한 시각이며, " +
                                        "화면에 표시할 게시일은 `date` 사용"
                                )
                        )
                )
                .build()

            given(documentFilter)
                .contentType(ContentType.JSON)
                .headers(AUTH_HEADER)
                .`when`()
                .get("/api/v1/daily-messages/{id}", dailyMessageId)
                .then()
                .statusCode(200)
        }

        @Test
        fun `존재하지 않는 덕담`() {
            val dailyMessageId = 999L

            `when`(getDailyMessageUseCase.execute(eq(dailyMessageId), anyOrNull()))
                .thenThrow(DailyMessageException(DailyMessageErrorCode.DAILY_MESSAGE_NOT_FOUND))

            val documentFilter = document("daily-message/detail", "DAILY_MESSAGE_NOT_FOUND")
                .request(request().applyConfig(apiConfig))
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            given(documentFilter)
                .contentType(ContentType.JSON)
                .`when`()
                .get("/api/v1/daily-messages/{id}", dailyMessageId)
                .then()
                .statusCode(404)
        }
    }
}
