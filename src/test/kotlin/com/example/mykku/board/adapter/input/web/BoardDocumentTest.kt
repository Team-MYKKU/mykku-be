package com.example.mykku.board.adapter.input.web

import com.example.mykku.BaseDocumentTest
import com.example.mykku.board.application.dto.BoardResult
import com.example.mykku.docs.ApiRequestConfig
import com.example.mykku.feed.adapter.input.web.dto.AuthorResponse
import com.example.mykku.feed.adapter.input.web.dto.CommentPreviewResponse
import com.example.mykku.feed.adapter.input.web.dto.FeedImageResponse
import com.example.mykku.feed.adapter.input.web.dto.FeedResponse
import com.example.mykku.feed.adapter.input.web.dto.PagedFeedsResponse
import com.example.mykku.feed.adapter.input.web.dto.PopularFeedResponse
import com.example.mykku.feed.adapter.input.web.dto.PopularFeedsResponse
import com.example.mykku.feed.adapter.input.web.dto.TagResponse
import com.example.mykku.feed.application.dto.AuthorResult
import com.example.mykku.feed.application.dto.CommentPreviewResult
import com.example.mykku.feed.application.dto.FeedImageResult
import com.example.mykku.feed.application.dto.FeedResult
import com.example.mykku.feed.application.dto.PagedFeedsResult
import com.example.mykku.feed.application.dto.PopularFeedResult
import com.example.mykku.feed.application.dto.PopularFeedsResult
import com.example.mykku.feed.application.dto.TagResult
import com.example.mykku.role.application.dto.RoleResult
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

class BoardDocumentTest : BaseDocumentTest() {

    @Nested
    @DisplayName("게시판 목록 조회")
    inner class GetBoards {

        private val apiConfig = ApiRequestConfig()

        @Test
        fun `성공`() {
            val boardResults = listOf(
                BoardResult(id = 1L, title = "자유게시판", logo = "https://example.com/logo1.png"),
                BoardResult(id = 2L, title = "정보게시판", logo = "https://example.com/logo2.png")
            )

            `when`(listBoardsUseCase.listBoards()).thenReturn(boardResults)

            val documentFilter = document("board/list", 200)
                .request(request().applyConfig(apiConfig))
                .response(
                    response()
                        .responseBodyField(
                            fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                            fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터"),
                            fieldWithPath("data.boards").type(JsonFieldType.ARRAY).description("게시판 목록"),
                            fieldWithPath("data.boards[].id").type(JsonFieldType.NUMBER).description("게시판 ID"),
                            fieldWithPath("data.boards[].title").type(JsonFieldType.STRING).description("게시판 제목"),
                            fieldWithPath("data.boards[].logo").type(JsonFieldType.STRING).description("게시판 로고 URL")
                        )
                )
                .build()

            given(documentFilter)
                .contentType(ContentType.JSON)
                .`when`()
                .get("/api/v1/boards")
                .then()
                .statusCode(200)
        }
    }

