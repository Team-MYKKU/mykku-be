package com.example.mykku.like.adapter.input.web

import com.example.mykku.BaseDocumentTest
import com.example.mykku.board.exception.BoardException
import com.example.mykku.dailymessage.exception.DailyMessageException
import com.example.mykku.docs.ApiRequestConfig
import com.example.mykku.docs.RestDocumentationResponse
import com.example.mykku.feed.exception.FeedException
import com.example.mykku.like.application.dto.LikeBoardInfoResult
import com.example.mykku.like.application.dto.LikeBoardResult
import com.example.mykku.like.application.dto.LikeDailyMessageCommentResult
import com.example.mykku.like.application.dto.LikeFeedCommentResult
import com.example.mykku.like.application.dto.LikeFeedResult
import com.example.mykku.like.exception.LikeErrorCode
import com.example.mykku.like.exception.LikeException
import io.restassured.http.ContentType
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName

class LikeDocumentTest : BaseDocumentTest() {

    @Nested
    @DisplayName("즐겨찾기한 게시판 목록 조회")
    inner class GetLikedBoards {

        private val apiConfig = ApiRequestConfig(
            queryParameters = listOf(
                parameterWithName("page").description("페이지 번호 (0부터 시작, 0 이상, 기본값: 0)").optional(),
                parameterWithName("size").description("페이지 크기 (1 이상 1000 이하, 기본값: 20)").optional()
            ),
            headerDescriptors = AUTH_HEADER_DESCRIPTOR
        )

        @Test
        fun `성공`() {
            val likedBoards = listOf(
                LikeBoardInfoResult(id = 1L, title = "자유게시판", logo = "https://example.com/logo1.png"),
                LikeBoardInfoResult(id = 2L, title = "질문게시판", logo = "https://example.com/logo2.png")
            )
            val pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"))
            val page = PageImpl(likedBoards, pageable, 2)

            `when`(likeBoardUseCase.getLikedBoards(any())).thenReturn(page)

            val documentFilter = document("like/board-list", 200)
                .request(request().applyConfig(apiConfig))
                .response(
                    response()
                        .responseBodyField(
                            fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                            fieldWithPath("data.content[]").type(JsonFieldType.ARRAY)
                                .description("즐겨찾기한 게시판 목록 (즐겨찾기한 시각 최신순)"),
                            fieldWithPath("data.content[].id").type(JsonFieldType.NUMBER)
                                .description("즐겨찾기 레코드 ID (게시판 ID 아님)"),
                            fieldWithPath("data.content[].title").type(JsonFieldType.STRING).description("게시판 제목"),
                            fieldWithPath("data.content[].logo").type(JsonFieldType.STRING).description("게시판 로고 URL"),
                            fieldWithPath("data.pageable").type(JsonFieldType.OBJECT).description("페이지 정보"),
                            fieldWithPath("data.pageable.pageNumber").type(JsonFieldType.NUMBER)
                                .description("현재 페이지 번호 (0부터 시작)"),
                            fieldWithPath("data.pageable.pageSize").type(JsonFieldType.NUMBER).description("페이지 크기"),
                            fieldWithPath("data.pageable.sort").type(JsonFieldType.OBJECT)
                                .description("정렬 정보 (항상 createdAt 내림차순)"),
                            fieldWithPath("data.pageable.sort.empty").type(JsonFieldType.BOOLEAN)
                                .description("정렬 조건이 없는지 여부 (항상 false)"),
                            fieldWithPath("data.pageable.sort.sorted").type(JsonFieldType.BOOLEAN)
                                .description("정렬 적용 여부 (항상 true)"),
                            fieldWithPath("data.pageable.sort.unsorted").type(JsonFieldType.BOOLEAN)
                                .description("정렬 미적용 여부 (항상 false)"),
                            fieldWithPath("data.pageable.offset").type(JsonFieldType.NUMBER).description("오프셋"),
                            fieldWithPath("data.pageable.paged").type(JsonFieldType.BOOLEAN).description("페이지네이션 여부"),
                            fieldWithPath("data.pageable.unpaged").type(JsonFieldType.BOOLEAN).description("페이지네이션 아님"),
                            fieldWithPath("data.totalElements").type(JsonFieldType.NUMBER).description("전체 요소 수"),
                            fieldWithPath("data.totalPages").type(JsonFieldType.NUMBER).description("전체 페이지 수"),
                            fieldWithPath("data.size").type(JsonFieldType.NUMBER).description("페이지 크기"),
                            fieldWithPath("data.number").type(JsonFieldType.NUMBER).description("현재 페이지 번호 (0부터 시작)"),
                            fieldWithPath("data.sort").type(JsonFieldType.OBJECT)
                                .description("정렬 정보 (항상 createdAt 내림차순)"),
                            fieldWithPath("data.sort.empty").type(JsonFieldType.BOOLEAN)
                                .description("정렬 조건이 없는지 여부 (항상 false)"),
                            fieldWithPath("data.sort.sorted").type(JsonFieldType.BOOLEAN)
                                .description("정렬 적용 여부 (항상 true)"),
                            fieldWithPath("data.sort.unsorted").type(JsonFieldType.BOOLEAN)
                                .description("정렬 미적용 여부 (항상 false)"),
                            fieldWithPath("data.first").type(JsonFieldType.BOOLEAN).description("첫 페이지 여부"),
                            fieldWithPath("data.last").type(JsonFieldType.BOOLEAN).description("마지막 페이지 여부"),
                            fieldWithPath("data.numberOfElements").type(JsonFieldType.NUMBER)
                                .description("현재 페이지 요소 수"),
                            fieldWithPath("data.empty").type(JsonFieldType.BOOLEAN).description("빈 페이지 여부")
                        )
                )
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .param("page", "0")
                .param("size", "20")
                .`when`()
                .get("/api/v1/likes/boards")
                .then()
                .statusCode(200)
        }
    }

