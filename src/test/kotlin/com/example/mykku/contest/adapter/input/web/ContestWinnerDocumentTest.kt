package com.example.mykku.contest.adapter.input.web

import com.example.mykku.BaseDocumentTest
import com.example.mykku.contest.application.dto.ContestWinnerAnnouncementResult
import com.example.mykku.contest.application.dto.ContestWinnerDetailResult
import com.example.mykku.contest.application.dto.ContestWinnerPreviewResult
import com.example.mykku.contest.application.dto.ContestWinnersListResult
import com.example.mykku.contest.application.dto.MyAwardContestResult
import com.example.mykku.contest.application.dto.MyAwardPreviewResult
import com.example.mykku.contest.application.dto.MyWinnerStatusResult
import com.example.mykku.contest.application.dto.PagedMyAwardsResult
import com.example.mykku.contest.application.dto.UpdateAcceptanceSpeechResult
import com.example.mykku.contest.application.dto.WinnerDetailResult
import com.example.mykku.contest.application.dto.WinnerThumbnailResult
import com.example.mykku.feed.application.dto.AuthorResult
import com.example.mykku.feed.application.dto.CommentPreviewResult
import com.example.mykku.feed.application.dto.FeedImageResult
import com.example.mykku.feed.application.dto.FeedResult
import com.example.mykku.feed.application.dto.PagedFeedsResult
import com.example.mykku.feed.application.dto.TagResult
import com.example.mykku.role.application.dto.RoleResult
import java.time.LocalDate
import java.time.LocalDateTime
import com.example.mykku.docs.ApiRequestConfig
import com.example.mykku.docs.RestDocumentationResponse
import com.example.mykku.contest.exception.ContestErrorCode
import com.example.mykku.contest.exception.ContestException
import io.restassured.http.ContentType
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName

class ContestWinnerDocumentTest : BaseDocumentTest() {

    @Nested
    @DisplayName("수상작 목록 조회")
    inner class GetContestsWithWinners {

        private val apiConfig = ApiRequestConfig()

        @Test
        fun `성공`() {
            val result = ContestWinnersListResult(
                contests = listOf(
                    ContestWinnerPreviewResult(
                        contestId = 1L,
                        contestTitle = "첫 번째 콘테스트",
                        startedAt = LocalDateTime.now().minusDays(30),
                        expiredAt = LocalDateTime.now().minusDays(1),
                        winners = listOf(
                            WinnerThumbnailResult(
                                winnerId = 1L,
                                winnerRank = 1,
                                feedImageUrl = "https://example.com/image1.jpg"
                            ),
                            WinnerThumbnailResult(
                                winnerId = 2L,
                                winnerRank = 2,
                                feedImageUrl = "https://example.com/image2.jpg"
                            ),
                            WinnerThumbnailResult(
                                winnerId = 3L,
                                winnerRank = 3,
                                feedImageUrl = null
                            )
                        )
                    ),
                    ContestWinnerPreviewResult(
                        contestId = 2L,
                        contestTitle = "두 번째 콘테스트",
                        startedAt = LocalDateTime.now().minusDays(60),
                        expiredAt = LocalDateTime.now().minusDays(31),
                        winners = listOf(
                            WinnerThumbnailResult(
                                winnerId = 4L,
                                winnerRank = 1,
                                feedImageUrl = "https://example.com/image4.jpg"
                            )
                        )
                    )
                )
            )

            `when`(getContestWinnersListUseCase.execute()).thenReturn(result)

            val documentFilter = document("contest-winner/list", 200)
                .request(request().applyConfig(apiConfig))
                .response(
                    response()
                        .responseBodyField(
                            fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                            fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터"),
                            fieldWithPath("data.contests[]").type(JsonFieldType.ARRAY).description("콘테스트 목록"),
                            fieldWithPath("data.contests[].contestId").type(JsonFieldType.NUMBER)
                                .description("콘테스트 ID"),
                            fieldWithPath("data.contests[].contestTitle").type(JsonFieldType.STRING)
                                .description("콘테스트 제목"),
                            fieldWithPath("data.contests[].startedAt").type(JsonFieldType.STRING)
                                .description("콘테스트 시작일시"),
                            fieldWithPath("data.contests[].expiredAt").type(JsonFieldType.STRING)
                                .description("콘테스트 종료일시"),
                            fieldWithPath("data.contests[].winners[]").type(JsonFieldType.ARRAY).description("수상자 목록"),
                            fieldWithPath("data.contests[].winners[].winnerId").type(JsonFieldType.NUMBER)
                                .description("수상자 ID"),
                            fieldWithPath("data.contests[].winners[].winnerRank").type(JsonFieldType.NUMBER)
                                .description("순위"),
                            fieldWithPath("data.contests[].winners[].feedImageUrl").type(JsonFieldType.STRING)
                                .description("피드 이미지 URL").optional()
                        )
                )
                .build()

            given(documentFilter)
                .contentType(ContentType.JSON)
                .`when`()
                .get("/api/v1/contests/winners")
                .then()
                .statusCode(200)
        }
    }

