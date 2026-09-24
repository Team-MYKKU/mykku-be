package com.example.mykku.dailymessage.adapter.input.web

import com.example.mykku.BaseDocumentTest
import com.example.mykku.docs.ApiRequestConfig
import com.example.mykku.docs.RestDocumentationResponse
import com.example.mykku.dailymessage.application.dto.CommentResult
import com.example.mykku.dailymessage.application.dto.DailyMessageCommentsResult
import com.example.mykku.dailymessage.application.dto.ReplyResult
import com.example.mykku.dailymessage.exception.DailyMessageErrorCode
import com.example.mykku.dailymessage.exception.DailyMessageException
import com.example.mykku.role.application.dto.RoleResult
import io.restassured.http.ContentType
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
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
                parameterWithName("page").description("페이지 번호 (기본값: 0)").optional(),
                parameterWithName("size").description("페이지 크기 (기본값: 20)").optional()
            )
        )

        @Test
        fun `성공`() {
            val dailyMessageId = 1L
            val result = DailyMessageCommentsResult(
                comments = listOf(
                    CommentResult(
                        id = 1L,
                        content = "좋은 덕담이네요!",
                        likeCount = 5,
                        isLiked = true,
                        memberId = "honggildong",
                        memberName = "홍길동",
                        role = RoleResult(1L, "덕담왕", "덕담을 많이 남긴 사람"),
                        profileImage = "https://example.com/profile1.jpg",
                        createdAt = LocalDateTime.now(),
                        replies = listOf(
                            ReplyResult(
                                id = 2L,
                                content = "저도 동감합니다!",
                                likeCount = 2,
                                isLiked = false,
                                memberId = "kimchulsoo",
                                memberName = "김철수",
                                role = null,
                                profileImage = "https://example.com/profile2.jpg",
                                createdAt = LocalDateTime.now()
                            )
                        )
                    ),
                    CommentResult(
                        id = 3L,
                        content = "오늘 하루도 힘내세요!",
                        likeCount = 3,
                        isLiked = false,
                        memberId = "leeyounghee",
                        memberName = "이영희",
                        role = null,
                        profileImage = "https://example.com/profile3.jpg",
                        createdAt = LocalDateTime.now(),
                        replies = emptyList()
                    )
                ),
                totalElements = 2,
                totalPages = 1,
                currentPage = 0,
                pageSize = 20,
                hasNext = false
            )

            `when`(getCommentsUseCase.execute(eq(dailyMessageId), anyOrNull(), any())).thenReturn(result)

            val documentFilter = document("daily-message-comment/list", 200)
                .request(request().applyConfig(apiConfig))
                .response(
                    response()
                        .responseBodyField(
                            fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                            fieldWithPath("data").type(JsonFieldType.OBJECT).description("댓글 목록 응답 데이터"),
                            fieldWithPath("data.comments[]").type(JsonFieldType.ARRAY).description("댓글 목록"),
                            fieldWithPath("data.comments[].id").type(JsonFieldType.NUMBER).description("댓글 ID"),
                            fieldWithPath("data.comments[].content").type(JsonFieldType.STRING).description("댓글 내용"),
                            fieldWithPath("data.comments[].likeCount").type(JsonFieldType.NUMBER).description("좋아요 수"),
                            fieldWithPath("data.comments[].isLiked").type(JsonFieldType.BOOLEAN).description("현재 로그인 멤버의 좋아요 여부 (비로그인 시 false)"),
                            fieldWithPath("data.comments[].memberId").type(JsonFieldType.STRING).description("작성자 아이디 (탈퇴 시 null)").optional(),
                            fieldWithPath("data.comments[].memberName").type(JsonFieldType.STRING).description("작성자 이름"),
                            fieldWithPath("data.comments[].role").type(JsonFieldType.OBJECT).description("작성자 대표 칭호 (없으면 null)").optional(),
                            fieldWithPath("data.comments[].role.id").type(JsonFieldType.NUMBER).description("칭호 ID").optional(),
                            fieldWithPath("data.comments[].role.name").type(JsonFieldType.STRING).description("칭호 이름").optional(),
                            fieldWithPath("data.comments[].role.description").type(JsonFieldType.STRING).description("칭호 설명").optional(),
                            fieldWithPath("data.comments[].profileImage").type(JsonFieldType.STRING).description("작성자 프로필 이미지 URL"),
                            fieldWithPath("data.comments[].createdAt").type(JsonFieldType.STRING).description("작성 일시"),
                            fieldWithPath("data.comments[].replies[]").type(JsonFieldType.ARRAY).description("답글 목록"),
                            fieldWithPath("data.comments[].replies[].id").type(JsonFieldType.NUMBER).description("답글 ID"),
                            fieldWithPath("data.comments[].replies[].content").type(JsonFieldType.STRING).description("답글 내용"),
                            fieldWithPath("data.comments[].replies[].likeCount").type(JsonFieldType.NUMBER).description("답글 좋아요 수"),
                            fieldWithPath("data.comments[].replies[].isLiked").type(JsonFieldType.BOOLEAN).description("현재 로그인 멤버의 답글 좋아요 여부 (비로그인 시 false)"),
                            fieldWithPath("data.comments[].replies[].memberId").type(JsonFieldType.STRING).description("답글 작성자 아이디 (탈퇴 시 null)").optional(),
                            fieldWithPath("data.comments[].replies[].memberName").type(JsonFieldType.STRING).description("답글 작성자 이름"),
                            fieldWithPath("data.comments[].replies[].role").type(JsonFieldType.OBJECT).description("답글 작성자 대표 칭호 (없으면 null)").optional(),
                            fieldWithPath("data.comments[].replies[].role.id").type(JsonFieldType.NUMBER).description("칭호 ID").optional(),
                            fieldWithPath("data.comments[].replies[].role.name").type(JsonFieldType.STRING).description("칭호 이름").optional(),
                            fieldWithPath("data.comments[].replies[].role.description").type(JsonFieldType.STRING).description("칭호 설명").optional(),
                            fieldWithPath("data.comments[].replies[].profileImage").type(JsonFieldType.STRING).description("답글 작성자 프로필 이미지 URL"),
                            fieldWithPath("data.comments[].replies[].createdAt").type(JsonFieldType.STRING).description("답글 작성 일시"),
                            fieldWithPath("data.totalElements").type(JsonFieldType.NUMBER).description("전체 댓글 수"),
                            fieldWithPath("data.totalPages").type(JsonFieldType.NUMBER).description("전체 페이지 수"),
                            fieldWithPath("data.currentPage").type(JsonFieldType.NUMBER).description("현재 페이지 번호"),
                            fieldWithPath("data.pageSize").type(JsonFieldType.NUMBER).description("페이지 크기"),
                            fieldWithPath("data.hasNext").type(JsonFieldType.BOOLEAN).description("다음 페이지 존재 여부")
                        )
                )
                .build()

            given(documentFilter)
                .contentType(ContentType.JSON)
                .param("page", "0")
                .param("size", "20")
                .`when`()
                .get("/api/v1/daily-messages/{dailyMessageId}/comments", dailyMessageId)
                .then()
                .statusCode(200)
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
                fieldWithPath("content").type(JsonFieldType.STRING).description("댓글 내용"),
                fieldWithPath("parentCommentId").type(JsonFieldType.NUMBER).description("부모 댓글 ID (답글인 경우)")
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
            val result = CommentResult(
                id = 1L,
                content = "좋은 덕담 감사합니다!",
                likeCount = 0,
                isLiked = false,
                memberId = "honggildong",
                memberName = "홍길동",
                role = RoleResult(1L, "덕담왕", "덕담을 많이 남긴 사람"),
                profileImage = "https://example.com/profile.jpg",
                createdAt = LocalDateTime.now(),
                replies = emptyList()
            )

            `when`(createCommentUseCase.execute(any())).thenReturn(result)

            val documentFilter = document("daily-message-comment/create", 200)
                .request(request().applyConfig(apiConfig))
                .response(
                    response()
                        .responseBodyField(
                            fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                            fieldWithPath("data").type(JsonFieldType.OBJECT).description("생성된 댓글 정보"),
                            fieldWithPath("data.id").type(JsonFieldType.NUMBER).description("댓글 ID"),
                            fieldWithPath("data.content").type(JsonFieldType.STRING).description("댓글 내용"),
                            fieldWithPath("data.likeCount").type(JsonFieldType.NUMBER).description("좋아요 수"),
                            fieldWithPath("data.isLiked").type(JsonFieldType.BOOLEAN).description("현재 로그인 멤버의 좋아요 여부 (비로그인 시 false)"),
                            fieldWithPath("data.memberId").type(JsonFieldType.STRING).description("작성자 아이디 (탈퇴 시 null)").optional(),
                            fieldWithPath("data.memberName").type(JsonFieldType.STRING).description("작성자 이름"),
                            fieldWithPath("data.role").type(JsonFieldType.OBJECT).description("작성자 대표 칭호 (없으면 null)").optional(),
                            fieldWithPath("data.role.id").type(JsonFieldType.NUMBER).description("칭호 ID").optional(),
                            fieldWithPath("data.role.name").type(JsonFieldType.STRING).description("칭호 이름").optional(),
                            fieldWithPath("data.role.description").type(JsonFieldType.STRING).description("칭호 설명").optional(),
                            fieldWithPath("data.createdAt").type(JsonFieldType.STRING).description("작성 일시"),
                            fieldWithPath("data.profileImage").type(JsonFieldType.STRING).description("작성자 프로필 이미지 URL"),
                            fieldWithPath("data.replies[]").type(JsonFieldType.ARRAY).description("답글 목록")
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
        fun `내용 길이 초과`() {
            val dailyMessageId = 1L
            val request = mapOf(
                "content" to "a".repeat(501),
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
                parameterWithName("dailyMessageId").description("댓글을 작성할 하루 덕담 ID")
            ),
            requestBodyFields = listOf(
                fieldWithPath("content").type(JsonFieldType.STRING).description("답글 내용"),
                fieldWithPath("parentCommentId").type(JsonFieldType.NUMBER).description("부모 댓글 ID")
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
            val result = CommentResult(
                id = 2L,
                content = "저도 동감합니다!",
                likeCount = 0,
                isLiked = false,
                memberId = "kimchulsoo",
                memberName = "김철수",
                role = null,
                profileImage = "https://example.com/profile2.jpg",
                createdAt = LocalDateTime.now(),
                replies = emptyList()
            )

            `when`(createCommentUseCase.execute(any())).thenReturn(result)

            val documentFilter = document("daily-message-comment/reply-create", 200)
                .request(request().applyConfig(apiConfig))
                .response(
                    response()
                        .responseBodyField(
                            fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                            fieldWithPath("data").type(JsonFieldType.OBJECT).description("생성된 답글 정보"),
                            fieldWithPath("data.id").type(JsonFieldType.NUMBER).description("답글 ID"),
                            fieldWithPath("data.content").type(JsonFieldType.STRING).description("답글 내용"),
                            fieldWithPath("data.likeCount").type(JsonFieldType.NUMBER).description("좋아요 수"),
                            fieldWithPath("data.isLiked").type(JsonFieldType.BOOLEAN).description("현재 로그인 멤버의 좋아요 여부 (비로그인 시 false)"),
                            fieldWithPath("data.memberId").type(JsonFieldType.STRING).description("작성자 아이디 (탈퇴 시 null)").optional(),
                            fieldWithPath("data.memberName").type(JsonFieldType.STRING).description("작성자 이름"),
                            fieldWithPath("data.role").type(JsonFieldType.OBJECT).description("작성자 대표 칭호 (없으면 null)").optional(),
                            fieldWithPath("data.role.id").type(JsonFieldType.NUMBER).description("칭호 ID").optional(),
                            fieldWithPath("data.role.name").type(JsonFieldType.STRING).description("칭호 이름").optional(),
                            fieldWithPath("data.role.description").type(JsonFieldType.STRING).description("칭호 설명").optional(),
                            fieldWithPath("data.createdAt").type(JsonFieldType.STRING).description("작성 일시"),
                            fieldWithPath("data.replies[]").type(JsonFieldType.ARRAY).description("답글 목록 (항상 빈 배열)"),
                            fieldWithPath("data.profileImage").type(JsonFieldType.STRING).description("작성자 프로필 이미지 URL")
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
    }

    @Nested
    @DisplayName("하루 덕담 댓글 수정")
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
            val request = mapOf("content" to "수정된 댓글 내용입니다!")
            val result = CommentResult(
                id = commentId,
                content = "수정된 댓글 내용입니다!",
                likeCount = 5,
                isLiked = false,
                memberId = "honggildong",
                memberName = "홍길동",
                role = RoleResult(1L, "덕담왕", "덕담을 많이 남긴 사람"),
                profileImage = "https://example.com/profile.jpg",
                createdAt = LocalDateTime.now(),
                replies = emptyList()
            )

            `when`(updateCommentUseCase.execute(any())).thenReturn(result)

            val documentFilter = document("daily-message-comment/update", 200)
                .request(request().applyConfig(apiConfig))
                .response(
                    response()
                        .responseBodyField(
                            fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                            fieldWithPath("data").type(JsonFieldType.OBJECT).description("수정된 댓글 정보"),
                            fieldWithPath("data.id").type(JsonFieldType.NUMBER).description("댓글 ID"),
                            fieldWithPath("data.content").type(JsonFieldType.STRING).description("댓글 내용"),
                            fieldWithPath("data.likeCount").type(JsonFieldType.NUMBER).description("좋아요 수"),
                            fieldWithPath("data.isLiked").type(JsonFieldType.BOOLEAN).description("현재 로그인 멤버의 좋아요 여부 (비로그인 시 false)"),
                            fieldWithPath("data.memberId").type(JsonFieldType.STRING).description("작성자 아이디 (탈퇴 시 null)").optional(),
                            fieldWithPath("data.memberName").type(JsonFieldType.STRING).description("작성자 이름"),
                            fieldWithPath("data.role").type(JsonFieldType.OBJECT).description("작성자 대표 칭호 (없으면 null)").optional(),
                            fieldWithPath("data.role.id").type(JsonFieldType.NUMBER).description("칭호 ID").optional(),
                            fieldWithPath("data.role.name").type(JsonFieldType.STRING).description("칭호 이름").optional(),
                            fieldWithPath("data.role.description").type(JsonFieldType.STRING).description("칭호 설명").optional(),
                            fieldWithPath("data.createdAt").type(JsonFieldType.STRING).description("작성 일시"),
                            fieldWithPath("data.profileImage").type(JsonFieldType.STRING).description("작성자 프로필 이미지 URL"),
                            fieldWithPath("data.replies[]").type(JsonFieldType.ARRAY).description("답글 목록")
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
    }

    @Nested
    @DisplayName("하루 덕담 댓글 삭제")
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

            val documentFilter = document("daily-message-comment/delete", 200)
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
    }
}
