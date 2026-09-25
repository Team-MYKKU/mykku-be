package com.example.mykku.contest.adapter.output.persistence

import com.example.mykku.BaseRepositoryTest
import com.example.mykku.contest.adapter.output.persistence.entity.ContestJpaEntity
import com.example.mykku.contest.adapter.output.persistence.repository.ContestJpaRepository
import com.example.mykku.contest.application.port.output.ContestRepository
import com.example.mykku.contest.domain.entity.Contest
import com.example.mykku.contest.domain.vo.ContestId
import com.example.mykku.contest.domain.vo.ContestListFilter
import com.example.mykku.contest.domain.vo.ContestSortType
import com.example.mykku.contest.domain.vo.ContestStatusType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import java.time.LocalDateTime

@DisplayName("ContestRepositoryAdapter 통합 테스트")
class ContestRepositoryAdapterTest : BaseRepositoryTest() {

    @Autowired
    private lateinit var contestRepository: ContestRepository

    @Autowired
    private lateinit var contestJpaRepository: ContestJpaRepository

    private fun createAndSaveContest(
        title: String = "테스트 콘테스트",
        description: String? = "테스트 설명",
        startedAt: LocalDateTime = LocalDateTime.now().minusDays(1),
        expiredAt: LocalDateTime = LocalDateTime.now().plusDays(7),
        status: ContestStatusType = ContestStatusType.ACTIVE
    ): ContestJpaEntity {
        val contest = ContestJpaEntity(
            title = title,
            description = description,
            startedAt = startedAt,
            expiredAt = expiredAt,
            status = status,
            thumbnailUrl = "https://example.com/thumbnail.jpg"
        )
        return contestJpaRepository.save(contest)
    }

    @Nested
    @DisplayName("save 메서드")
    inner class SaveTest {

        @Test
        @DisplayName("새로운 콘테스트를 저장할 수 있다")
        fun saveNewContest() {
            val contest = Contest.create(
                title = "새 콘테스트",
                description = "콘테스트 설명",
                startedAt = LocalDateTime.now(),
                expiredAt = LocalDateTime.now().plusDays(7),
                thumbnailUrl = "https://example.com/thumbnail.jpg"
            )

            val saved = contestRepository.save(contest)

            assertThat(saved.id.value).isGreaterThan(0)
            assertThat(saved.title).isEqualTo("새 콘테스트")
            assertThat(saved.description).isEqualTo("콘테스트 설명")
            assertThat(saved.status).isEqualTo(ContestStatusType.ACTIVE)
        }

        @Test
        @DisplayName("기존 콘테스트를 업데이트할 수 있다")
        fun updateExistingContest() {
            val savedEntity = createAndSaveContest(title = "원본 제목")
            val contest = savedEntity.toDomain()
            contest.updateStatus(ContestStatusType.WINNER_SELECTING)

            val updated = contestRepository.save(contest)

            assertThat(updated.id).isEqualTo(contest.id)
            assertThat(updated.status).isEqualTo(ContestStatusType.WINNER_SELECTING)
        }

        @Test
        @DisplayName("기존 콘테스트를 업데이트하면 createdAt이 보존되고 행이 추가되지 않는다")
        fun updateKeepsCreatedAt() {
            val savedEntity = createAndSaveContest(title = "원본 제목")
            contestJpaRepository.flush()
            val originalCreatedAt = savedEntity.createdAt
            Thread.sleep(5)
            val contest = savedEntity.toDomain()
            contest.updateStatus(ContestStatusType.WINNER_SELECTED)

            val updated = contestRepository.save(contest)

            assertThat(updated.createdAt).isEqualTo(originalCreatedAt)
            assertThat(updated.status).isEqualTo(ContestStatusType.WINNER_SELECTED)
            assertThat(contestJpaRepository.count()).isEqualTo(1)
        }
    }

    @Nested
    @DisplayName("findById 메서드")
    inner class FindByIdTest {

        @Test
        @DisplayName("ID로 콘테스트를 조회할 수 있다")
        fun findById() {
            val savedEntity = createAndSaveContest(title = "조회할 콘테스트")

            val found = contestRepository.findById(ContestId(savedEntity.id!!))

            assertThat(found).isNotNull
            assertThat(found!!.title).isEqualTo("조회할 콘테스트")
        }

        @Test
        @DisplayName("존재하지 않는 ID로 조회하면 null을 반환한다")
        fun findByIdNotFound() {
            val found = contestRepository.findById(ContestId(999999L))

            assertThat(found).isNull()
        }
    }

