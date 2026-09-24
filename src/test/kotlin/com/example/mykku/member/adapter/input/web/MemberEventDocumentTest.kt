package com.example.mykku.member.adapter.input.web

import com.example.mykku.BaseDocumentTest
import com.example.mykku.docs.ApiRequestConfig
import com.example.mykku.event.application.dto.MyParticipatedEventResult
import com.example.mykku.event.application.dto.PagedMyParticipatedEventsResult
import com.example.mykku.event.domain.vo.EventStatusType
import com.example.mykku.event.domain.vo.EventWinnerStatus
import io.restassured.http.ContentType
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import java.time.LocalDateTime

class MemberEventDocumentTest : BaseDocumentTest() {

    @Nested
    @DisplayName("내가 참여한 이벤트 목록 조회")
    inner class GetMyParticipatedEvents {

        private val apiConfig = ApiRequestConfig(
            queryParameters = listOf(
                parameterWithName("page").description("페이지 번호 (0부터 시작, 기본값: 0)").optional(),
                parameterWithName("size").description("페이지 크기 (기본값: 20)").optional()
            ),
            headerDescriptors = AUTH_HEADER_DESCRIPTOR
        )

        @Test
        fun `성공`() {
            val eventList = listOf(
                MyParticipatedEventResult(
                    id = 1L,
                    title = "첫 번째 이벤트",
                    startedAt = LocalDateTime.of(2025, 1, 1, 0, 0, 0),
                    expiredAt = LocalDateTime.of(2025, 12, 31, 23, 59, 59),
                    status = EventStatusType.WINNER_SELECTED,
                    thumbnailUrl = "https://example.com/thumbnail1.jpg",
                    winnerStatus = EventWinnerStatus.WON
                ),
                MyParticipatedEventResult(
                    id = 2L,
                    title = "두 번째 이벤트",
                    startedAt = LocalDateTime.of(2025, 1, 1, 0, 0, 0),
                    expiredAt = LocalDateTime.of(2025, 11, 30, 23, 59, 59),
                    status = EventStatusType.ACTIVE,
                    thumbnailUrl = "https://example.com/thumbnail2.jpg",
                    winnerStatus = EventWinnerStatus.PENDING
                )
            )

            val result = PagedMyParticipatedEventsResult(
                content = eventList,
                page = 0,
                size = 20,
                totalElements = 2,
                totalPages = 1,
                isLast = true
            )

            whenever(getMyParticipatedEventsUseCase.execute(any(), any(), any())).thenReturn(result)

            val documentFilter = document("member/events", 200)
                .request(request().applyConfig(apiConfig))
                .response(
                    response()
                        .responseBodyField(
                            fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                            fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터"),
                            fieldWithPath("data.content[]").type(JsonFieldType.ARRAY).description("이벤트 목록"),
                            fieldWithPath("data.content[].id").type(JsonFieldType.NUMBER).description("이벤트 ID"),
                            fieldWithPath("data.content[].title").type(JsonFieldType.STRING).description("이벤트 제목"),
                            fieldWithPath("data.content[].startedAt").type(JsonFieldType.STRING).description("시작일"),
                            fieldWithPath("data.content[].expiredAt").type(JsonFieldType.STRING).description("만료일"),
                            fieldWithPath("data.content[].status").type(JsonFieldType.STRING).description("이벤트 상태"),
                            fieldWithPath("data.content[].thumbnailUrl").type(JsonFieldType.STRING)
                                .description("썸네일 이미지 URL").optional(),
                            fieldWithPath("data.content[].winnerStatus").type(JsonFieldType.STRING)
                                .description("당첨 상태 (WON: 당첨, LOST: 낙첨, PENDING: 발표 전)"),
                            fieldWithPath("data.page").type(JsonFieldType.NUMBER).description("현재 페이지 번호"),
                            fieldWithPath("data.size").type(JsonFieldType.NUMBER).description("페이지 크기"),
                            fieldWithPath("data.totalElements").type(JsonFieldType.NUMBER).description("전체 요소 수"),
                            fieldWithPath("data.totalPages").type(JsonFieldType.NUMBER).description("전체 페이지 수"),
                            fieldWithPath("data.isLast").type(JsonFieldType.BOOLEAN).description("마지막 페이지 여부")
                        )
                )
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .param("page", 0)
                .param("size", 20)
                .`when`()
                .get("/api/v1/members/me/events")
                .then()
                .statusCode(200)
        }

        @Test
        fun `빈 목록`() {
            val result = PagedMyParticipatedEventsResult(
                content = emptyList(),
                page = 0,
                size = 20,
                totalElements = 0,
                totalPages = 0,
                isLast = true
            )

            whenever(getMyParticipatedEventsUseCase.execute(any(), any(), any())).thenReturn(result)

            val documentFilter = document("member/events", "empty")
                .request(request().applyConfig(apiConfig))
                .response(
                    response()
                        .responseBodyField(
                            fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                            fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터"),
                            fieldWithPath("data.content[]").type(JsonFieldType.ARRAY).description("빈 이벤트 목록"),
                            fieldWithPath("data.page").type(JsonFieldType.NUMBER).description("현재 페이지 번호"),
                            fieldWithPath("data.size").type(JsonFieldType.NUMBER).description("페이지 크기"),
                            fieldWithPath("data.totalElements").type(JsonFieldType.NUMBER).description("전체 요소 수"),
                            fieldWithPath("data.totalPages").type(JsonFieldType.NUMBER).description("전체 페이지 수"),
                            fieldWithPath("data.isLast").type(JsonFieldType.BOOLEAN).description("마지막 페이지 여부")
                        )
                )
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .param("page", 0)
                .param("size", 20)
                .`when`()
                .get("/api/v1/members/me/events")
                .then()
                .statusCode(200)
        }
    }
}
