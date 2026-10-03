package com.example.mykku.feed.adapter.input.web

import com.example.mykku.BaseDocumentTest
import com.example.mykku.comment.CommentDocumentFields
import com.example.mykku.comment.CommentFixtures
import com.example.mykku.docs.ApiRequestConfig
import com.example.mykku.docs.RestDocumentationResponse
import com.example.mykku.feed.adapter.input.web.dto.CreateFeedCommentRequest
import com.example.mykku.feed.adapter.input.web.dto.UpdateFeedCommentRequest
import com.example.mykku.feed.exception.FeedErrorCode
import com.example.mykku.feed.exception.FeedException
import com.example.mykku.member.exception.MemberErrorCode
import com.example.mykku.member.exception.MemberException
import io.restassured.http.ContentType
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import java.time.LocalDateTime

class FeedCommentDocumentTest : BaseDocumentTest() {

    @Nested
    @DisplayName("피드 댓글 생성")
    inner class CreateComment {

        private val apiConfig = ApiRequestConfig(
            pathParameters = listOf(
                parameterWithName("feedId").description("댓글을 작성할 피드 ID")
            ),
            requestBodyFields = listOf(
                fieldWithPath("content").type(JsonFieldType.STRING)
                    .description("댓글 내용 (최대 1000자, 초과 시 400 FD105. 빈 문자열도 허용)"),
                fieldWithPath("parentCommentId").type(JsonFieldType.NUMBER)
                    .description("부모 댓글 ID. 일반 댓글은 생략하거나 null, 답글은 같은 피드의 최상위 댓글 ID")
                    .optional()
            ),
            headerDescriptors = AUTH_HEADER_DESCRIPTOR
        )

        @Test
        fun `성공`() {
            val feedId = 1L
            val request = CreateFeedCommentRequest(
                content = "좋은 피드네요!",
                parentCommentId = null
            )
            val result = CommentFixtures.comment(
                id = 1L,
                content = "좋은 피드네요!",
                author = CommentFixtures.author(memberId = testMember.memberId, nickname = "testuser")
            )

            `when`(createFeedCommentUseCase.execute(any(), any())).thenReturn(result)

            val documentFilter = document("feed-comment/create", 200)
                .request(request().applyConfig(apiConfig))
                .response(
                    response().responseBodyField(
                        *CommentDocumentFields.singleFields(
                            "생성된 댓글 정보 (피드 댓글 목록 아이템과 같은 형식)",
                            "답글 목록 (새 댓글은 항상 빈 배열)"
                        )
                    )
                )
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
                .`when`()
                .post("/api/v1/feeds/{feedId}/comments", feedId)
                .then()
                .statusCode(200)
        }

        @Test
        fun `피드를 찾을 수 없음`() {
            val feedId = 999L
            val request = CreateFeedCommentRequest(
                content = "댓글 내용",
                parentCommentId = null
            )

            `when`(createFeedCommentUseCase.execute(any(), any()))
                .thenThrow(FeedException(FeedErrorCode.FEED_NOT_FOUND))

            val documentFilter = document("feed-comment/create", "FEED_NOT_FOUND")
                .request(request().applyConfig(apiConfig))
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
                .`when`()
                .post("/api/v1/feeds/{feedId}/comments", feedId)
                .then()
                .statusCode(404)
        }

        @Test
        fun `프로필 설정을 완료하지 않은 회원`() {
            val feedId = 1L
            val request = CreateFeedCommentRequest(
                content = "댓글 내용",
                parentCommentId = null
            )

            `when`(createFeedCommentUseCase.execute(any(), any()))
                .thenThrow(MemberException(MemberErrorCode.PROFILE_NOT_COMPLETED))

            val documentFilter = document("feed-comment/create", "PROFILE_NOT_COMPLETED")
                .request(request().applyConfig(apiConfig))
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
                .`when`()
                .post("/api/v1/feeds/{feedId}/comments", feedId)
                .then()
                .statusCode(403)
        }

        @Test
        fun `댓글 내용이 1000자를 초과함`() {
            val feedId = 1L
            val request = CreateFeedCommentRequest(
                content = "a".repeat(1001),
                parentCommentId = null
            )

            `when`(createFeedCommentUseCase.execute(any(), any()))
                .thenThrow(FeedException(FeedErrorCode.FEED_COMMENT_CONTENT_TOO_LONG))

            val documentFilter = document("feed-comment/create", "FEED_COMMENT_CONTENT_TOO_LONG")
                .request(request().applyConfig(apiConfig))
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
                .`when`()
                .post("/api/v1/feeds/{feedId}/comments", feedId)
                .then()
                .statusCode(400)
        }
    }

