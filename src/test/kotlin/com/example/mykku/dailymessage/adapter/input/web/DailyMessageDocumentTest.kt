package com.example.mykku.dailymessage.adapter.input.web

import com.example.mykku.BaseDocumentTest
import com.example.mykku.docs.ApiRequestConfig
import com.example.mykku.docs.RestDocumentationResponse
import com.example.mykku.dailymessage.application.dto.DailyMessageResult
import com.example.mykku.dailymessage.application.dto.DailyMessageSummaryResult
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
                parameterWithName("date").description("기준 날짜 (YYYY-MM-DD 형식)"),
                parameterWithName("page").description("페이지 번호 (기본값: 0)").optional(),
                parameterWithName("size").description("페이지 크기 (기본값: 20)").optional()
            )
        )

        @Test
        fun `성공`() {
            val date = LocalDate.now()
            val dailyMessages = listOf(
                DailyMessageSummaryResult(
                    id = 1L,
                    title = "오늘의 덕담",
                    content = "좋은 하루 되세요!",
                    date = date,
                    createdAt = LocalDateTime.now()
                ),
                DailyMessageSummaryResult(
                    id = 2L,
                    title = "희망찬 하루",
                    content = "모든 소망이 이루어지길!",
                    date = date,
                    createdAt = LocalDateTime.now()
                )
            )
            val pageable = PageRequest.of(0, 20)
            val page = PageImpl(dailyMessages, pageable, 2)

            `when`(getDailyMessagesUseCase.execute(any(), any())).thenReturn(page)

            val documentFilter = document("daily-message/list", 200)
                .request(request().applyConfig(apiConfig))
                .response(
                    response()
                        .responseBodyField(
                            fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                            fieldWithPath("data.content[]").type(JsonFieldType.ARRAY).description("하루 덕담 목록"),
                            fieldWithPath("data.content[].id").type(JsonFieldType.NUMBER).description("덕담 ID"),
                            fieldWithPath("data.content[].title").type(JsonFieldType.STRING).description("덕담 제목"),
                            fieldWithPath("data.content[].content").type(JsonFieldType.STRING).description("덕담 내용"),
                            fieldWithPath("data.content[].date").type(JsonFieldType.STRING).description("덕담 날짜"),
                            fieldWithPath("data.pageable").type(JsonFieldType.OBJECT).description("페이지 정보"),
                            fieldWithPath("data.pageable.pageNumber").type(JsonFieldType.NUMBER).description("현재 페이지 번호"),
                            fieldWithPath("data.pageable.pageSize").type(JsonFieldType.NUMBER).description("페이지 크기"),
                            fieldWithPath("data.pageable.sort").type(JsonFieldType.OBJECT).description("정렬 정보"),
                            fieldWithPath("data.pageable.sort.empty").type(JsonFieldType.BOOLEAN).description("정렬 정보 비어있음"),
                            fieldWithPath("data.pageable.sort.sorted").type(JsonFieldType.BOOLEAN).description("정렬됨"),
                            fieldWithPath("data.pageable.sort.unsorted").type(JsonFieldType.BOOLEAN).description("정렬되지 않음"),
                            fieldWithPath("data.pageable.offset").type(JsonFieldType.NUMBER).description("오프셋"),
                            fieldWithPath("data.pageable.paged").type(JsonFieldType.BOOLEAN).description("페이지네이션 여부"),
                            fieldWithPath("data.pageable.unpaged").type(JsonFieldType.BOOLEAN).description("페이지네이션 아님"),
                            fieldWithPath("data.totalElements").type(JsonFieldType.NUMBER).description("전체 요소 수"),
                            fieldWithPath("data.totalPages").type(JsonFieldType.NUMBER).description("전체 페이지 수"),
                            fieldWithPath("data.size").type(JsonFieldType.NUMBER).description("페이지 크기"),
                            fieldWithPath("data.number").type(JsonFieldType.NUMBER).description("현재 페이지 번호"),
                            fieldWithPath("data.sort").type(JsonFieldType.OBJECT).description("정렬 정보"),
                            fieldWithPath("data.sort.empty").type(JsonFieldType.BOOLEAN).description("정렬 정보 비어있음"),
                            fieldWithPath("data.sort.sorted").type(JsonFieldType.BOOLEAN).description("정렬됨"),
                            fieldWithPath("data.sort.unsorted").type(JsonFieldType.BOOLEAN).description("정렬되지 않음"),
                            fieldWithPath("data.first").type(JsonFieldType.BOOLEAN).description("첫 페이지 여부"),
                            fieldWithPath("data.last").type(JsonFieldType.BOOLEAN).description("마지막 페이지 여부"),
                            fieldWithPath("data.numberOfElements").type(JsonFieldType.NUMBER).description("현재 페이지 요소 수"),
                            fieldWithPath("data.empty").type(JsonFieldType.BOOLEAN).description("빈 페이지 여부")
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
                parameterWithName("id").description("조회할 하루 덕담 ID")
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
                            fieldWithPath("data.title").type(JsonFieldType.STRING).description("덕담 제목"),
                            fieldWithPath("data.content").type(JsonFieldType.STRING).description("덕담 내용"),
                            fieldWithPath("data.date").type(JsonFieldType.STRING).description("게시일 (yyyy-MM-dd)"),
                            fieldWithPath("data.createdAt").type(JsonFieldType.STRING).description("작성 일시")
                        )
                )
                .build()

            given(documentFilter)
                .contentType(ContentType.JSON)
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
