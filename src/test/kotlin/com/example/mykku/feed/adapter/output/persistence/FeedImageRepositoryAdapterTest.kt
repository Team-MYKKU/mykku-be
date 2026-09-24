package com.example.mykku.feed.adapter.output.persistence

import com.example.mykku.BaseRepositoryTest
import com.example.mykku.board.adapter.output.persistence.entity.BoardJpaEntity
import com.example.mykku.feed.adapter.output.persistence.entity.FeedJpaEntity
import com.example.mykku.feed.application.port.output.FeedImageRepository
import com.example.mykku.feed.domain.entity.FeedImage
import com.example.mykku.feed.domain.vo.FeedId
import com.example.mykku.member.adapter.output.persistence.entity.MemberJpaEntity
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional

@DisplayName("FeedImageRepositoryAdapter 통합 테스트")
@Transactional
class FeedImageRepositoryAdapterTest : BaseRepositoryTest() {

    @Autowired
    private lateinit var feedImageRepository: FeedImageRepository

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
        @DisplayName("피드 이미지 목록을 정상적으로 저장한다")
        fun `피드 이미지 저장 - 정상 케이스`() {
            val feedId = FeedId.of(feed.id!!)
            val images = listOf(
                FeedImage.create(url = "http://example.com/image1.jpg", width = 800, height = 600, feedId = feedId),
                FeedImage.create(url = "http://example.com/image2.jpg", width = 1024, height = 768, feedId = feedId)
            )

            val savedImages = feedImageRepository.saveAll(images, feedId)

            assertThat(savedImages).hasSize(2)
            assertThat(savedImages[0].id).isNotNull()
            assertThat(savedImages[0].url).isEqualTo("http://example.com/image1.jpg")
            assertThat(savedImages[1].url).isEqualTo("http://example.com/image2.jpg")
        }

        @Test
        @DisplayName("빈 목록을 저장하면 빈 목록을 반환한다")
        fun `피드 이미지 저장 - 빈 목록`() {
            val feedId = FeedId.of(feed.id!!)
            val images = emptyList<FeedImage>()

            val savedImages = feedImageRepository.saveAll(images, feedId)

            assertThat(savedImages).isEmpty()
        }