    @Nested
    @DisplayName("게시판 즐겨찾기")
    inner class LikeBoard {

        private val apiConfig = ApiRequestConfig(
            pathParameters = listOf(
                parameterWithName("boardId").description("즐겨찾기할 게시판 ID")
            ),
            headerDescriptors = AUTH_HEADER_DESCRIPTOR
        )

        @Test
        fun `성공`() {
            val boardId = 1L
            val response = LikeBoardResult(id = 1L, memberId = testMember.id, boardId = boardId)

            `when`(likeBoardUseCase.likeBoard(any())).thenReturn(response)

            val documentFilter = document("like/board-create", 200)
                .request(request().applyConfig(apiConfig))
                .response(
                    response()
                        .responseBodyField(
                            fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                            fieldWithPath("data").type(JsonFieldType.OBJECT).description("생성된 즐겨찾기 정보"),
                            fieldWithPath("data.id").type(JsonFieldType.NUMBER)
                                .description("생성된 즐겨찾기 레코드 ID (게시판 ID 아님). 즐겨찾기 취소는 boardId로 요청"),
                            fieldWithPath("data.memberId").type(JsonFieldType.STRING)
                                .description("즐겨찾기한 회원(요청자 본인)의 아이디 (회원이 설정한 문자열 ID, DB PK 아님). 프로필 설정 전이면 null")
                                .optional(),
                            fieldWithPath("data.boardId").type(JsonFieldType.NUMBER).description("즐겨찾기한 게시판 ID")
                        )
                )
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .`when`()
                .post("/api/v1/likes/boards/{boardId}", boardId)
                .then()
                .statusCode(200)
        }

        @Test
        fun `이미 즐겨찾기한 경우`() {
            val boardId = 1L

            `when`(likeBoardUseCase.likeBoard(any()))
                .thenThrow(LikeException(LikeErrorCode.LIKE_BOARD_ALREADY_LIKED))

            val documentFilter = document("like/board-create", "LIKE_BOARD_ALREADY_LIKED")
                .request(request().applyConfig(apiConfig))
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .`when`()
                .post("/api/v1/likes/boards/{boardId}", boardId)
                .then()
                .statusCode(400)
        }

        @Test
        fun `존재하지 않는 게시판`() {
            val boardId = 999L

            `when`(likeBoardUseCase.likeBoard(any()))
                .thenThrow(BoardException.boardNotFound())

            val documentFilter = document("like/board-create", "BOARD_NOT_FOUND")
                .request(request().applyConfig(apiConfig))
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .`when`()
                .post("/api/v1/likes/boards/{boardId}", boardId)
                .then()
                .statusCode(404)
        }

        @Test
        fun `동시 중복 요청으로 저장이 거부된 경우`() {
            val boardId = 1L

            `when`(likeBoardUseCase.likeBoard(any()))
                .thenThrow(DataIntegrityViolationException("Duplicate entry"))

            val documentFilter = document("like/board-create", "DATA_INTEGRITY_VIOLATION")
                .request(request().applyConfig(apiConfig))
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .`when`()
                .post("/api/v1/likes/boards/{boardId}", boardId)
                .then()
                .statusCode(409)
        }
    }

