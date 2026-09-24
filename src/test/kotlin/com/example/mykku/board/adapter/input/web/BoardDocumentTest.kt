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
                parameterWithName("page").description("페이지 번호 (0부터 시작, 기본값: 0). 음수면 400 C103")
                    .optional(),
                parameterWithName("size").description("페이지 크기 (1~1000, 기본값: 20). 범위를 벗어나면 400 C104")
                    .optional()
            ),
            headerDescriptors = OPTIONAL_AUTH_HEADER_DESCRIPTOR
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
                            fieldWithPath("data.currentPage").type(JsonFieldType.NUMBER)
                                .description("현재 페이지 번호 (0부터 시작)"),
                            fieldWithPath("data.totalPages").type(JsonFieldType.NUMBER)
                                .description("전체 페이지 수 (차단 필터 적용 전 기준)"),
                            fieldWithPath("data.totalElements").type(JsonFieldType.NUMBER)
                                .description("게시판 전체 피드 수 (차단 필터 적용 전 기준)"),
                            fieldWithPath("data.size").type(JsonFieldType.NUMBER)
                                .description("요청한 페이지 크기 (실제 feeds 개수와 다를 수 있음)"),
                            fieldWithPath("data.hasNext").type(JsonFieldType.BOOLEAN)
                                .description("다음 페이지 존재 여부 (차단 필터 적용 전 기준). 다음 페이지 요청 여부는 이 값으로 판단"),
                            fieldWithPath("data.hasPrevious").type(JsonFieldType.BOOLEAN).description("이전 페이지 존재 여부"),
                            fieldWithPath("data.feeds").type(JsonFieldType.ARRAY)
                                .description("피드 목록 (작성 일시 내림차순). 로그인 요청은 차단 필터가 적용되어 size보다 적게, 혹은 0개가 올 수 있음"),
                            fieldWithPath("data.feeds[].id").type(JsonFieldType.NUMBER).description("피드 ID"),
                            fieldWithPath("data.feeds[].author").type(JsonFieldType.OBJECT)
                                .description("작성자 정보 (작성자가 탈퇴한 경우 null이며 하위 필드도 없음)").optional(),
                            fieldWithPath("data.feeds[].author.memberId").type(JsonFieldType.STRING)
                                .description("작성자 회원 아이디 (사용자가 설정한 영문·숫자 문자열, 최대 16자. 내부 PK가 아니며 회원이 변경할 수 있음)"),
                            fieldWithPath("data.feeds[].author.nickname").type(JsonFieldType.STRING)
                                .description("작성자 닉네임"),
                            fieldWithPath("data.feeds[].author.profileImage").type(JsonFieldType.STRING)
                                .description("작성자 프로필 이미지 URL (프로필 이미지가 없으면 빈 문자열 \"\")"),
                            fieldWithPath("data.feeds[].author.role").type(JsonFieldType.OBJECT)
                                .description("작성자 대표 칭호 (대표 칭호가 없으면 null)").optional(),
                            fieldWithPath("data.feeds[].author.role.id").type(JsonFieldType.NUMBER)
                                .description("칭호 ID"),
                            fieldWithPath("data.feeds[].author.role.name").type(JsonFieldType.STRING)
                                .description("칭호 이름"),
                            fieldWithPath("data.feeds[].author.role.description").type(JsonFieldType.STRING)
                                .description("칭호 설명 (설명이 없으면 null)").optional(),
                            fieldWithPath("data.feeds[].board").type(JsonFieldType.STRING).description("게시판 이름"),
                            fieldWithPath("data.feeds[].title").type(JsonFieldType.STRING).description("피드 제목"),
                            fieldWithPath("data.feeds[].content").type(JsonFieldType.STRING)
                                .description("피드 본문 전체 (잘림 없음)"),
                            fieldWithPath("data.feeds[].images").type(JsonFieldType.ARRAY)
                                .description("피드 이미지 목록 (이미지가 없으면 빈 배열 [])"),
                            fieldWithPath("data.feeds[].images[].id").type(JsonFieldType.NUMBER).description("이미지 ID"),
                            fieldWithPath("data.feeds[].images[].url").type(JsonFieldType.STRING)
                                .description("이미지 URL"),
                            fieldWithPath("data.feeds[].images[].width").type(JsonFieldType.NUMBER)
                                .description("이미지 가로 크기 (px)"),
                            fieldWithPath("data.feeds[].images[].height").type(JsonFieldType.NUMBER)
                                .description("이미지 세로 크기 (px)"),
                            fieldWithPath("data.feeds[].tags").type(JsonFieldType.ARRAY)
                                .description("피드 태그 목록 (태그가 없으면 빈 배열 [])"),
                            fieldWithPath("data.feeds[].tags[].title").type(JsonFieldType.STRING).description("태그 제목"),
                            fieldWithPath("data.feeds[].tags[].isContest").type(JsonFieldType.BOOLEAN)
                                .description(
                                    "콘테스트 태그 여부 (true: 태그 제목이 콘테스트에 등록된 태그와 일치, " +
                                        "콘테스트 진행·종료 여부와 무관 / false: 일반 태그)"
                                ),
                            fieldWithPath("data.feeds[].likeCount").type(JsonFieldType.NUMBER).description("좋아요 수"),
                            fieldWithPath("data.feeds[].commentCount").type(JsonFieldType.NUMBER)
                                .description("댓글 수 (대댓글 포함 전체 댓글 수)"),
                            fieldWithPath("data.feeds[].isLiked").type(JsonFieldType.BOOLEAN)
                                .description("요청자의 좋아요 여부 (true: 좋아요 누름, false: 누르지 않음 또는 비로그인 요청)"),
                            fieldWithPath("data.feeds[].createdAt").type(JsonFieldType.STRING)
                                .description("작성 일시 (KST, ISO-8601)"),
                            fieldWithPath("data.feeds[].comment").type(JsonFieldType.OBJECT)
                                .description("첫 댓글 미리보기. 가장 먼저 작성된 최상위 댓글(대댓글 제외) 기준이며, 댓글이 없어도 객체는 항상 존재함"),
                            fieldWithPath("data.feeds[].comment.profileImage").type(JsonFieldType.STRING)
                                .description("첫 댓글 작성자 프로필 이미지 URL (미리보기가 비어 있으면 null, 작성자의 프로필 이미지가 없으면 빈 문자열 \"\")")
                                .optional(),
                            fieldWithPath("data.feeds[].comment.content").type(JsonFieldType.STRING)
                                .description(
                                    "첫 댓글 내용 (댓글이 없거나 첫 댓글 작성자가 탈퇴한 경우 빈 문자열 \"\". " +
                                        "이때 commentCount가 1 이상일 수 있음)"
                                )
                        )
                )
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
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
            ),
            headerDescriptors = OPTIONAL_AUTH_HEADER_DESCRIPTOR
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
                            fieldWithPath("data.feeds").type(JsonFieldType.ARRAY)
                                .description(
                                    "인기 피드 목록 (rank 오름차순, 최대 3개). 로그인 요청은 차단 필터 적용 후 " +
                                        "3개보다 적을 수 있고, 대상 피드가 없으면 빈 배열 []"
                                ),
                            fieldWithPath("data.feeds[].id").type(JsonFieldType.NUMBER).description("피드 ID"),
                            fieldWithPath("data.feeds[].rank").type(JsonFieldType.NUMBER)
                                .description("순위 (1부터 시작, 차단 필터 적용 후 연속 번호로 다시 매김)"),
                            fieldWithPath("data.feeds[].title").type(JsonFieldType.STRING).description("피드 제목"),
                            fieldWithPath("data.feeds[].content").type(JsonFieldType.STRING)
                                .description("피드 본문 전체 (잘림 없음)")
                        )
                )
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .`when`()
                .get("/api/v1/boards/{boardId}/feeds/popular", boardId)
                .then()
                .statusCode(200)
        }
    }
}
