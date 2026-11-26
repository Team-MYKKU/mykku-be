package com.example.mykku.feed.adapter.output.persistence

import com.example.mykku.BaseRepositoryTest
import com.example.mykku.board.adapter.output.persistence.entity.BoardJpaEntity
import com.example.mykku.feed.application.port.output.FeedRepository
import com.example.mykku.feed.domain.entity.Feed
import com.example.mykku.feed.domain.vo.FeedId
import com.example.mykku.feed.exception.FeedException
import com.example.mykku.feed.exception.FeedErrorCode
import com.example.mykku.member.adapter.output.persistence.entity.MemberJpaEntity
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest

@DisplayName("FeedRepositoryAdapter 통합 테스트")
class FeedRepositoryAdapterTest : BaseRepositoryTest() {

    @Autowired
    private lateinit var feedRepository: FeedRepository

    private lateinit var member: MemberJpaEntity
    private lateinit var board: BoardJpaEntity

    @BeforeEach
    fun setUp() {
        member = createAndSaveMember(memberId = "test-member", nickname = "테스트유저")
        board = createAndSaveBoard(title = "테스트 게시판")
    }

    @Nested
    @DisplayName("save 메서드")
    inner class Save {

        @Test
        @DisplayName("피드를 정상적으로 저장한다")
        fun `피드 저장 - 정상 케이스`() {
            val feed = Feed.create(
                title = "테스트 피드",
                content = "테스트 내용",
                boardId = board.id!!,
                memberId = member.id
            )

            val savedFeed = feedRepository.save(feed, board.id!!, member.id)

            assertThat(savedFeed.id).isNotNull()
            assertThat(savedFeed.title).isEqualTo("테스트 피드")
            assertThat(savedFeed.content).isEqualTo("테스트 내용")
            assertThat(savedFeed.boardId).isEqualTo(board.id)
            assertThat(savedFeed.memberId).isEqualTo(member.id)
        }

        @Test
        @DisplayName("피드 저장 시 likeCount와 commentCount가 0으로 초기화된다")
        fun `피드 저장 - 카운트 초기값 검증`() {
            val feed = Feed.create(
                title = "테스트 피드",
                content = "테스트 내용",
                boardId = board.id!!,
                memberId = member.id
            )

            val savedFeed = feedRepository.save(feed, board.id!!, member.id)

            assertThat(savedFeed.likeCount).isEqualTo(0)
            assertThat(savedFeed.commentCount).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("update 메서드")
    inner class Update {

        @Test
        @DisplayName("피드를 정상적으로 수정한다")
        fun `피드 수정 - 정상 케이스`() {
            val savedFeed = saveFeed(title = "원본 제목", content = "원본 내용")

            val updatedFeed = savedFeed.update(title = "수정된 제목", content = "수정된 내용")
            val result = feedRepository.update(updatedFeed)
            flushAndClear()

            assertThat(result.title).isEqualTo("수정된 제목")
            assertThat(result.content).isEqualTo("수정된 내용")
            val storedFeed = feedRepository.findByIdOrThrow(savedFeed.id!!)
            assertThat(storedFeed.title).isEqualTo("수정된 제목")
            assertThat(storedFeed.content).isEqualTo("수정된 내용")
            assertThat(storedFeed.boardId).isEqualTo(board.id)
        }

        @Test
        @DisplayName("존재하지 않는 피드를 수정하면 예외가 발생한다")
        fun `피드 수정 - 존재하지 않는 피드`() {
            val nonExistentFeed = Feed.reconstitute(
                id = 99999L,
                title = "제목",
                content = "내용",
                likeCount = 0,
                commentCount = 0,
                boardId = board.id!!,
                memberId = member.id,
                createdAt = java.time.LocalDateTime.now(),
                updatedAt = java.time.LocalDateTime.now()
            )

            val exception = assertThrows<FeedException> {
                feedRepository.update(nonExistentFeed)
            }

            assertThat(exception.errorCode).isEqualTo(FeedErrorCode.FEED_NOT_FOUND)
        }

        private fun saveFeed(title: String, content: String): Feed {
            val feed = Feed.create(
                title = title,
                content = content,
                boardId = board.id!!,
                memberId = member.id
            )
            return feedRepository.save(feed, board.id!!, member.id)
        }
    }

    @Nested
    @DisplayName("findById 메서드")
    inner class FindById {

        @Test
        @DisplayName("ID로 피드를 조회한다")
        fun `피드 조회 - 정상 케이스`() {
            val feed = Feed.create(
                title = "테스트 피드",
                content = "테스트 내용",
                boardId = board.id!!,
                memberId = member.id
            )
            val savedFeed = feedRepository.save(feed, board.id!!, member.id)

            val foundFeed = feedRepository.findById(savedFeed.id!!)

            assertThat(foundFeed).isNotNull()
            assertThat(foundFeed!!.id).isEqualTo(savedFeed.id)
            assertThat(foundFeed.title).isEqualTo("테스트 피드")
        }

        @Test
        @DisplayName("존재하지 않는 ID로 조회하면 null을 반환한다")
        fun `피드 조회 - 존재하지 않는 ID`() {
            val foundFeed = feedRepository.findById(FeedId.of(99999L))

            assertThat(foundFeed).isNull()
        }
    }

    @Nested
    @DisplayName("findByIdOrThrow 메서드")
    inner class FindByIdOrThrow {

        @Test
        @DisplayName("ID로 피드를 조회한다")
        fun `피드 조회 - 정상 케이스`() {
            val feed = Feed.create(
                title = "테스트 피드",
                content = "테스트 내용",
                boardId = board.id!!,
                memberId = member.id
            )
            val savedFeed = feedRepository.save(feed, board.id!!, member.id)

            val foundFeed = feedRepository.findByIdOrThrow(savedFeed.id!!)

            assertThat(foundFeed.id).isEqualTo(savedFeed.id)
            assertThat(foundFeed.title).isEqualTo("테스트 피드")
        }

        @Test
        @DisplayName("존재하지 않는 ID로 조회하면 예외가 발생한다")
        fun `피드 조회 - 존재하지 않는 ID`() {
            val exception = assertThrows<FeedException> {
                feedRepository.findByIdOrThrow(FeedId.of(99999L))
            }

            assertThat(exception.errorCode).isEqualTo(FeedErrorCode.FEED_NOT_FOUND)
        }
    }

    @Nested
    @DisplayName("findByBoardId 메서드")
    inner class FindByBoardId {

        @Test
        @DisplayName("게시판 ID로 피드 목록을 조회한다")
        fun `게시판별 피드 조회 - 정상 케이스`() {
            repeat(5) { index ->
                val feed = Feed.create(
                    title = "피드 $index",
                    content = "내용 $index",
                    boardId = board.id!!,
                    memberId = member.id
                )
                feedRepository.save(feed, board.id!!, member.id)
            }

            val pageable = PageRequest.of(0, 10)
            val result = feedRepository.findByBoardId(board.id!!, pageable)

            assertThat(result.content).hasSize(5)
            assertThat(result.totalElements).isEqualTo(5)
        }

        @Test
        @DisplayName("게시판별 피드 조회 시 생성일 기준 내림차순으로 정렬된다")
        fun `게시판별 피드 조회 - 정렬 검증`() {
            repeat(3) { index ->
                val feed = Feed.create(
                    title = "피드 $index",
                    content = "내용 $index",
                    boardId = board.id!!,
                    memberId = member.id
                )
                feedRepository.save(feed, board.id!!, member.id)
                Thread.sleep(10)
            }

            val pageable = PageRequest.of(0, 10)
            val result = feedRepository.findByBoardId(board.id!!, pageable)

            assertThat(result.content[0].title).isEqualTo("피드 2")
            assertThat(result.content[1].title).isEqualTo("피드 1")
            assertThat(result.content[2].title).isEqualTo("피드 0")
        }

        @Test
        @DisplayName("페이지네이션이 정상적으로 동작한다")
        fun `게시판별 피드 조회 - 페이지네이션`() {
            repeat(15) { index ->
                val feed = Feed.create(
                    title = "피드 $index",
                    content = "내용 $index",
                    boardId = board.id!!,
                    memberId = member.id
                )
                feedRepository.save(feed, board.id!!, member.id)
            }

            val firstPage = feedRepository.findByBoardId(board.id!!, PageRequest.of(0, 10))
            val secondPage = feedRepository.findByBoardId(board.id!!, PageRequest.of(1, 10))

            assertThat(firstPage.content).hasSize(10)
            assertThat(secondPage.content).hasSize(5)
            assertThat(firstPage.totalPages).isEqualTo(2)
        }

        @Test
        @DisplayName("다른 게시판의 피드는 조회되지 않는다")
        fun `게시판별 피드 조회 - 다른 게시판 피드 제외`() {
            val otherBoard = createAndSaveBoard(title = "다른 게시판")
            val feed1 = Feed.create(
                title = "피드 1",
                content = "내용 1",
                boardId = board.id!!,
                memberId = member.id
            )
            val feed2 = Feed.create(
                title = "피드 2",
                content = "내용 2",
                boardId = otherBoard.id!!,
                memberId = member.id
            )
            feedRepository.save(feed1, board.id!!, member.id)
            feedRepository.save(feed2, otherBoard.id!!, member.id)

            val pageable = PageRequest.of(0, 10)
            val result = feedRepository.findByBoardId(board.id!!, pageable)

            assertThat(result.content).hasSize(1)
            assertThat(result.content[0].title).isEqualTo("피드 1")
        }
    }

    @Nested
    @DisplayName("findPopularFeedsByBoardId 메서드")
    inner class FindPopularFeedsByBoardId {

        @Test
        @DisplayName("인기 피드를 좋아요 수 기준 내림차순으로 조회한다")
        fun `인기 피드 조회 - 정상 케이스`() {
            val feed1 = feedRepository.save(
                Feed.create(
                    title = "피드 1",
                    content = "내용 1",
                    boardId = board.id!!,
                    memberId = member.id
                ),
                board.id!!,
                member.id
            )
            val feed2 = feedRepository.save(
                Feed.create(
                    title = "피드 2",
                    content = "내용 2",
                    boardId = board.id!!,
                    memberId = member.id
                ),
                board.id!!,
                member.id
            )

            val result = feedRepository.findPopularFeedsByBoardId(board.id!!, 3, 7)

            assertThat(result).isNotEmpty()
            assertThat(result.size).isLessThanOrEqualTo(3)
        }

        @Test
        @DisplayName("limit 값에 따라 결과 개수가 제한된다")
        fun `인기 피드 조회 - 개수 제한`() {
            repeat(10) { index ->
                feedRepository.save(
                    Feed.create(
                        title = "피드 $index",
                        content = "내용 $index",
                        boardId = board.id!!,
                        memberId = member.id
                    ),
                    board.id!!,
                    member.id
                )
            }

            val result = feedRepository.findPopularFeedsByBoardId(board.id!!, 5, 7)

            assertThat(result.size).isLessThanOrEqualTo(5)
        }
    }

    @Nested
    @DisplayName("delete 메서드")
    inner class Delete {

        @Test
        @DisplayName("피드를 정상적으로 삭제한다")
        fun `피드 삭제 - 정상 케이스`() {
            val feed = Feed.create(
                title = "삭제할 피드",
                content = "삭제할 내용",
                boardId = board.id!!,
                memberId = member.id
            )
            val savedFeed = feedRepository.save(feed, board.id!!, member.id)

            feedRepository.delete(savedFeed)

            val foundFeed = feedRepository.findById(savedFeed.id!!)
            assertThat(foundFeed).isNull()
            flushAndClear()
            assertThat(feedRepository.findById(savedFeed.id!!)).isNull()
        }

        @Test
        @DisplayName("존재하지 않는 피드를 삭제하면 예외가 발생한다")
        fun `피드 삭제 - 존재하지 않는 피드`() {
            val nonExistentFeed = Feed.reconstitute(
                id = 99999L,
                title = "제목",
                content = "내용",
                likeCount = 0,
                commentCount = 0,
                boardId = board.id!!,
                memberId = member.id,
                createdAt = java.time.LocalDateTime.now(),
                updatedAt = java.time.LocalDateTime.now()
            )

            val exception = assertThrows<FeedException> {
                feedRepository.delete(nonExistentFeed)
            }

            assertThat(exception.errorCode).isEqualTo(FeedErrorCode.FEED_NOT_FOUND)
        }
    }
}
