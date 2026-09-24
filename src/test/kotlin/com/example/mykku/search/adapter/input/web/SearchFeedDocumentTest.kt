package com.example.mykku.search.adapter.input.web

import com.example.mykku.BaseDocumentTest
import com.example.mykku.board.exception.BoardException
import com.example.mykku.docs.ApiRequestConfig
import com.example.mykku.docs.RestDocumentationResponse
import com.example.mykku.feed.application.dto.BoardSearchGroupResult
import com.example.mykku.feed.application.dto.FeedSearchItemResult
import com.example.mykku.feed.application.dto.FeedSearchResult
import com.example.mykku.feed.application.dto.PagedFeedSearchResult
import com.example.mykku.feed.exception.FeedException
import io.restassured.http.ContentType
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.restdocs.payload.FieldDescriptor
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.request.ParameterDescriptor
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.restassured.RestDocumentationFilter
import java.time.LocalDateTime

class SearchFeedDocumentTest : BaseDocumentTest() {

    @Nested
    @DisplayName("게시판별 피드 검색")
    inner class SearchFeeds {

        private val apiConfig = ApiRequestConfig(
            queryParameters = listOf(KEYWORD_PARAMETER),
            headerDescriptors = OPTIONAL_AUTH_HEADER_DESCRIPTOR
        )

        @Test
        fun `성공`() {
            whenever(searchFeedsUseCase.execute(any())).thenReturn(searchResult())

            val documentFilter = document("search/feeds", 200)
                .request(request().applyConfig(apiConfig))
                .response(response().responseBodyField(*groupFields().toTypedArray()))
                .build()

            requestSearchFeeds(documentFilter, "IVE", 200)
        }

        @Test
        fun `검색 결과 없음`() {
            whenever(searchFeedsUseCase.execute(any())).thenReturn(FeedSearchResult(boards = emptyList()))

            val documentFilter = document("search/feeds", "empty")
                .request(request().applyConfig(apiConfig))
                .response(
                    response().responseBodyField(
                        fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                        fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터"),
                        fieldWithPath("data.boards[]").type(JsonFieldType.ARRAY).description("빈 게시판 그룹 목록 []")
                    )
                )
                .build()

            requestSearchFeeds(documentFilter, "없는검색어", 200)
        }

        @Test
        fun `빈 검색어 에러`() {
            whenever(searchFeedsUseCase.execute(any())).thenThrow(FeedException.searchKeywordEmpty())

            val documentFilter = document("search/feeds", "SEARCH_KEYWORD_EMPTY")
                .request(request().applyConfig(apiConfig))
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            requestSearchFeeds(documentFilter, "   ", 400)
        }

        @Test
        fun `검색어 길이 초과 에러`() {
            whenever(searchFeedsUseCase.execute(any())).thenThrow(FeedException.searchKeywordTooLong())

            val documentFilter = document("search/feeds", "SEARCH_KEYWORD_TOO_LONG")
                .request(request().applyConfig(apiConfig))
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            requestSearchFeeds(documentFilter, "a".repeat(51), 400)
        }

        private fun requestSearchFeeds(
            documentFilter: RestDocumentationFilter,
            keyword: String,
            status: Int
        ) {
            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .queryParam("keyword", keyword)
                .`when`()
                .get("/api/v1/search/feeds")
                .then()
                .statusCode(status)
        }
    }

