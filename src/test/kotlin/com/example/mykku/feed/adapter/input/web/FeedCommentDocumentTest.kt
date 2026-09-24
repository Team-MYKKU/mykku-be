package com.example.mykku.feed.adapter.input.web

import com.example.mykku.BaseDocumentTest
import com.example.mykku.docs.ApiRequestConfig
import com.example.mykku.docs.RestDocumentationResponse
import com.example.mykku.feed.adapter.input.web.dto.CreateFeedCommentRequest
import com.example.mykku.feed.adapter.input.web.dto.UpdateFeedCommentRequest
import com.example.mykku.feed.application.dto.CommentAuthorResult
import com.example.mykku.feed.application.dto.SingleFeedCommentResult
import com.example.mykku.feed.exception.FeedErrorCode
import com.example.mykku.feed.exception.FeedException
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
                fieldWithPath("content").type(JsonFieldType.STRING).description("댓글 내용"),
                fieldWithPath("parentCommentId").type(JsonFieldType.NUMBER).description("부모 댓글 ID (답글인 경우)")
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
            val result = SingleFeedCommentResult(
                id = 1L,
                content = "좋은 피드네요!",
                author = CommentAuthorResult(
                    memberId = testMember.memberId,
                    nickname = "testuser",
                    profileImage = "https://example.com/profile.jpg"
                ),
                likeCount = 0,
                createdAt = LocalDateTime.now()
            )

            `when`(createFeedCommentUseCase.execute(any(), any())).thenReturn(result)

            val documentFilter = document("feed-comment/create", 200)
                .request(request().applyConfig(apiConfig))
                .response(
                    response()
                        .responseBodyField(
                            fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                            fieldWithPath("data").type(JsonFieldType.OBJECT).description("생성된 댓글 정보"),
                            fieldWithPath("data.id").type(JsonFieldType.NUMBER).description("댓글 ID"),
                            fieldWithPath("data.content").type(JsonFieldType.STRING).description("댓글 내용"),
                            fieldWithPath("data.author").type(JsonFieldType.OBJECT).description("작성자 정보"),
                            fieldWithPath("data.author.memberId").type(JsonFieldType.STRING).description("작성자 회원 ID"),
                            fieldWithPath("data.author.nickname").type(JsonFieldType.STRING).description("작성자 닉네임"),
                            fieldWithPath("data.author.profileImage").type(JsonFieldType.STRING).description("작성자 프로필 이미지 URL"),
                            fieldWithPath("data.likeCount").type(JsonFieldType.NUMBER).description("좋아요 수"),
                            fieldWithPath("data.createdAt").type(JsonFieldType.STRING).description("작성 일시")
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
    }

    @Nested
    @DisplayName("피드 답글 생성")
    inner class CreateReply {

        private val apiConfig = ApiRequestConfig(
            pathParameters = listOf(
                parameterWithName("feedId").description("댓글을 작성할 피드 ID")
            ),
            requestBodyFields = listOf(
                fieldWithPath("content").type(JsonFieldType.STRING).description("답글 내용"),
                fieldWithPath("parentCommentId").type(JsonFieldType.NUMBER).description("부모 댓글 ID")
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
            val result = SingleFeedCommentResult(
                id = 2L,
                content = "저도 동감합니다!",
                author = CommentAuthorResult(
                    memberId = testMember.memberId,
                    nickname = "testuser",
                    profileImage = "https://example.com/profile.jpg"
                ),
                likeCount = 0,
                createdAt = LocalDateTime.now()
            )

            `when`(createFeedCommentUseCase.execute(any(), any())).thenReturn(result)

            val documentFilter = document("feed-comment/reply-create", 200)
                .request(request().applyConfig(apiConfig))
                .response(
                    response()
                        .responseBodyField(
                            fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                            fieldWithPath("data").type(JsonFieldType.OBJECT).description("생성된 답글 정보"),
                            fieldWithPath("data.id").type(JsonFieldType.NUMBER).description("답글 ID"),
                            fieldWithPath("data.content").type(JsonFieldType.STRING).description("답글 내용"),
                            fieldWithPath("data.author").type(JsonFieldType.OBJECT).description("작성자 정보"),
                            fieldWithPath("data.author.memberId").type(JsonFieldType.STRING).description("작성자 회원 ID"),
                            fieldWithPath("data.author.nickname").type(JsonFieldType.STRING).description("작성자 닉네임"),
                            fieldWithPath("data.author.profileImage").type(JsonFieldType.STRING).description("작성자 프로필 이미지 URL"),
                            fieldWithPath("data.likeCount").type(JsonFieldType.NUMBER).description("좋아요 수"),
                            fieldWithPath("data.createdAt").type(JsonFieldType.STRING).description("작성 일시")
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
                parameterWithName("commentId").description("수정할 댓글 ID")
            ),
            requestBodyFields = listOf(
                fieldWithPath("content").type(JsonFieldType.STRING).description("수정할 댓글 내용")
            ),
            headerDescriptors = AUTH_HEADER_DESCRIPTOR
        )

        @Test
        fun `성공`() {
            val commentId = 1L
            val request = UpdateFeedCommentRequest(content = "수정된 댓글 내용입니다!")
            val result = SingleFeedCommentResult(
                id = commentId,
                content = "수정된 댓글 내용입니다!",
                author = CommentAuthorResult(
                    memberId = testMember.memberId,
                    nickname = "testuser",
                    profileImage = "https://example.com/profile.jpg"
                ),
                likeCount = 5,
                createdAt = LocalDateTime.now()
            )

            `when`(updateFeedCommentUseCase.execute(any(), any())).thenReturn(result)

            val documentFilter = document("feed-comment/update", 200)
                .request(request().applyConfig(apiConfig))
                .response(
                    response()
                        .responseBodyField(
                            fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                            fieldWithPath("data").type(JsonFieldType.OBJECT).description("수정된 댓글 정보"),
                            fieldWithPath("data.id").type(JsonFieldType.NUMBER).description("댓글 ID"),
                            fieldWithPath("data.content").type(JsonFieldType.STRING).description("댓글 내용"),
                            fieldWithPath("data.author").type(JsonFieldType.OBJECT).description("작성자 정보"),
                            fieldWithPath("data.author.memberId").type(JsonFieldType.STRING).description("작성자 회원 ID"),
                            fieldWithPath("data.author.nickname").type(JsonFieldType.STRING).description("작성자 닉네임"),
                            fieldWithPath("data.author.profileImage").type(JsonFieldType.STRING).description("작성자 프로필 이미지 URL"),
                            fieldWithPath("data.likeCount").type(JsonFieldType.NUMBER).description("좋아요 수"),
                            fieldWithPath("data.createdAt").type(JsonFieldType.STRING).description("작성 일시")
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
    }

    @Nested
    @DisplayName("피드 댓글 삭제")
    inner class DeleteComment {

        private val apiConfig = ApiRequestConfig(
            pathParameters = listOf(
                parameterWithName("commentId").description("삭제할 댓글 ID")
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
                            fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터")
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