    @Nested
    @DisplayName("게시판 즐겨찾기 취소")
    inner class UnlikeBoard {

        private val apiConfig = ApiRequestConfig(
            pathParameters = listOf(
                parameterWithName("boardId").description("즐겨찾기 취소할 게시판 ID")
            ),
            headerDescriptors = AUTH_HEADER_DESCRIPTOR
        )

        @Test
        fun `성공`() {
            val boardId = 1L

            doNothing().`when`(likeBoardUseCase).unlikeBoard(any())

            val documentFilter = document("like/board-delete", 200)
                .request(request().applyConfig(apiConfig))
                .response(
                    response()
                        .responseBodyField(
                            fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                            fieldWithPath("data").type(JsonFieldType.OBJECT).description("항상 빈 객체({})")
                        )
                )
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .`when`()
                .delete("/api/v1/likes/boards/{boardId}", boardId)
                .then()
                .statusCode(200)
        }

        @Test
        fun `즐겨찾기한 게시판을 찾을 수 없음`() {
            val boardId = 999L

            `when`(likeBoardUseCase.unlikeBoard(any()))
                .thenThrow(LikeException(LikeErrorCode.LIKE_BOARD_NOT_FOUND))

            val documentFilter = document("like/board-delete", "LIKE_BOARD_NOT_FOUND")
                .request(request().applyConfig(apiConfig))
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .`when`()
                .delete("/api/v1/likes/boards/{boardId}", boardId)
                .then()
                .statusCode(404)
        }
    }

    @Nested
    @DisplayName("피드 좋아요")
    inner class LikeFeed {

        private val apiConfig = ApiRequestConfig(
            pathParameters = listOf(
                parameterWithName("feedId").description("좋아요할 피드 ID")
            ),
            headerDescriptors = AUTH_HEADER_DESCRIPTOR
        )

        @Test
        fun `성공`() {
            val feedId = 10L
            val response = LikeFeedResult(id = 1L, memberId = testMember.id, feedId = feedId)

            `when`(likeFeedUseCase.likeFeed(any())).thenReturn(response)

            val documentFilter = document("like/feed-create", 200)
                .request(request().applyConfig(apiConfig))
                .response(
                    response()
                        .responseBodyField(
                            fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                            fieldWithPath("data").type(JsonFieldType.OBJECT).description("생성된 좋아요 정보"),
                            fieldWithPath("data.id").type(JsonFieldType.NUMBER)
                                .description("생성된 좋아요 레코드 ID (피드 ID 아님). 좋아요 취소는 feedId로 요청"),
                            fieldWithPath("data.memberId").type(JsonFieldType.STRING)
                                .description("좋아요한 회원(요청자 본인)의 아이디 (회원이 설정한 문자열 ID, DB PK 아님). 프로필 설정 전이면 null")
                                .optional(),
                            fieldWithPath("data.feedId").type(JsonFieldType.NUMBER).description("좋아요한 피드 ID")
                        )
                )
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .`when`()
                .post("/api/v1/likes/feeds/{feedId}", feedId)
                .then()
                .statusCode(200)
        }

        @Test
        fun `이미 좋아요한 경우`() {
            val feedId = 10L

            `when`(likeFeedUseCase.likeFeed(any()))
                .thenThrow(LikeException(LikeErrorCode.LIKE_FEED_ALREADY_LIKED))

            val documentFilter = document("like/feed-create", "LIKE_FEED_ALREADY_LIKED")
                .request(request().applyConfig(apiConfig))
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .`when`()
                .post("/api/v1/likes/feeds/{feedId}", feedId)
                .then()
                .statusCode(400)
        }

        @Test
        fun `존재하지 않는 피드`() {
            val feedId = 999L

            `when`(likeFeedUseCase.likeFeed(any()))
                .thenThrow(FeedException.feedNotFound())

            val documentFilter = document("like/feed-create", "FEED_NOT_FOUND")
                .request(request().applyConfig(apiConfig))
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .`when`()
                .post("/api/v1/likes/feeds/{feedId}", feedId)
                .then()
                .statusCode(404)
        }

        @Test
        fun `동시 중복 요청으로 저장이 거부된 경우`() {
            val feedId = 10L

            `when`(likeFeedUseCase.likeFeed(any()))
                .thenThrow(DataIntegrityViolationException("Duplicate entry"))

            val documentFilter = document("like/feed-create", "DATA_INTEGRITY_VIOLATION")
                .request(request().applyConfig(apiConfig))
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .`when`()
                .post("/api/v1/likes/feeds/{feedId}", feedId)
                .then()
                .statusCode(409)
        }
    }

