package com.example.mykku.feed.adapter.output.persistence

import com.example.mykku.BaseRepositoryTest
import com.example.mykku.board.adapter.output.persistence.entity.BoardJpaEntity
import com.example.mykku.feed.adapter.output.persistence.entity.FeedCommentJpaEntity
import com.example.mykku.feed.adapter.output.persistence.entity.FeedJpaEntity
import com.example.mykku.feed.application.port.output.FeedCommentRepository
import com.example.mykku.feed.domain.entity.FeedComment
import com.example.mykku.feed.domain.vo.FeedCommentId
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
import org.springframework.transaction.annotation.Transactional

@DisplayName("FeedCommentRepositoryAdapter 통합 테스트")
@Transactional
class FeedCommentRepositoryAdapterTest : BaseRepositoryTest() {

    @Autowired
    private lateinit var feedCommentRepository: FeedCommentRepository

    @Autowired
    private lateinit var feedJpaRepository: FeedJpaRepository

    @Autowired
    private lateinit var feedCommentJpaRepository: FeedCommentJpaRepository

    private lateinit var member: MemberJpaEntity
    private lateinit var board: BoardJpaEntity
    private lateinit var feed: FeedJpaEntity

    @BeforeEach
    fun setUp() {
        member = createAndSaveMember(memberId = "test-member", nickname = "테스트유저")
        board = createAndSaveBoard(title = "테스트 게시판")
        feed = feedJpaRepository.save(
            FeedJpaEntity(
                title = "테스트 피드",
                content = "테스트 내용",
                board = board,
                member = member
            )
        )
    }

    private fun createAndSaveFeed(title: String): FeedJpaEntity {
        return feedJpaRepository.save(
            FeedJpaEntity(
                title = title,
                content = "테스트 내용",
                board = board,
                member = member
            )
        )
    }

    private fun saveComments(feedId: FeedId, count: Int) {
        repeat(count) { index ->
            feedCommentRepository.save(
                FeedComment.create(content = "댓글 $index", feedId = feedId, memberId = member.id),
                feedId,
                member.id
            )
        }
    }