    @Nested
    @DisplayName("수상작 상세 조회")
    inner class GetContestWinnerDetail {

        private val apiConfig = ApiRequestConfig(
            pathParameters = listOf(
                parameterWithName("contestId").description("콘테스트 ID")
            )
        )

        @Test
        fun `성공`() {
            val contestId = 1L
            val result = ContestWinnerDetailResult(
                contestId = contestId,
                contestTitle = "테스트 콘테스트",
                winners = listOf(
                    WinnerDetailResult(
                        winnerId = 1L,
                        winnerRank = 1,
                        awardTitle = "최우수상",
                        feedId = 10L,
                        feedTitle = "1등 작품",
                        feedImageUrl = "https://example.com/image1.jpg",
                        authorNickname = "user1",
                        authorProfileImage = "https://example.com/profile1.jpg",
                        description = "1등 수상 설명",
                        acceptanceSpeech = "감사합니다!"
                    ),
                    WinnerDetailResult(
                        winnerId = 2L,
                        winnerRank = 2,
                        awardTitle = "우수상",
                        feedId = 20L,
                        feedTitle = "2등 작품",
                        feedImageUrl = "https://example.com/image2.jpg",
                        authorNickname = "user2",
                        authorProfileImage = null,
                        description = "2등 수상 설명",
                        acceptanceSpeech = ""
                    )
                )
            )

            `when`(getContestWinnerDetailUseCase.execute(contestId)).thenReturn(result)

            val documentFilter = document("contest-winner/detail", 200)
                .request(request().applyConfig(apiConfig))
                .response(
                    response()
                        .responseBodyField(
                            fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                            fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터"),
                            fieldWithPath("data.contestId").type(JsonFieldType.NUMBER).description("콘테스트 ID"),
                            fieldWithPath("data.contestTitle").type(JsonFieldType.STRING).description("콘테스트 제목"),
                            fieldWithPath("data.winners[]").type(JsonFieldType.ARRAY).description("수상자 목록"),
                            fieldWithPath("data.winners[].winnerId").type(JsonFieldType.NUMBER).description("수상자 ID"),
                            fieldWithPath("data.winners[].winnerRank").type(JsonFieldType.NUMBER).description("순위"),
                            fieldWithPath("data.winners[].awardTitle").type(JsonFieldType.STRING)
                                .description("수상명 (예: 최우수상, 미입력 시 null)").optional(),
                            fieldWithPath("data.winners[].feedId").type(JsonFieldType.NUMBER).description("피드 ID"),
                            fieldWithPath("data.winners[].feedTitle").type(JsonFieldType.STRING).description("피드 제목"),
                            fieldWithPath("data.winners[].feedImageUrl").type(JsonFieldType.STRING)
                                .description("피드 이미지 URL").optional(),
                            fieldWithPath("data.winners[].authorNickname").type(JsonFieldType.STRING)
                                .description("작성자 닉네임"),
                            fieldWithPath("data.winners[].authorProfileImage").type(JsonFieldType.STRING)
                                .description("작성자 프로필 이미지").optional(),
                            fieldWithPath("data.winners[].description").type(JsonFieldType.STRING).description("수상 설명"),
                            fieldWithPath("data.winners[].acceptanceSpeech").type(JsonFieldType.STRING)
                                .description("수상 소감")
                        )
                )
                .build()

            given(documentFilter)
                .contentType(ContentType.JSON)
                .`when`()
                .get("/api/v1/contests/{contestId}/winners", contestId)
                .then()
                .statusCode(200)
        }

        @Test
        fun `콘테스트 없음`() {
            val contestId = 999L

            `when`(getContestWinnerDetailUseCase.execute(contestId))
                .thenThrow(ContestException(ContestErrorCode.CONTEST_NOT_FOUND))

            val documentFilter = document("contest-winner/detail", "CONTEST_NOT_FOUND")
                .request(request().applyConfig(apiConfig))
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            given(documentFilter)
                .contentType(ContentType.JSON)
                .`when`()
                .get("/api/v1/contests/{contestId}/winners", contestId)
                .then()
                .statusCode(404)
        }
    }

