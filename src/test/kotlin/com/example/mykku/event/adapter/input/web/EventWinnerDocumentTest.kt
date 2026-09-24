package com.example.mykku.event.adapter.input.web

import com.example.mykku.BaseDocumentTest
import com.example.mykku.docs.ApiRequestConfig
import com.example.mykku.docs.RestDocumentationResponse
import com.example.mykku.event.application.dto.EventWinnerAnnouncementResult
import com.example.mykku.event.application.dto.EventWinnerResult
import com.example.mykku.event.application.dto.EventWinnersResult
import com.example.mykku.event.application.dto.MyAwardEventResult
import com.example.mykku.event.application.dto.MyEventWinnerStatusResult
import com.example.mykku.event.application.dto.PagedMyAwardEventsResult
import com.example.mykku.event.exception.EventErrorCode
import com.example.mykku.event.exception.EventException
import io.restassured.http.ContentType
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import java.time.LocalDate
import java.time.LocalDateTime

class EventWinnerDocumentTest : BaseDocumentTest() {

    @Nested
    @DisplayName("이벤트 당첨자 목록 조회")
    inner class GetEventWinners {

        private val apiConfig = ApiRequestConfig(
            pathParameters = listOf(
                parameterWithName("eventId").description("이벤트 ID")
            )
        )

        @Test
        fun `성공`() {
            val eventId = 1L
            val result = EventWinnersResult(
                eventId = eventId,
                eventTitle = "테스트 이벤트",
                winners = listOf(
                    EventWinnerResult(
                        winnerId = 1L,
                        memberId = "winner1",
                        nickname = "당첨자1",
                        profileImage = "https://example.com/profile1.jpg"
                    ),
                    EventWinnerResult(
                        winnerId = 2L,
                        memberId = "winner2",
                        nickname = "당첨자2",
                        profileImage = "https://example.com/profile2.jpg"
                    )
                )
            )

            `when`(getEventWinnersUseCase.execute(eventId)).thenReturn(result)

            val documentFilter = document("event-winner/list", 200)
                .request(request().applyConfig(apiConfig))
                .response(
                    response()
                        .responseBodyField(
                            fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                            fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터"),
                            fieldWithPath("data.eventId").type(JsonFieldType.NUMBER).description("이벤트 ID"),
                            fieldWithPath("data.eventTitle").type(JsonFieldType.STRING).description("이벤트 제목"),
                            fieldWithPath("data.winners[]").type(JsonFieldType.ARRAY).description("당첨자 목록"),
                            fieldWithPath("data.winners[].winnerId").type(JsonFieldType.NUMBER).description("당첨자 ID"),
                            fieldWithPath("data.winners[].memberId").type(JsonFieldType.STRING)
                                .description("당첨 회원 ID").optional(),
                            fieldWithPath("data.winners[].nickname").type(JsonFieldType.STRING)
                                .description("당첨 회원 닉네임").optional(),
                            fieldWithPath("data.winners[].profileImage").type(JsonFieldType.STRING)
                                .description("당첨 회원 프로필 이미지")
                        )
                )
                .build()

            given(documentFilter)
                .contentType(ContentType.JSON)
                .`when`()
                .get("/api/v1/events/{eventId}/winners", eventId)
                .then()
                .statusCode(200)
        }

        @Test
        fun `이벤트 없음`() {
            val eventId = 999L

            `when`(getEventWinnersUseCase.execute(eventId))
                .thenThrow(EventException(EventErrorCode.EVENT_NOT_FOUND))

            val documentFilter = document("event-winner/list", "EVENT_NOT_FOUND")
                .request(request().applyConfig(apiConfig))
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            given(documentFilter)
                .contentType(ContentType.JSON)
                .`when`()
                .get("/api/v1/events/{eventId}/winners", eventId)
                .then()
                .statusCode(404)
        }
    }

    @Nested
    @DisplayName("당첨자 발표 공지 조회")
    inner class GetEventWinnerAnnouncement {

        private val apiConfig = ApiRequestConfig(
            pathParameters = listOf(
                parameterWithName("eventId").description("이벤트 ID")
            )
        )

        @Test
        fun `성공`() {
            val eventId = 1L
            val result = EventWinnerAnnouncementResult(
                eventId = eventId,
                eventTitle = "봄맞이 이벤트",
                title = "[봄맞이 이벤트] 수상자 발표",
                content = "참여해 주신 모든 분들께 감사드립니다.\n대상: OOO",
                announcedAt = LocalDate.of(2025, 10, 10)
            )

            `when`(getEventWinnerAnnouncementUseCase.execute(eventId)).thenReturn(result)

            val documentFilter = document("event-winner/announcement", 200)
                .request(request().applyConfig(apiConfig))
                .response(
                    response()
                        .responseBodyField(
                            fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                            fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터"),
                            fieldWithPath("data.eventId").type(JsonFieldType.NUMBER).description("이벤트 ID"),
                            fieldWithPath("data.eventTitle").type(JsonFieldType.STRING).description("이벤트 제목"),
                            fieldWithPath("data.title").type(JsonFieldType.STRING).description("공지 제목"),
                            fieldWithPath("data.content").type(JsonFieldType.STRING).description("공지 본문"),
                            fieldWithPath("data.announcedAt").type(JsonFieldType.STRING).description("발표일 (yyyy-MM-dd)")
                        )
                )
                .build()

            given(documentFilter)
                .contentType(ContentType.JSON)
                .`when`()
                .get("/api/v1/events/{eventId}/winner-announcement", eventId)
                .then()
                .statusCode(200)
        }

        @Test
        fun `공지 없음`() {
            val eventId = 999L

            `when`(getEventWinnerAnnouncementUseCase.execute(eventId))
                .thenThrow(EventException(EventErrorCode.WINNER_ANNOUNCEMENT_NOT_FOUND))

            val documentFilter = document("event-winner/announcement", "WINNER_ANNOUNCEMENT_NOT_FOUND")
                .request(request().applyConfig(apiConfig))
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            given(documentFilter)
                .contentType(ContentType.JSON)
                .`when`()
                .get("/api/v1/events/{eventId}/winner-announcement", eventId)
                .then()
                .statusCode(404)
        }
    }

