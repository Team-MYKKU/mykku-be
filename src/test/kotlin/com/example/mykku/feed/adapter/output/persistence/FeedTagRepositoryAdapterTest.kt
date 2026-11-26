package com.example.mykku.feed.adapter.output.persistence

import com.example.mykku.BaseRepositoryTest
import com.example.mykku.board.adapter.output.persistence.entity.BoardJpaEntity
import com.example.mykku.feed.adapter.output.persistence.entity.FeedJpaEntity
import com.example.mykku.feed.application.port.output.FeedTagRepository
import com.example.mykku.feed.domain.entity.FeedTag
import com.example.mykku.feed.domain.vo.FeedId
import com.example.mykku.member.adapter.output.persistence.entity.MemberJpaEntity
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@DisplayName("FeedTagRepositoryAdapter 통합 테스트")
class FeedTagRepositoryAdapterTest : BaseRepositoryTest() {

    @Autowired
    private lateinit var feedTagRepository: FeedTagRepository

    @Autowired
    private lateinit var feedJpaRepository: FeedJpaRepository

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

    @Nested
    @DisplayName("saveAll 메서드")
    inner class SaveAll {

        @Test
        @DisplayName("피드 태그 목록을 정상적으로 저장한다")
        fun `피드 태그 저장 - 정상 케이스`() {
            val feedId = FeedId.of(feed.id!!)
            val tags = listOf(
                FeedTag.create(title = "태그1", feedId = feedId),
                FeedTag.create(title = "태그2", feedId = feedId)
            )

            val savedTags = feedTagRepository.saveAll(tags, feedId)

            assertThat(savedTags).hasSize(2)
            assertThat(savedTags[0].id).isNotNull()
            assertThat(savedTags[0].title).isEqualTo("태그1")
            assertThat(savedTags[1].title).isEqualTo("태그2")
        }

        @Test
        @DisplayName("빈 목록을 저장하면 빈 목록을 반환한다")
        fun `피드 태그 저장 - 빈 목록`() {
            val feedId = FeedId.of(feed.id!!)
            val tags = emptyList<FeedTag>()

            val savedTags = feedTagRepository.saveAll(tags, feedId)

            assertThat(savedTags).isEmpty()
        }

        @Test
        @DisplayName("영문 태그를 저장할 수 있다")
        fun `피드 태그 저장 - 영문 태그`() {
            val feedId = FeedId.of(feed.id!!)
            val tags = listOf(
                FeedTag.create(title = "EnglishTag", feedId = feedId)
            )

            val savedTags = feedTagRepository.saveAll(tags, feedId)

            assertThat(savedTags[0].title).isEqualTo("EnglishTag")
        }
    }

    @Nested
    @DisplayName("findByFeedId 메서드")
    inner class FindByFeedId {

        @Test
        @DisplayName("피드 ID로 태그 목록을 조회한다")
        fun `피드 태그 조회 - 정상 케이스`() {
            val feedId = FeedId.of(feed.id!!)
            val tags = listOf(
                FeedTag.create(title = "태그1", feedId = feedId),
                FeedTag.create(title = "태그2", feedId = feedId),
                FeedTag.create(title = "태그3", feedId = feedId)
            )
            feedTagRepository.saveAll(tags, feedId)

            val foundTags = feedTagRepository.findByFeedId(feedId)

            assertThat(foundTags).hasSize(3)
        }

        @Test
        @DisplayName("태그가 없는 피드의 경우 빈 목록을 반환한다")
        fun `피드 태그 조회 - 태그 없음`() {
            val feedId = FeedId.of(feed.id!!)

            val foundTags = feedTagRepository.findByFeedId(feedId)

            assertThat(foundTags).isEmpty()
        }

        @Test
        @DisplayName("존재하지 않는 피드 ID로 조회하면 빈 목록을 반환한다")
        fun `피드 태그 조회 - 존재하지 않는 피드`() {
            val nonExistentFeedId = FeedId.of(99999L)

            val foundTags = feedTagRepository.findByFeedId(nonExistentFeedId)

            assertThat(foundTags).isEmpty()
        }
    }

