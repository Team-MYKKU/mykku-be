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
                parameterWithName("page").description("페이지 번호 (0부터 시작, 0 이상 정수, 기본값: 0)").optional(),
                parameterWithName("size").description("페이지 크기 (1~1000 정수, 기본값: 20)").optional()
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
                        commentCount = 0,
                        comment = CommentPreviewResult(
                            profileImage = null,
                            content = ""
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
                            fieldWithPath("data.feeds[]").type(JsonFieldType.ARRAY)
                                .description("피드 목록 (작성 일시 내림차순, 모든 게시판 포함)"),
                            fieldWithPath("data.feeds[].id").type(JsonFieldType.NUMBER).description("피드 ID"),
                            fieldWithPath("data.feeds[].author").type(JsonFieldType.OBJECT)
                                .description("작성자 정보 (이 API에서는 항상 로그인한 본인)"),
                            fieldWithPath("data.feeds[].author.memberId").type(JsonFieldType.STRING)
                                .description("작성자 회원 아이디 (사용자가 설정한 영문·숫자 문자열, 최대 16자. 내부 PK가 아니며 회원이 변경할 수 있음)"),
                            fieldWithPath("data.feeds[].author.nickname").type(JsonFieldType.STRING)
                                .description("작성자 닉네임"),
                            fieldWithPath("data.feeds[].author.profileImage").type(JsonFieldType.STRING)
                                .description("작성자 프로필 이미지 URL (null이 아니며, 프로필 이미지가 없으면 빈 문자열 \"\")"),
                            fieldWithPath("data.feeds[].author.role").type(JsonFieldType.OBJECT)
                                .description("작성자 대표 칭호 (대표 칭호가 없으면 null)").optional(),
                            fieldWithPath("data.feeds[].author.role.id").type(JsonFieldType.NUMBER)
                                .description("칭호 ID (보유 칭호 ID가 아닌 칭호 자체의 ID, role이 있으면 항상 존재)"),
                            fieldWithPath("data.feeds[].author.role.name").type(JsonFieldType.STRING)
                                .description("칭호 이름 (role이 있으면 항상 존재)"),
                            fieldWithPath("data.feeds[].author.role.description").type(JsonFieldType.STRING)
                                .description("칭호 설명 (설명이 없으면 null)").optional(),
                            fieldWithPath("data.feeds[].board").type(JsonFieldType.STRING).description("게시판 이름"),
                            fieldWithPath("data.feeds[].createdAt").type(JsonFieldType.STRING)
                                .description("작성 일시 (KST, ISO-8601)"),
                            fieldWithPath("data.feeds[].title").type(JsonFieldType.STRING).description("피드 제목"),
                            fieldWithPath("data.feeds[].content").type(JsonFieldType.STRING).description("피드 내용"),
                            fieldWithPath("data.feeds[].images[]").type(JsonFieldType.ARRAY)
                                .description("피드 이미지 목록 (이미지가 없으면 빈 배열 [])"),
                            fieldWithPath("data.feeds[].images[].id").type(JsonFieldType.NUMBER).description("이미지 ID"),
                            fieldWithPath("data.feeds[].images[].url").type(JsonFieldType.STRING)
                                .description("이미지 URL"),
                            fieldWithPath("data.feeds[].images[].width").type(JsonFieldType.NUMBER)
                                .description("이미지 가로 크기 (px)"),
                            fieldWithPath("data.feeds[].images[].height").type(JsonFieldType.NUMBER)
                                .description("이미지 세로 크기 (px)"),
                            fieldWithPath("data.feeds[].tags[]").type(JsonFieldType.ARRAY)
                                .description("피드 태그 목록 (태그가 없으면 빈 배열 [])"),
                            fieldWithPath("data.feeds[].tags[].title").type(JsonFieldType.STRING).description("태그 제목"),
                            fieldWithPath("data.feeds[].tags[].isContest").type(JsonFieldType.BOOLEAN)
                                .description(
                                    "콘테스트 태그 여부 (true: 태그 제목이 콘테스트에 등록된 태그와 일치, " +
                                        "콘테스트 진행·종료 여부와 무관 / false: 일반 태그)"
                                ),
                            fieldWithPath("data.feeds[].likeCount").type(JsonFieldType.NUMBER).description("좋아요 수"),
                            fieldWithPath("data.feeds[].isLiked").type(JsonFieldType.BOOLEAN)
                                .description("로그인한 회원(본인)의 좋아요 여부 (true: 좋아요 누름, false: 누르지 않음)"),
                            fieldWithPath("data.feeds[].commentCount").type(JsonFieldType.NUMBER)
                                .description("댓글 수 (대댓글 포함 전체 댓글 수)"),
                            fieldWithPath("data.feeds[].comment").type(JsonFieldType.OBJECT)
                                .description("첫 댓글 미리보기. 가장 먼저 작성된 최상위 댓글(대댓글 제외) 기준이며, 댓글이 없어도 객체는 항상 존재함"),
                            fieldWithPath("data.feeds[].comment.profileImage").type(JsonFieldType.STRING)
                                .description("첫 댓글 작성자 프로필 이미지 URL (미리보기가 비어 있으면 null, 작성자의 프로필 이미지가 없으면 빈 문자열 \"\")")
                                .optional(),
                            fieldWithPath("data.feeds[].comment.content").type(JsonFieldType.STRING)
                                .description(
                                    "첫 댓글 내용 (댓글이 없거나 첫 댓글 작성자가 탈퇴한 경우 빈 문자열 \"\". " +
                                        "이때 commentCount가 1 이상일 수 있음)"
                                ),
                            fieldWithPath("data.currentPage").type(JsonFieldType.NUMBER)
                                .description("현재 페이지 번호 (0부터 시작)"),
                            fieldWithPath("data.totalPages").type(JsonFieldType.NUMBER)
                                .description("전체 페이지 수 (작성한 피드가 없으면 0)"),
                            fieldWithPath("data.totalElements").type(JsonFieldType.NUMBER).description("전체 피드 수"),
                            fieldWithPath("data.size").type(JsonFieldType.NUMBER)
                                .description("요청한 페이지 크기 (이번 페이지의 실제 항목 수는 feeds 배열 길이)"),
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
                            fieldWithPath("data.feeds[]").type(JsonFieldType.ARRAY).description("빈 피드 목록 []"),
                            fieldWithPath("data.currentPage").type(JsonFieldType.NUMBER)
                                .description("현재 페이지 번호 (0부터 시작)"),
                            fieldWithPath("data.totalPages").type(JsonFieldType.NUMBER)
                                .description("전체 페이지 수 (작성한 피드가 없으면 0)"),
                            fieldWithPath("data.totalElements").type(JsonFieldType.NUMBER).description("전체 피드 수"),
                            fieldWithPath("data.size").type(JsonFieldType.NUMBER)
                                .description("요청한 페이지 크기 (이번 페이지의 실제 항목 수는 feeds 배열 길이)"),
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