    @Nested
    @DisplayName("save 메서드")
    inner class Save {

        @Test
        @DisplayName("피드 댓글을 정상적으로 저장한다")
        fun `피드 댓글 저장 - 정상 케이스`() {
            val feedId = FeedId.of(feed.id!!)
            val comment = FeedComment.create(
                content = "테스트 댓글",
                feedId = feedId,
                memberId = member.id
            )

            val savedComment = feedCommentRepository.save(comment, feedId, member.id)

            assertThat(savedComment.id).isNotNull()
            assertThat(savedComment.content).isEqualTo("테스트 댓글")
            assertThat(savedComment.feedId).isEqualTo(feedId)
            assertThat(savedComment.memberId).isEqualTo(member.id)
        }

        @Test
        @DisplayName("대댓글을 정상적으로 저장한다")
        fun `대댓글 저장 - 정상 케이스`() {
            val feedId = FeedId.of(feed.id!!)
            val parentComment = FeedComment.create(
                content = "부모 댓글",
                feedId = feedId,
                memberId = member.id
            )
            val savedParentComment = feedCommentRepository.save(parentComment, feedId, member.id)

            val replyComment = FeedComment.create(
                content = "대댓글",
                feedId = feedId,
                memberId = member.id,
                parentCommentId = savedParentComment.id
            )
            val savedReplyComment = feedCommentRepository.save(replyComment, feedId, member.id)

            assertThat(savedReplyComment.id).isNotNull()
            assertThat(savedReplyComment.parentCommentId).isEqualTo(savedParentComment.id)
        }

        @Test
        @DisplayName("댓글 저장 시 likeCount가 0으로 초기화된다")
        fun `피드 댓글 저장 - 카운트 초기값 검증`() {
            val feedId = FeedId.of(feed.id!!)
            val comment = FeedComment.create(
                content = "테스트 댓글",
                feedId = feedId,
                memberId = member.id
            )

            val savedComment = feedCommentRepository.save(comment, feedId, member.id)

            assertThat(savedComment.likeCount).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("findById 메서드")
    inner class FindById {

        @Test
        @DisplayName("ID로 댓글을 조회한다")
        fun `댓글 조회 - 정상 케이스`() {
            val feedId = FeedId.of(feed.id!!)
            val comment = FeedComment.create(
                content = "테스트 댓글",
                feedId = feedId,
                memberId = member.id
            )
            val savedComment = feedCommentRepository.save(comment, feedId, member.id)

            val foundComment = feedCommentRepository.findById(savedComment.id!!)

            assertThat(foundComment).isNotNull()
            assertThat(foundComment!!.id).isEqualTo(savedComment.id)
            assertThat(foundComment.content).isEqualTo("테스트 댓글")
        }

        @Test
        @DisplayName("존재하지 않는 ID로 조회하면 null을 반환한다")
        fun `댓글 조회 - 존재하지 않는 ID`() {
            val foundComment = feedCommentRepository.findById(FeedCommentId.of(99999L))

            assertThat(foundComment).isNull()
        }
    }

    @Nested
    @DisplayName("findByIdOrThrow 메서드")
    inner class FindByIdOrThrow {

        @Test
        @DisplayName("ID로 댓글을 조회한다")
        fun `댓글 조회 - 정상 케이스`() {
            val feedId = FeedId.of(feed.id!!)
            val comment = FeedComment.create(
                content = "테스트 댓글",
                feedId = feedId,
                memberId = member.id
            )
            val savedComment = feedCommentRepository.save(comment, feedId, member.id)

            val foundComment = feedCommentRepository.findByIdOrThrow(savedComment.id!!)

            assertThat(foundComment.id).isEqualTo(savedComment.id)
            assertThat(foundComment.content).isEqualTo("테스트 댓글")
        }

        @Test
        @DisplayName("존재하지 않는 ID로 조회하면 예외가 발생한다")
        fun `댓글 조회 - 존재하지 않는 ID`() {
            val exception = assertThrows<FeedException> {
                feedCommentRepository.findByIdOrThrow(FeedCommentId.of(99999L))
            }

            assertThat(exception.errorCode).isEqualTo(FeedErrorCode.FEED_COMMENT_NOT_FOUND)
        }
    }

    @Nested
    @DisplayName("findByFeedIdAndParentCommentIsNull 메서드")
    inner class FindByFeedIdAndParentCommentIsNull {

        @Test
        @DisplayName("피드의 최상위 댓글 목록을 조회한다")
        fun `최상위 댓글 조회 - 정상 케이스`() {
            val feedId = FeedId.of(feed.id!!)
            repeat(5) { index ->
                val comment = FeedComment.create(
                    content = "댓글 $index",
                    feedId = feedId,
                    memberId = member.id
                )
                feedCommentRepository.save(comment, feedId, member.id)
            }

            val pageable = PageRequest.of(0, 10)
            val result = feedCommentRepository.findByFeedIdAndParentCommentIsNull(feedId, pageable)

            assertThat(result.content).hasSize(5)
            assertThat(result.totalElements).isEqualTo(5)
        }

        @Test
        @DisplayName("대댓글은 최상위 댓글 목록에 포함되지 않는다")
        fun `최상위 댓글 조회 - 대댓글 제외`() {
            val feedId = FeedId.of(feed.id!!)
            val parentComment = FeedComment.create(
                content = "부모 댓글",
                feedId = feedId,
                memberId = member.id
            )
            val savedParentComment = feedCommentRepository.save(parentComment, feedId, member.id)

            val replyComment = FeedComment.create(
                content = "대댓글",
                feedId = feedId,
                memberId = member.id,
                parentCommentId = savedParentComment.id
            )
            feedCommentRepository.save(replyComment, feedId, member.id)

            val pageable = PageRequest.of(0, 10)
            val result = feedCommentRepository.findByFeedIdAndParentCommentIsNull(feedId, pageable)

            assertThat(result.content).hasSize(1)
            assertThat(result.content[0].parentCommentId).isNull()
        }

        @Test
        @DisplayName("존재하지 않는 피드 ID로 조회하면 빈 페이지를 반환한다")
        fun `최상위 댓글 조회 - 존재하지 않는 피드`() {
            val nonExistentFeedId = FeedId.of(99999L)
            val pageable = PageRequest.of(0, 10)

            val result = feedCommentRepository.findByFeedIdAndParentCommentIsNull(nonExistentFeedId, pageable)

            assertThat(result.content).isEmpty()
        }

        @Test
        @DisplayName("페이지네이션이 정상적으로 동작한다")
        fun `최상위 댓글 조회 - 페이지네이션`() {
            val feedId = FeedId.of(feed.id!!)
            repeat(15) { index ->
                val comment = FeedComment.create(
                    content = "댓글 $index",
                    feedId = feedId,
                    memberId = member.id
                )
                feedCommentRepository.save(comment, feedId, member.id)
            }

            val firstPage = feedCommentRepository.findByFeedIdAndParentCommentIsNull(feedId, PageRequest.of(0, 10))
            val secondPage = feedCommentRepository.findByFeedIdAndParentCommentIsNull(feedId, PageRequest.of(1, 10))

            assertThat(firstPage.content).hasSize(10)
            assertThat(secondPage.content).hasSize(5)
            assertThat(firstPage.totalPages).isEqualTo(2)
        }
    }

    @Nested
    @DisplayName("findByParentCommentIds 메서드")
    inner class FindByParentCommentIds {

        @Test
        @DisplayName("여러 부모 댓글 ID로 대댓글 목록을 조회한다")
        fun `여러 대댓글 조회 - 정상 케이스`() {
            val feedId = FeedId.of(feed.id!!)
            val parentComment1 = feedCommentRepository.save(
                FeedComment.create(content = "부모 댓글 1", feedId = feedId, memberId = member.id),
                feedId,
                member.id
            )
            val parentComment2 = feedCommentRepository.save(
                FeedComment.create(content = "부모 댓글 2", feedId = feedId, memberId = member.id),
                feedId,
                member.id
            )
            feedCommentRepository.save(
                FeedComment.create(content = "대댓글 1-1", feedId = feedId, memberId = member.id, parentCommentId = parentComment1.id),
                feedId,
                member.id
            )
            feedCommentRepository.save(
                FeedComment.create(content = "대댓글 2-1", feedId = feedId, memberId = member.id, parentCommentId = parentComment2.id),
                feedId,
                member.id
            )
            feedCommentRepository.save(
                FeedComment.create(content = "대댓글 2-2", feedId = feedId, memberId = member.id, parentCommentId = parentComment2.id),
                feedId,
                member.id
            )

            val replies = feedCommentRepository.findByParentCommentIds(listOf(parentComment1.id!!, parentComment2.id!!))

            assertThat(replies).hasSize(3)
        }

        @Test
        @DisplayName("빈 부모 댓글 ID 목록으로 조회하면 빈 목록을 반환한다")
        fun `여러 대댓글 조회 - 빈 목록`() {
            val replies = feedCommentRepository.findByParentCommentIds(emptyList())

            assertThat(replies).isEmpty()
        }
    }

    @Nested
    @DisplayName("countByFeedId 메서드")
    inner class CountByFeedId {

        @Test
        @DisplayName("피드의 댓글 수를 조회한다")
        fun `댓글 수 조회 - 정상 케이스`() {
            val feedId = FeedId.of(feed.id!!)
            repeat(5) { index ->
                feedCommentRepository.save(
                    FeedComment.create(content = "댓글 $index", feedId = feedId, memberId = member.id),
                    feedId,
                    member.id
                )
            }

            val count = feedCommentRepository.countByFeedId(feedId)

            assertThat(count).isEqualTo(5)
        }

        @Test
        @DisplayName("대댓글도 댓글 수에 포함된다")
        fun `댓글 수 조회 - 대댓글 포함`() {
            val feedId = FeedId.of(feed.id!!)
            val parentComment = feedCommentRepository.save(
                FeedComment.create(content = "부모 댓글", feedId = feedId, memberId = member.id),
                feedId,
                member.id
            )
            feedCommentRepository.save(
                FeedComment.create(content = "대댓글 1", feedId = feedId, memberId = member.id, parentCommentId = parentComment.id),
                feedId,
                member.id
            )
            feedCommentRepository.save(
                FeedComment.create(content = "대댓글 2", feedId = feedId, memberId = member.id, parentCommentId = parentComment.id),
                feedId,
                member.id
            )

            val count = feedCommentRepository.countByFeedId(feedId)

            assertThat(count).isEqualTo(3)
        }

        @Test
        @DisplayName("댓글이 없는 피드의 경우 0을 반환한다")
        fun `댓글 수 조회 - 댓글 없음`() {
            val feedId = FeedId.of(feed.id!!)

            val count = feedCommentRepository.countByFeedId(feedId)

            assertThat(count).isEqualTo(0)
        }

        @Test
        @DisplayName("존재하지 않는 피드 ID로 조회하면 0을 반환한다")
        fun `댓글 수 조회 - 존재하지 않는 피드`() {
            val nonExistentFeedId = FeedId.of(99999L)

            val count = feedCommentRepository.countByFeedId(nonExistentFeedId)

            assertThat(count).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("countByFeedIdIn 메서드")
    inner class CountByFeedIdIn {

        @Test
        @DisplayName("여러 피드의 댓글 수를 피드별로 집계한다")
        fun `댓글 수 일괄 조회 - 정상 케이스`() {
            val feed2 = createAndSaveFeed("테스트 피드 2")
            val feed3 = createAndSaveFeed("테스트 피드 3")
            val feedId1 = FeedId.of(feed.id!!)
            val feedId2 = FeedId.of(feed2.id!!)
            val feedId3 = FeedId.of(feed3.id!!)
            saveComments(feedId1, 3)
            saveComments(feedId2, 1)

            val counts = feedCommentRepository.countByFeedIdIn(listOf(feedId1, feedId2, feedId3))

            assertThat(counts).containsEntry(feed.id!!, 3)
            assertThat(counts).containsEntry(feed2.id!!, 1)
        }

        @Test
        @DisplayName("댓글이 없는 피드는 맵에 키가 존재하지 않는다")
        fun `댓글 수 일괄 조회 - 댓글 없는 피드는 키 없음`() {
            val feedWithoutComment = createAndSaveFeed("댓글 없는 피드")
            val feedId1 = FeedId.of(feed.id!!)
            saveComments(feedId1, 2)

            val counts = feedCommentRepository.countByFeedIdIn(
                listOf(feedId1, FeedId.of(feedWithoutComment.id!!))
            )

            assertThat(counts).hasSize(1)
            assertThat(counts).containsEntry(feed.id!!, 2)
            assertThat(counts).doesNotContainKey(feedWithoutComment.id!!)
        }

        @Test
        @DisplayName("대댓글도 댓글 수에 포함된다")
        fun `댓글 수 일괄 조회 - 대댓글 포함`() {
            val feed2 = createAndSaveFeed("테스트 피드 2")
            val feedId1 = FeedId.of(feed.id!!)
            val feedId2 = FeedId.of(feed2.id!!)
            val parentComment = feedCommentRepository.save(
                FeedComment.create(content = "부모 댓글", feedId = feedId1, memberId = member.id),
                feedId1,
                member.id
            )
            feedCommentRepository.save(
                FeedComment.create(
                    content = "대댓글 1",
                    feedId = feedId1,
                    memberId = member.id,
                    parentCommentId = parentComment.id
                ),
                feedId1,
                member.id
            )
            feedCommentRepository.save(
                FeedComment.create(
                    content = "대댓글 2",
                    feedId = feedId1,
                    memberId = member.id,
                    parentCommentId = parentComment.id
                ),
                feedId1,
                member.id
            )
            saveComments(feedId2, 1)

            val counts = feedCommentRepository.countByFeedIdIn(listOf(feedId1, feedId2))

            assertThat(counts).containsEntry(feed.id!!, 3)
            assertThat(counts).containsEntry(feed2.id!!, 1)
        }

        @Test
        @DisplayName("조회 대상에 포함되지 않은 피드의 댓글은 집계되지 않는다")
        fun `댓글 수 일괄 조회 - 조회 대상 외 피드 제외`() {
            val otherFeed = createAndSaveFeed("다른 피드")
            val feedId1 = FeedId.of(feed.id!!)
            saveComments(feedId1, 1)
            saveComments(FeedId.of(otherFeed.id!!), 2)

            val counts = feedCommentRepository.countByFeedIdIn(listOf(feedId1))

            assertThat(counts).hasSize(1)
            assertThat(counts).containsEntry(feed.id!!, 1)
            assertThat(counts).doesNotContainKey(otherFeed.id!!)
        }

        @Test
        @DisplayName("빈 피드 ID 목록으로 조회하면 빈 맵을 반환한다")
        fun `댓글 수 일괄 조회 - 빈 목록`() {
            val counts = feedCommentRepository.countByFeedIdIn(emptyList())

            assertThat(counts).isEmpty()
        }
    }

    @Nested
    @DisplayName("findIdsByFeedId 메서드")
    inner class FindIdsByFeedId {

        @Test
        @DisplayName("피드의 모든 댓글 ID를 조회한다")
        fun `댓글 ID 조회 - 정상 케이스`() {
            val feedId = FeedId.of(feed.id!!)
            repeat(3) { index ->
                feedCommentRepository.save(
                    FeedComment.create(content = "댓글 $index", feedId = feedId, memberId = member.id),
                    feedId,
                    member.id
                )
            }

            val ids = feedCommentRepository.findIdsByFeedId(feedId)

            assertThat(ids).hasSize(3)
        }

        @Test
        @DisplayName("댓글이 없는 피드의 경우 빈 목록을 반환한다")
        fun `댓글 ID 조회 - 댓글 없음`() {
            val feedId = FeedId.of(feed.id!!)

            val ids = feedCommentRepository.findIdsByFeedId(feedId)

            assertThat(ids).isEmpty()
        }

        @Test
        @DisplayName("존재하지 않는 피드 ID로 조회하면 빈 목록을 반환한다")
        fun `댓글 ID 조회 - 존재하지 않는 피드`() {
            val nonExistentFeedId = FeedId.of(99999L)

            val ids = feedCommentRepository.findIdsByFeedId(nonExistentFeedId)

            assertThat(ids).isEmpty()
        }
    }

    @Nested
    @DisplayName("delete 메서드")
    inner class Delete {

        @Test
        @DisplayName("댓글을 정상적으로 삭제한다")
        fun `댓글 삭제 - 정상 케이스`() {
            val feedId = FeedId.of(feed.id!!)
            val comment = feedCommentRepository.save(
                FeedComment.create(content = "삭제할 댓글", feedId = feedId, memberId = member.id),
                feedId,
                member.id
            )

            feedCommentRepository.delete(comment)

            val foundComment = feedCommentRepository.findById(comment.id!!)
            assertThat(foundComment).isNull()
        }

        @Test
        @DisplayName("ID가 없는 댓글을 삭제해도 예외가 발생하지 않는다")
        fun `댓글 삭제 - ID 없는 댓글`() {
            val feedId = FeedId.of(feed.id!!)
            val commentWithoutId = FeedComment.create(
                content = "ID 없는 댓글",
                feedId = feedId,
                memberId = member.id
            )

            feedCommentRepository.delete(commentWithoutId)
        }
    }

    @Nested
    @DisplayName("deleteAllByFeedId 메서드")
    inner class DeleteAllByFeedId {

        @Test
        @DisplayName("피드 ID로 모든 댓글을 삭제한다")
        fun `피드 ID로 댓글 삭제 - 정상 케이스`() {
            val feedId = FeedId.of(feed.id!!)
            repeat(5) { index ->
                feedCommentRepository.save(
                    FeedComment.create(content = "댓글 $index", feedId = feedId, memberId = member.id),
                    feedId,
                    member.id
                )
            }

            feedCommentRepository.deleteAllByFeedId(feedId)

            val count = feedCommentRepository.countByFeedId(feedId)
            assertThat(count).isEqualTo(0)
        }

        @Test
        @DisplayName("대댓글도 함께 삭제된다")
        fun `피드 ID로 댓글 삭제 - 대댓글 포함`() {
            val feedId = FeedId.of(feed.id!!)
            val parentComment = feedCommentRepository.save(
                FeedComment.create(content = "부모 댓글", feedId = feedId, memberId = member.id),
                feedId,
                member.id
            )
            feedCommentRepository.save(
                FeedComment.create(content = "대댓글", feedId = feedId, memberId = member.id, parentCommentId = parentComment.id),
                feedId,
                member.id
            )

            feedCommentRepository.deleteAllByFeedId(feedId)

            val count = feedCommentRepository.countByFeedId(feedId)
            assertThat(count).isEqualTo(0)
        }

        @Test
        @DisplayName("존재하지 않는 피드 ID로 삭제해도 예외가 발생하지 않는다")
        fun `피드 ID로 댓글 삭제 - 존재하지 않는 피드`() {
            val nonExistentFeedId = FeedId.of(99999L)

            feedCommentRepository.deleteAllByFeedId(nonExistentFeedId)
        }

        @Test
        @DisplayName("다른 피드의 댓글은 삭제되지 않는다")
        fun `피드 ID로 댓글 삭제 - 다른 피드 댓글 유지`() {
            val feed2 = feedJpaRepository.save(
                FeedJpaEntity(
                    title = "테스트 피드 2",
                    content = "테스트 내용 2",
                    board = board,
                    member = member
                )
            )
            val feedId1 = FeedId.of(feed.id!!)
            val feedId2 = FeedId.of(feed2.id!!)
            feedCommentRepository.save(
                FeedComment.create(content = "피드1 댓글", feedId = feedId1, memberId = member.id),
                feedId1,
                member.id
            )
            feedCommentRepository.save(
                FeedComment.create(content = "피드2 댓글", feedId = feedId2, memberId = member.id),
                feedId2,
                member.id
            )

            feedCommentRepository.deleteAllByFeedId(feedId1)

            assertThat(feedCommentRepository.countByFeedId(feedId1)).isEqualTo(0)
            assertThat(feedCommentRepository.countByFeedId(feedId2)).isEqualTo(1)
        }
    }

    @Nested
    @DisplayName("탈퇴한 회원의 댓글")
    inner class WithdrawnAuthor {

        @Test
        @DisplayName("최상위 댓글 목록은 작성자가 없는 댓글을 포함하고 개수와 내용이 일치한다")
        fun `findByFeedIdAndParentCommentIsNull - 탈퇴 회원 댓글 포함`() {
            feedCommentJpaRepository.save(FeedCommentJpaEntity(content = "살아있는 댓글", feed = feed, member = member))
            feedCommentJpaRepository.save(FeedCommentJpaEntity(content = "탈퇴자 댓글", feed = feed, member = null))

            val page = feedCommentRepository.findByFeedIdAndParentCommentIsNull(
                FeedId.of(feed.id!!),
                PageRequest.of(0, 20)
            )

            assertThat(page.content.map { it.content }).containsExactlyInAnyOrder("살아있는 댓글", "탈퇴자 댓글")
            assertThat(page.totalElements).isEqualTo(page.content.size.toLong())
        }

        @Test
        @DisplayName("답글 조회는 작성자가 없는 답글을 포함하고 오래된 순으로 반환한다")
        fun `findByParentCommentIds - 탈퇴 회원 답글 포함`() {
            val parent = feedCommentJpaRepository.save(FeedCommentJpaEntity(content = "부모", feed = feed, member = member))
            feedCommentJpaRepository.save(
                FeedCommentJpaEntity(content = "탈퇴자 답글", feed = feed, member = null, parentComment = parent)
            )
            feedCommentJpaRepository.save(
                FeedCommentJpaEntity(content = "회원 답글", feed = feed, member = member, parentComment = parent)
            )

            val replies = feedCommentRepository.findByParentCommentIds(listOf(FeedCommentId.of(parent.id!!)))

            assertThat(replies.map { it.content }).containsExactly("탈퇴자 답글", "회원 답글")
            assertThat(replies.first().memberId).isNull()
        }

        @Test
        @DisplayName("첫 댓글 미리보기는 작성자가 탈퇴한 첫 댓글을 반환한다")
        fun `findFirstCommentsByFeedIds - 탈퇴 회원 첫 댓글 포함`() {
            feedCommentJpaRepository.save(FeedCommentJpaEntity(content = "탈퇴자 첫 댓글", feed = feed, member = null))
            feedCommentJpaRepository.save(FeedCommentJpaEntity(content = "두번째 댓글", feed = feed, member = member))

            val firstComments = feedCommentRepository.findFirstCommentsByFeedIds(listOf(FeedId.of(feed.id!!)))

            assertThat(firstComments[feed.id!!]?.content).isEqualTo("탈퇴자 첫 댓글")
            assertThat(firstComments[feed.id!!]?.memberId).isNull()
        }
    }
}
