package com.example.mykku.contest.adapter.output.persistence

import com.example.mykku.BaseRepositoryTest
import com.example.mykku.contest.adapter.output.persistence.repository.ContestJpaRepository
import com.example.mykku.contest.application.port.output.ContestRepository
import com.example.mykku.contest.domain.entity.Contest
import com.example.mykku.contest.domain.vo.ContestId
import com.example.mykku.contest.domain.vo.ContestListFilter
import com.example.mykku.contest.domain.vo.ContestSortType
import com.example.mykku.contest.domain.vo.ContestStatusType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@DisplayName("ContestRepositoryAdapter 통합 테스트")
class ContestRepositoryAdapterTest : BaseRepositoryTest() {

    @Autowired
    private lateinit var contestRepository: ContestRepository

    @Autowired
    private lateinit var contestJpaRepository: ContestJpaRepository

    @Nested
    @DisplayName("save 메서드")
    inner class SaveTest {

        @Test
        @DisplayName("새로운 콘테스트를 저장할 수 있다")
        fun saveNewContest() {
            val contest = Contest.create(
                title = "새 콘테스트",
                subTitle = "새 부제",
                description = "콘테스트 설명",
                startedAt = LocalDateTime.now(),
                expiredAt = LocalDateTime.now().plusDays(7),
                thumbnailUrl = "https://example.com/thumbnail.jpg"
            )

            val saved = contestRepository.save(contest)

            assertThat(saved.id.value).isGreaterThan(0)
            assertThat(saved.title).isEqualTo("새 콘테스트")
            assertThat(contestRepository.findById(saved.id)?.subTitle).isEqualTo("새 부제")
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
            flushAndClear()

            assertThat(updated.id).isEqualTo(contest.id)
            assertThat(updated.status).isEqualTo(ContestStatusType.WINNER_SELECTING)
            val persisted = contestRepository.findById(contest.id)!!
            assertThat(persisted.status).isEqualTo(ContestStatusType.WINNER_SELECTING)
            assertThat(persisted.title).isEqualTo("원본 제목")
        }

        @Test
        @DisplayName("기존 콘테스트를 업데이트하면 createdAt이 보존되고 행이 추가되지 않는다")
        fun updateKeepsCreatedAt() {
            val rowsBefore = contestJpaRepository.count()
            val savedEntity = createAndSaveContest(title = "원본 제목")
            contestJpaRepository.flush()
            val originalCreatedAt = savedEntity.createdAt
            Thread.sleep(5)
            val contest = savedEntity.toDomain()
            contest.updateStatus(ContestStatusType.WINNER_SELECTED)

            val updated = contestRepository.save(contest)
            flushAndClear()

            assertThat(updated.createdAt).isEqualTo(originalCreatedAt)
            assertThat(updated.status).isEqualTo(ContestStatusType.WINNER_SELECTED)
            assertThat(contestJpaRepository.count()).isEqualTo(rowsBefore + 1)
            assertPersistedUpdate(contest.id, originalCreatedAt)
        }

        private fun assertPersistedUpdate(id: ContestId, originalCreatedAt: LocalDateTime) {
            val persisted = contestRepository.findById(id)!!
            assertThat(persisted.status).isEqualTo(ContestStatusType.WINNER_SELECTED)
            assertThat(persisted.createdAt).isCloseTo(originalCreatedAt, within(1, ChronoUnit.MICROS))
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
            val active1 = createAndSaveContest(title = "활성 콘테스트1", status = ContestStatusType.ACTIVE)
            val active2 = createAndSaveContest(title = "활성 콘테스트2", status = ContestStatusType.ACTIVE)
            createAndSaveContest(title = "만료 콘테스트", status = ContestStatusType.EXPIRED)

            val activeContests = contestRepository.findByStatus(ContestStatusType.ACTIVE)

            assertThat(activeContests.map { it.id.value }).containsExactlyInAnyOrder(active1.id!!, active2.id!!)
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
            val ongoing = createAndSaveContest(title = "진행중 콘테스트", expiredAt = now.plusDays(7))
            createAndSaveContest(title = "만료된 콘테스트", expiredAt = now.minusDays(1))

            val activeContests = contestRepository.findByExpiredAtAfter(now)

            assertThat(activeContests.map { it.id.value }).containsExactly(ongoing.id!!)
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
            val activeOngoing = createAndSaveContest(
                title = "활성 진행중",
                status = ContestStatusType.ACTIVE,
                expiredAt = now.plusDays(7)
            )
            createAndSaveContest(title = "활성 만료됨", status = ContestStatusType.ACTIVE, expiredAt = now.minusDays(1))
            createAndSaveContest(title = "만료 상태", status = ContestStatusType.EXPIRED, expiredAt = now.plusDays(7))

            val results = contestRepository.findByStatusAndExpiredAtAfter(ContestStatusType.ACTIVE, now)

            assertThat(results.map { it.id.value }).containsExactly(activeOngoing.id!!)
            assertThat(results[0].title).isEqualTo("활성 진행중")
        }
    }