    @Nested
    @DisplayName("내 당첨 여부 조회")
    inner class GetMyWinnerStatus {

        private val apiConfig = ApiRequestConfig(
            pathParameters = listOf(
                parameterWithName("eventId").description("이벤트 ID")
            ),
            headerDescriptors = AUTH_HEADER_DESCRIPTOR
        )

        @Test
        fun `성공 - 당첨자`() {
            val eventId = 1L
            val result = MyEventWinnerStatusResult(isWinner = true, winnerId = 5L)

            `when`(getMyEventWinnerStatusUseCase.execute(any())).thenReturn(result)

            val documentFilter = document("event-winner/my-status", 200)
                .request(request().applyConfig(apiConfig))
                .response(
                    response()
                        .responseBodyField(
                            fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                            fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터"),
                            fieldWithPath("data.isWinner").type(JsonFieldType.BOOLEAN).description("당첨 여부"),
                            fieldWithPath("data.winnerId").type(JsonFieldType.NUMBER).description("당첨자 ID").optional()
                        )
                )
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .`when`()
                .get("/api/v1/events/{eventId}/my-winner-status", eventId)
                .then()
                .statusCode(200)
        }

        @Test
        fun `당첨자 미발표`() {
            val eventId = 1L

            `when`(getMyEventWinnerStatusUseCase.execute(any()))
                .thenThrow(EventException(EventErrorCode.WINNER_NOT_ANNOUNCED))

            val documentFilter = document("event-winner/my-status", "WINNER_NOT_ANNOUNCED")
                .request(request().applyConfig(apiConfig))
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .`when`()
                .get("/api/v1/events/{eventId}/my-winner-status", eventId)
                .then()
                .statusCode(400)
        }
    }

    @Nested
    @DisplayName("내 당첨 이벤트 목록 조회")
    inner class GetMyAwardEvents {

        private val apiConfig = ApiRequestConfig(
            queryParameters = listOf(
                parameterWithName("page").description("페이지 번호 (0부터 시작)").optional(),
                parameterWithName("size").description("페이지 크기 (기본 20)").optional()
            ),
            headerDescriptors = AUTH_HEADER_DESCRIPTOR
        )

        @Test
        fun `성공`() {
            val result = PagedMyAwardEventsResult(
                content = listOf(
                    MyAwardEventResult(
                        eventId = 1L,
                        eventTitle = "첫 번째 이벤트",
                        thumbnailUrl = "https://example.com/thumbnail1.jpg",
                        startedAt = LocalDateTime.now().minusDays(30),
                        expiredAt = LocalDateTime.now().minusDays(1)
                    )
                ),
                page = 0,
                size = 20,
                totalElements = 1,
                totalPages = 1,
                isLast = true
            )

            `when`(getMyAwardEventsUseCase.execute(any())).thenReturn(result)

            val documentFilter = document("event-winner/my-awards", 200)
                .request(request().applyConfig(apiConfig))
                .response(
                    response()
                        .responseBodyField(
                            fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                            fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터"),
                            fieldWithPath("data.content[]").type(JsonFieldType.ARRAY).description("당첨 이벤트 목록"),
                            fieldWithPath("data.content[].eventId").type(JsonFieldType.NUMBER).description("이벤트 ID"),
                            fieldWithPath("data.content[].eventTitle").type(JsonFieldType.STRING).description("이벤트 제목"),
                            fieldWithPath("data.content[].thumbnailUrl").type(JsonFieldType.STRING).description("이벤트 썸네일 URL"),
                            fieldWithPath("data.content[].startedAt").type(JsonFieldType.STRING).description("시작일시"),
                            fieldWithPath("data.content[].expiredAt").type(JsonFieldType.STRING).description("종료일시"),
                            fieldWithPath("data.page").type(JsonFieldType.NUMBER).description("현재 페이지"),
                            fieldWithPath("data.size").type(JsonFieldType.NUMBER).description("페이지 크기"),
                            fieldWithPath("data.totalElements").type(JsonFieldType.NUMBER).description("전체 항목 수"),
                            fieldWithPath("data.totalPages").type(JsonFieldType.NUMBER).description("전체 페이지 수"),
                            fieldWithPath("data.isLast").type(JsonFieldType.BOOLEAN).description("마지막 페이지 여부")
                        )
                )
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .`when`()
                .get("/api/v1/events/my-awards")
                .then()
                .statusCode(200)
        }
    }
}
