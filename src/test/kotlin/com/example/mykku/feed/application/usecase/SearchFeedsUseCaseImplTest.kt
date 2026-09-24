package com.example.mykku.feed.application.usecase

import com.example.mykku.board.application.port.output.BoardRepository
import com.example.mykku.board.domain.entity.Board
import com.example.mykku.board.domain.vo.BoardId
import com.example.mykku.feed.application.dto.BoardMatchSummary
import com.example.mykku.feed.application.dto.FeedSearchCondition
import com.example.mykku.feed.application.dto.FeedSearchItemResult
import com.example.mykku.feed.application.dto.SearchFeedsQuery
import com.example.mykku.feed.application.port.output.FeedImageRepository
import com.example.mykku.feed.application.port.output.FeedSearchRepository
import com.example.mykku.feed.domain.entity.Feed
import com.example.mykku.feed.domain.vo.FeedId
import com.example.mykku.feed.domain.vo.SearchKeyword
import com.example.mykku.feed.exception.FeedErrorCode
import com.example.mykku.feed.exception.FeedException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
@DisplayName("게시판별 피드 검색 UseCase 단위 테스트")
class SearchFeedsUseCaseImplTest {

    @Mock
    private lateinit var feedSearchRepository: FeedSearchRepository

    @Mock
    private lateinit var boardRepository: BoardRepository

    @Mock
    private lateinit var feedImageRepository: FeedImageRepository

    private lateinit var useCase: SearchFeedsUseCaseImpl

    private val base = LocalDateTime.of(2025, 3, 1, 12, 0)

    @BeforeEach
    fun setUp() {
        val assembler = FeedSearchItemAssembler(feedImageRepository)
        useCase = SearchFeedsUseCaseImpl(feedSearchRepository, boardRepository, assembler)
    }

    @Test
    @DisplayName("검색어를 정규화하고 로그인 회원 id를 조건으로 넘긴다")
    fun `검색 조건 - 정규화된 검색어와 viewerId`() {
        useCase.execute(SearchFeedsQuery(keyword = "  IVE 컴백\n", memberId = 7L))

        verify(feedSearchRepository).summarizeByBoard(FeedSearchCondition(SearchKeyword.of("ive 컴백"), 7L))
    }

    @Test
    @DisplayName("비로그인이면 viewerId가 null인 조건으로 검색한다")
    fun `검색 조건 - 비로그인`() {
        useCase.execute(SearchFeedsQuery(keyword = "ive", memberId = null))

        verify(feedSearchRepository).summarizeByBoard(FeedSearchCondition(SearchKeyword.of("ive"), null))
    }

    @Test
    @DisplayName("매칭이 없으면 빈 boards를 반환하고 게시판과 썸네일을 조회하지 않는다")
    fun `검색 결과 없음`() {
        val result = useCase.execute(SearchFeedsQuery(keyword = "ive", memberId = null))

        assertThat(result.boards).isEmpty()
        verifyNoInteractions(boardRepository, feedImageRepository)
    }

    @Test
    @DisplayName("그룹은 매칭 수 내림차순, 최신 매칭 시각 내림차순, boardId 오름차순으로 정렬한다")
    fun `그룹 순서 - 동점 규칙`() {
        stubSummaries(
            summary(boardId = 3L, count = 2L),
            summary(boardId = 4L, count = 2L, latestCreatedAt = base.plusDays(1)),
            summary(boardId = 1L, count = 5L, latestCreatedAt = base.minusDays(1)),
            summary(boardId = 2L, count = 2L, latestCreatedAt = base.plusDays(1))
        )
        stubBoards(1L, 2L, 3L, 4L)

        val result = search()

        assertThat(result.boards.map { it.boardId }).containsExactly(1L, 2L, 4L, 3L)
    }

    @Test
    @DisplayName("게시판마다 미리보기 3건을 요청하고 전체 매칭 수로 hasMore를 계산한다")
    fun `미리보기 - 3건과 hasMore`() {
        stubSummaries(summary(boardId = 1L, count = 5L), summary(boardId = 2L, count = 1L))
        stubBoards(1L, 2L)
        stubPreview(1L, 13L, 12L, 11L)
        stubPreview(2L, 21L)

        val result = search()

        assertThat(result.boards.map { Triple(it.boardId, it.totalCount, it.hasMore) })
            .containsExactly(Triple(1L, 5L, true), Triple(2L, 1L, false))
        assertThat(result.boards[0].feeds.map { it.id }).containsExactly(13L, 12L, 11L)
    }

