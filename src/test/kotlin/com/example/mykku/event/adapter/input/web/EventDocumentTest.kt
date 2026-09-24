package com.example.mykku.event.adapter.input.web

import com.example.mykku.BaseDocumentTest
import com.example.mykku.docs.ApiRequestConfig
import com.example.mykku.docs.RestDocumentationResponse
import com.example.mykku.event.application.dto.EventDetailResult
import com.example.mykku.event.application.dto.EventImageResult
import com.example.mykku.event.application.dto.EventListResult
import com.example.mykku.event.application.dto.PagedEventsResult
import com.example.mykku.event.domain.vo.EventStatusType
import com.example.mykku.event.exception.EventErrorCode
import com.example.mykku.event.exception.EventException
import io.restassured.http.ContentType
import java.time.LocalDateTime
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName

class EventDocumentTest : BaseDocumentTest() {

    @Nested
    @DisplayName("이벤트 목록 조회")
    inner class GetEventList {

        private val apiConfig = ApiRequestConfig(
            queryParameters = listOf(
                parameterWithName("status").description("이벤트 상태 (ACTIVE, EXPIRED, ALL) 기본값: ACTIVE").optional(),
                parameterWithName("sortType").description("정렬 방식 (LATEST, OLDEST, POPULAR) 기본값: LATEST")
                    .optional(),
                parameterWithName("page").description("페이지 번호 (0부터 시작, 기본값: 0)").optional(),
                parameterWithName("size").description("페이지 크기 (기본값: 20)").optional()
            ),
            headerDescriptors = AUTH_HEADER_DESCRIPTOR
        )

        @Test
        fun `성공`() {
            val eventList = listOf(
                EventListResult(
                    id = 1L,
                    title = "첫 번째 이벤트",
                    subTitle = "부제목입니다.",
                    description = "첫 번째 이벤트 설명입니다.",
                    startedAt = LocalDateTime.of(2025, 1, 1, 0, 0, 0),
                    expiredAt = LocalDateTime.of(2025, 12, 31, 23, 59, 59),
                    status = EventStatusType.ACTIVE,
                    thumbnailUrl = "https://example.com/thumbnail1.jpg"
                ),
                EventListResult(
                    id = 2L,
                    title = "두 번째 이벤트",
                    subTitle = "부제목입니다.",
                    description = "두 번째 이벤트 설명입니다.",
                    startedAt = LocalDateTime.of(2025, 1, 1, 0, 0, 0),
                    expiredAt = LocalDateTime.of(2025, 11, 30, 23, 59, 59),
                    status = EventStatusType.ACTIVE,
                    thumbnailUrl = "https://example.com/thumbnail2.jpg"
                )
            )

            val response = PagedEventsResult(
                content = eventList,
                page = 0,
                size = 20,
                totalElements = 2,
                totalPages = 1,
                isLast = true
            )

            `when`(listEventsUseCase.execute(any())).thenReturn(response)

            val documentFilter = document("event/list", 200)
                .request(request().applyConfig(apiConfig))
                .response(
                    response()
                        .responseBodyField(
                            fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                            fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터"),
                            fieldWithPath("data.content[]").type(JsonFieldType.ARRAY).description("이벤트 목록"),
                            fieldWithPath("data.content[].id").type(JsonFieldType.NUMBER).description("이벤트 ID"),
                            fieldWithPath("data.content[].title").type(JsonFieldType.STRING).description("이벤트 제목"),
                            fieldWithPath("data.content[].subTitle").type(JsonFieldType.STRING)
                                .description("이벤트 부제목").optional(),
                            fieldWithPath("data.content[].description").type(JsonFieldType.STRING)
                                .description("이벤트 설명").optional(),
                            fieldWithPath("data.content[].expiredAt").type(JsonFieldType.STRING).description("만료일"),
                            fieldWithPath("data.content[].thumbnailUrl").type(JsonFieldType.STRING)
                                .description("썸네일 이미지 URL"),
                            fieldWithPath("data.content[].status").type(JsonFieldType.STRING).description("이벤트 상태"),
                            fieldWithPath("data.content[].startedAt").type(JsonFieldType.STRING).description("시작일"),
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
                .param("status", "ACTIVE")
                .param("sortType", "LATEST")
                .`when`()
                .get("/api/v1/events")
                .then()
                .statusCode(200)
        }
    }

    @Nested
    @DisplayName("이벤트 상세 조회")
    inner class GetEventDetail {

        private val apiConfig = ApiRequestConfig(
            pathParameters = listOf(
                parameterWithName("eventId").description("이벤트 ID")
            ),
            headerDescriptors = AUTH_HEADER_DESCRIPTOR
        )

        @Test
        fun `성공`() {
            val eventId = 1L
            val response = EventDetailResult(
                id = eventId,
                title = "이벤트 제목",
                subTitle = "부제목입니다.",
                description = "이벤트 상세 설명입니다.",
                startedAt = LocalDateTime.of(2025, 1, 1, 0, 0, 0),
                expiredAt = LocalDateTime.of(2025, 12, 31, 23, 59, 59),
                status = EventStatusType.ACTIVE,
                thumbnailUrl = "https://example.com/thumbnail.jpg",
                images = listOf(
                    EventImageResult(url = "https://example.com/image1.jpg", orderIndex = 0),
                    EventImageResult(url = "https://example.com/image2.jpg", orderIndex = 1)
                ),
                createdAt = LocalDateTime.now(),
                isWinner = true
            )

            `when`(getEventUseCase.execute(any(), any())).thenReturn(response)

            val documentFilter = document("event/detail", 200)
                .request(request().applyConfig(apiConfig))
                .response(
                    response()
                        .responseBodyField(
                            fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                            fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터"),
                            fieldWithPath("data.id").type(JsonFieldType.NUMBER).description("이벤트 ID"),
                            fieldWithPath("data.title").type(JsonFieldType.STRING).description("이벤트 제목"),
                            fieldWithPath("data.subTitle").type(JsonFieldType.STRING).description("이벤트 부제목")
                                .optional(),
                            fieldWithPath("data.description").type(JsonFieldType.STRING).description("이벤트 설명")
                                .optional(),
                            fieldWithPath("data.expiredAt").type(JsonFieldType.STRING).description("만료일"),
                            fieldWithPath("data.images[]").type(JsonFieldType.ARRAY).description("이벤트 이미지 목록"),
                            fieldWithPath("data.images[].url").type(JsonFieldType.STRING).description("이미지 URL"),
                            fieldWithPath("data.images[].orderIndex").type(JsonFieldType.NUMBER).description("이미지 순서"),
                            fieldWithPath("data.createdAt").type(JsonFieldType.STRING).description("생성일시"),
                            fieldWithPath("data.startedAt").type(JsonFieldType.STRING).description("시작일"),
                            fieldWithPath("data.status").type(JsonFieldType.STRING).description("이벤트 상태"),
                            fieldWithPath("data.thumbnailUrl").type(JsonFieldType.STRING).description("썸네일 이미지 URL"),
                            fieldWithPath("data.isWinner").type(JsonFieldType.BOOLEAN).description("현재 사용자의 당첨 여부")
                        )
                )
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .`when`()
                .get("/api/v1/events/{eventId}", eventId)
                .then()
                .statusCode(200)
        }

        @Test
        fun `존재하지 않는 이벤트`() {
            val eventId = 999L
            `when`(getEventUseCase.execute(any(), any()))
                .thenThrow(EventException(EventErrorCode.EVENT_NOT_FOUND))

            val documentFilter = document("event/detail", "EVENT_NOT_FOUND")
                .request(request().applyConfig(apiConfig))
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .`when`()
                .get("/api/v1/events/{eventId}", eventId)
                .then()
                .statusCode(404)
        }
    }
}
