package com.example.mykku.member.adapter.input.web

import com.example.mykku.BaseDocumentTest
import com.example.mykku.docs.ApiRequestConfig
import com.example.mykku.feed.application.dto.AuthorResult
import com.example.mykku.feed.application.dto.CommentPreviewResult
import com.example.mykku.feed.application.dto.FeedImageResult
import com.example.mykku.feed.application.dto.FeedResult
import com.example.mykku.feed.application.dto.PagedFeedsResult
import com.example.mykku.feed.application.dto.TagResult
import com.example.mykku.role.application.dto.RoleResult
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

class MemberFeedDocumentTest : BaseDocumentTest() {

    @Nested
    @DisplayName("내가 쓴 피드 목록 조회")
    inner class GetMyFeeds {

        private val apiConfig = ApiRequestConfig(
            queryParameters = listOf(
                parameterWithName("page").description("페이지 번호 (0부터 시작, 기본값: 0)").optional(),
                parameterWithName("size").description("페이지 크기 (기본값: 20)").optional()
            ),
            headerDescriptors = AUTH_HEADER_DESCRIPTOR
        )

        @Test
        fun `성공`() {
            val result = PagedFeedsResult(
                feeds = listOf(
                    FeedResult(
                        id = 1L,
                        author = AuthorResult(
                            memberId = "testmemberid",
                            nickname = "testuser",
                            profileImage = "https://example.com/profile.jpg",
                            role = RoleResult(1L, "아미", "BTS 팬클럽")
                        ),
                        board = "자유게시판",
                        createdAt = LocalDateTime.now(),
                        title = "내가 쓴 피드",
                        content = "피드 내용입니다.",
                        images = listOf(
                            FeedImageResult(1L, "https://example.com/feed-image.jpg", 1080, 1080)
                        ),
                        tags = listOf(
                            TagResult("일상", false)
                        ),
                        likeCount = 10,
                        isLiked = false,
                        commentCount = 3,
                        comment = CommentPreviewResult(
                            profileImage = "https://example.com/commenter.jpg",
                            content = "좋은 글이네요!"
                        )
                    ),
                    FeedResult(
                        id = 2L,
                        author = AuthorResult(
                            memberId = "testmemberid",
                            nickname = "testuser",
                            profileImage = "https://example.com/profile.jpg",
                            role = null
                        ),
                        board = "콘테스트게시판",
                        createdAt = LocalDateTime.now().minusDays(1),
                        title = "두 번째 피드",
                        content = "두 번째 피드 내용입니다.",
                        images = emptyList(),
                        tags = listOf(
                            TagResult("콘테스트태그", true)
                        ),
                        likeCount = 5,
                        isLiked = true,
                        commentCount = 1,
                        comment = CommentPreviewResult(
                            profileImage = null,
                            content = "멋져요!"
                        )
                    )
                ),
                currentPage = 0,
                totalPages = 1,
                totalElements = 2,
                size = 20,
                hasNext = false,
                hasPrevious = false
            )

            whenever(getMyFeedsUseCase.execute(any())).thenReturn(result)

            val documentFilter = document("member/feeds", 200)
                .request(request().applyConfig(apiConfig))
                .response(
                    response()
                        .responseBodyField(
                            fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                            fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터"),
                            fieldWithPath("data.feeds[]").type(JsonFieldType.ARRAY).description("피드 목록"),
                            fieldWithPath("data.feeds[].id").type(JsonFieldType.NUMBER).description("피드 ID"),
                            fieldWithPath("data.feeds[].author").type(JsonFieldType.OBJECT).description("작성자 정보").optional(),
                            fieldWithPath("data.feeds[].author.memberId").type(JsonFieldType.STRING).description("작성자 회원 ID").optional(),
                            fieldWithPath("data.feeds[].author.nickname").type(JsonFieldType.STRING).description("작성자 닉네임").optional(),
                            fieldWithPath("data.feeds[].author.profileImage").type(JsonFieldType.STRING).description("작성자 프로필 이미지").optional(),
                            fieldWithPath("data.feeds[].author.role").type(JsonFieldType.OBJECT).description("작성자 역할").optional(),
                            fieldWithPath("data.feeds[].author.role.id").type(JsonFieldType.NUMBER).description("역할 ID").optional(),
                            fieldWithPath("data.feeds[].author.role.name").type(JsonFieldType.STRING).description("역할 이름").optional(),
                            fieldWithPath("data.feeds[].author.role.description").type(JsonFieldType.STRING).description("역할 설명").optional(),
                            fieldWithPath("data.feeds[].board").type(JsonFieldType.STRING).description("게시판 이름"),
                            fieldWithPath("data.feeds[].createdAt").type(JsonFieldType.STRING).description("생성일시"),
                            fieldWithPath("data.feeds[].title").type(JsonFieldType.STRING).description("피드 제목"),
                            fieldWithPath("data.feeds[].content").type(JsonFieldType.STRING).description("피드 내용"),
                            fieldWithPath("data.feeds[].images[]").type(JsonFieldType.ARRAY).description("이미지 목록"),
                            fieldWithPath("data.feeds[].images[].id").type(JsonFieldType.NUMBER).description("이미지 ID").optional(),
                            fieldWithPath("data.feeds[].images[].url").type(JsonFieldType.STRING).description("이미지 URL").optional(),
                            fieldWithPath("data.feeds[].images[].width").type(JsonFieldType.NUMBER).description("이미지 너비").optional(),
                            fieldWithPath("data.feeds[].images[].height").type(JsonFieldType.NUMBER).description("이미지 높이").optional(),
                            fieldWithPath("data.feeds[].tags[]").type(JsonFieldType.ARRAY).description("태그 목록"),
                            fieldWithPath("data.feeds[].tags[].title").type(JsonFieldType.STRING).description("태그 제목").optional(),
                            fieldWithPath("data.feeds[].tags[].isContest").type(JsonFieldType.BOOLEAN).description("콘테스트 태그 여부").optional(),
                            fieldWithPath("data.feeds[].likeCount").type(JsonFieldType.NUMBER).description("좋아요 수"),
                            fieldWithPath("data.feeds[].isLiked").type(JsonFieldType.BOOLEAN).description("좋아요 여부"),
                            fieldWithPath("data.feeds[].commentCount").type(JsonFieldType.NUMBER).description("댓글 수"),
                            fieldWithPath("data.feeds[].comment").type(JsonFieldType.OBJECT).description("첫 번째 댓글 미리보기"),
                            fieldWithPath("data.feeds[].comment.profileImage").type(JsonFieldType.STRING).description("댓글 작성자 프로필").optional(),
                            fieldWithPath("data.feeds[].comment.content").type(JsonFieldType.STRING).description("댓글 내용"),
                            fieldWithPath("data.currentPage").type(JsonFieldType.NUMBER).description("현재 페이지"),
                            fieldWithPath("data.totalPages").type(JsonFieldType.NUMBER).description("전체 페이지 수"),
                            fieldWithPath("data.totalElements").type(JsonFieldType.NUMBER).description("전체 항목 수"),
                            fieldWithPath("data.size").type(JsonFieldType.NUMBER).description("페이지 크기"),
                            fieldWithPath("data.hasNext").type(JsonFieldType.BOOLEAN).description("다음 페이지 존재 여부"),
                            fieldWithPath("data.hasPrevious").type(JsonFieldType.BOOLEAN).description("이전 페이지 존재 여부")
                        )
                )
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .param("page", 0)
                .param("size", 20)
                .`when`()
                .get("/api/v1/members/me/feeds")
                .then()
                .statusCode(200)
        }

        @Test
        fun `빈 목록`() {
            val result = PagedFeedsResult(
                feeds = emptyList(),
                currentPage = 0,
                totalPages = 0,
                totalElements = 0,
                size = 20,
                hasNext = false,
                hasPrevious = false
            )

            whenever(getMyFeedsUseCase.execute(any())).thenReturn(result)

            val documentFilter = document("member/feeds", "empty")
                .request(request().applyConfig(apiConfig))
                .response(
                    response()
                        .responseBodyField(
                            fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                            fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터"),
                            fieldWithPath("data.feeds[]").type(JsonFieldType.ARRAY).description("빈 피드 목록"),
                            fieldWithPath("data.currentPage").type(JsonFieldType.NUMBER).description("현재 페이지"),
                            fieldWithPath("data.totalPages").type(JsonFieldType.NUMBER).description("전체 페이지 수"),
                            fieldWithPath("data.totalElements").type(JsonFieldType.NUMBER).description("전체 항목 수"),
                            fieldWithPath("data.size").type(JsonFieldType.NUMBER).description("페이지 크기"),
                            fieldWithPath("data.hasNext").type(JsonFieldType.BOOLEAN).description("다음 페이지 존재 여부"),
                            fieldWithPath("data.hasPrevious").type(JsonFieldType.BOOLEAN).description("이전 페이지 존재 여부")
                        )
                )
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .param("page", 0)
                .param("size", 20)
                .`when`()
                .get("/api/v1/members/me/feeds")
                .then()
                .statusCode(200)
        }
    }
}