    @Nested
    @DisplayName("피드 좋아요 취소")
    inner class UnlikeFeed {

        private val apiConfig = ApiRequestConfig(
            pathParameters = listOf(
                parameterWithName("feedId").description("좋아요 취소할 피드 ID")
            ),
            headerDescriptors = AUTH_HEADER_DESCRIPTOR
        )

        @Test
        fun `성공`() {
            val feedId = 10L

            doNothing().`when`(likeFeedUseCase).unlikeFeed(any())

            val documentFilter = document("like/feed-delete", 200)
                .request(request().applyConfig(apiConfig))
                .response(
                    response()
                        .responseBodyField(
                            fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                            fieldWithPath("data").type(JsonFieldType.OBJECT).description("항상 빈 객체({})")
                        )
                )
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .`when`()
                .delete("/api/v1/likes/feeds/{feedId}", feedId)
                .then()
                .statusCode(200)
        }

        @Test
        fun `좋아요한 피드를 찾을 수 없음`() {
            val feedId = 999L

            `when`(likeFeedUseCase.unlikeFeed(any()))
                .thenThrow(LikeException(LikeErrorCode.LIKE_FEED_NOT_FOUND))

            val documentFilter = document("like/feed-delete", "LIKE_FEED_NOT_FOUND")
                .request(request().applyConfig(apiConfig))
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .`when`()
                .delete("/api/v1/likes/feeds/{feedId}", feedId)
                .then()
                .statusCode(404)
        }
    }

    @Nested
    @DisplayName("피드 댓글 좋아요")
    inner class LikeFeedComment {

        private val apiConfig = ApiRequestConfig(
            pathParameters = listOf(
                parameterWithName("feedCommentId").description("좋아요할 피드 댓글 ID (답글 ID도 가능)")
            ),
            headerDescriptors = AUTH_HEADER_DESCRIPTOR
        )

        @Test
        fun `성공`() {
            val feedCommentId = 20L
            val response = LikeFeedCommentResult(id = 1L, memberId = testMember.id, feedCommentId = feedCommentId)

            `when`(likeFeedCommentUseCase.likeFeedComment(any())).thenReturn(response)

            val documentFilter = document("like/comment-create", 200)
                .request(request().applyConfig(apiConfig))
                .response(
                    response()
                        .responseBodyField(
                            fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                            fieldWithPath("data").type(JsonFieldType.OBJECT).description("생성된 좋아요 정보"),
                            fieldWithPath("data.id").type(JsonFieldType.NUMBER)
                                .description("생성된 좋아요 레코드 ID (댓글 ID 아님). 좋아요 취소는 feedCommentId로 요청"),
                            fieldWithPath("data.memberId").type(JsonFieldType.STRING)
                                .description("좋아요한 회원(요청자 본인)의 아이디 (회원이 설정한 문자열 ID, DB PK 아님). 프로필 설정 전이면 null")
                                .optional(),
                            fieldWithPath("data.feedCommentId").type(JsonFieldType.NUMBER)
                                .description("좋아요한 피드 댓글(답글) ID")
                        )
                )
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .`when`()
                .post("/api/v1/likes/feed-comments/{feedCommentId}", feedCommentId)
                .then()
                .statusCode(200)
        }

        @Test
        fun `이미 좋아요한 경우`() {
            val feedCommentId = 20L

            `when`(likeFeedCommentUseCase.likeFeedComment(any()))
                .thenThrow(LikeException(LikeErrorCode.LIKE_FEED_COMMENT_ALREADY_LIKED))

            val documentFilter = document("like/comment-create", "LIKE_FEED_COMMENT_ALREADY_LIKED")
                .request(request().applyConfig(apiConfig))
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .`when`()
                .post("/api/v1/likes/feed-comments/{feedCommentId}", feedCommentId)
                .then()
                .statusCode(400)
        }

        @Test
        fun `존재하지 않는 피드 댓글`() {
            val feedCommentId = 999L

            `when`(likeFeedCommentUseCase.likeFeedComment(any()))
                .thenThrow(FeedException.feedCommentNotFound())

            val documentFilter = document("like/comment-create", "FEED_COMMENT_NOT_FOUND")
                .request(request().applyConfig(apiConfig))
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .`when`()
                .post("/api/v1/likes/feed-comments/{feedCommentId}", feedCommentId)
                .then()
                .statusCode(404)
        }

        @Test
        fun `동시 중복 요청으로 저장이 거부된 경우`() {
            val feedCommentId = 20L

            `when`(likeFeedCommentUseCase.likeFeedComment(any()))
                .thenThrow(DataIntegrityViolationException("Duplicate entry"))

            val documentFilter = document("like/comment-create", "DATA_INTEGRITY_VIOLATION")
                .request(request().applyConfig(apiConfig))
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .`when`()
                .post("/api/v1/likes/feed-comments/{feedCommentId}", feedCommentId)
                .then()
                .statusCode(409)
        }
    }