    @Nested
    @DisplayName("게시판 안 피드 검색")
    inner class SearchBoardFeeds {

        private val apiConfig = ApiRequestConfig(
            pathParameters = listOf(
                parameterWithName("boardId").description("검색할 게시판의 ID")
            ),
            queryParameters = listOf(
                KEYWORD_PARAMETER,
                parameterWithName("page").description("페이지 번호 (0부터 시작, 기본값: 0). 음수면 400 C103")
                    .optional(),
                parameterWithName("size").description("페이지 크기 (1~1000, 기본값: 20). 범위를 벗어나면 400 C104")
                    .optional()
            ),
            headerDescriptors = OPTIONAL_AUTH_HEADER_DESCRIPTOR
        )

        @Test
        fun `성공`() {
            whenever(searchBoardFeedsUseCase.execute(any())).thenReturn(pagedResult())

            val documentFilter = document("search/board-feeds", 200)
                .request(request().applyConfig(apiConfig))
                .response(response().responseBodyField(*pagedFields().toTypedArray()))
                .build()

            requestSearchBoardFeeds(documentFilter, "IVE", 200)
        }

        @Test
        fun `빈 검색어 에러`() {
            whenever(searchBoardFeedsUseCase.execute(any())).thenThrow(FeedException.searchKeywordEmpty())

            val documentFilter = document("search/board-feeds", "SEARCH_KEYWORD_EMPTY")
                .request(request().applyConfig(apiConfig))
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            requestSearchBoardFeeds(documentFilter, "   ", 400)
        }

        @Test
        fun `검색어 길이 초과 에러`() {
            whenever(searchBoardFeedsUseCase.execute(any())).thenThrow(FeedException.searchKeywordTooLong())

            val documentFilter = document("search/board-feeds", "SEARCH_KEYWORD_TOO_LONG")
                .request(request().applyConfig(apiConfig))
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            requestSearchBoardFeeds(documentFilter, "a".repeat(51), 400)
        }

        @Test
        fun `게시판 없음 에러`() {
            whenever(searchBoardFeedsUseCase.execute(any())).thenThrow(BoardException.boardNotFound())

            val documentFilter = document("search/board-feeds", "BOARD_NOT_FOUND")
                .request(request().applyConfig(apiConfig))
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            requestSearchBoardFeeds(documentFilter, "IVE", 404, boardId = 999L)
        }

        private fun requestSearchBoardFeeds(
            documentFilter: RestDocumentationFilter,
            keyword: String,
            status: Int,
            boardId: Long = 2L
        ) {
            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .queryParam("keyword", keyword)
                .queryParam("page", 0)
                .queryParam("size", 20)
                .`when`()
                .get("/api/v1/search/feeds/boards/{boardId}", boardId)
                .then()
                .statusCode(status)
        }
    }

    private fun searchResult(): FeedSearchResult = FeedSearchResult(boards = listOf(iveGroup(), freeBoardGroup()))

    private fun iveGroup(): BoardSearchGroupResult = BoardSearchGroupResult(
        boardId = 2L,
        boardTitle = "아이브",
        boardLogo = "https://example.com/ive-logo.png",
        totalCount = 12L,
        hasMore = true,
        feeds = listOf(
            item(18L, 2L, "아이브", "https://example.com/feed18.jpg"),
            item(17L, 2L, "아이브", null),
            item(16L, 2L, "아이브", "https://example.com/feed16.jpg")
        )
    )

    private fun freeBoardGroup(): BoardSearchGroupResult = BoardSearchGroupResult(
        boardId = 1L,
        boardTitle = "자유게시판",
        boardLogo = "https://example.com/free-logo.png",
        totalCount = 1L,
        hasMore = false,
        feeds = listOf(item(5L, 1L, "자유게시판", "https://example.com/feed5.jpg"))
    )

    private fun pagedResult(): PagedFeedSearchResult = PagedFeedSearchResult(
        feeds = listOf(item(18L, 2L, "아이브", "https://example.com/feed18.jpg"), item(17L, 2L, "아이브", null)),
        currentPage = 0,
        totalPages = 1,
        totalElements = 2L,
        size = 20,
        hasNext = false,
        hasPrevious = false
    )

    private fun item(id: Long, boardId: Long, boardTitle: String, thumbnailUrl: String?) = FeedSearchItemResult(
        id = id,
        boardId = boardId,
        boardTitle = boardTitle,
        title = "IVE 컴백 무대 $id",
        content = "오늘 ive 컴백 무대 본문 전체입니다.",
        thumbnailUrl = thumbnailUrl,
        createdAt = LocalDateTime.of(2025, 3, 1, 12, 34, 56).minusHours(18L - id)
    )