    @Nested
    @DisplayName("수상자 발표 공지 조회")
    inner class GetContestWinnerAnnouncement {

        private val apiConfig = ApiRequestConfig(
            pathParameters = listOf(
                parameterWithName("contestId").description("콘테스트 ID")
            )
        )

        @Test
        fun `성공`() {
            val contestId = 1L
            val result = ContestWinnerAnnouncementResult(
                contestId = contestId,
                contestTitle = "봄맞이 콘테스트",
                title = "[봄맞이 콘테스트] 수상자 발표",
                content = "참여해 주신 모든 분들께 감사드립니다.\n대상: OOO\n최우수상: OOO",
                announcedAt = LocalDate.of(2025, 10, 10)
            )

            `when`(getContestWinnerAnnouncementUseCase.execute(contestId)).thenReturn(result)

            val documentFilter = document("contest-winner/announcement", 200)
                .request(request().applyConfig(apiConfig))
                .response(
                    response()
                        .responseBodyField(
                            fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                            fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터"),
                            fieldWithPath("data.contestId").type(JsonFieldType.NUMBER).description("콘테스트 ID"),
                            fieldWithPath("data.contestTitle").type(JsonFieldType.STRING).description("콘테스트 제목"),
                            fieldWithPath("data.title").type(JsonFieldType.STRING).description("공지 제목"),
                            fieldWithPath("data.content").type(JsonFieldType.STRING).description("공지 본문"),
                            fieldWithPath("data.announcedAt").type(JsonFieldType.STRING).description("발표일 (yyyy-MM-dd)")
                        )
                )
                .build()

            given(documentFilter)
                .contentType(ContentType.JSON)
                .`when`()
                .get("/api/v1/contests/{contestId}/winner-announcement", contestId)
                .then()
                .statusCode(200)
        }

        @Test
        fun `공지 없음`() {
            val contestId = 999L

            `when`(getContestWinnerAnnouncementUseCase.execute(contestId))
                .thenThrow(ContestException(ContestErrorCode.WINNER_ANNOUNCEMENT_NOT_FOUND))

            val documentFilter = document("contest-winner/announcement", "WINNER_ANNOUNCEMENT_NOT_FOUND")
                .request(request().applyConfig(apiConfig))
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            given(documentFilter)
                .contentType(ContentType.JSON)
                .`when`()
                .get("/api/v1/contests/{contestId}/winner-announcement", contestId)
                .then()
                .statusCode(404)
        }
    }