    @Nested
    @DisplayName("findByStatus 메서드")
    inner class FindByStatusTest {

        @Test
        @DisplayName("상태별로 콘테스트 목록을 조회할 수 있다")
        fun findByStatus() {
            createAndSaveContest(title = "활성 콘테스트1", status = ContestStatusType.ACTIVE)
            createAndSaveContest(title = "활성 콘테스트2", status = ContestStatusType.ACTIVE)
            createAndSaveContest(title = "만료 콘테스트", status = ContestStatusType.EXPIRED)

            val activeContests = contestRepository.findByStatus(ContestStatusType.ACTIVE)

            assertThat(activeContests).hasSize(2)
            assertThat(activeContests.map { it.title }).containsExactlyInAnyOrder("활성 콘테스트1", "활성 콘테스트2")
        }

        @Test
        @DisplayName("해당 상태의 콘테스트가 없으면 빈 목록을 반환한다")
        fun findByStatusEmpty() {
            createAndSaveContest(title = "활성 콘테스트", status = ContestStatusType.ACTIVE)

            val expiredContests = contestRepository.findByStatus(ContestStatusType.EXPIRED)

            assertThat(expiredContests).isEmpty()
        }
    }

    @Nested
    @DisplayName("findByExpiredAtAfter 메서드")
    inner class FindByExpiredAtAfterTest {

        @Test
        @DisplayName("만료 시간 이후인 콘테스트 목록을 조회할 수 있다")
        fun findByExpiredAtAfter() {
            val now = LocalDateTime.now()
            createAndSaveContest(title = "진행중 콘테스트", expiredAt = now.plusDays(7))
            createAndSaveContest(title = "만료된 콘테스트", expiredAt = now.minusDays(1))

            val activeContests = contestRepository.findByExpiredAtAfter(now)

            assertThat(activeContests).hasSize(1)
            assertThat(activeContests[0].title).isEqualTo("진행중 콘테스트")
        }
    }

    @Nested
    @DisplayName("findByStatusAndExpiredAtAfter 메서드")
    inner class FindByStatusAndExpiredAtAfterTest {

        @Test
        @DisplayName("상태와 만료 시간 조건으로 콘테스트를 조회할 수 있다")
        fun findByStatusAndExpiredAtAfter() {
            val now = LocalDateTime.now()
            createAndSaveContest(title = "활성 진행중", status = ContestStatusType.ACTIVE, expiredAt = now.plusDays(7))
            createAndSaveContest(title = "활성 만료됨", status = ContestStatusType.ACTIVE, expiredAt = now.minusDays(1))
            createAndSaveContest(title = "만료 상태", status = ContestStatusType.EXPIRED, expiredAt = now.plusDays(7))

            val results = contestRepository.findByStatusAndExpiredAtAfter(ContestStatusType.ACTIVE, now)

            assertThat(results).hasSize(1)
            assertThat(results[0].title).isEqualTo("활성 진행중")
        }
    }