    @Test
    @DisplayName("그룹에 게시판 정보와 썸네일이 담긴 카드를 채우고 썸네일은 한 번만 조회한다")
    fun `그룹 조립 - 게시판 정보와 카드`() {
        stubSummaries(summary(boardId = 1L, count = 1L), summary(boardId = 2L, count = 1L, base.minusDays(1)))
        stubBoards(1L, 2L)
        stubPreview(1L, 10L)
        stubPreview(2L, 20L)
        whenever(feedImageRepository.findThumbnailUrlsByFeedIds(any()))
            .thenReturn(mapOf(FeedId.of(10L) to "https://img/10.jpg"))

        val result = search()

        val first = result.boards[0]
        assertThat(listOf(first.boardTitle, first.boardLogo)).containsExactly("게시판1", "https://logo/1.png")
        assertThat(first.feeds).containsExactly(stubItem(10L, 1L, "https://img/10.jpg"))
        assertThat(result.boards[1].feeds).containsExactly(stubItem(20L, 2L, null))
        verify(feedImageRepository, times(1)).findThumbnailUrlsByFeedIds(any())
    }

    @Test
    @DisplayName("게시판이 조회되지 않는 그룹은 건너뛰고 미리보기도 조회하지 않는다")
    fun `그룹 조립 - 없는 게시판 건너뛰기`() {
        stubSummaries(summary(boardId = 9L, count = 3L), summary(boardId = 1L, count = 1L))
        stubBoards(1L)

        val result = search()

        assertThat(result.boards.map { it.boardId }).containsExactly(1L)
        verify(feedSearchRepository, never()).findTopByBoard(condition(), 9L, 3)
    }

    @Test
    @DisplayName("공백만 있는 검색어는 SEARCH_KEYWORD_EMPTY 예외를 던지고 검색하지 않는다")
    fun `검색어 검증 - 빈 검색어`() {
        val exception = assertThrows<FeedException> {
            useCase.execute(SearchFeedsQuery(keyword = "   ", memberId = 7L))
        }

        assertThat(exception.errorCode).isEqualTo(FeedErrorCode.SEARCH_KEYWORD_EMPTY)
        verifyNoInteractions(feedSearchRepository, boardRepository)
    }

    @Test
    @DisplayName("51자 검색어는 SEARCH_KEYWORD_TOO_LONG 예외를 던지고 검색하지 않는다")
    fun `검색어 검증 - 너무 긴 검색어`() {
        val exception = assertThrows<FeedException> {
            useCase.execute(SearchFeedsQuery(keyword = "가".repeat(SearchKeyword.MAX_LENGTH + 1), memberId = null))
        }

        assertThat(exception.errorCode).isEqualTo(FeedErrorCode.SEARCH_KEYWORD_TOO_LONG)
        verifyNoInteractions(feedSearchRepository, boardRepository)
    }

    private fun condition(): FeedSearchCondition = FeedSearchCondition(SearchKeyword.of("ive"), null)

    private fun search() = useCase.execute(SearchFeedsQuery(keyword = "ive", memberId = null))

    private fun summary(boardId: Long, count: Long, latestCreatedAt: LocalDateTime = base): BoardMatchSummary =
        BoardMatchSummary(boardId = boardId, count = count, latestCreatedAt = latestCreatedAt)

    private fun stubSummaries(vararg summaries: BoardMatchSummary) {
        whenever(feedSearchRepository.summarizeByBoard(condition())).thenReturn(summaries.toList())
    }

    private fun stubBoards(vararg ids: Long) {
        whenever(boardRepository.findByIds(any())).thenReturn(ids.map { stubBoard(it) })
    }

    private fun stubPreview(boardId: Long, vararg feedIds: Long) {
        whenever(feedSearchRepository.findTopByBoard(condition(), boardId, 3))
            .thenReturn(feedIds.map { stubFeed(it, boardId) })
    }

    private fun stubItem(id: Long, boardId: Long, thumbnailUrl: String?): FeedSearchItemResult =
        FeedSearchItemResult(
            id = id,
            boardId = boardId,
            boardTitle = "게시판$boardId",
            title = "제목$id",
            content = "내용$id",
            thumbnailUrl = thumbnailUrl,
            createdAt = base
        )

    private fun stubFeed(id: Long, boardId: Long): Feed =
        Feed.reconstitute(
            id = id,
            title = "제목$id",
            content = "내용$id",
            likeCount = 0,
            commentCount = 0,
            boardId = boardId,
            memberId = null,
            createdAt = base,
            updatedAt = base
        )

    private fun stubBoard(id: Long): Board =
        Board.reconstitute(
            id = BoardId(id),
            title = "게시판$id",
            logo = "https://logo/$id.png",
            createdAt = base,
            updatedAt = base
        )
}