    @Nested
    @DisplayName("피드 댓글 좋아요 취소")
    inner class UnlikeFeedComment {

        private val apiConfig = ApiRequestConfig(
            pathParameters = listOf(
                parameterWithName("feedCommentId").description("좋아요 취소할 피드 댓글 ID (답글 ID도 가능)")
            ),
            headerDescriptors = AUTH_HEADER_DESCRIPTOR
        )

        @Test
        fun `성공`() {
            val feedCommentId = 20L

            doNothing().`when`(likeFeedCommentUseCase).unlikeFeedComment(any())

            val documentFilter = document("like/comment-delete", 200)
                .request(request().applyConfig(apiConfig))
                .response(
                    response()
                        .responseBodyField(
                            fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                            fieldWithPath("data").type(JsonFieldType.OBJECT).description("항상 빈 객체({})")
                        )
                )
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .`when`()
                .delete("/api/v1/likes/feed-comments/{feedCommentId}", feedCommentId)
                .then()
                .statusCode(200)
        }

        @Test
        fun `좋아요한 댓글을 찾을 수 없음`() {
            val feedCommentId = 999L

            `when`(likeFeedCommentUseCase.unlikeFeedComment(any()))
                .thenThrow(LikeException(LikeErrorCode.LIKE_FEED_COMMENT_NOT_FOUND))

            val documentFilter = document("like/comment-delete", "LIKE_FEED_COMMENT_NOT_FOUND")
                .request(request().applyConfig(apiConfig))
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .`when`()
                .delete("/api/v1/likes/feed-comments/{feedCommentId}", feedCommentId)
                .then()
                .statusCode(404)
        }
    }