    @Nested
    @DisplayName("보드별 피드 목록 조회")
    inner class GetFeedsByBoard {

        private val apiConfig = ApiRequestConfig(
            pathParameters = listOf(
                parameterWithName("boardId").description("조회할 게시판의 ID")
            ),
            queryParameters = listOf(
                parameterWithName("page").description("페이지 번호 (0부터 시작, 기본값: 0)").optional(),
                parameterWithName("size").description("페이지 크기 (기본값: 20)").optional()
            )
        )

        @Test
        fun `성공`() {
            val boardId = 1L
            val feedsResult = PagedFeedsResult(
                feeds = listOf(
                    FeedResult(
                        id = 1L,
                        author = AuthorResult(
                            memberId = "member1",
                            nickname = "닉네임1",
                            profileImage = "https://example.com/profile1.jpg",
                            role = RoleResult(id = 1L, name = "일반 덕후", description = "일반 덕후 칭호")
                        ),
                        board = "자유게시판",
                        createdAt = LocalDateTime.of(2024, 1, 1, 12, 0),
                        title = "자유게시판 피드 제목",
                        content = "자유게시판 피드 내용입니다.",
                        images = listOf(
                            FeedImageResult(
                                id = 1L,
                                url = "https://example.com/image1.jpg",
                                width = 1920,
                                height = 1080
                            )
                        ),
                        tags = listOf(
                            TagResult(title = "자유", isContest = false)
                        ),
                        likeCount = 15,
                        isLiked = true,
                        commentCount = 3,
                        comment = CommentPreviewResult(
                            profileImage = "https://example.com/commenter1.jpg",
                            content = "좋은 글입니다."
                        )
                    )
                ),
                currentPage = 0,
                totalPages = 1,
                totalElements = 1,
                size = 20,
                hasNext = false,
                hasPrevious = false
            )

            `when`(listFeedsUseCase.execute(any())).thenReturn(feedsResult)

            val documentFilter = document("board/feeds", 200)
                .request(request().applyConfig(apiConfig))
                .response(
                    response()
                        .responseBodyField(
                            fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                            fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터"),
                            fieldWithPath("data.currentPage").type(JsonFieldType.NUMBER).description("현재 페이지 번호"),
                            fieldWithPath("data.totalPages").type(JsonFieldType.NUMBER).description("전체 페이지 수"),
                            fieldWithPath("data.totalElements").type(JsonFieldType.NUMBER).description("전체 요소 수"),
                            fieldWithPath("data.size").type(JsonFieldType.NUMBER).description("페이지 크기"),
                            fieldWithPath("data.hasNext").type(JsonFieldType.BOOLEAN).description("다음 페이지 존재 여부"),
                            fieldWithPath("data.hasPrevious").type(JsonFieldType.BOOLEAN).description("이전 페이지 존재 여부"),
                            fieldWithPath("data.feeds").type(JsonFieldType.ARRAY).description("피드 목록"),
                            fieldWithPath("data.feeds[].id").type(JsonFieldType.NUMBER).description("피드 ID"),
                            fieldWithPath("data.feeds[].author").type(JsonFieldType.OBJECT).description("작성자 정보"),
                            fieldWithPath("data.feeds[].author.memberId").type(JsonFieldType.STRING)
                                .description("작성자 ID"),
                            fieldWithPath("data.feeds[].author.nickname").type(JsonFieldType.STRING)
                                .description("작성자 닉네임"),
                            fieldWithPath("data.feeds[].author.profileImage").type(JsonFieldType.STRING)
                                .description("작성자 프로필 이미지 URL").optional(),
                            fieldWithPath("data.feeds[].author.role").type(JsonFieldType.OBJECT).description("작성자 칭호").optional(),
                            fieldWithPath("data.feeds[].author.role.id").type(JsonFieldType.NUMBER).description("칭호 ID"),
                            fieldWithPath("data.feeds[].author.role.name").type(JsonFieldType.STRING).description("칭호 이름"),
                            fieldWithPath("data.feeds[].author.role.description").type(JsonFieldType.STRING).description("칭호 설명").optional(),
                            fieldWithPath("data.feeds[].board").type(JsonFieldType.STRING).description("게시판 이름"),
                            fieldWithPath("data.feeds[].title").type(JsonFieldType.STRING).description("피드 제목"),
                            fieldWithPath("data.feeds[].content").type(JsonFieldType.STRING).description("피드 내용"),
                            fieldWithPath("data.feeds[].images").type(JsonFieldType.ARRAY).description("피드 이미지 목록"),
                            fieldWithPath("data.feeds[].images[].id").type(JsonFieldType.NUMBER).description("이미지 ID")
                                .optional(),
                            fieldWithPath("data.feeds[].images[].url").type(JsonFieldType.STRING).description("이미지 URL")
                                .optional(),
                            fieldWithPath("data.feeds[].images[].width").type(JsonFieldType.NUMBER)
                                .description("이미지 가로 크기 (픽셀)").optional(),
                            fieldWithPath("data.feeds[].images[].height").type(JsonFieldType.NUMBER)
                                .description("이미지 세로 크기 (픽셀)").optional(),
                            fieldWithPath("data.feeds[].tags").type(JsonFieldType.ARRAY).description("피드 태그 목록"),
                            fieldWithPath("data.feeds[].tags[].title").type(JsonFieldType.STRING).description("태그 제목"),
                            fieldWithPath("data.feeds[].tags[].isContest").type(JsonFieldType.BOOLEAN)
                                .description("콘테스트 태그 여부"),
                            fieldWithPath("data.feeds[].likeCount").type(JsonFieldType.NUMBER).description("좋아요 수"),
                            fieldWithPath("data.feeds[].commentCount").type(JsonFieldType.NUMBER).description("댓글 수"),
                            fieldWithPath("data.feeds[].isLiked").type(JsonFieldType.BOOLEAN)
                                .description("현재 사용자의 좋아요 여부"),
                            fieldWithPath("data.feeds[].createdAt").type(JsonFieldType.STRING).description("작성 일시"),
                            fieldWithPath("data.feeds[].comment").type(JsonFieldType.OBJECT).description("첫 댓글 미리보기"),
                            fieldWithPath("data.feeds[].comment.profileImage").type(JsonFieldType.STRING)
                                .description("댓글 작성자 프로필 이미지"),
                            fieldWithPath("data.feeds[].comment.content").type(JsonFieldType.STRING)
                                .description("댓글 내용")
                        )
                )
                .build()

            given(documentFilter)
                .contentType(ContentType.JSON)
                .`when`()
                .get("/api/v1/boards/{boardId}/feeds", boardId)
                .then()
                .statusCode(200)
        }
    }

    @Nested
    @DisplayName("보드별 인기 피드 목록 조회")
    inner class GetPopularFeedsByBoard {

        private val apiConfig = ApiRequestConfig(
            pathParameters = listOf(
                parameterWithName("boardId").description("조회할 게시판의 ID")
            )
        )

        @Test
        fun `성공`() {
            val boardId = 1L
            val result = PopularFeedsResult(
                feeds = listOf(
                    PopularFeedResult(id = 1L, rank = 1, title = "인기 피드 1", content = "인기 피드 내용 1"),
                    PopularFeedResult(id = 2L, rank = 2, title = "인기 피드 2", content = "인기 피드 내용 2"),
                    PopularFeedResult(id = 3L, rank = 3, title = "인기 피드 3", content = "인기 피드 내용 3")
                )
            )

            `when`(getPopularFeedsUseCase.execute(any())).thenReturn(result)

            val documentFilter = document("board/popular-feeds", 200)
                .request(request().applyConfig(apiConfig))
                .response(
                    response()
                        .responseBodyField(
                            fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                            fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터"),
                            fieldWithPath("data.feeds").type(JsonFieldType.ARRAY).description("인기 피드 목록 (최대 3개)"),
                            fieldWithPath("data.feeds[].id").type(JsonFieldType.NUMBER).description("피드 ID"),
                            fieldWithPath("data.feeds[].rank").type(JsonFieldType.NUMBER).description("순위"),
                            fieldWithPath("data.feeds[].title").type(JsonFieldType.STRING).description("피드 제목"),
                            fieldWithPath("data.feeds[].content").type(JsonFieldType.STRING).description("피드 내용")
                        )
                )
                .build()

            given(documentFilter)
                .contentType(ContentType.JSON)
                .`when`()
                .get("/api/v1/boards/{boardId}/feeds/popular", boardId)
                .then()
                .statusCode(200)
        }
    }
}