    @Nested
    @DisplayName("findWithPagination 메서드")
    inner class FindWithPaginationTest {

        @Test
        @DisplayName("ALL 상태로 전체 콘테스트를 페이지네이션으로 조회할 수 있다")
        fun findWithPaginationAll() {
            createAndSaveContest(title = "콘테스트1")
            createAndSaveContest(title = "콘테스트2")
            createAndSaveContest(title = "콘테스트3")

            val page = contestRepository.findWithPagination(
                filter = ContestListFilter.ALL,
                sortType = ContestSortType.LATEST,
                pageable = PageRequest.of(0, 10),
                currentTime = LocalDateTime.now()
            )

            assertThat(page.content).hasSize(3)
            assertThat(page.totalElements).isEqualTo(3)
        }

        @Test
        @DisplayName("ACTIVE 상태를 LATEST 정렬로 조회할 수 있다")
        fun findWithPaginationActiveLatest() {
            val now = LocalDateTime.now()
            createAndSaveContest(title = "콘테스트1", expiredAt = now.plusDays(7))
            createAndSaveContest(title = "콘테스트2", expiredAt = now.plusDays(7))
            createAndSaveContest(title = "만료된 콘테스트", expiredAt = now.minusDays(1))

            val page = contestRepository.findWithPagination(
                filter = ContestListFilter.ACTIVE,
                sortType = ContestSortType.LATEST,
                pageable = PageRequest.of(0, 10),
                currentTime = now
            )

            assertThat(page.content).hasSize(2)
        }

        @Test
        @DisplayName("ACTIVE 상태를 OLDEST 정렬로 조회할 수 있다")
        fun findWithPaginationActiveOldest() {
            val now = LocalDateTime.now()
            createAndSaveContest(title = "콘테스트1", expiredAt = now.plusDays(7))
            createAndSaveContest(title = "콘테스트2", expiredAt = now.plusDays(7))

            val page = contestRepository.findWithPagination(
                filter = ContestListFilter.ACTIVE,
                sortType = ContestSortType.OLDEST,
                pageable = PageRequest.of(0, 10),
                currentTime = now
            )

            assertThat(page.content).hasSize(2)
        }

        @Test
        @DisplayName("ACTIVE 상태를 POPULAR 정렬로 조회할 수 있다")
        fun findWithPaginationActivePopular() {
            val now = LocalDateTime.now()
            createAndSaveContest(title = "이전 콘테스트", expiredAt = now.plusDays(7))
            Thread.sleep(10)
            createAndSaveContest(title = "최근 콘테스트", expiredAt = now.plusDays(7))

            val page = contestRepository.findWithPagination(
                filter = ContestListFilter.ACTIVE,
                sortType = ContestSortType.POPULAR,
                pageable = PageRequest.of(0, 10),
                currentTime = now
            )

            assertThat(page.content).hasSize(2)
            assertThat(page.content[0].title).isEqualTo("최근 콘테스트")
        }

        @Test
        @DisplayName("EXPIRED 상태로 만료된 콘테스트를 조회할 수 있다")
        fun findWithPaginationExpired() {
            val now = LocalDateTime.now()
            createAndSaveContest(title = "만료된 콘테스트", expiredAt = now.minusDays(1))
            createAndSaveContest(title = "진행중 콘테스트", expiredAt = now.plusDays(7))

            val page = contestRepository.findWithPagination(
                filter = ContestListFilter.EXPIRED,
                sortType = ContestSortType.LATEST,
                pageable = PageRequest.of(0, 10),
                currentTime = now
            )

            assertThat(page.content).hasSize(1)
            assertThat(page.content[0].title).isEqualTo("만료된 콘테스트")
        }

        @Test
        @DisplayName("페이지네이션이 올바르게 동작한다")
        fun paginationWorks() {
            val now = LocalDateTime.now()
            repeat(15) { index ->
                createAndSaveContest(title = "콘테스트$index", expiredAt = now.plusDays(7))
            }

            val firstPage = contestRepository.findWithPagination(
                filter = ContestListFilter.ACTIVE,
                sortType = ContestSortType.LATEST,
                pageable = PageRequest.of(0, 10),
                currentTime = now
            )

            val secondPage = contestRepository.findWithPagination(
                filter = ContestListFilter.ACTIVE,
                sortType = ContestSortType.LATEST,
                pageable = PageRequest.of(1, 10),
                currentTime = now
            )

            assertThat(firstPage.content).hasSize(10)
            assertThat(secondPage.content).hasSize(5)
            assertThat(firstPage.totalElements).isEqualTo(15)
        }

        @Test
        @DisplayName("WINNER_SELECTED 필터는 저장 상태가 WINNER_SELECTED인 콘테스트만 조회한다")
        fun findWithPaginationWinnerSelected() {
            val now = LocalDateTime.now()
            val yesterday = now.minusDays(1)
            createAndSaveContest(title = "선정 완료", expiredAt = yesterday, status = ContestStatusType.WINNER_SELECTED)
            createAndSaveContest(title = "만료만 됨", expiredAt = yesterday, status = ContestStatusType.ACTIVE)
            createAndSaveContest(title = "진행중", expiredAt = now.plusDays(7), status = ContestStatusType.ACTIVE)

            val page = contestRepository.findWithPagination(
                filter = ContestListFilter.WINNER_SELECTED,
                sortType = ContestSortType.LATEST,
                pageable = PageRequest.of(0, 10),
                currentTime = now
            )

            assertThat(page.content).hasSize(1)
            assertThat(page.content[0].title).isEqualTo("선정 완료")
        }

        @Test
        @DisplayName("EXPIRED 필터는 저장 상태와 무관하게 만료된 콘테스트를 모두 조회한다")
        fun findWithPaginationExpiredIncludesWinnerSelected() {
            val now = LocalDateTime.now()
            val yesterday = now.minusDays(1)
            createAndSaveContest(title = "선정 완료", expiredAt = yesterday, status = ContestStatusType.WINNER_SELECTED)
            createAndSaveContest(title = "만료만 됨", expiredAt = yesterday, status = ContestStatusType.ACTIVE)
            createAndSaveContest(title = "진행중", expiredAt = now.plusDays(7), status = ContestStatusType.ACTIVE)

            val page = contestRepository.findWithPagination(
                filter = ContestListFilter.EXPIRED,
                sortType = ContestSortType.LATEST,
                pageable = PageRequest.of(0, 10),
                currentTime = now
            )

            assertThat(page.content.map { it.title }).containsExactlyInAnyOrder("선정 완료", "만료만 됨")
        }

        @Test
        @DisplayName("PENDING_SELECTION 필터는 만료됐고 수상자를 선정하지 않은 콘테스트만 조회한다")
        fun findWithPaginationPendingSelection() {
            val now = LocalDateTime.now()
            val yesterday = now.minusDays(1)
            createAndSaveContest(title = "선정 완료", expiredAt = yesterday, status = ContestStatusType.WINNER_SELECTED)
            createAndSaveContest(title = "만료만 됨", expiredAt = yesterday, status = ContestStatusType.ACTIVE)
            createAndSaveContest(title = "지금 만료", expiredAt = now, status = ContestStatusType.ACTIVE)
            createAndSaveContest(title = "진행중", expiredAt = now.plusDays(7), status = ContestStatusType.ACTIVE)

            val page = contestRepository.findWithPagination(
                filter = ContestListFilter.PENDING_SELECTION,
                sortType = ContestSortType.LATEST,
                pageable = PageRequest.of(0, 10),
                currentTime = now
            )

            assertThat(page.content.map { it.title }).containsExactlyInAnyOrder("만료만 됨", "지금 만료")
        }
    }
}