        @Test
        @DisplayName("이미지 크기 정보가 올바르게 저장된다")
        fun `피드 이미지 저장 - 크기 정보 검증`() {
            val feedId = FeedId.of(feed.id!!)
            val image = FeedImage.create(
                url = "http://example.com/image.jpg",
                width = 1920,
                height = 1080,
                feedId = feedId
            )

            val savedImages = feedImageRepository.saveAll(listOf(image), feedId)

            assertThat(savedImages[0].width).isEqualTo(1920)
            assertThat(savedImages[0].height).isEqualTo(1080)
        }
    }

    @Nested
    @DisplayName("findByFeedId 메서드")
    inner class FindByFeedId {

        @Test
        @DisplayName("피드 ID로 이미지 목록을 조회한다")
        fun `피드 이미지 조회 - 정상 케이스`() {
            val feedId = FeedId.of(feed.id!!)
            val images = listOf(
                FeedImage.create(url = "http://example.com/image1.jpg", width = 800, height = 600, feedId = feedId),
                FeedImage.create(url = "http://example.com/image2.jpg", width = 1024, height = 768, feedId = feedId)
            )
            feedImageRepository.saveAll(images, feedId)

            val foundImages = feedImageRepository.findByFeedId(feedId)

            assertThat(foundImages).hasSize(2)
        }

        @Test
        @DisplayName("이미지가 없는 피드의 경우 빈 목록을 반환한다")
        fun `피드 이미지 조회 - 이미지 없음`() {
            val feedId = FeedId.of(feed.id!!)

            val foundImages = feedImageRepository.findByFeedId(feedId)

            assertThat(foundImages).isEmpty()
        }

        @Test
        @DisplayName("존재하지 않는 피드 ID로 조회하면 빈 목록을 반환한다")
        fun `피드 이미지 조회 - 존재하지 않는 피드`() {
            val nonExistentFeedId = FeedId.of(99999L)

            val foundImages = feedImageRepository.findByFeedId(nonExistentFeedId)

            assertThat(foundImages).isEmpty()
        }
    }

    @Nested
    @DisplayName("findByFeedIds 메서드")
    inner class FindByFeedIds {

        @Test
        @DisplayName("여러 피드 ID로 이미지 목록을 조회한다")
        fun `여러 피드 이미지 조회 - 정상 케이스`() {
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
            feedImageRepository.saveAll(
                listOf(FeedImage.create(url = "http://example.com/image1.jpg", width = 800, height = 600, feedId = feedId1)),
                feedId1
            )
            feedImageRepository.saveAll(
                listOf(FeedImage.create(url = "http://example.com/image2.jpg", width = 1024, height = 768, feedId = feedId2)),
                feedId2
            )

            val foundImages = feedImageRepository.findByFeedIds(listOf(feedId1, feedId2))

            assertThat(foundImages).hasSize(2)
        }

        @Test
        @DisplayName("빈 피드 ID 목록으로 조회하면 빈 목록을 반환한다")
        fun `여러 피드 이미지 조회 - 빈 목록`() {
            val foundImages = feedImageRepository.findByFeedIds(emptyList())

            assertThat(foundImages).isEmpty()
        }

        @Test
        @DisplayName("존재하지 않는 피드 ID가 포함되어도 존재하는 이미지만 반환한다")
        fun `여러 피드 이미지 조회 - 일부 존재하지 않는 피드`() {
            val feedId = FeedId.of(feed.id!!)
            feedImageRepository.saveAll(
                listOf(FeedImage.create(url = "http://example.com/image.jpg", width = 800, height = 600, feedId = feedId)),
                feedId
            )

            val foundImages = feedImageRepository.findByFeedIds(listOf(feedId, FeedId.of(99999L)))

            assertThat(foundImages).hasSize(1)
        }
    }

    @Nested
    @DisplayName("findThumbnailUrlsByFeedIds 메서드")
    inner class FindThumbnailUrlsByFeedIds {

        @Test
        @DisplayName("피드마다 id가 가장 작은 이미지의 URL을 썸네일로 반환한다")
        fun `썸네일 조회 - id가 가장 작은 이미지`() {
            val feed2 = saveFeed("테스트 피드 2")
            val feedId1 = FeedId.of(feed.id!!)
            val feedId2 = FeedId.of(feed2.id!!)
            saveImage(feedId1, "http://example.com/feed1-first.jpg")
            saveImage(feedId2, "http://example.com/feed2-first.jpg")
            saveImage(feedId1, "http://example.com/feed1-second.jpg")
            saveImage(feedId2, "http://example.com/feed2-second.jpg")

            val thumbnails = feedImageRepository.findThumbnailUrlsByFeedIds(listOf(feedId1, feedId2))

            assertThat(thumbnails).containsExactlyInAnyOrderEntriesOf(
                mapOf(
                    feedId1 to "http://example.com/feed1-first.jpg",
                    feedId2 to "http://example.com/feed2-first.jpg"
                )
            )
        }

        @Test
        @DisplayName("이미지가 없는 피드는 결과에 포함되지 않는다")
        fun `썸네일 조회 - 이미지 없는 피드 제외`() {
            val feedWithoutImage = saveFeed("이미지 없는 피드")
            val feedId = FeedId.of(feed.id!!)
            saveImage(feedId, "http://example.com/image.jpg")

            val thumbnails = feedImageRepository.findThumbnailUrlsByFeedIds(
                listOf(feedId, FeedId.of(feedWithoutImage.id!!))
            )

            assertThat(thumbnails).containsExactlyEntriesOf(mapOf(feedId to "http://example.com/image.jpg"))
        }

        @Test
        @DisplayName("요청하지 않은 피드의 이미지는 포함되지 않는다")
        fun `썸네일 조회 - 요청하지 않은 피드 제외`() {
            val otherFeed = saveFeed("다른 피드")
            saveImage(FeedId.of(otherFeed.id!!), "http://example.com/other.jpg")

            val thumbnails = feedImageRepository.findThumbnailUrlsByFeedIds(listOf(FeedId.of(feed.id!!)))

            assertThat(thumbnails).isEmpty()
        }

        @Test
        @DisplayName("빈 피드 ID 목록으로 조회하면 빈 맵을 반환한다")
        fun `썸네일 조회 - 빈 목록`() {
            val thumbnails = feedImageRepository.findThumbnailUrlsByFeedIds(emptyList())

            assertThat(thumbnails).isEmpty()
        }

        private fun saveFeed(title: String): FeedJpaEntity {
            return feedJpaRepository.save(
                FeedJpaEntity(title = title, content = "테스트 내용", board = board, member = member)
            )
        }

        private fun saveImage(feedId: FeedId, url: String) {
            feedImageRepository.saveAll(
                listOf(FeedImage.create(url = url, width = 800, height = 600, feedId = feedId)),
                feedId
            )
        }
    }

    @Nested
    @DisplayName("findAllByIdInAndFeedId 메서드")
    inner class FindAllByIdInAndFeedId {

        @Test
        @DisplayName("특정 피드의 이미지 중 ID 목록에 해당하는 이미지를 조회한다")
        fun `ID와 피드 ID로 이미지 조회 - 정상 케이스`() {
            val feedId = FeedId.of(feed.id!!)
            val savedImages = feedImageRepository.saveAll(
                listOf(
                    FeedImage.create(url = "http://example.com/image1.jpg", width = 800, height = 600, feedId = feedId),
                    FeedImage.create(url = "http://example.com/image2.jpg", width = 1024, height = 768, feedId = feedId),
                    FeedImage.create(url = "http://example.com/image3.jpg", width = 640, height = 480, feedId = feedId)
                ),
                feedId
            )
            val targetIds = listOf(savedImages[0].id!!.value, savedImages[1].id!!.value)

            val foundImages = feedImageRepository.findAllByIdInAndFeedId(targetIds, feedId)

            assertThat(foundImages).hasSize(2)
        }

        @Test
        @DisplayName("빈 ID 목록으로 조회하면 빈 목록을 반환한다")
        fun `ID와 피드 ID로 이미지 조회 - 빈 ID 목록`() {
            val feedId = FeedId.of(feed.id!!)

            val foundImages = feedImageRepository.findAllByIdInAndFeedId(emptyList(), feedId)

            assertThat(foundImages).isEmpty()
        }

        @Test
        @DisplayName("다른 피드의 이미지 ID는 조회되지 않는다")
        fun `ID와 피드 ID로 이미지 조회 - 다른 피드 이미지 제외`() {
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
            val images1 = feedImageRepository.saveAll(
                listOf(FeedImage.create(url = "http://example.com/image1.jpg", width = 800, height = 600, feedId = feedId1)),
                feedId1
            )
            val images2 = feedImageRepository.saveAll(
                listOf(FeedImage.create(url = "http://example.com/image2.jpg", width = 1024, height = 768, feedId = feedId2)),
                feedId2
            )

            val foundImages = feedImageRepository.findAllByIdInAndFeedId(
                listOf(images1[0].id!!.value, images2[0].id!!.value),
                feedId1
            )

            assertThat(foundImages).hasSize(1)
            assertThat(foundImages[0].feedId).isEqualTo(feedId1)
        }
    }

    @Nested
    @DisplayName("deleteAll 메서드")
    inner class DeleteAll {

        @Test
        @DisplayName("피드 이미지 목록을 삭제한다")
        fun `피드 이미지 삭제 - 정상 케이스`() {
            val feedId = FeedId.of(feed.id!!)
            val savedImages = feedImageRepository.saveAll(
                listOf(
                    FeedImage.create(url = "http://example.com/image1.jpg", width = 800, height = 600, feedId = feedId),
                    FeedImage.create(url = "http://example.com/image2.jpg", width = 1024, height = 768, feedId = feedId)
                ),
                feedId
            )

            feedImageRepository.deleteAll(savedImages)

            val foundImages = feedImageRepository.findByFeedId(feedId)
            assertThat(foundImages).isEmpty()
        }

        @Test
        @DisplayName("빈 목록을 삭제해도 예외가 발생하지 않는다")
        fun `피드 이미지 삭제 - 빈 목록`() {
            feedImageRepository.deleteAll(emptyList())
        }
    }

    @Nested
    @DisplayName("deleteAllByIds 메서드")
    inner class DeleteAllByIds {

        @Test
        @DisplayName("ID 목록으로 이미지를 삭제한다")
        fun `ID로 이미지 삭제 - 정상 케이스`() {
            val feedId = FeedId.of(feed.id!!)
            val savedImages = feedImageRepository.saveAll(
                listOf(
                    FeedImage.create(url = "http://example.com/image1.jpg", width = 800, height = 600, feedId = feedId),
                    FeedImage.create(url = "http://example.com/image2.jpg", width = 1024, height = 768, feedId = feedId)
                ),
                feedId
            )
            val idsToDelete = savedImages.map { it.id!!.value }

            feedImageRepository.deleteAllByIds(idsToDelete)

            val foundImages = feedImageRepository.findByFeedId(feedId)
            assertThat(foundImages).isEmpty()
        }

        @Test
        @DisplayName("빈 ID 목록으로 삭제해도 예외가 발생하지 않는다")
        fun `ID로 이미지 삭제 - 빈 목록`() {
            feedImageRepository.deleteAllByIds(emptyList())
        }

        @Test
        @DisplayName("일부 ID만 삭제하면 나머지 이미지는 유지된다")
        fun `ID로 이미지 삭제 - 일부 삭제`() {
            val feedId = FeedId.of(feed.id!!)
            val savedImages = feedImageRepository.saveAll(
                listOf(
                    FeedImage.create(url = "http://example.com/image1.jpg", width = 800, height = 600, feedId = feedId),
                    FeedImage.create(url = "http://example.com/image2.jpg", width = 1024, height = 768, feedId = feedId),
                    FeedImage.create(url = "http://example.com/image3.jpg", width = 640, height = 480, feedId = feedId)
                ),
                feedId
            )

            feedImageRepository.deleteAllByIds(listOf(savedImages[0].id!!.value))

            val foundImages = feedImageRepository.findByFeedId(feedId)
            assertThat(foundImages).hasSize(2)
        }
    }

    @Nested
    @DisplayName("deleteAllByFeedId 메서드")
    inner class DeleteAllByFeedId {

        @Test
        @DisplayName("피드 ID로 모든 이미지를 삭제한다")
        fun `피드 ID로 이미지 삭제 - 정상 케이스`() {
            val feedId = FeedId.of(feed.id!!)
            feedImageRepository.saveAll(
                listOf(
                    FeedImage.create(url = "http://example.com/image1.jpg", width = 800, height = 600, feedId = feedId),
                    FeedImage.create(url = "http://example.com/image2.jpg", width = 1024, height = 768, feedId = feedId)
                ),
                feedId
            )

            feedImageRepository.deleteAllByFeedId(feedId)

            val foundImages = feedImageRepository.findByFeedId(feedId)
            assertThat(foundImages).isEmpty()
        }

        @Test
        @DisplayName("존재하지 않는 피드 ID로 삭제해도 예외가 발생하지 않는다")
        fun `피드 ID로 이미지 삭제 - 존재하지 않는 피드`() {
            val nonExistentFeedId = FeedId.of(99999L)

            feedImageRepository.deleteAllByFeedId(nonExistentFeedId)
        }

        @Test
        @DisplayName("다른 피드의 이미지는 삭제되지 않는다")
        fun `피드 ID로 이미지 삭제 - 다른 피드 이미지 유지`() {
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
            feedImageRepository.saveAll(
                listOf(FeedImage.create(url = "http://example.com/image1.jpg", width = 800, height = 600, feedId = feedId1)),
                feedId1
            )
            feedImageRepository.saveAll(
                listOf(FeedImage.create(url = "http://example.com/image2.jpg", width = 1024, height = 768, feedId = feedId2)),
                feedId2
            )

            feedImageRepository.deleteAllByFeedId(feedId1)

            assertThat(feedImageRepository.findByFeedId(feedId1)).isEmpty()
            assertThat(feedImageRepository.findByFeedId(feedId2)).hasSize(1)
        }
    }
}