    @Nested
    @DisplayName("피드 답글 생성")
    inner class CreateReply {

        private val apiConfig = ApiRequestConfig(
            pathParameters = listOf(
                parameterWithName("feedId").description("답글을 작성할 피드 ID (부모 댓글이 속한 피드)")
            ),
            requestBodyFields = listOf(
                fieldWithPath("content").type(JsonFieldType.STRING)
                    .description("답글 내용 (최대 1000자, 초과 시 400 FD105. 빈 문자열도 허용)"),
                fieldWithPath("parentCommentId").type(JsonFieldType.NUMBER)
                    .description("부모 댓글 ID (같은 피드의 최상위 댓글 ID)")
            ),
            headerDescriptors = AUTH_HEADER_DESCRIPTOR
        )

        @Test
        fun `성공`() {
            val feedId = 1L
            val request = CreateFeedCommentRequest(
                content = "저도 동감합니다!",
                parentCommentId = 10L
            )
            val result = CommentFixtures.comment(
                id = 2L,
                content = "저도 동감합니다!",
                author = CommentFixtures.author(memberId = testMember.memberId, nickname = "testuser")
            )

            `when`(createFeedCommentUseCase.execute(any(), any())).thenReturn(result)

            val documentFilter = document("feed-comment/reply-create", 200)
                .request(request().applyConfig(apiConfig))
                .response(
                    response().responseBodyField(
                        *CommentDocumentFields.singleFields(
                            "생성된 답글 정보 (피드 댓글 목록 아이템과 같은 형식)",
                            "답글 목록 (새 답글은 항상 빈 배열)"
                        )
                    )
                )
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
                .`when`()
                .post("/api/v1/feeds/{feedId}/comments", feedId)
                .then()
                .statusCode(200)
        }
    }

