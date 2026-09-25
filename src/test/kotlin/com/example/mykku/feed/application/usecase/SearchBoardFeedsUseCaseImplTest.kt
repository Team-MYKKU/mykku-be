package com.example.mykku.feed.application.usecase

import com.example.mykku.board.application.port.output.BoardRepository
import com.example.mykku.board.domain.entity.Board
import com.example.mykku.board.domain.vo.BoardId
import com.example.mykku.board.exception.BoardErrorCode
import com.example.mykku.board.exception.BoardException
import com.example.mykku.feed.application.dto.FeedSearchCondition
import com.example.mykku.feed.application.dto.FeedSearchItemResult
import com.example.mykku.feed.application.dto.SearchBoardFeedsQuery
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
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
@DisplayName("게시판 안 피드 검색 UseCase 단위 테스트")
class SearchBoardFeedsUseCaseImplTest {

    @Mock
    private lateinit var feedSearchRepository: FeedSearchRepository

    @Mock
    private lateinit var boardRepository: BoardRepository

    @Mock
    private lateinit var feedImageRepository: FeedImageRepository

    private lateinit var useCase: SearchBoardFeedsUseCaseImpl

    private val base = LocalDateTime.of(2025, 3, 1, 12, 0)

    @BeforeEach
    fun setUp() {
        val assembler = FeedSearchItemAssembler(feedImageRepository)
        useCase = SearchBoardFeedsUseCaseImpl(feedSearchRepository, boardRepository, assembler)
    }

    @Test
    @DisplayName("게시판 안 매칭 피드를 카드와 페이지 정보로 반환한다")
    fun `게시판 검색 - 카드와 페이지 정보`() {
        val pageable = PageRequest.of(0, 2)
        val condition = FeedSearchCondition(SearchKeyword.of("ive"), 7L)
        whenever(boardRepository.findById(BoardId(2L))).thenReturn(stubBoard(2L))
        whenever(feedSearchRepository.findByBoard(condition, 2L, pageable))
            .thenReturn(PageImpl(listOf(stubFeed(12L), stubFeed(11L)), pageable, 5L))
        whenever(feedImageRepository.findThumbnailUrlsByFeedIds(listOf(FeedId.of(12L), FeedId.of(11L))))
            .thenReturn(mapOf(FeedId.of(11L) to "https://img/11.jpg"))

        val result = useCase.execute(SearchBoardFeedsQuery("  IVE ", 2L, 7L, pageable))

        assertThat(result.feeds).containsExactly(stubItem(12L, null), stubItem(11L, "https://img/11.jpg"))
        assertThat(listOf(result.currentPage, result.totalPages, result.size)).containsExactly(0, 3, 2)
        assertThat(listOf(result.totalElements, result.hasNext, result.hasPrevious)).containsExactly(5L, true, false)
    }

    @Test
    @DisplayName("비로그인이면 viewerId가 null인 조건으로 검색한다")
    fun `게시판 검색 - 비로그인`() {
        val pageable = PageRequest.of(0, 20)
        val condition = FeedSearchCondition(SearchKeyword.of("ive"), null)
        whenever(boardRepository.findById(BoardId(2L))).thenReturn(stubBoard(2L))
        whenever(feedSearchRepository.findByBoard(condition, 2L, pageable))
            .thenReturn(PageImpl(emptyList(), pageable, 0L))

        useCase.execute(SearchBoardFeedsQuery("ive", 2L, null, pageable))

        verify(feedSearchRepository).findByBoard(condition, 2L, pageable)
    }

    @Test
    @DisplayName("마지막 페이지를 넘기면 빈 목록과 정확한 전체 수를 반환하고 썸네일을 조회하지 않는다")
    fun `게시판 검색 - 범위 밖 페이지`() {
        val pageable = PageRequest.of(5, 20)
        val condition = FeedSearchCondition(SearchKeyword.of("ive"), null)
        whenever(boardRepository.findById(BoardId(2L))).thenReturn(stubBoard(2L))
        whenever(feedSearchRepository.findByBoard(condition, 2L, pageable))
            .thenReturn(PageImpl(emptyList(), pageable, 12L))

        val result = useCase.execute(SearchBoardFeedsQuery("ive", 2L, null, pageable))

        assertThat(result.feeds).isEmpty()
        assertThat(listOf(result.totalElements, result.totalPages.toLong(), result.currentPage.toLong()))
            .containsExactly(12L, 1L, 5L)
        assertThat(result.hasNext).isFalse()
        verifyNoInteractions(feedImageRepository)
    }

    @Test
    @DisplayName("없는 게시판이면 BOARD_NOT_FOUND 예외를 던지고 검색하지 않는다")
    fun `게시판 검색 - 없는 게시판`() {
        val exception = assertThrows<BoardException> {
            useCase.execute(SearchBoardFeedsQuery("ive", 99L, null, PageRequest.of(0, 20)))
        }

        assertThat(exception.errorCode).isEqualTo(BoardErrorCode.BOARD_NOT_FOUND)
        verifyNoInteractions(feedSearchRepository, feedImageRepository)
    }

    @Test
    @DisplayName("빈 검색어는 게시판 존재 여부보다 먼저 SEARCH_KEYWORD_EMPTY로 거절한다")
    fun `검색어 검증 - 빈 검색어가 없는 게시판보다 우선`() {
        val exception = assertThrows<FeedException> {
            useCase.execute(SearchBoardFeedsQuery(" \n ", 99L, null, PageRequest.of(0, 20)))
        }

        assertThat(exception.errorCode).isEqualTo(FeedErrorCode.SEARCH_KEYWORD_EMPTY)
        verifyNoInteractions(boardRepository, feedSearchRepository)
    }

    @Test
    @DisplayName("51자 검색어는 게시판 존재 여부보다 먼저 SEARCH_KEYWORD_TOO_LONG으로 거절한다")
    fun `검색어 검증 - 너무 긴 검색어가 없는 게시판보다 우선`() {
        val keyword = "a".repeat(SearchKeyword.MAX_LENGTH + 1)

        val exception = assertThrows<FeedException> {
            useCase.execute(SearchBoardFeedsQuery(keyword, 99L, null, PageRequest.of(0, 20)))
        }

        assertThat(exception.errorCode).isEqualTo(FeedErrorCode.SEARCH_KEYWORD_TOO_LONG)
        verifyNoInteractions(boardRepository, feedSearchRepository)
    }

    private fun stubItem(id: Long, thumbnailUrl: String?): FeedSearchItemResult =
        FeedSearchItemResult(
            id = id,
            boardId = 2L,
            boardTitle = "게시판2",
            title = "제목$id",
            content = "내용$id",
            thumbnailUrl = thumbnailUrl,
            createdAt = base
        )

    private fun stubFeed(id: Long): Feed =
        Feed.reconstitute(
            id = id,
            title = "제목$id",
            content = "내용$id",
            likeCount = 0,
            commentCount = 0,
            boardId = 2L,
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
