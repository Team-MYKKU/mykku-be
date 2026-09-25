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
                parameterWithName("page").description("페이지 번호 (0부터 시작, 0 이상, 기본값: 0)").optional(),
                parameterWithName("size").description("페이지 크기 (1~1000, 기본값: 20)").optional()
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
                    startedAt = LocalDateTime.of(2030, 1, 1, 0, 0, 0),
                    expiredAt = LocalDateTime.of(2030, 11, 30, 23, 59, 59),
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
                            fieldWithPath("data.content[].startedAt").type(JsonFieldType.STRING)
                                .description("이벤트 시작 일시 (yyyy-MM-dd'T'HH:mm:ss, KST, 오프셋 없음)"),
                            fieldWithPath("data.content[].expiredAt").type(JsonFieldType.STRING)
                                .description(
                                    "이벤트 마감 일시 (yyyy-MM-dd'T'HH:mm:ss, KST, 오프셋 없음). " +
                                        "이 시각부터 status가 EXPIRED로 바뀜 (당첨자 선정 전까지)"
                                ),
                            fieldWithPath("data.content[].status").type(JsonFieldType.STRING)
                                .description(
                                    "이벤트 상태 (ACTIVE: 마감 전으로 현재 시각이 expiredAt 이전, " +
                                        "startedAt 이전인 시작 전 이벤트도 ACTIVE, " +
                                        "EXPIRED: 현재 시각이 expiredAt 이후(같은 시각 포함)이고 아직 당첨자 미선정, " +
                                        "WINNER_SELECTED: 당첨자 선정 완료). " +
                                        "이 API에서는 WINNER_SELECTING, ALL은 반환되지 않음"
                                ),
                            fieldWithPath("data.content[].thumbnailUrl").type(JsonFieldType.STRING)
                                .description("이벤트 썸네일 이미지 URL (항상 존재)"),
                            fieldWithPath("data.content[].winnerStatus").type(JsonFieldType.STRING)
                                .description(
                                    "당첨 상태 (WON: 본인이 당첨자로 선정됨, " +
                                        "LOST: 당첨자 선정이 끝났지만 본인은 미당첨, " +
                                        "PENDING: 아직 당첨자가 선정되지 않음). " +
                                        "status가 WINNER_SELECTED일 때만 WON 또는 LOST이며 " +
                                        "ACTIVE·EXPIRED이면 항상 PENDING. 당첨자 발표 공지글 게시 여부와는 무관"
                                ),
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
