package com.example.mykku.contest.adapter.output.persistence

import com.example.mykku.BaseRepositoryTest
import com.example.mykku.contest.application.port.output.ContestImageRepository
import com.example.mykku.contest.domain.entity.ContestImage
import com.example.mykku.contest.domain.vo.ContestId
import com.example.mykku.contest.exception.ContestException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@DisplayName("ContestImageRepositoryAdapter 통합 테스트")
class ContestImageRepositoryAdapterTest : BaseRepositoryTest() {

    @Autowired
    private lateinit var contestImageRepository: ContestImageRepository

    @Nested
    @DisplayName("save 메서드")
    inner class SaveTest {

        @Test
        @DisplayName("콘테스트 이미지를 저장할 수 있다")
        fun saveContestImage() {
            val contest = createAndSaveContest()
            val image = ContestImage.create(
                url = "https://example.com/image.jpg",
                orderIndex = 0,
                contestId = ContestId(contest.id!!)
            )

            val saved = contestImageRepository.save(image)

            assertThat(saved.id.value).isGreaterThan(0)
            assertThat(saved.url).isEqualTo("https://example.com/image.jpg")
            assertThat(saved.orderIndex).isEqualTo(0)
            assertThat(saved.contestId.value).isEqualTo(contest.id)
        }

        @Test
        @DisplayName("존재하지 않는 콘테스트에 이미지 저장시 예외가 발생한다")
        fun saveContestImageWithInvalidContest() {
            val image = ContestImage.create(
                url = "https://example.com/image.jpg",
                orderIndex = 0,
                contestId = ContestId(999999L)
            )

            assertThatThrownBy { contestImageRepository.save(image) }
                .isInstanceOf(ContestException::class.java)
        }
    }

    @Nested
    @DisplayName("saveAll 메서드")
    inner class SaveAllTest {

        @Test
        @DisplayName("여러 콘테스트 이미지를 한번에 저장할 수 있다")
        fun saveAllContestImages() {
            val contest = createAndSaveContest()
            val images = listOf(
                ContestImage.create(
                    url = "https://example.com/image1.jpg",
                    orderIndex = 0,
                    contestId = ContestId(contest.id!!)
                ),
                ContestImage.create(
                    url = "https://example.com/image2.jpg",
                    orderIndex = 1,
                    contestId = ContestId(contest.id!!)
                ),
                ContestImage.create(
                    url = "https://example.com/image3.jpg",
                    orderIndex = 2,
                    contestId = ContestId(contest.id!!)
                )
            )

            val savedImages = contestImageRepository.saveAll(images)

            assertThat(savedImages).hasSize(3)
            assertThat(savedImages.map { it.url }).containsExactlyInAnyOrder(
                "https://example.com/image1.jpg",
                "https://example.com/image2.jpg",
                "https://example.com/image3.jpg"
            )
        }

        @Test
        @DisplayName("빈 리스트로 저장하면 빈 리스트를 반환한다")
        fun saveAllEmptyList() {
            val savedImages = contestImageRepository.saveAll(emptyList())

            assertThat(savedImages).isEmpty()
        }

        @Test
        @DisplayName("존재하지 않는 콘테스트에 이미지 저장시 예외가 발생한다")
        fun saveAllWithInvalidContest() {
            val images = listOf(
                ContestImage.create(
                    url = "https://example.com/image1.jpg",
                    orderIndex = 0,
                    contestId = ContestId(999999L)
                )
            )

            assertThatThrownBy { contestImageRepository.saveAll(images) }
                .isInstanceOf(ContestException::class.java)
        }
    }

    @Nested
    @DisplayName("findByContestIds 메서드")
    inner class FindByContestIdsTest {

        @Test
        @DisplayName("콘테스트 ID 목록으로 이미지를 조회할 수 있다")
        fun findByContestIds() {
            val contest1 = createAndSaveContest(title = "콘테스트1")
            val contest2 = createAndSaveContest(title = "콘테스트2")

            contestImageRepository.saveAll(
                listOf(
                    ContestImage.create("url1", 0, ContestId(contest1.id!!)),
                    ContestImage.create("url2", 1, ContestId(contest1.id!!)),
                    ContestImage.create("url3", 0, ContestId(contest2.id!!))
                )
            )

            val images = contestImageRepository.findByContestIds(
                listOf(ContestId(contest1.id!!), ContestId(contest2.id!!))
            )

            assertThat(images).hasSize(3)
        }

        @Test
        @DisplayName("빈 콘테스트 ID 목록으로 조회하면 빈 리스트를 반환한다")
        fun findByContestIdsEmpty() {
            val images = contestImageRepository.findByContestIds(emptyList())

            assertThat(images).isEmpty()
        }

        @Test
        @DisplayName("존재하지 않는 콘테스트 ID로 조회하면 빈 리스트를 반환한다")
        fun findByContestIdsNotFound() {
            val images = contestImageRepository.findByContestIds(listOf(ContestId(999999L)))

            assertThat(images).isEmpty()
        }

        @Test
        @DisplayName("특정 콘테스트의 이미지만 조회할 수 있다")
        fun findBySpecificContestId() {
            val contest1 = createAndSaveContest(title = "콘테스트1")
            val contest2 = createAndSaveContest(title = "콘테스트2")

            contestImageRepository.saveAll(
                listOf(
                    ContestImage.create("url1", 0, ContestId(contest1.id!!)),
                    ContestImage.create("url2", 1, ContestId(contest1.id!!)),
                    ContestImage.create("url3", 0, ContestId(contest2.id!!))
                )
            )

            val images = contestImageRepository.findByContestIds(listOf(ContestId(contest1.id!!)))

            assertThat(images).hasSize(2)
            assertThat(images.all { it.contestId.value == contest1.id }).isTrue()
        }
    }
}
