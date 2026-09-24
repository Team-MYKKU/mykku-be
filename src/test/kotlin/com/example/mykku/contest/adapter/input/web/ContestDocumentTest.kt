package com.example.mykku.contest.adapter.input.web

import com.example.mykku.BaseDocumentTest
import com.example.mykku.contest.application.dto.ContestDetailResult
import com.example.mykku.contest.application.dto.ContestImageResult
import com.example.mykku.contest.application.dto.ContestListResult
import com.example.mykku.contest.application.dto.PagedContestsResult
import com.example.mykku.contest.domain.vo.ContestStatusType
import com.example.mykku.docs.ApiRequestConfig
import com.example.mykku.docs.RestDocumentationResponse
import com.example.mykku.contest.exception.ContestErrorCode
import com.example.mykku.contest.exception.ContestException
import io.restassured.http.ContentType
import java.time.LocalDateTime
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName

class ContestDocumentTest : BaseDocumentTest() {

    @Nested
    @DisplayName("공모전 목록 조회")
    inner class GetContestList {

        private val apiConfig = ApiRequestConfig(
            queryParameters = listOf(
                parameterWithName("status")
                    .description(
                        "기간 필터 (ACTIVE: 마감 전(expiredAt > 현재 시각)이며 아직 시작하지 않은 공모전도 포함, " +
                            "EXPIRED: 마감됨(expiredAt ≤ 현재 시각)이며 수상자 선정 여부와 무관, ALL: 전체, " +
                            "WINNER_SELECTING: ACTIVE와 같은 결과, WINNER_SELECTED: EXPIRED와 같은 결과). " +
                            "기본값: ACTIVE. 저장된 status가 아니라 expiredAt 기준으로 거름. " +
                            "대문자 enum 이름 그대로 보내야 함 (대소문자 구분, 그 외 값은 400 C106)"
                    )
                    .optional(),
                parameterWithName("sortType")
                    .description(
                        "정렬 방식 (LATEST: 등록일시(createdAt) 최신순, OLDEST: 등록일시 오래된순, " +
                            "POPULAR: 현재는 LATEST와 같은 순서). 기본값: LATEST. " +
                            "status가 ACTIVE, WINNER_SELECTING일 때만 적용되며 " +
                            "EXPIRED, WINNER_SELECTED, ALL은 항상 등록일시 최신순. " +
                            "대문자 enum 이름 그대로 보내야 함 (대소문자 구분, 그 외 값은 400 C106)"
                    )
                    .optional(),
                parameterWithName("page").description("페이지 번호 (0부터 시작, 0 이상, 기본값: 0)").optional(),
                parameterWithName("size").description("페이지 크기 (1~1000, 기본값: 20)").optional()
            ),
            headerDescriptors = AUTH_HEADER_DESCRIPTOR
        )

        @Test
        fun `성공`() {
            val contestList = listOf(
                ContestListResult(
                    id = 1L,
                    title = "첫 번째 공모전",
                    startedAt = LocalDateTime.of(2026, 9, 15, 0, 0, 0),
                    expiredAt = LocalDateTime.of(2026, 11, 30, 23, 59, 59),
                    status = ContestStatusType.ACTIVE,
                    thumbnailUrl = "https://example.com/thumbnail1.jpg",
                    tags = listOf("디자인", "개발")
                ),
                ContestListResult(
                    id = 2L,
                    title = "두 번째 공모전",
                    startedAt = LocalDateTime.of(2026, 9, 1, 0, 0, 0),
                    expiredAt = LocalDateTime.of(2026, 10, 31, 23, 59, 59),
                    status = ContestStatusType.ACTIVE,
                    thumbnailUrl = "https://example.com/thumbnail2.jpg",
                    tags = listOf("기획")
                )
            )

            val result = PagedContestsResult(
                content = contestList,
                page = 0,
                size = 20,
                totalElements = 2,
                totalPages = 1,
                isLast = true
            )

            `when`(listContestsUseCase.execute(any())).thenReturn(result)

            val documentFilter = document("contest/list", 200)
                .request(request().applyConfig(apiConfig))
                .response(
                    response()
                        .responseBodyField(
                            fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                            fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터"),
                            fieldWithPath("data.content[]").type(JsonFieldType.ARRAY)
                                .description("공모전 목록 (결과가 없으면 빈 배열)"),
                            fieldWithPath("data.content[].id").type(JsonFieldType.NUMBER).description("공모전 ID"),
                            fieldWithPath("data.content[].title").type(JsonFieldType.STRING).description("공모전 제목"),
                            fieldWithPath("data.content[].expiredAt").type(JsonFieldType.STRING)
                                .description(
                                    "공모전 마감 일시 (KST, ISO-8601, 오프셋 없음). " +
                                        "이 시각과 같아지는 순간부터 마감으로 분류됨 (EXPIRED 필터 조건: expiredAt ≤ 현재 시각)"
                                ),
                            fieldWithPath("data.content[].status").type(JsonFieldType.STRING)
                                .description(
                                    "공모전 저장 상태 (ACTIVE: 수상자 선정 전, WINNER_SELECTED: 수상자 선정 완료). " +
                                        "응답에 오는 값은 이 두 가지이며 " +
                                        "expiredAt이 지나도 자동으로 바뀌지 않으므로 마감 여부는 expiredAt으로 판단"
                                ),
                            fieldWithPath("data.content[].startedAt").type(JsonFieldType.STRING)
                                .description("공모전 시작 일시 (KST, ISO-8601, 오프셋 없음)"),
                            fieldWithPath("data.content[].thumbnailUrl").type(JsonFieldType.STRING)
                                .description("썸네일 이미지 URL"),
                            fieldWithPath("data.content[].tags[]").type(JsonFieldType.ARRAY)
                                .description(
                                    "태그 문자열 목록 ('#' 미포함, 한글/영문/숫자 1~20자, 중복 없음, 최대 7개, " +
                                        "없으면 빈 배열, 순서 보장 안 됨)"
                                ),
                            fieldWithPath("data.content[].winnerStatus").type(JsonFieldType.STRING)
                                .description(
                                    "수상 상태 (WON: 수상, LOST: 낙선, PENDING: 발표 전). " +
                                        "이 API에서는 항상 PENDING (내가 참여한 콘테스트 목록 API와 공통 응답 형식)"
                                ),
                            fieldWithPath("data.content[].winnerRank").type(JsonFieldType.NUMBER)
                                .description(
                                    "수상 순위 (1~3, winnerStatus가 WON일 때만 값 존재). " +
                                        "이 API에서는 항상 null (내가 참여한 콘테스트 목록 API와 공통 응답 형식)"
                                )
                                .optional(),
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
                .get("/api/v1/contests")
                .then()
                .statusCode(200)
        }
    }

    @Nested
    @DisplayName("공모전 상세 조회")
    inner class GetContestDetail {

        private val apiConfig = ApiRequestConfig(
            pathParameters = listOf(
                parameterWithName("contestId").description("공모전 ID (숫자, 목록 조회 응답의 data.content[].id)")
            ),
            headerDescriptors = AUTH_HEADER_DESCRIPTOR
        )

        @Test
        fun `성공`() {
            val contestId = 1L
            val result = ContestDetailResult(
                id = contestId,
                title = "공모전 제목",
                description = "공모전 상세 설명입니다.",
                startedAt = LocalDateTime.of(2026, 9, 1, 0, 0, 0),
                expiredAt = LocalDateTime.of(2026, 10, 31, 23, 59, 59),
                status = ContestStatusType.ACTIVE,
                thumbnailUrl = "https://example.com/thumbnail.jpg",
                images = listOf(
                    ContestImageResult(url = "https://example.com/image1.jpg", orderIndex = 0),
                    ContestImageResult(url = "https://example.com/image2.jpg", orderIndex = 1)
                ),
                tags = listOf("디자인", "개발", "기획"),
                createdAt = LocalDateTime.of(2026, 8, 25, 10, 0, 0)
            )

            `when`(getContestUseCase.execute(any(), any())).thenReturn(result)

            val documentFilter = document("contest/detail", 200)
                .request(request().applyConfig(apiConfig))
                .response(
                    response()
                        .responseBodyField(
                            fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                            fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터"),
                            fieldWithPath("data.id").type(JsonFieldType.NUMBER).description("공모전 ID"),
                            fieldWithPath("data.title").type(JsonFieldType.STRING).description("공모전 제목"),
                            fieldWithPath("data.description").type(JsonFieldType.STRING)
                                .description("공모전 설명 (등록 시 입력하지 않았으면 null 또는 빈 문자열)")
                                .optional(),
                            fieldWithPath("data.expiredAt").type(JsonFieldType.STRING)
                                .description(
                                    "공모전 마감 일시 (KST, ISO-8601, 오프셋 없음). " +
                                        "이 시각과 같아지는 순간부터 마감으로 분류됨"
                                ),
                            fieldWithPath("data.images[]").type(JsonFieldType.ARRAY)
                                .description(
                                    "공모전 상세 이미지 목록 (orderIndex 오름차순 정렬, 최대 10장, 없으면 빈 배열. " +
                                        "썸네일(thumbnailUrl)은 포함되지 않음)"
                                ),
                            fieldWithPath("data.images[].url").type(JsonFieldType.STRING).description("이미지 URL"),
                            fieldWithPath("data.images[].orderIndex").type(JsonFieldType.NUMBER)
                                .description("이미지 표시 순서 (0부터 시작)"),
                            fieldWithPath("data.tags[]").type(JsonFieldType.ARRAY)
                                .description(
                                    "태그 문자열 목록 ('#' 미포함, 한글/영문/숫자 1~20자, 중복 없음, 최대 7개, " +
                                        "없으면 빈 배열, 순서 보장 안 됨)"
                                ),
                            fieldWithPath("data.createdAt").type(JsonFieldType.STRING)
                                .description(
                                    "공모전이 서버에 등록된 일시 (KST, ISO-8601, 오프셋 없음). " +
                                        "공모전 기간과 무관하며 공모전 목록 조회의 정렬(sortType) 기준"
                                ),
                            fieldWithPath("data.startedAt").type(JsonFieldType.STRING)
                                .description("공모전 시작 일시 (KST, ISO-8601, 오프셋 없음)"),
                            fieldWithPath("data.status").type(JsonFieldType.STRING)
                                .description(
                                    "공모전 저장 상태 (ACTIVE: 수상자 선정 전, WINNER_SELECTED: 수상자 선정 완료). " +
                                        "응답에 오는 값은 이 두 가지이며 " +
                                        "expiredAt이 지나도 자동으로 바뀌지 않으므로 마감 여부는 expiredAt으로 판단"
                                ),
                            fieldWithPath("data.thumbnailUrl").type(JsonFieldType.STRING)
                                .description("썸네일 이미지 URL (images[]와 별도)")
                        )
                )
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .`when`()
                .get("/api/v1/contests/{contestId}", contestId)
                .then()
                .statusCode(200)
        }

        @Test
        fun `존재하지 않는 공모전`() {
            val contestId = 999L
            `when`(getContestUseCase.execute(any(), any()))
                .thenThrow(ContestException(ContestErrorCode.CONTEST_NOT_FOUND))

            val documentFilter = document("contest/detail", "CONTEST_NOT_FOUND")
                .request(request().applyConfig(apiConfig))
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .`when`()
                .get("/api/v1/contests/{contestId}", contestId)
                .then()
                .statusCode(404)
        }
    }
}
