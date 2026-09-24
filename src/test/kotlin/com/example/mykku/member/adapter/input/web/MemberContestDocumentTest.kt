package com.example.mykku.member.adapter.input.web

import com.example.mykku.BaseDocumentTest
import com.example.mykku.contest.application.dto.ContestListResult
import com.example.mykku.contest.application.dto.PagedContestsResult
import com.example.mykku.contest.domain.vo.ContestStatusType
import com.example.mykku.contest.domain.vo.ContestWinnerStatus
import com.example.mykku.docs.ApiRequestConfig
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

class MemberContestDocumentTest : BaseDocumentTest() {

    @Nested
    @DisplayName("내가 참여한 콘테스트 목록 조회")
    inner class GetMyParticipatedContests {

        private val apiConfig = ApiRequestConfig(
            queryParameters = listOf(
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
                    title = "첫 번째 콘테스트",
                    startedAt = LocalDateTime.of(2025, 1, 1, 0, 0, 0),
                    expiredAt = LocalDateTime.of(2025, 12, 31, 23, 59, 59),
                    status = ContestStatusType.WINNER_SELECTED,
                    thumbnailUrl = "https://example.com/thumbnail1.jpg",
                    tags = listOf("디자인", "개발"),
                    winnerStatus = ContestWinnerStatus.WON,
                    winnerRank = 1
                ),
                ContestListResult(
                    id = 2L,
                    title = "두 번째 콘테스트",
                    startedAt = LocalDateTime.of(2025, 1, 1, 0, 0, 0),
                    expiredAt = LocalDateTime.of(2025, 11, 30, 23, 59, 59),
                    status = ContestStatusType.ACTIVE,
                    thumbnailUrl = "https://example.com/thumbnail2.jpg",
                    tags = listOf("기획"),
                    winnerStatus = ContestWinnerStatus.PENDING,
                    winnerRank = null
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

            whenever(getMyParticipatedContestsUseCase.execute(any(), any(), any())).thenReturn(result)

            val documentFilter = document("member/contests", 200)
                .request(request().applyConfig(apiConfig))
                .response(
                    response()
                        .responseBodyField(
                            fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                            fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터"),
                            fieldWithPath("data.content[]").type(JsonFieldType.ARRAY).description("콘테스트 목록"),
                            fieldWithPath("data.content[].id").type(JsonFieldType.NUMBER).description("콘테스트 ID"),
                            fieldWithPath("data.content[].title").type(JsonFieldType.STRING).description("콘테스트 제목"),
                            fieldWithPath("data.content[].startedAt").type(JsonFieldType.STRING).description("시작일"),
                            fieldWithPath("data.content[].expiredAt").type(JsonFieldType.STRING).description("만료일"),
                            fieldWithPath("data.content[].status").type(JsonFieldType.STRING).description("콘테스트 상태 (ACTIVE, EXPIRED)"),
                            fieldWithPath("data.content[].thumbnailUrl").type(JsonFieldType.STRING)
                                .description("썸네일 이미지 URL").optional(),
                            fieldWithPath("data.content[].tags[]").type(JsonFieldType.ARRAY).description("태그 목록"),
                            fieldWithPath("data.content[].winnerStatus").type(JsonFieldType.STRING)
                                .description("수상 상태 (WON: 수상, LOST: 낙선, PENDING: 발표 전)"),
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
                .param("page", 0)
                .param("size", 20)
                .`when`()
                .get("/api/v1/members/me/contests")
                .then()
                .statusCode(200)
        }

        @Test
        fun `빈 목록`() {
            val result = PagedContestsResult(
                content = emptyList(),
                page = 0,
                size = 20,
                totalElements = 0,
                totalPages = 0,
                isLast = true
            )

            whenever(getMyParticipatedContestsUseCase.execute(any(), any(), any())).thenReturn(result)

            val documentFilter = document("member/contests", "empty")
                .request(request().applyConfig(apiConfig))
                .response(
                    response()
                        .responseBodyField(
                            fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                            fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터"),
                            fieldWithPath("data.content[]").type(JsonFieldType.ARRAY).description("빈 콘테스트 목록"),
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
                .get("/api/v1/members/me/contests")
                .then()
                .statusCode(200)
        }
    }
}