    @Nested
    @DisplayName("하루 덕담 댓글 좋아요")
    inner class LikeDailyMessageComment {

        private val apiConfig = ApiRequestConfig(
            pathParameters = listOf(
                parameterWithName("dailyMessageCommentId").description("좋아요할 하루 덕담 댓글 ID (답글 ID도 가능)")
            ),
            headerDescriptors = AUTH_HEADER_DESCRIPTOR
        )

        @Test
        fun `성공`() {
            val dailyMessageCommentId = 30L
            val response = LikeDailyMessageCommentResult(
                id = 1L,
                memberId = testMember.id,
                dailyMessageCommentId = dailyMessageCommentId
            )

            `when`(likeDailyMessageCommentUseCase.likeDailyMessageComment(any())).thenReturn(response)

            val documentFilter = document("like/daily-message-comment-create", 200)
                .request(request().applyConfig(apiConfig))
                .response(
                    response()
                        .responseBodyField(
                            fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                            fieldWithPath("data").type(JsonFieldType.OBJECT).description("생성된 좋아요 정보"),
                            fieldWithPath("data.id").type(JsonFieldType.NUMBER)
                                .description("생성된 좋아요 레코드 ID (댓글 ID 아님). 좋아요 취소는 dailyMessageCommentId로 요청"),
                            fieldWithPath("data.memberId").type(JsonFieldType.STRING)
                                .description("좋아요한 회원(요청자 본인)의 아이디 (회원이 설정한 문자열 ID, DB PK 아님). 프로필 설정 전이면 null")
                                .optional(),
                            fieldWithPath("data.dailyMessageCommentId").type(JsonFieldType.NUMBER)
                                .description("좋아요한 하루 덕담 댓글(답글) ID")
                        )
                )
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .`when`()
                .post("/api/v1/likes/daily-message-comments/{dailyMessageCommentId}", dailyMessageCommentId)
                .then()
                .statusCode(200)
        }

        @Test
        fun `이미 좋아요한 경우`() {
            val dailyMessageCommentId = 30L

            `when`(likeDailyMessageCommentUseCase.likeDailyMessageComment(any()))
                .thenThrow(LikeException(LikeErrorCode.LIKE_DAILY_MESSAGE_COMMENT_ALREADY_LIKED))

            val documentFilter =
                document("like/daily-message-comment-create", "LIKE_DAILY_MESSAGE_COMMENT_ALREADY_LIKED")
                    .request(request().applyConfig(apiConfig))
                    .response(RestDocumentationResponse.ERROR_RESPONSE)
                    .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .`when`()
                .post("/api/v1/likes/daily-message-comments/{dailyMessageCommentId}", dailyMessageCommentId)
                .then()
                .statusCode(400)
        }

        @Test
        fun `존재하지 않는 하루 덕담 댓글`() {
            val dailyMessageCommentId = 999L

            `when`(likeDailyMessageCommentUseCase.likeDailyMessageComment(any()))
                .thenThrow(DailyMessageException.dailyMessageCommentNotFound())

            val documentFilter =
                document("like/daily-message-comment-create", "DAILY_MESSAGE_COMMENT_NOT_FOUND")
                    .request(request().applyConfig(apiConfig))
                    .response(RestDocumentationResponse.ERROR_RESPONSE)
                    .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .`when`()
                .post("/api/v1/likes/daily-message-comments/{dailyMessageCommentId}", dailyMessageCommentId)
                .then()
                .statusCode(404)
        }

        @Test
        fun `동시 중복 요청으로 저장이 거부된 경우`() {
            val dailyMessageCommentId = 30L

            `when`(likeDailyMessageCommentUseCase.likeDailyMessageComment(any()))
                .thenThrow(DataIntegrityViolationException("Duplicate entry"))

            val documentFilter =
                document("like/daily-message-comment-create", "DATA_INTEGRITY_VIOLATION")
                    .request(request().applyConfig(apiConfig))
                    .response(RestDocumentationResponse.ERROR_RESPONSE)
                    .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .`when`()
                .post("/api/v1/likes/daily-message-comments/{dailyMessageCommentId}", dailyMessageCommentId)
                .then()
                .statusCode(409)
        }
    }

    @Nested
    @DisplayName("하루 덕담 댓글 좋아요 취소")
    inner class UnlikeDailyMessageComment {

        private val apiConfig = ApiRequestConfig(
            pathParameters = listOf(
                parameterWithName("dailyMessageCommentId")
                    .description("좋아요 취소할 하루 덕담 댓글 ID (답글 ID도 가능)")
            ),
            headerDescriptors = AUTH_HEADER_DESCRIPTOR
        )

        @Test
        fun `성공`() {
            val dailyMessageCommentId = 30L

            doNothing().`when`(likeDailyMessageCommentUseCase).unlikeDailyMessageComment(any())

            val documentFilter = document("like/daily-message-comment-delete", 200)
                .request(request().applyConfig(apiConfig))
                .response(
                    response()
                        .responseBodyField(
                            fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                            fieldWithPath("data").type(JsonFieldType.OBJECT).description("항상 빈 객체({})")
                        )
                )
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .`when`()
                .delete("/api/v1/likes/daily-message-comments/{dailyMessageCommentId}", dailyMessageCommentId)
                .then()
                .statusCode(200)
        }

        @Test
        fun `좋아요한 하루 덕담 댓글을 찾을 수 없음`() {
            val dailyMessageCommentId = 999L

            `when`(likeDailyMessageCommentUseCase.unlikeDailyMessageComment(any()))
                .thenThrow(LikeException(LikeErrorCode.LIKE_DAILY_MESSAGE_COMMENT_NOT_FOUND))

            val documentFilter =
                document("like/daily-message-comment-delete", "LIKE_DAILY_MESSAGE_COMMENT_NOT_FOUND")
                    .request(request().applyConfig(apiConfig))
                    .response(RestDocumentationResponse.ERROR_RESPONSE)
                    .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .`when`()
                .delete("/api/v1/likes/daily-message-comments/{dailyMessageCommentId}", dailyMessageCommentId)
                .then()
                .statusCode(404)
        }
    }
}
