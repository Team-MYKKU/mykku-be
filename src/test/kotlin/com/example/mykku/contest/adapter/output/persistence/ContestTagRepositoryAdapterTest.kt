package com.example.mykku.contest.adapter.output.persistence

import com.example.mykku.BaseRepositoryTest
import com.example.mykku.contest.application.port.output.ContestTagRepository
import com.example.mykku.contest.domain.entity.ContestTag
import com.example.mykku.contest.domain.vo.ContestId
import com.example.mykku.contest.exception.ContestException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@DisplayName("ContestTagRepositoryAdapter 통합 테스트")
class ContestTagRepositoryAdapterTest : BaseRepositoryTest() {

    @Autowired
    private lateinit var contestTagRepository: ContestTagRepository

    @Nested
    @DisplayName("save 메서드")
    inner class SaveTest {

        @Test
        @DisplayName("콘테스트 태그를 저장할 수 있다")
        fun saveContestTag() {
            val contest = createAndSaveContest()
            val tag = ContestTag.create(
                title = "테스트태그",
                contestId = ContestId(contest.id!!)
            )

            val saved = contestTagRepository.save(tag)

            assertThat(saved.id.value).isGreaterThan(0)
            assertThat(saved.title).isEqualTo("테스트태그")
            assertThat(saved.contestId.value).isEqualTo(contest.id)
        }

        @Test
        @DisplayName("존재하지 않는 콘테스트에 태그 저장시 예외가 발생한다")
        fun saveContestTagWithInvalidContest() {
            val tag = ContestTag.create(
                title = "테스트태그",
                contestId = ContestId(999999L)
            )

            assertThatThrownBy { contestTagRepository.save(tag) }
                .isInstanceOf(ContestException::class.java)
        }
    }

    @Nested
    @DisplayName("saveAll 메서드")
    inner class SaveAllTest {

        @Test
        @DisplayName("여러 콘테스트 태그를 한번에 저장할 수 있다")
        fun saveAllContestTags() {
            val contest = createAndSaveContest()
            val tags = listOf(
                ContestTag.create(title = "태그1", contestId = ContestId(contest.id!!)),
                ContestTag.create(title = "태그2", contestId = ContestId(contest.id!!)),
                ContestTag.create(title = "태그3", contestId = ContestId(contest.id!!))
            )

            val savedTags = contestTagRepository.saveAll(tags)

            assertThat(savedTags).hasSize(3)
            assertThat(savedTags.map { it.title }).containsExactlyInAnyOrder("태그1", "태그2", "태그3")
        }

        @Test
        @DisplayName("빈 리스트로 저장하면 빈 리스트를 반환한다")
        fun saveAllEmptyList() {
            val savedTags = contestTagRepository.saveAll(emptyList())

            assertThat(savedTags).isEmpty()
        }

        @Test
        @DisplayName("존재하지 않는 콘테스트에 태그 저장시 예외가 발생한다")
        fun saveAllWithInvalidContest() {
            val tags = listOf(
                ContestTag.create(title = "태그1", contestId = ContestId(999999L))
            )

            assertThatThrownBy { contestTagRepository.saveAll(tags) }
                .isInstanceOf(ContestException::class.java)
        }
    }

    @Nested
    @DisplayName("findByContestIds 메서드")
    inner class FindByContestIdsTest {

        @Test
        @DisplayName("콘테스트 ID 목록으로 태그를 조회할 수 있다")
        fun findByContestIds() {
            val contest1 = createAndSaveContest(title = "콘테스트1")
            val contest2 = createAndSaveContest(title = "콘테스트2")

            contestTagRepository.saveAll(
                listOf(
                    ContestTag.create("태그1", ContestId(contest1.id!!)),
                    ContestTag.create("태그2", ContestId(contest1.id!!)),
                    ContestTag.create("태그3", ContestId(contest2.id!!))
                )
            )

            val tags = contestTagRepository.findByContestIds(
                listOf(ContestId(contest1.id!!), ContestId(contest2.id!!))
            )

            assertThat(tags).hasSize(3)
        }

        @Test
        @DisplayName("빈 콘테스트 ID 목록으로 조회하면 빈 리스트를 반환한다")
        fun findByContestIdsEmpty() {
            val tags = contestTagRepository.findByContestIds(emptyList())

            assertThat(tags).isEmpty()
        }

        @Test
        @DisplayName("존재하지 않는 콘테스트 ID로 조회하면 빈 리스트를 반환한다")
        fun findByContestIdsNotFound() {
            val tags = contestTagRepository.findByContestIds(listOf(ContestId(999999L)))

            assertThat(tags).isEmpty()
        }

        @Test
        @DisplayName("특정 콘테스트의 태그만 조회할 수 있다")
        fun findBySpecificContestId() {
            val contest1 = createAndSaveContest(title = "콘테스트1")
            val contest2 = createAndSaveContest(title = "콘테스트2")

            contestTagRepository.saveAll(
                listOf(
                    ContestTag.create("태그1", ContestId(contest1.id!!)),
                    ContestTag.create("태그2", ContestId(contest1.id!!)),
                    ContestTag.create("태그3", ContestId(contest2.id!!))
                )
            )

            val tags = contestTagRepository.findByContestIds(listOf(ContestId(contest1.id!!)))

            assertThat(tags).hasSize(2)
            assertThat(tags.all { it.contestId.value == contest1.id }).isTrue()
        }
    }

    @Nested
    @DisplayName("findAllByTitleIn 메서드")
    inner class FindAllByTitleInTest {

        @Test
        @DisplayName("태그 제목 목록으로 태그를 조회할 수 있다")
        fun findAllByTitleIn() {
            val contest = createAndSaveContest()
            val (tagA, tagB) = contestTagRepository.saveAll(
                listOf(
                    ContestTag.create("태그A", ContestId(contest.id!!)),
                    ContestTag.create("태그B", ContestId(contest.id!!)),
                    ContestTag.create("태그C", ContestId(contest.id!!))
                )
            )

            val tags = contestTagRepository.findAllByTitleIn(listOf("태그A", "태그B"))

            assertThat(tags.map { it.id }).containsExactlyInAnyOrder(tagA.id, tagB.id)
            assertThat(tags.map { it.title }).containsExactlyInAnyOrder("태그A", "태그B")
        }

        @Test
        @DisplayName("빈 제목 목록으로 조회하면 빈 리스트를 반환한다")
        fun findAllByTitleInEmpty() {
            val tags = contestTagRepository.findAllByTitleIn(emptyList())

            assertThat(tags).isEmpty()
        }

        @Test
        @DisplayName("존재하지 않는 제목으로 조회하면 빈 리스트를 반환한다")
        fun findAllByTitleInNotFound() {
            val tags = contestTagRepository.findAllByTitleIn(listOf("존재하지않는태그"))

            assertThat(tags).isEmpty()
        }

        @Test
        @DisplayName("일부만 존재하는 제목으로 조회하면 존재하는 것만 반환한다")
        fun findAllByTitleInPartial() {
            val contest = createAndSaveContest()
            val savedTags = contestTagRepository.saveAll(
                listOf(
                    ContestTag.create("존재하는태그", ContestId(contest.id!!))
                )
            )

            val tags = contestTagRepository.findAllByTitleIn(listOf("존재하는태그", "없는태그"))

            assertThat(tags.map { it.id }).containsExactly(savedTags.single().id)
            assertThat(tags[0].title).isEqualTo("존재하는태그")
        }
    }
}
