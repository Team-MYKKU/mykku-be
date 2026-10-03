package com.example.mykku.dailymessage.adapter.input.web

import com.example.mykku.BaseDocumentTest
import com.example.mykku.comment.CommentDocumentFields
import com.example.mykku.comment.CommentFixtures
import com.example.mykku.comment.application.dto.CommentAuthorResult
import com.example.mykku.docs.ApiRequestConfig
import com.example.mykku.docs.RestDocumentationResponse
import com.example.mykku.dailymessage.exception.DailyMessageErrorCode
import com.example.mykku.dailymessage.exception.DailyMessageException
import com.example.mykku.member.exception.MemberErrorCode
import com.example.mykku.member.exception.MemberException
import io.restassured.http.ContentType
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import java.time.LocalDateTime

class DailyMessageCommentDocumentTest : BaseDocumentTest() {

    @Nested
    @DisplayName("하루 덕담 댓글 목록 조회")
    inner class GetComments {

        private val apiConfig = ApiRequestConfig(
            pathParameters = listOf(
                parameterWithName("dailyMessageId").description("조회할 하루 덕담 ID")
            ),
            queryParameters = listOf(
                parameterWithName("page").description("페이지 번호 (0부터 시작, 0 이상, 기본값: 0)").optional(),
                parameterWithName("size").description("페이지 크기 (1~1000, 기본값: 20)").optional()
            ),
            headerDescriptors = OPTIONAL_AUTH_HEADER_DESCRIPTOR
        )

        @Test
        fun `성공`() {
            val dailyMessageId = 1L
            val result = CommentFixtures.page(
                listOf(
                    CommentFixtures.comment(
                        id = 1L,
                        content = "좋은 덕담이네요!",
                        author = CommentFixtures.author(memberId = "honggildong", nickname = "홍길동"),
                        likeCount = 5,
                        isLiked = true,
                        replies = listOf(
                            CommentFixtures.reply(
                                id = 2L,
                                content = "저도 동감합니다!",
                                author = CommentFixtures.author(memberId = "kimchulsoo", nickname = "김철수", role = null)
                            )
                        )
                    ),
                    CommentFixtures.comment(
                        id = 3L,
                        content = "오늘 하루도 힘내세요!",
                        author = CommentAuthorResult.withdrawn()
                    )
                )
            )

            `when`(getCommentsUseCase.execute(eq(dailyMessageId), anyOrNull(), any())).thenReturn(result)

            val documentFilter = document("daily-message-comment/list", 200)
                .request(request().applyConfig(apiConfig))
                .response(response().responseBodyField(*CommentDocumentFields.pageFields()))
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .param("page", "0")
                .param("size", "20")
                .`when`()
                .get("/api/v1/daily-messages/{dailyMessageId}/comments", dailyMessageId)
                .then()
                .statusCode(200)
        }

        @Test
        fun `하루 덕담을 찾을 수 없음`() {
            val dailyMessageId = 999L

            `when`(getCommentsUseCase.execute(eq(dailyMessageId), anyOrNull(), any()))
                .thenThrow(DailyMessageException(DailyMessageErrorCode.DAILY_MESSAGE_NOT_FOUND))

            val documentFilter = document("daily-message-comment/list", "DAILY_MESSAGE_NOT_FOUND")
                .request(request().applyConfig(apiConfig))
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            given(documentFilter)
                .contentType(ContentType.JSON)
                .param("page", "0")
                .param("size", "20")
                .`when`()
                .get("/api/v1/daily-messages/{dailyMessageId}/comments", dailyMessageId)
                .then()
                .statusCode(404)
        }
    }

