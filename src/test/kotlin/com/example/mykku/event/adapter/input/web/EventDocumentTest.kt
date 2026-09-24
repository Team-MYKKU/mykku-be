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
                parameterWithName("status")
                    .description(
                        "조회할 이벤트 상태 (ACTIVE: 종료 일시 전 이벤트(시작 전 포함), " +
                            "EXPIRED: 종료된 이벤트(당첨자 발표 완료 포함), ALL: 전체). " +
                            "기본값: ACTIVE, 대문자만 허용. WINNER_SELECTED 등 그 외 값은 400 C106"
                    )
                    .optional(),
                parameterWithName("sortType")
                    .description(
                        "정렬 방식, status=ACTIVE일 때만 적용 (LATEST: 등록일시 최신순, OLDEST: 등록일시 오래된순, " +
                            "POPULAR: 현재 LATEST와 동일). 기본값: LATEST, 대문자만 허용. " +
                            "EXPIRED, ALL은 sortType과 관계없이 등록일시 최신순"
                    )
                    .optional(),
                parameterWithName("page").description("페이지 번호 (0부터 시작, 0 이상, 기본값: 0)").optional(),
                parameterWithName("size").description("페이지 크기 (1~1000, 기본값: 20)").optional()
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
                            fieldWithPath("data.content[]").type(JsonFieldType.ARRAY)
                                .description("이벤트 목록 (결과가 없으면 빈 배열)"),
                            fieldWithPath("data.content[].id").type(JsonFieldType.NUMBER).description("이벤트 ID"),
                            fieldWithPath("data.content[].title").type(JsonFieldType.STRING).description("이벤트 제목"),
                            fieldWithPath("data.content[].subTitle").type(JsonFieldType.STRING)
                                .description("이벤트 부제목 (미입력 시 null 또는 빈 문자열)").optional(),
                            fieldWithPath("data.content[].description").type(JsonFieldType.STRING)
                                .description("이벤트 설명 (미입력 시 null 또는 빈 문자열)").optional(),
                            fieldWithPath("data.content[].expiredAt").type(JsonFieldType.STRING)
                                .description(
                                    "이벤트 종료 일시 (ISO-8601, KST, 오프셋 없음, 예: 2025-12-31T23:59:59). " +
                                        "이 시각부터 EXPIRED"
                                ),
                            fieldWithPath("data.content[].thumbnailUrl").type(JsonFieldType.STRING)
                                .description("썸네일 이미지 URL"),
                            fieldWithPath("data.content[].status").type(JsonFieldType.STRING)
                                .description(
                                    "이벤트 상태, 조회 시점 기준으로 계산 (ACTIVE: 종료 일시 전(시작 전 포함), " +
                                        "EXPIRED: 종료됨·당첨자 발표 전, WINNER_SELECTED: 종료 후 당첨자 발표 완료). " +
                                        "WINNER_SELECTING, ALL은 응답에 오지 않음"
                                ),
                            fieldWithPath("data.content[].startedAt").type(JsonFieldType.STRING)
                                .description(
                                    "이벤트 시작 일시 (ISO-8601, KST, 오프셋 없음, 예: 2025-01-01T00:00:00). " +
                                        "시작 전이어도 종료 전이면 ACTIVE"
                                ),
                            fieldWithPath("data.page").type(JsonFieldType.NUMBER).description("현재 페이지 번호 (0부터 시작)"),
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
                parameterWithName("eventId").description("이벤트 ID (존재하지 않거나 0 이하이면 404 EVENT_NOT_FOUND)")
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
                status = EventStatusType.WINNER_SELECTED,
                thumbnailUrl = "https://example.com/thumbnail.jpg",
                images = listOf(
                    EventImageResult(url = "https://example.com/image1.jpg", orderIndex = 0),
                    EventImageResult(url = "https://example.com/image2.jpg", orderIndex = 1)
                ),
                createdAt = LocalDateTime.of(2024, 12, 20, 10, 30, 15, 123456000),
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
                            fieldWithPath("data.subTitle").type(JsonFieldType.STRING)
                                .description("이벤트 부제목 (미입력 시 null 또는 빈 문자열)")
                                .optional(),
                            fieldWithPath("data.description").type(JsonFieldType.STRING)
                                .description("이벤트 설명 (미입력 시 null 또는 빈 문자열)")
                                .optional(),
                            fieldWithPath("data.expiredAt").type(JsonFieldType.STRING)
                                .description(
                                    "이벤트 종료 일시 (ISO-8601, KST, 오프셋 없음, 예: 2025-12-31T23:59:59). " +
                                        "이 시각부터 EXPIRED"
                                ),
                            fieldWithPath("data.images[]").type(JsonFieldType.ARRAY)
                                .description(
                                    "이벤트 본문 이미지 목록 (0~10개, 없으면 빈 배열, orderIndex 오름차순 정렬됨). " +
                                        "썸네일(thumbnailUrl)은 포함되지 않음"
                                ),
                            fieldWithPath("data.images[].url").type(JsonFieldType.STRING).description("이미지 URL"),
                            fieldWithPath("data.images[].orderIndex").type(JsonFieldType.NUMBER)
                                .description("이미지 노출 순서 (0부터 시작)"),
                            fieldWithPath("data.createdAt").type(JsonFieldType.STRING)
                                .description(
                                    "이벤트 등록 일시 (ISO-8601, KST, 오프셋 없음). " +
                                        "소수점 이하 초(최대 6자리)가 붙을 수 있음"
                                ),
                            fieldWithPath("data.startedAt").type(JsonFieldType.STRING)
                                .description("이벤트 시작 일시 (ISO-8601, KST, 오프셋 없음, 예: 2025-01-01T00:00:00)"),
                            fieldWithPath("data.status").type(JsonFieldType.STRING)
                                .description(
                                    "이벤트 상태, 조회 시점 기준으로 계산 (ACTIVE: 종료 일시 전(시작 전 포함), " +
                                        "EXPIRED: 종료됨·당첨자 발표 전, WINNER_SELECTED: 종료 후 당첨자 발표 완료). " +
                                        "WINNER_SELECTING, ALL은 응답에 오지 않음. " +
                                        "WINNER_SELECTED 전에는 내 당첨 여부 조회 API가 WINNER_NOT_ANNOUNCED를 반환"
                                ),
                            fieldWithPath("data.thumbnailUrl").type(JsonFieldType.STRING).description("썸네일 이미지 URL"),
                            fieldWithPath("data.isWinner").type(JsonFieldType.BOOLEAN)
                                .description(
                                    "로그인 사용자의 당첨 여부 (true: 당첨자로 선정됨, false: 미당첨 또는 아직 발표 전). " +
                                        "발표 전에는 항상 false이므로 status가 WINNER_SELECTED인지 함께 확인. " +
                                        "참여 여부와는 무관"
                                )
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
