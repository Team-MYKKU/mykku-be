package com.example.mykku.feed.application.usecase

import com.example.mykku.board.domain.entity.Board
import com.example.mykku.board.domain.vo.BoardId
import com.example.mykku.feed.application.dto.FeedSearchItemResult
import com.example.mykku.feed.application.port.output.FeedImageRepository
import com.example.mykku.feed.domain.entity.Feed
import com.example.mykku.feed.domain.vo.FeedId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
@DisplayName("피드 검색 카드 조립 단위 테스트")
class FeedSearchItemAssemblerTest {

    @Mock
    private lateinit var feedImageRepository: FeedImageRepository

    private lateinit var assembler: FeedSearchItemAssembler

    @BeforeEach
    fun setUp() {
        assembler = FeedSearchItemAssembler(feedImageRepository)
    }

    @Test
    @DisplayName("피드와 게시판 정보, 썸네일로 카드를 만든다")
    fun `카드 조립 - 게시판과 썸네일 매핑`() {
        val createdAt = LocalDateTime.of(2025, 3, 1, 12, 34, 56)
        val feed = stubFeed(id = 18L, boardId = 2L, createdAt = createdAt)
        whenever(feedImageRepository.findThumbnailUrlsByFeedIds(listOf(FeedId.of(18L))))
            .thenReturn(mapOf(FeedId.of(18L) to "https://img/18.jpg"))

        val results = assembler.assemble(listOf(feed), mapOf(2L to stubBoard(2L, "아이브")))

        assertThat(results).containsExactly(expectedItem(18L, 2L, "아이브", "https://img/18.jpg", createdAt))
    }

    @Test
    @DisplayName("썸네일이 없는 피드는 thumbnailUrl이 null이다")
    fun `카드 조립 - 썸네일 없음`() {
        val feeds = listOf(stubFeed(id = 1L, boardId = 2L), stubFeed(id = 2L, boardId = 2L))
        whenever(feedImageRepository.findThumbnailUrlsByFeedIds(listOf(FeedId.of(1L), FeedId.of(2L))))
            .thenReturn(mapOf(FeedId.of(2L) to "https://img/2.jpg"))

        val results = assembler.assemble(feeds, mapOf(2L to stubBoard(2L, "아이브")))

        assertThat(results.map { it.thumbnailUrl }).containsExactly(null, "https://img/2.jpg")
    }

    @Test
    @DisplayName("여러 게시판의 피드도 썸네일은 한 번만 조회하고 입력 순서를 유지한다")
    fun `카드 조립 - 썸네일 단일 조회`() {
        val feeds = listOf(stubFeed(id = 3L, boardId = 1L), stubFeed(id = 1L, boardId = 2L))
        val feedIds = listOf(FeedId.of(3L), FeedId.of(1L))
        whenever(feedImageRepository.findThumbnailUrlsByFeedIds(feedIds)).thenReturn(emptyMap())
        val boards = mapOf(1L to stubBoard(1L, "뉴진스"), 2L to stubBoard(2L, "아이브"))

        val results = assembler.assemble(feeds, boards)

        assertThat(results.map { it.id to it.boardTitle }).containsExactly(3L to "뉴진스", 1L to "아이브")
        verify(feedImageRepository, times(1)).findThumbnailUrlsByFeedIds(feedIds)
    }

    @Test
    @DisplayName("피드가 없으면 썸네일을 조회하지 않고 빈 목록을 반환한다")
    fun `카드 조립 - 빈 입력`() {
        val results = assembler.assemble(emptyList(), mapOf(2L to stubBoard(2L, "아이브")))

        assertThat(results).isEmpty()
        verifyNoInteractions(feedImageRepository)
    }

    private fun expectedItem(
        id: Long,
        boardId: Long,
        boardTitle: String,
        thumbnailUrl: String?,
        createdAt: LocalDateTime
    ): FeedSearchItemResult =
        FeedSearchItemResult(
            id = id,
            boardId = boardId,
            boardTitle = boardTitle,
            title = "제목$id",
            content = "내용$id",
            thumbnailUrl = thumbnailUrl,
            createdAt = createdAt
        )

    private fun stubFeed(id: Long, boardId: Long, createdAt: LocalDateTime = LocalDateTime.now()): Feed =
        Feed.reconstitute(
            id = id,
            title = "제목$id",
            content = "내용$id",
            likeCount = 0,
            commentCount = 0,
            boardId = boardId,
            memberId = null,
            createdAt = createdAt,
            updatedAt = createdAt
        )

    private fun stubBoard(id: Long, title: String): Board =
        Board.reconstitute(
            id = BoardId(id),
            title = title,
            logo = "https://logo/$id.png",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
}
