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
                parameterWithName("status").description("공모전 상태 (ACTIVE, EXPIRED, ALL) 기본값: ACTIVE").optional(),
                parameterWithName("sortType").description("정렬 방식 (LATEST, OLDEST, POPULAR) 기본값: LATEST")
                    .optional(),
                parameterWithName("page").description("페이지 번호 (0부터 시작, 기본값: 0)").optional(),
                parameterWithName("size").description("페이지 크기 (기본값: 20)").optional()
            ),
            headerDescriptors = AUTH_HEADER_DESCRIPTOR
        )

        @Test
        fun `성공`() {
            val contestList = listOf(
                ContestListResult(
                    id = 1L,
                    title = "첫 번째 공모전",
                    startedAt = LocalDateTime.now(),
                    expiredAt = LocalDateTime.of(2025, 12, 31, 23, 59, 59),
                    status = ContestStatusType.ACTIVE,
                    thumbnailUrl = "https://example.com/thumbnail1.jpg",
                    tags = listOf("디자인", "개발")
                ),
                ContestListResult(
                    id = 2L,
                    title = "두 번째 공모전",
                    startedAt = LocalDateTime.now(),
                    expiredAt = LocalDateTime.of(2025, 11, 30, 23, 59, 59),
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
                            fieldWithPath("data.content[]").type(JsonFieldType.ARRAY).description("공모전 목록"),
                            fieldWithPath("data.content[].id").type(JsonFieldType.NUMBER).description("공모전 ID"),
                            fieldWithPath("data.content[].title").type(JsonFieldType.STRING).description("공모전 제목"),
                            fieldWithPath("data.content[].expiredAt").type(JsonFieldType.STRING).description("만료일"),
                            fieldWithPath("data.content[].status").type(JsonFieldType.STRING).description("공모전 상태"),
                            fieldWithPath("data.content[].startedAt").type(JsonFieldType.STRING).description("시작일"),
                            fieldWithPath("data.content[].thumbnailUrl").type(JsonFieldType.STRING)
                                .description("썸네일 이미지 URL"),
                            fieldWithPath("data.content[].tags[]").type(JsonFieldType.ARRAY).description("태그 목록"),
                            fieldWithPath("data.content[].winnerStatus").type(JsonFieldType.STRING)
                                .description("수상 상태 (WON: 수상, LOST: 낙선, PENDING: 발표 전). 일반 목록에서는 항상 PENDING"),
                            fieldWithPath("data.content[].winnerRank").type(JsonFieldType.NUMBER).description("수상 순위").optional(),
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
                parameterWithName("contestId").description("공모전 ID")
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
                startedAt = LocalDateTime.now(),
                expiredAt = LocalDateTime.of(2025, 12, 31, 23, 59, 59),
                status = ContestStatusType.ACTIVE,
                thumbnailUrl = "https://example.com/thumbnail.jpg",
                images = listOf(
                    ContestImageResult(url = "https://example.com/image1.jpg", orderIndex = 0),
                    ContestImageResult(url = "https://example.com/image2.jpg", orderIndex = 1)
                ),
                tags = listOf("디자인", "개발", "기획"),
                createdAt = LocalDateTime.now()
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
                            fieldWithPath("data.description").type(JsonFieldType.STRING).description("공모전 설명")
                                .optional(),
                            fieldWithPath("data.expiredAt").type(JsonFieldType.STRING).description("만료일"),
                            fieldWithPath("data.images[]").type(JsonFieldType.ARRAY).description("공모전 이미지 목록"),
                            fieldWithPath("data.images[].url").type(JsonFieldType.STRING).description("이미지 URL"),
                            fieldWithPath("data.images[].orderIndex").type(JsonFieldType.NUMBER).description("이미지 순서"),
                            fieldWithPath("data.tags[]").type(JsonFieldType.ARRAY).description("태그 목록"),
                            fieldWithPath("data.createdAt").type(JsonFieldType.STRING).description("생성일시"),
                            fieldWithPath("data.startedAt").type(JsonFieldType.STRING).description("시작일시"),
                            fieldWithPath("data.status").type(JsonFieldType.STRING).description("공모전 상태"),
                            fieldWithPath("data.thumbnailUrl").type(JsonFieldType.STRING).description("썸네일 이미지 URL")
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