    @Nested
    @DisplayName("findWithPagination 메서드")
    inner class FindWithPaginationTest {

        @Test
        @DisplayName("ALL 상태로 전체 콘테스트를 페이지네이션으로 조회할 수 있다")
        fun findWithPaginationAll() {
            val contest1 = createAndSaveContest(title = "콘테스트1")
            val contest2 = createAndSaveContest(title = "콘테스트2")
            val contest3 = createAndSaveContest(title = "콘테스트3")

            val page = contestRepository.findWithPagination(
                filter = ContestListFilter.ALL,
                sortType = ContestSortType.LATEST,
                pageable = PageRequest.of(0, 10),
                currentTime = LocalDateTime.now()
            )

            assertThat(page.content.map { it.id.value })
                .containsExactlyInAnyOrder(contest1.id!!, contest2.id!!, contest3.id!!)
            assertThat(page.totalElements).isEqualTo(3)
        }

        @Test
        @DisplayName("ACTIVE 상태를 LATEST 정렬로 조회할 수 있다")
        fun findWithPaginationActiveLatest() {
            val now = LocalDateTime.now()
            val contest1 = createAndSaveContest(title = "콘테스트1", expiredAt = now.plusDays(7))
            val contest2 = createAndSaveContest(title = "콘테스트2", expiredAt = now.plusDays(7))
            createAndSaveContest(title = "만료된 콘테스트", expiredAt = now.minusDays(1))

            val page = contestRepository.findWithPagination(
                filter = ContestListFilter.ACTIVE,
                sortType = ContestSortType.LATEST,
                pageable = PageRequest.of(0, 10),
                currentTime = now
            )

            assertThat(page.content.map { it.id.value }).containsExactlyInAnyOrder(contest1.id!!, contest2.id!!)
        }

        @Test
        @DisplayName("ACTIVE 상태를 OLDEST 정렬로 조회할 수 있다")
        fun findWithPaginationActiveOldest() {
            val now = LocalDateTime.now()
            val contest1 = createAndSaveContest(title = "콘테스트1", expiredAt = now.plusDays(7))
            val contest2 = createAndSaveContest(title = "콘테스트2", expiredAt = now.plusDays(7))

            val page = contestRepository.findWithPagination(
                filter = ContestListFilter.ACTIVE,
                sortType = ContestSortType.OLDEST,
                pageable = PageRequest.of(0, 10),
                currentTime = now
            )

            assertThat(page.content.map { it.id.value }).containsExactlyInAnyOrder(contest1.id!!, contest2.id!!)
        }

        @Test
        @DisplayName("ACTIVE 상태를 POPULAR 정렬로 조회할 수 있다")
        fun findWithPaginationActivePopular() {
            val now = LocalDateTime.now()
            val previous = createAndSaveContest(title = "이전 콘테스트", expiredAt = now.plusDays(7))
            Thread.sleep(10)
            val recent = createAndSaveContest(title = "최근 콘테스트", expiredAt = now.plusDays(7))

            val page = contestRepository.findWithPagination(
                filter = ContestListFilter.ACTIVE,
                sortType = ContestSortType.POPULAR,
                pageable = PageRequest.of(0, 10),
                currentTime = now
            )

            assertThat(page.content.map { it.id.value }).containsExactlyInAnyOrder(previous.id!!, recent.id!!)
            assertThat(page.content[0].title).isEqualTo("최근 콘테스트")
        }

        @Test
        @DisplayName("EXPIRED 상태로 만료된 콘테스트를 조회할 수 있다")
        fun findWithPaginationExpired() {
            val now = LocalDateTime.now()
            val expired = createAndSaveContest(title = "만료된 콘테스트", expiredAt = now.minusDays(1))
            createAndSaveContest(title = "진행중 콘테스트", expiredAt = now.plusDays(7))

            val page = contestRepository.findWithPagination(
                filter = ContestListFilter.EXPIRED,
                sortType = ContestSortType.LATEST,
                pageable = PageRequest.of(0, 10),
                currentTime = now
            )

            assertThat(page.content.map { it.id.value }).containsExactly(expired.id!!)
            assertThat(page.content[0].title).isEqualTo("만료된 콘테스트")
        }

        @Test
        @DisplayName("페이지네이션이 올바르게 동작한다")
        fun paginationWorks() {
            val now = LocalDateTime.now()
            val savedIds = List(15) { index ->
                createAndSaveContest(title = "콘테스트$index", expiredAt = now.plusDays(7)).id!!
            }

            val firstPage = findLatest(ContestListFilter.ACTIVE, PageRequest.of(0, 10), now)
            val secondPage = findLatest(ContestListFilter.ACTIVE, PageRequest.of(1, 10), now)

            assertThat(firstPage.content).hasSize(10)
            assertThat((firstPage.content + secondPage.content).map { it.id.value })
                .containsExactlyInAnyOrderElementsOf(savedIds)
            assertThat(firstPage.totalElements).isEqualTo(15)
        }

        @Test
        @DisplayName("WINNER_SELECTED 필터는 저장 상태가 WINNER_SELECTED인 콘테스트만 조회한다")
        fun findWithPaginationWinnerSelected() {
            val now = LocalDateTime.now()
            val yesterday = now.minusDays(1)
            val selected = createAndSaveContest(
                title = "선정 완료",
                expiredAt = yesterday,
                status = ContestStatusType.WINNER_SELECTED
            )
            createAndSaveContest(title = "만료만 됨", expiredAt = yesterday, status = ContestStatusType.ACTIVE)
            createAndSaveContest(title = "진행중", expiredAt = now.plusDays(7), status = ContestStatusType.ACTIVE)

            val page = findLatest(ContestListFilter.WINNER_SELECTED, PageRequest.of(0, 10), now)

            assertThat(page.content.map { it.id.value }).containsExactly(selected.id!!)
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

        private fun findLatest(filter: ContestListFilter, pageable: Pageable, now: LocalDateTime): Page<Contest> {
            return contestRepository.findWithPagination(
                filter = filter,
                sortType = ContestSortType.LATEST,
                pageable = pageable,
                currentTime = now
            )
        }
    }
}
