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
                    status = ContestStatusType.EXPIRED,
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
                            fieldWithPath("data.content[]").type(JsonFieldType.ARRAY)
                                .description("참여한 콘테스트 목록 (참여 시각 최신순, 참여 피드 1건당 1항목)"),
                            fieldWithPath("data.content[].id").type(JsonFieldType.NUMBER).description("콘테스트 ID"),
                            fieldWithPath("data.content[].title").type(JsonFieldType.STRING).description("콘테스트 제목"),
                            fieldWithPath("data.content[].startedAt").type(JsonFieldType.STRING)
                                .description("콘테스트 시작 일시 (yyyy-MM-dd'T'HH:mm:ss, KST, 오프셋 없음)"),
                            fieldWithPath("data.content[].expiredAt").type(JsonFieldType.STRING)
                                .description(
                                    "콘테스트 마감 일시 (yyyy-MM-dd'T'HH:mm:ss, KST, 오프셋 없음). " +
                                        "이 시각 이후(이 시각 포함)에 작성한 피드는 참여로 인정되지 않음"
                                ),
                            fieldWithPath("data.content[].status").type(JsonFieldType.STRING)
                                .description(
                                    "조회 시점 기준 콘테스트 상태 (ACTIVE: 마감 전, " +
                                        "EXPIRED: 마감됨(수상자 선정 완료 포함)). 이 두 값만 옴. " +
                                        "수상자 선정 여부는 winnerStatus로 판단"
                                ),
                            fieldWithPath("data.content[].thumbnailUrl").type(JsonFieldType.STRING)
                                .description("콘테스트 썸네일 이미지 URL (항상 존재)"),
                            fieldWithPath("data.content[].tags[]").type(JsonFieldType.ARRAY)
                                .description(
                                    "콘테스트 참여 조건 태그 목록 ('#' 없이 한글·영문·숫자만, 태그당 1~20자, 중복 없음, 최대 7개). " +
                                        "피드 태그가 이 태그를 모두(대소문자까지 정확히 일치) 포함해야 참여로 인정됨. 순서 보장 없음"
                                ),
                            fieldWithPath("data.content[].winnerStatus").type(JsonFieldType.STRING)
                                .description(
                                    "피드가 아닌 콘테스트 단위의 수상 상태 " +
                                        "(WON:이 콘테스트에서 본인 참여 피드 중 하나 이상이 수상작으로 선정됨, " +
                                        "LOST: 수상자 선정이 끝났지만 본인 피드는 모두 미수상, " +
                                        "PENDING: 아직 수상자 선정 전)"
                                ),
                            fieldWithPath("data.content[].winnerRank").type(JsonFieldType.NUMBER)
                                .description(
                                    "수상 순위 (1~3, 1이 최고 순위). winnerStatus가 WON일 때만 값이 있고 LOST·PENDING이면 null. " +
                                        "같은 콘테스트에서 본인 피드가 여러 개 수상한 경우 그중 하나의 순위만 반환됨"
                                )
                                .optional(),
                            fieldWithPath("data.page").type(JsonFieldType.NUMBER).description("현재 페이지 번호"),
                            fieldWithPath("data.size").type(JsonFieldType.NUMBER).description("페이지 크기"),
                            fieldWithPath("data.totalElements").type(JsonFieldType.NUMBER)
                                .description("전체 요소 수 (참여 기록 수 기준)"),
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