    private fun groupFields(): List<FieldDescriptor> = listOf(
        fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
        fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터"),
        fieldWithPath("data.boards[]").type(JsonFieldType.ARRAY)
            .description("게시판 그룹 목록 (매칭 수 내림차순 → 최근 매칭 피드가 최신인 순 → boardId 오름차순). 결과가 없으면 빈 배열 []"),
        fieldWithPath("data.boards[].boardId").type(JsonFieldType.NUMBER).description("게시판 ID (그룹 키)"),
        fieldWithPath("data.boards[].boardTitle").type(JsonFieldType.STRING).description("게시판 이름 (UNIQUE 아님)"),
        fieldWithPath("data.boards[].boardLogo").type(JsonFieldType.STRING).description("게시판 로고 URL"),
        fieldWithPath("data.boards[].totalCount").type(JsonFieldType.NUMBER)
            .description("이 게시판의 전체 매칭 피드 수 (차단 필터 적용 후 기준)"),
        fieldWithPath("data.boards[].hasMore").type(JsonFieldType.BOOLEAN)
            .description("미리보기 외에 매칭 피드가 더 있는지 여부 (totalCount > feeds 개수). \"더보기\" 표시 여부"),
        fieldWithPath("data.boards[].feeds[]").type(JsonFieldType.ARRAY)
            .description("미리보기 피드 목록 (작성 일시 내림차순, 같으면 피드 ID 내림차순, 최대 3개)")
    ) + feedItemFields("data.boards[].feeds[]")

    private fun pagedFields(): List<FieldDescriptor> = listOf(
        fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
        fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터"),
        fieldWithPath("data.feeds[]").type(JsonFieldType.ARRAY)
            .description("피드 목록 (작성 일시 내림차순, 같으면 피드 ID 내림차순). 마지막 페이지를 넘으면 빈 배열 []"),
        fieldWithPath("data.currentPage").type(JsonFieldType.NUMBER).description("현재 페이지 번호 (0부터 시작)"),
        fieldWithPath("data.totalPages").type(JsonFieldType.NUMBER)
            .description("전체 페이지 수 (차단 필터 적용 후 기준, 매칭 피드가 없으면 0)"),
        fieldWithPath("data.totalElements").type(JsonFieldType.NUMBER)
            .description("게시판 안 전체 매칭 피드 수 (차단 필터 적용 후 기준)"),
        fieldWithPath("data.size").type(JsonFieldType.NUMBER)
            .description("요청한 페이지 크기 (이번 페이지의 실제 항목 수는 feeds 배열 길이)"),
        fieldWithPath("data.hasNext").type(JsonFieldType.BOOLEAN).description("다음 페이지 존재 여부 (차단 필터 적용 후 기준)"),
        fieldWithPath("data.hasPrevious").type(JsonFieldType.BOOLEAN).description("이전 페이지 존재 여부")
    ) + feedItemFields("data.feeds[]")

    private fun feedItemFields(prefix: String): List<FieldDescriptor> = listOf(
        fieldWithPath("$prefix.id").type(JsonFieldType.NUMBER).description("피드 ID"),
        fieldWithPath("$prefix.boardId").type(JsonFieldType.NUMBER).description("피드가 속한 게시판 ID"),
        fieldWithPath("$prefix.boardTitle").type(JsonFieldType.STRING).description("피드가 속한 게시판 이름"),
        fieldWithPath("$prefix.title").type(JsonFieldType.STRING).description("피드 제목"),
        fieldWithPath("$prefix.content").type(JsonFieldType.STRING)
            .description("피드 본문 전체 (잘림 없음, 한 줄 표시는 클라이언트가 자름)"),
        fieldWithPath("$prefix.thumbnailUrl").type(JsonFieldType.STRING)
            .description("썸네일 이미지 URL (피드의 첫 번째로 업로드된 이미지, 이미지가 없으면 null)").optional(),
        fieldWithPath("$prefix.createdAt").type(JsonFieldType.STRING).description("작성 일시 (KST, ISO-8601)")
    )

    companion object {
        private val KEYWORD_PARAMETER: ParameterDescriptor = parameterWithName("keyword").description(
            "검색어 (필수). 앞뒤 공백을 지우고 소문자로 바꾼 뒤 1~50자(UTF-16 코드 유닛 기준)여야 함. " +
                "파라미터가 없으면 400 C105, 공백만 있으면 400 FD109, 50자를 넘으면 400 FD110"
        )
    }
}