    @Nested
    @DisplayName("수상 여부 조회")
    inner class GetMyWinnerStatus {

        private val apiConfig = ApiRequestConfig(
            pathParameters = listOf(
                parameterWithName("contestId").description("콘테스트 ID")
            ),
            headerDescriptors = AUTH_HEADER_DESCRIPTOR
        )

        @Test
        fun `성공 - 수상자`() {
            val contestId = 1L
            val result = MyWinnerStatusResult(
                isWinner = true,
                winnerId = 5L,
                winnerRank = 1
            )

            `when`(getMyWinnerStatusUseCase.execute(any())).thenReturn(result)

            val documentFilter = document("contest-winner/my-status", 200)
                .request(request().applyConfig(apiConfig))
                .response(
                    response()
                        .responseBodyField(
                            fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                            fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터"),
                            fieldWithPath("data.isWinner").type(JsonFieldType.BOOLEAN).description("수상 여부"),
                            fieldWithPath("data.winnerId").type(JsonFieldType.NUMBER).description("수상자 ID").optional(),
                            fieldWithPath("data.winnerRank").type(JsonFieldType.NUMBER).description("수상 순위").optional()
                        )
                )
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .`when`()
                .get("/api/v1/contests/{contestId}/my-winner-status", contestId)
                .then()
                .statusCode(200)
        }

        @Test
        fun `콘테스트 없음`() {
            val contestId = 999L

            `when`(getMyWinnerStatusUseCase.execute(any()))
                .thenThrow(ContestException(ContestErrorCode.CONTEST_NOT_FOUND))

            val documentFilter = document("contest-winner/my-status", "CONTEST_NOT_FOUND")
                .request(request().applyConfig(apiConfig))
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .`when`()
                .get("/api/v1/contests/{contestId}/my-winner-status", contestId)
                .then()
                .statusCode(404)
        }

        @Test
        fun `수상자 미발표`() {
            val contestId = 1L

            `when`(getMyWinnerStatusUseCase.execute(any()))
                .thenThrow(ContestException(ContestErrorCode.WINNER_NOT_ANNOUNCED))

            val documentFilter = document("contest-winner/my-status", "WINNER_NOT_ANNOUNCED")
                .request(request().applyConfig(apiConfig))
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .`when`()
                .get("/api/v1/contests/{contestId}/my-winner-status", contestId)
                .then()
                .statusCode(400)
        }
    }

    @Nested
    @DisplayName("내 수상 콘테스트 목록 조회")
    inner class GetMyAwardContests {

        private val apiConfig = ApiRequestConfig(
            queryParameters = listOf(
                parameterWithName("page").description("페이지 번호 (0부터 시작)").optional(),
                parameterWithName("size").description("페이지 크기 (기본 20)").optional()
            ),
            headerDescriptors = AUTH_HEADER_DESCRIPTOR
        )

        @Test
        fun `성공`() {
            val result = PagedMyAwardsResult(
                content = listOf(
                    MyAwardContestResult(
                        contestId = 1L,
                        contestTitle = "첫 번째 콘테스트",
                        thumbnailUrl = "https://example.com/thumbnail1.jpg",
                        winnerRank = 1,
                        awardTitle = "최우수상",
                        acceptanceSpeech = "감사합니다!",
                        feedId = 101L,
                        feedTitle = "수상작 피드 제목",
                        feedImageUrl = "https://example.com/feed1.jpg"
                    ),
                    MyAwardContestResult(
                        contestId = 2L,
                        contestTitle = "두 번째 콘테스트",
                        thumbnailUrl = "https://example.com/thumbnail2.jpg",
                        winnerRank = 2,
                        awardTitle = "우수상",
                        acceptanceSpeech = "",
                        feedId = 102L,
                        feedTitle = "두 번째 수상작",
                        feedImageUrl = null
                    )
                ),
                page = 0,
                size = 20,
                totalElements = 2,
                totalPages = 1,
                isLast = true
            )

            `when`(getMyAwardContestsUseCase.execute(any())).thenReturn(result)

            val documentFilter = document("contest-winner/my-awards", 200)
                .request(request().applyConfig(apiConfig))
                .response(
                    response()
                        .responseBodyField(
                            fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                            fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터"),
                            fieldWithPath("data.content[]").type(JsonFieldType.ARRAY).description("수상 콘테스트 목록"),
                            fieldWithPath("data.content[].contestId").type(JsonFieldType.NUMBER).description("콘테스트 ID"),
                            fieldWithPath("data.content[].contestTitle").type(JsonFieldType.STRING).description("콘테스트 제목"),
                            fieldWithPath("data.content[].thumbnailUrl").type(JsonFieldType.STRING).description("콘테스트 썸네일 URL"),
                            fieldWithPath("data.content[].winnerRank").type(JsonFieldType.NUMBER).description("수상 순위"),
                            fieldWithPath("data.content[].awardTitle").type(JsonFieldType.STRING)
                                .description("수상명 (예: 최우수상, 미입력 시 null)").optional(),
                            fieldWithPath("data.content[].acceptanceSpeech").type(JsonFieldType.STRING).description("수상 소감"),
                            fieldWithPath("data.content[].feedId").type(JsonFieldType.NUMBER).description("피드 ID"),
                            fieldWithPath("data.content[].feedTitle").type(JsonFieldType.STRING).description("피드 제목"),
                            fieldWithPath("data.content[].feedImageUrl").type(JsonFieldType.STRING).description("피드 이미지 URL").optional(),
                            fieldWithPath("data.page").type(JsonFieldType.NUMBER).description("현재 페이지"),
                            fieldWithPath("data.size").type(JsonFieldType.NUMBER).description("페이지 크기"),
                            fieldWithPath("data.totalElements").type(JsonFieldType.NUMBER).description("전체 항목 수"),
                            fieldWithPath("data.totalPages").type(JsonFieldType.NUMBER).description("전체 페이지 수"),
                            fieldWithPath("data.isLast").type(JsonFieldType.BOOLEAN).description("마지막 페이지 여부")
                        )
                )
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .`when`()
                .get("/api/v1/contests/my-awards")
                .then()
                .statusCode(200)
        }
    }