    @Nested
    @DisplayName("피드 댓글 수정")
    inner class UpdateComment {

        private val apiConfig = ApiRequestConfig(
            pathParameters = listOf(
                parameterWithName("commentId").description("수정할 댓글 또는 답글 ID")
            ),
            requestBodyFields = listOf(
                fieldWithPath("content").type(JsonFieldType.STRING)
                    .description("새 내용. 기존 내용을 통째로 교체 (최대 1000자, 초과 시 400 FD105)")
            ),
            headerDescriptors = AUTH_HEADER_DESCRIPTOR
        )

        @Test
        fun `성공`() {
            val commentId = 1L
            val request = UpdateFeedCommentRequest(content = "수정된 댓글 내용입니다!")
            val result = CommentFixtures.comment(
                id = commentId,
                content = "수정된 댓글 내용입니다!",
                author = CommentFixtures.author(memberId = testMember.memberId, nickname = "testuser"),
                likeCount = 5,
                replies = listOf(
                    CommentFixtures.reply(
                        id = 2L,
                        author = CommentFixtures.author(memberId = "otheruser", nickname = "다른회원", role = null)
                    )
                ),
                createdAt = LocalDateTime.of(2026, 10, 1, 12, 0, 0),
                updatedAt = LocalDateTime.of(2026, 10, 2, 9, 30, 0)
            )

            `when`(updateFeedCommentUseCase.execute(any(), any())).thenReturn(result)

            val documentFilter = document("feed-comment/update", 200)
                .request(request().applyConfig(apiConfig))
                .response(
                    response().responseBodyField(
                        *CommentDocumentFields.singleFields(
                            "수정된 댓글 또는 답글 정보 (피드 댓글 목록 아이템과 같은 형식)",
                            "현재 답글 목록 (최상위 댓글 수정 시 모든 답글이 오래된 순으로, 답글 수정 시 빈 배열)"
                        )
                    )
                )
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
                .`when`()
                .put("/api/v1/feeds/comments/{commentId}", commentId)
                .then()
                .statusCode(200)
        }

        @Test
        fun `댓글을 찾을 수 없음`() {
            val commentId = 999L
            val request = UpdateFeedCommentRequest(content = "수정된 댓글 내용입니다!")

            `when`(updateFeedCommentUseCase.execute(any(), any()))
                .thenThrow(FeedException(FeedErrorCode.FEED_COMMENT_NOT_FOUND))

            val documentFilter = document("feed-comment/update", "FEED_COMMENT_NOT_FOUND")
                .request(request().applyConfig(apiConfig))
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
                .`when`()
                .put("/api/v1/feeds/comments/{commentId}", commentId)
                .then()
                .statusCode(404)
        }

        @Test
        fun `권한 없음`() {
            val commentId = 1L
            val request = UpdateFeedCommentRequest(content = "수정된 댓글 내용입니다!")

            `when`(updateFeedCommentUseCase.execute(any(), any()))
                .thenThrow(FeedException(FeedErrorCode.FEED_COMMENT_FORBIDDEN_ACCESS))

            val documentFilter = document("feed-comment/update", "FEED_COMMENT_FORBIDDEN_ACCESS")
                .request(request().applyConfig(apiConfig))
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
                .`when`()
                .put("/api/v1/feeds/comments/{commentId}", commentId)
                .then()
                .statusCode(403)
        }

        @Test
        fun `댓글 내용이 1000자를 초과함`() {
            val commentId = 1L
            val request = UpdateFeedCommentRequest(content = "a".repeat(1001))

            `when`(updateFeedCommentUseCase.execute(any(), any()))
                .thenThrow(FeedException(FeedErrorCode.FEED_COMMENT_CONTENT_TOO_LONG))

            val documentFilter = document("feed-comment/update", "FEED_COMMENT_CONTENT_TOO_LONG")
                .request(request().applyConfig(apiConfig))
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
                .`when`()
                .put("/api/v1/feeds/comments/{commentId}", commentId)
                .then()
                .statusCode(400)
        }
    }

    @Nested
    @DisplayName("피드 댓글 삭제")
    inner class DeleteComment {

        private val apiConfig = ApiRequestConfig(
            pathParameters = listOf(
                parameterWithName("commentId").description("삭제할 댓글 또는 답글 ID")
            ),
            headerDescriptors = AUTH_HEADER_DESCRIPTOR
        )

        @Test
        fun `성공`() {
            val commentId = 1L

            val documentFilter = document("feed-comment/delete", 200)
                .request(request().applyConfig(apiConfig))
                .response(
                    response()
                        .responseBodyField(
                            fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                            fieldWithPath("data").type(JsonFieldType.OBJECT)
                                .description("항상 빈 객체({}). 삭제 성공 여부는 HTTP 200으로 판단")
                        )
                )
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .`when`()
                .delete("/api/v1/feeds/comments/{commentId}", commentId)
                .then()
                .statusCode(200)
        }

        @Test
        fun `댓글을 찾을 수 없음`() {
            val commentId = 999L

            `when`(deleteFeedCommentUseCase.execute(any(), any()))
                .thenThrow(FeedException(FeedErrorCode.FEED_COMMENT_NOT_FOUND))

            val documentFilter = document("feed-comment/delete", "FEED_COMMENT_NOT_FOUND")
                .request(request().applyConfig(apiConfig))
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .`when`()
                .delete("/api/v1/feeds/comments/{commentId}", commentId)
                .then()
                .statusCode(404)
        }

        @Test
        fun `권한 없음`() {
            val commentId = 1L

            `when`(deleteFeedCommentUseCase.execute(any(), any()))
                .thenThrow(FeedException(FeedErrorCode.FEED_COMMENT_FORBIDDEN_ACCESS))

            val documentFilter = document("feed-comment/delete", "FEED_COMMENT_FORBIDDEN_ACCESS")
                .request(request().applyConfig(apiConfig))
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .`when`()
                .delete("/api/v1/feeds/comments/{commentId}", commentId)
                .then()
                .statusCode(403)
        }
    }
}