    @Nested
    @DisplayName("findByFeedIds 메서드")
    inner class FindByFeedIds {

        @Test
        @DisplayName("여러 피드 ID로 태그 목록을 조회한다")
        fun `여러 피드 태그 조회 - 정상 케이스`() {
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
            feedTagRepository.saveAll(
                listOf(
                    FeedTag.create(title = "태그1", feedId = feedId1),
                    FeedTag.create(title = "태그2", feedId = feedId1)
                ),
                feedId1
            )
            feedTagRepository.saveAll(
                listOf(
                    FeedTag.create(title = "태그3", feedId = feedId2)
                ),
                feedId2
            )

            val foundTags = feedTagRepository.findByFeedIds(listOf(feedId1, feedId2))

            assertThat(foundTags).hasSize(3)
        }

        @Test
        @DisplayName("빈 피드 ID 목록으로 조회하면 빈 목록을 반환한다")
        fun `여러 피드 태그 조회 - 빈 목록`() {
            val foundTags = feedTagRepository.findByFeedIds(emptyList())

            assertThat(foundTags).isEmpty()
        }

        @Test
        @DisplayName("존재하지 않는 피드 ID가 포함되어도 존재하는 태그만 반환한다")
        fun `여러 피드 태그 조회 - 일부 존재하지 않는 피드`() {
            val feedId = FeedId.of(feed.id!!)
            feedTagRepository.saveAll(
                listOf(FeedTag.create(title = "태그1", feedId = feedId)),
                feedId
            )

            val foundTags = feedTagRepository.findByFeedIds(listOf(feedId, FeedId.of(99999L)))

            assertThat(foundTags).hasSize(1)
        }

        @Test
        @DisplayName("태그가 있는 피드와 없는 피드를 함께 조회한다")
        fun `여러 피드 태그 조회 - 혼합 케이스`() {
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
            feedTagRepository.saveAll(
                listOf(FeedTag.create(title = "태그1", feedId = feedId1)),
                feedId1
            )

            val foundTags = feedTagRepository.findByFeedIds(listOf(feedId1, feedId2))

            assertThat(foundTags).hasSize(1)
            assertThat(foundTags[0].feedId).isEqualTo(feedId1)
        }
    }

    @Nested
    @DisplayName("deleteAllByFeedId 메서드")
    inner class DeleteAllByFeedId {

        @Test
        @DisplayName("피드 ID로 모든 태그를 삭제한다")
        fun `피드 ID로 태그 삭제 - 정상 케이스`() {
            val feedId = FeedId.of(feed.id!!)
            feedTagRepository.saveAll(
                listOf(
                    FeedTag.create(title = "태그1", feedId = feedId),
                    FeedTag.create(title = "태그2", feedId = feedId),
                    FeedTag.create(title = "태그3", feedId = feedId)
                ),
                feedId
            )

            feedTagRepository.deleteAllByFeedId(feedId)

            val foundTags = feedTagRepository.findByFeedId(feedId)
            assertThat(foundTags).isEmpty()
        }

        @Test
        @DisplayName("존재하지 않는 피드 ID로 삭제해도 예외가 발생하지 않는다")
        fun `피드 ID로 태그 삭제 - 존재하지 않는 피드`() {
            val nonExistentFeedId = FeedId.of(99999L)

            feedTagRepository.deleteAllByFeedId(nonExistentFeedId)
        }

        @Test
        @DisplayName("태그가 없는 피드에 대해 삭제해도 예외가 발생하지 않는다")
        fun `피드 ID로 태그 삭제 - 태그 없는 피드`() {
            val feedId = FeedId.of(feed.id!!)

            feedTagRepository.deleteAllByFeedId(feedId)

            val foundTags = feedTagRepository.findByFeedId(feedId)
            assertThat(foundTags).isEmpty()
        }

        @Test
        @DisplayName("다른 피드의 태그는 삭제되지 않는다")
        fun `피드 ID로 태그 삭제 - 다른 피드 태그 유지`() {
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
            feedTagRepository.saveAll(
                listOf(FeedTag.create(title = "태그1", feedId = feedId1)),
                feedId1
            )
            feedTagRepository.saveAll(
                listOf(FeedTag.create(title = "태그2", feedId = feedId2)),
                feedId2
            )

            feedTagRepository.deleteAllByFeedId(feedId1)

            assertThat(feedTagRepository.findByFeedId(feedId1)).isEmpty()
            assertThat(feedTagRepository.findByFeedId(feedId2)).hasSize(1)
        }
    }
}