    @Nested
    @DisplayName("내 수상 피드 목록 조회")
    inner class GetMyAwardFeeds {

        private val apiConfig = ApiRequestConfig(
            queryParameters = listOf(
                parameterWithName("page").description("페이지 번호 (0부터 시작)").optional(),
                parameterWithName("size").description("페이지 크기 (기본 20)").optional()
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
                        title = "수상작 피드",
                        content = "수상작 내용입니다.",
                        images = listOf(
                            FeedImageResult(1L, "https://example.com/feed-image.jpg", 1080, 1080)
                        ),
                        tags = listOf(
                            TagResult("콘테스트태그", true)
                        ),
                        likeCount = 10,
                        isLiked = false,
                        commentCount = 3,
                        comment = CommentPreviewResult(
                            profileImage = "https://example.com/commenter.jpg",
                            content = "축하합니다!"
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

            `when`(getMyAwardFeedsUseCase.execute(any())).thenReturn(result)

            val documentFilter = document("contest-winner/my-award-feeds", 200)
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
                            fieldWithPath("data.feeds[].author.profileImage").type(JsonFieldType.STRING).description("작성자 프로필 이미지"),
                            fieldWithPath("data.feeds[].author.role").type(JsonFieldType.OBJECT).description("작성자 역할").optional(),
                            fieldWithPath("data.feeds[].author.role.id").type(JsonFieldType.NUMBER).description("역할 ID"),
                            fieldWithPath("data.feeds[].author.role.name").type(JsonFieldType.STRING).description("역할 이름"),
                            fieldWithPath("data.feeds[].author.role.description").type(JsonFieldType.STRING).description("역할 설명"),
                            fieldWithPath("data.feeds[].board").type(JsonFieldType.STRING).description("게시판 이름"),
                            fieldWithPath("data.feeds[].createdAt").type(JsonFieldType.STRING).description("생성일시"),
                            fieldWithPath("data.feeds[].title").type(JsonFieldType.STRING).description("피드 제목"),
                            fieldWithPath("data.feeds[].content").type(JsonFieldType.STRING).description("피드 내용"),
                            fieldWithPath("data.feeds[].images[]").type(JsonFieldType.ARRAY).description("이미지 목록"),
                            fieldWithPath("data.feeds[].images[].id").type(JsonFieldType.NUMBER).description("이미지 ID"),
                            fieldWithPath("data.feeds[].images[].url").type(JsonFieldType.STRING).description("이미지 URL"),
                            fieldWithPath("data.feeds[].images[].width").type(JsonFieldType.NUMBER).description("이미지 너비"),
                            fieldWithPath("data.feeds[].images[].height").type(JsonFieldType.NUMBER).description("이미지 높이"),
                            fieldWithPath("data.feeds[].tags[]").type(JsonFieldType.ARRAY).description("태그 목록"),
                            fieldWithPath("data.feeds[].tags[].title").type(JsonFieldType.STRING).description("태그 제목"),
                            fieldWithPath("data.feeds[].tags[].isContest").type(JsonFieldType.BOOLEAN).description("콘테스트 태그 여부"),
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
                .`when`()
                .get("/api/v1/contests/my-awards/feeds")
                .then()
                .statusCode(200)
        }
    }

    @Nested
    @DisplayName("내 수상 미리보기 조회")
    inner class GetMyAwardsPreview {

        private val apiConfig = ApiRequestConfig(
            headerDescriptors = AUTH_HEADER_DESCRIPTOR
        )

        @Test
        fun `성공`() {
            val result = listOf(
                MyAwardPreviewResult(contestId = 1L, thumbnailUrl = "https://example.com/thumb1.jpg"),
                MyAwardPreviewResult(contestId = 2L, thumbnailUrl = "https://example.com/thumb2.jpg"),
                MyAwardPreviewResult(contestId = 3L, thumbnailUrl = "https://example.com/thumb3.jpg")
            )

            `when`(getMyAwardsPreviewUseCase.execute(any())).thenReturn(result)

            val documentFilter = document("contest-winner/my-awards-preview", 200)
                .request(request().applyConfig(apiConfig))
                .response(
                    response()
                        .responseBodyField(
                            fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                            fieldWithPath("data[]").type(JsonFieldType.ARRAY).description("미리보기 목록"),
                            fieldWithPath("data[].contestId").type(JsonFieldType.NUMBER).description("콘테스트 ID"),
                            fieldWithPath("data[].thumbnailUrl").type(JsonFieldType.STRING).description("콘테스트 썸네일 URL")
                        )
                )
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .`when`()
                .get("/api/v1/contests/my-awards/preview")
                .then()
                .statusCode(200)
        }
    }

    @Nested
    @DisplayName("수상 소감 등록")
    inner class UpdateAcceptanceSpeech {

        private val apiConfig = ApiRequestConfig(
            pathParameters = listOf(
                parameterWithName("winnerId").description("수상자 ID")
            ),
            requestBodyFields = listOf(
                fieldWithPath("acceptanceSpeech").type(JsonFieldType.STRING).description("수상 소감 (최대 1000자)")
            ),
            headerDescriptors = AUTH_HEADER_DESCRIPTOR
        )

        @Test
        fun `성공`() {
            val winnerId = 1L
            val request = UpdateAcceptanceSpeechRequest(acceptanceSpeech = "정말 감사합니다. 이 영광을 가족에게 돌립니다.")
            val result = UpdateAcceptanceSpeechResult(winnerId = winnerId, acceptanceSpeech = request.acceptanceSpeech)

            `when`(updateAcceptanceSpeechUseCase.execute(any())).thenReturn(result)

            val documentFilter = document("contest-winner/update-acceptance-speech", 200)
                .request(request().applyConfig(apiConfig))
                .response(
                    response()
                        .responseBodyField(
                            fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                            fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터"),
                            fieldWithPath("data.winnerId").type(JsonFieldType.NUMBER).description("수상자 ID"),
                            fieldWithPath("data.acceptanceSpeech").type(JsonFieldType.STRING).description("수상 소감")
                        )
                )
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
                .`when`()
                .patch("/api/v1/contests/winners/{winnerId}/acceptance-speech", winnerId)
                .then()
                .statusCode(200)
        }

        @Test
        fun `수상자 아님`() {
            val winnerId = 1L
            val request = UpdateAcceptanceSpeechRequest(acceptanceSpeech = "소감입니다.")

            `when`(updateAcceptanceSpeechUseCase.execute(any()))
                .thenThrow(ContestException(ContestErrorCode.NOT_WINNER_OWNER))

            val documentFilter = document("contest-winner/update-acceptance-speech", "NOT_WINNER_OWNER")
                .request(request().applyConfig(apiConfig))
                .response(RestDocumentationResponse.ERROR_RESPONSE)
                .build()

            given(documentFilter)
                .headers(AUTH_HEADER)
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(request))
                .`when`()
                .patch("/api/v1/contests/winners/{winnerId}/acceptance-speech", winnerId)
                .then()
                .statusCode(403)
        }
    }
}