    @Nested
    @DisplayName("하루 덕담 댓글 생성")
    inner class CreateComment {

        private val apiConfig = ApiRequestConfig(
            pathParameters = listOf(
                parameterWithName("dailyMessageId").description("댓글을 작성할 하루 덕담 ID")
            ),
            requestBodyFields = listOf(
                fieldWithPath("content").type(JsonFieldType.STRING)
                    .description(
                        "댓글 내용 (최대 1000자, UTF-16 길이 기준이라 이모지 등은 한 글자가 2자 이상으로 계산됨. " +
                            "빈 문자열이나 공백만 있는 값도 서버에서 거부하지 않으므로 클라이언트에서 입력 여부를 검증. " +
                            "필드를 빼거나 null이면 400 C101)"
                    ),
                fieldWithPath("parentCommentId").type(JsonFieldType.NUMBER)
                    .description(
                        "부모 댓글 ID. 일반 댓글이면 생략하거나 null. " +
                            "값을 넣으면 답글로 생성됨 (하루 덕담 답글 생성 참고)"
                    )
                    .optional()
            ),
            headerDescriptors = AUTH_HEADER_DESCRIPTOR
        )

        @Test
        fun `성공`() {
            val dailyMessageId = 1L
            val request = mapOf(
                "content" to "좋은 덕담 감사합니다!",
                "parentCommentId" to null
            )
            val result = CommentFixtures.comment(
                id = 1L,
                content = "좋은 덕담 감사합니다!",
                author = CommentFixtures.author(memberId = "honggildong", nickname = "홍길동")
            )

            `when`(createCommentUseCase.execute(any())).thenReturn(result)

            val documentFilter = document("daily-message-comment/create", 200)
                .request(request().applyConfig(apiConfig))
                .response(
                    response().responseBodyField(
                        *CommentDocumentFields.singleFields(
                            "생성된 댓글 정보 (목록 아이템과 같은 형식)",
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
                .post("/api/v1/daily-messages/{dailyMessageId}/comments", dailyMessageId)
                .then()
                .statusCode(200)
        }

        @Test
        fun `프로필 미완성`() {
            val dailyMessageId = 1L
            val request = mapOf(
                "content" to "좋은 덕담 감사합니다!",
                "parentCommentId" to null
            )

            `when`(createCommentUseCase.execute(any()))
                .thenThrow(MemberException(MemberErrorCode.PROFILE_NOT_COMPLETED))

            val documentFilter = document("daily-message-comment/create", "PROFILE_NOT_COMPLETED")
                .request(request().applyConfig(apiConfig))
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
                .`when`()
                .post("/api/v1/daily-messages/{dailyMessageId}/comments", dailyMessageId)
                .then()
                .statusCode(403)
        }

        @Test
        fun `하루 덕담을 찾을 수 없음`() {
            val dailyMessageId = 999L
            val request = mapOf(
                "content" to "좋은 덕담 감사합니다!",
                "parentCommentId" to null
            )

            `when`(createCommentUseCase.execute(any()))
                .thenThrow(DailyMessageException(DailyMessageErrorCode.DAILY_MESSAGE_NOT_FOUND))

            val documentFilter = document("daily-message-comment/create", "DAILY_MESSAGE_NOT_FOUND")
                .request(request().applyConfig(apiConfig))
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
                .`when`()
                .post("/api/v1/daily-messages/{dailyMessageId}/comments", dailyMessageId)
                .then()
                .statusCode(404)
        }

        @Test
        fun `내용 길이 초과`() {
            val dailyMessageId = 1L
            val request = mapOf(
                "content" to "a".repeat(1001),
                "parentCommentId" to null
            )

            `when`(createCommentUseCase.execute(any()))
                .thenThrow(DailyMessageException(DailyMessageErrorCode.DAILY_MESSAGE_COMMENT_CONTENT_TOO_LONG))

            val documentFilter = document("daily-message-comment/create", "DAILY_MESSAGE_COMMENT_CONTENT_TOO_LONG")
                .request(request().applyConfig(apiConfig))
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
                .`when`()
                .post("/api/v1/daily-messages/{dailyMessageId}/comments", dailyMessageId)
                .then()
                .statusCode(400)
        }
    }

    @Nested
    @DisplayName("하루 덕담 답글 생성")
    inner class CreateReply {

        private val apiConfig = ApiRequestConfig(
            pathParameters = listOf(
                parameterWithName("dailyMessageId").description("부모 댓글이 속한 하루 덕담 ID")
            ),
            requestBodyFields = listOf(
                fieldWithPath("content").type(JsonFieldType.STRING)
                    .description(
                        "답글 내용 (최대 1000자, UTF-16 길이 기준이라 이모지 등은 한 글자가 2자 이상으로 계산됨. " +
                            "빈 문자열이나 공백만 있는 값도 서버에서 거부하지 않으므로 클라이언트에서 입력 여부를 검증. " +
                            "필드를 빼거나 null이면 400 C101)"
                    ),
                fieldWithPath("parentCommentId").type(JsonFieldType.NUMBER)
                    .description(
                        "답글을 달 최상위 댓글 ID. 같은 하루 덕담(dailyMessageId)의 댓글이어야 하며 아니면 404 DM002. " +
                            "답글 ID는 지정할 수 없음 (답글은 1단계만 허용, 400 DM103)"
                    )
            ),
            headerDescriptors = AUTH_HEADER_DESCRIPTOR
        )

        @Test
        fun `성공`() {
            val dailyMessageId = 1L
            val request = mapOf(
                "content" to "저도 동감합니다!",
                "parentCommentId" to 10L
            )
            val result = CommentFixtures.comment(
                id = 2L,
                content = "저도 동감합니다!",
                author = CommentFixtures.author(memberId = "kimchulsoo", nickname = "김철수", role = null)
            )

            `when`(createCommentUseCase.execute(any())).thenReturn(result)

            val documentFilter = document("daily-message-comment/reply-create", 200)
                .request(request().applyConfig(apiConfig))
                .response(
                    response().responseBodyField(
                        *CommentDocumentFields.singleFields(
                            "생성된 답글 정보 (목록 아이템과 같은 형식)",
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
                .post("/api/v1/daily-messages/{dailyMessageId}/comments", dailyMessageId)
                .then()
                .statusCode(200)
        }

        @Test
        fun `프로필 미완성`() {
            val dailyMessageId = 1L
            val request = mapOf(
                "content" to "저도 동감합니다!",
                "parentCommentId" to 10L
            )

            `when`(createCommentUseCase.execute(any()))
                .thenThrow(MemberException(MemberErrorCode.PROFILE_NOT_COMPLETED))

            val documentFilter = document("daily-message-comment/reply-create", "PROFILE_NOT_COMPLETED")
                .request(request().applyConfig(apiConfig))
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
                .`when`()
                .post("/api/v1/daily-messages/{dailyMessageId}/comments", dailyMessageId)
                .then()
                .statusCode(403)
        }

        @Test
        fun `하루 덕담을 찾을 수 없음`() {
            val dailyMessageId = 999L
            val request = mapOf(
                "content" to "저도 동감합니다!",
                "parentCommentId" to 10L
            )

            `when`(createCommentUseCase.execute(any()))
                .thenThrow(DailyMessageException(DailyMessageErrorCode.DAILY_MESSAGE_NOT_FOUND))

            val documentFilter = document("daily-message-comment/reply-create", "DAILY_MESSAGE_NOT_FOUND")
                .request(request().applyConfig(apiConfig))
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
                .`when`()
                .post("/api/v1/daily-messages/{dailyMessageId}/comments", dailyMessageId)
                .then()
                .statusCode(404)
        }

        @Test
        fun `부모 댓글을 찾을 수 없음`() {
            val dailyMessageId = 1L
            val request = mapOf(
                "content" to "저도 동감합니다!",
                "parentCommentId" to 999L
            )

            `when`(createCommentUseCase.execute(any()))
                .thenThrow(DailyMessageException(DailyMessageErrorCode.DAILY_MESSAGE_COMMENT_NOT_FOUND))

            val documentFilter = document("daily-message-comment/reply-create", "DAILY_MESSAGE_COMMENT_NOT_FOUND")
                .request(request().applyConfig(apiConfig))
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
                .`when`()
                .post("/api/v1/daily-messages/{dailyMessageId}/comments", dailyMessageId)
                .then()
                .statusCode(404)
        }

        @Test
        fun `답글에 답글 작성`() {
            val dailyMessageId = 1L
            val request = mapOf(
                "content" to "저도 동감합니다!",
                "parentCommentId" to 11L
            )

            `when`(createCommentUseCase.execute(any()))
                .thenThrow(DailyMessageException(DailyMessageErrorCode.REPLY_DEPTH_EXCEEDED))

            val documentFilter = document("daily-message-comment/reply-create", "REPLY_DEPTH_EXCEEDED")
                .request(request().applyConfig(apiConfig))
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
                .`when`()
                .post("/api/v1/daily-messages/{dailyMessageId}/comments", dailyMessageId)
                .then()
                .statusCode(400)
        }

        @Test
        fun `내용 길이 초과`() {
            val dailyMessageId = 1L
            val request = mapOf(
                "content" to "a".repeat(1001),
                "parentCommentId" to 10L
            )

            `when`(createCommentUseCase.execute(any()))
                .thenThrow(DailyMessageException(DailyMessageErrorCode.DAILY_MESSAGE_COMMENT_CONTENT_TOO_LONG))

            val documentFilter = document(
                "daily-message-comment/reply-create",
                "DAILY_MESSAGE_COMMENT_CONTENT_TOO_LONG"
            )
                .request(request().applyConfig(apiConfig))
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
                .`when`()
                .post("/api/v1/daily-messages/{dailyMessageId}/comments", dailyMessageId)
                .then()
                .statusCode(400)
        }
    }

    @Nested
    @DisplayName("하루 덕담 댓글 수정")
    inner class UpdateComment {

        private val apiConfig = ApiRequestConfig(
            pathParameters = listOf(
                parameterWithName("commentId").description("수정할 댓글 또는 답글 ID")
            ),
            requestBodyFields = listOf(
                fieldWithPath("content").type(JsonFieldType.STRING)
                    .description(
                        "수정할 댓글 내용 (최대 1000자, UTF-16 길이 기준이라 이모지 등은 한 글자가 2자 이상으로 계산됨. " +
                            "빈 문자열이나 공백만 있는 값도 서버에서 거부하지 않으므로 클라이언트에서 입력 여부를 검증. " +
                            "필드를 빼거나 null이면 400 C101)"
                    )
            ),
            headerDescriptors = AUTH_HEADER_DESCRIPTOR
        )

        @Test
        fun `성공`() {
            val commentId = 1L
            val request = mapOf("content" to "수정된 댓글 내용입니다!")
            val result = CommentFixtures.comment(
                id = commentId,
                content = "수정된 댓글 내용입니다!",
                author = CommentFixtures.author(memberId = "honggildong", nickname = "홍길동"),
                likeCount = 5,
                replies = listOf(
                    CommentFixtures.reply(
                        id = 2L,
                        author = CommentFixtures.author(memberId = "kimchulsoo", nickname = "김철수", role = null)
                    )
                ),
                createdAt = LocalDateTime.of(2026, 10, 1, 12, 0, 0),
                updatedAt = LocalDateTime.of(2026, 10, 2, 9, 30, 0)
            )

            `when`(updateCommentUseCase.execute(any())).thenReturn(result)

            val documentFilter = document("daily-message-comment/update", 200)
                .request(request().applyConfig(apiConfig))
                .response(
                    response().responseBodyField(
                        *CommentDocumentFields.singleFields(
                            "수정된 댓글 정보 (목록 아이템과 같은 형식)",
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
                .put("/api/v1/daily-messages/comments/{commentId}", commentId)
                .then()
                .statusCode(200)
        }

        @Test
        fun `댓글을 찾을 수 없음`() {
            val commentId = 999L
            val request = mapOf("content" to "수정된 댓글 내용입니다!")

            `when`(updateCommentUseCase.execute(any()))
                .thenThrow(DailyMessageException(DailyMessageErrorCode.DAILY_MESSAGE_COMMENT_NOT_FOUND))

            val documentFilter = document("daily-message-comment/update", "DAILY_MESSAGE_COMMENT_NOT_FOUND")
                .request(request().applyConfig(apiConfig))
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
                .`when`()
                .put("/api/v1/daily-messages/comments/{commentId}", commentId)
                .then()
                .statusCode(404)
        }

        @Test
        fun `권한 없음`() {
            val commentId = 1L
            val request = mapOf("content" to "수정된 댓글 내용입니다!")

            `when`(updateCommentUseCase.execute(any()))
                .thenThrow(DailyMessageException(DailyMessageErrorCode.COMMENT_FORBIDDEN_ACCESS))

            val documentFilter = document("daily-message-comment/update", "COMMENT_FORBIDDEN_ACCESS")
                .request(request().applyConfig(apiConfig))
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
                .`when`()
                .put("/api/v1/daily-messages/comments/{commentId}", commentId)
                .then()
                .statusCode(403)
        }

        @Test
        fun `내용 길이 초과`() {
            val commentId = 1L
            val request = mapOf("content" to "a".repeat(1001))

            `when`(updateCommentUseCase.execute(any()))
                .thenThrow(DailyMessageException(DailyMessageErrorCode.DAILY_MESSAGE_COMMENT_CONTENT_TOO_LONG))

            val documentFilter = document("daily-message-comment/update", "DAILY_MESSAGE_COMMENT_CONTENT_TOO_LONG")
                .request(request().applyConfig(apiConfig))
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
                .`when`()
                .put("/api/v1/daily-messages/comments/{commentId}", commentId)
                .then()
                .statusCode(400)
        }
    }

    @Nested
    @DisplayName("하루 덕담 댓글 삭제")
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

            val documentFilter = document("daily-message-comment/delete", 200)
                .request(request().applyConfig(apiConfig))
                .response(
                    response()
                        .responseBodyField(
                            fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                            fieldWithPath("data").type(JsonFieldType.OBJECT).description("빈 객체({}). 사용하지 않음")
                        )
                )
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .`when`()
                .delete("/api/v1/daily-messages/comments/{commentId}", commentId)
                .then()
                .statusCode(200)
        }

        @Test
        fun `댓글을 찾을 수 없음`() {
            val commentId = 999L

            `when`(deleteCommentUseCase.execute(eq(commentId), any()))
                .thenThrow(DailyMessageException(DailyMessageErrorCode.DAILY_MESSAGE_COMMENT_NOT_FOUND))

            val documentFilter = document("daily-message-comment/delete", "DAILY_MESSAGE_COMMENT_NOT_FOUND")
                .request(request().applyConfig(apiConfig))
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .`when`()
                .delete("/api/v1/daily-messages/comments/{commentId}", commentId)
                .then()
                .statusCode(404)
        }

        @Test
        fun `권한 없음`() {
            val commentId = 1L

            `when`(deleteCommentUseCase.execute(eq(commentId), any()))
                .thenThrow(DailyMessageException(DailyMessageErrorCode.COMMENT_FORBIDDEN_ACCESS))

            val documentFilter = document("daily-message-comment/delete", "COMMENT_FORBIDDEN_ACCESS")
                .request(request().applyConfig(apiConfig))
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .`when`()
                .delete("/api/v1/daily-messages/comments/{commentId}", commentId)
                .then()
                .statusCode(403)
        }

        @Test
        fun `답글이 남아 있는 댓글 삭제`() {
            val commentId = 1L

            `when`(deleteCommentUseCase.execute(eq(commentId), any()))
                .thenThrow(DataIntegrityViolationException("Cannot delete or update a parent row"))

            val documentFilter = document("daily-message-comment/delete", "DATA_INTEGRITY_VIOLATION")
                .request(request().applyConfig(apiConfig))
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .`when`()
                .delete("/api/v1/daily-messages/comments/{commentId}", commentId)
                .then()
                .statusCode(409)
        }
    }
}
