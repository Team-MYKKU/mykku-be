package com.example.mykku.contest.domain

import com.example.mykku.contest.domain.entity.Contest
import com.example.mykku.contest.domain.vo.ContestId
import com.example.mykku.contest.domain.vo.ContestStatusType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

@DisplayName("Contest 도메인 엔티티 테스트")
class ContestTest {

    @Nested
    @DisplayName("create 메서드")
    inner class Create {

        @Test
        @DisplayName("정상적으로 콘테스트를 생성한다")
        fun `콘테스트 생성 - 정상 케이스`() {
            val startedAt = LocalDateTime.now().plusDays(1)
            val expiredAt = LocalDateTime.now().plusDays(7)

            val contest = Contest.create(
                title = "테스트 콘테스트",
                description = "콘테스트 설명",
                startedAt = startedAt,
                expiredAt = expiredAt,
                thumbnailUrl = "https://example.com/thumbnail.jpg"
            )

            assertThat(contest.id.value).isEqualTo(0L)
            assertThat(contest.title).isEqualTo("테스트 콘테스트")
            assertThat(contest.description).isEqualTo("콘테스트 설명")
            assertThat(contest.startedAt).isEqualTo(startedAt)
            assertThat(contest.expiredAt).isEqualTo(expiredAt)
        }

        @Test
        @DisplayName("콘테스트 생성시 status가 ACTIVE이다")
        fun `콘테스트 생성 - 기본 상태 검증`() {
            val contest = createContest()

            assertThat(contest.status).isEqualTo(ContestStatusType.ACTIVE)
        }

        @Test
        @DisplayName("콘테스트 생성시 createdAt과 updatedAt이 설정된다")
        fun `콘테스트 생성 - 시간 설정 검증`() {
            val contest = createContest()

            assertThat(contest.createdAt).isNotNull()
            assertThat(contest.updatedAt).isNotNull()
        }

        @Test
        @DisplayName("설명 없이 콘테스트를 생성할 수 있다")
        fun `콘테스트 생성 - 설명 없음`() {
            val contest = Contest.create(
                title = "테스트 콘테스트",
                description = null,
                startedAt = LocalDateTime.now().plusDays(1),
                expiredAt = LocalDateTime.now().plusDays(7),
                thumbnailUrl = "https://example.com/thumbnail.jpg"
            )

            assertThat(contest.description).isNull()
        }
    }

    @Nested
    @DisplayName("updateStatus 메서드")
    inner class UpdateStatus {

        @Test
        @DisplayName("콘테스트 상태를 EXPIRED로 변경할 수 있다")
        fun `상태 변경 - EXPIRED`() {
            val contest = createContest()

            contest.updateStatus(ContestStatusType.EXPIRED)

            assertThat(contest.status).isEqualTo(ContestStatusType.EXPIRED)
        }

        @Test
        @DisplayName("콘테스트 상태를 WINNER_SELECTING으로 변경할 수 있다")
        fun `상태 변경 - WINNER_SELECTING`() {
            val contest = createContest()

            contest.updateStatus(ContestStatusType.WINNER_SELECTING)

            assertThat(contest.status).isEqualTo(ContestStatusType.WINNER_SELECTING)
        }

        @Test
        @DisplayName("콘테스트 상태를 WINNER_SELECTED로 변경할 수 있다")
        fun `상태 변경 - WINNER_SELECTED`() {
            val contest = createContest()

            contest.updateStatus(ContestStatusType.WINNER_SELECTED)

            assertThat(contest.status).isEqualTo(ContestStatusType.WINNER_SELECTED)
        }

        @Test
        @DisplayName("EXPIRED 상태에서 ACTIVE로 다시 변경할 수 있다")
        fun `상태 변경 - ACTIVE 복원`() {
            val contest = createContest()
            contest.updateStatus(ContestStatusType.EXPIRED)

            contest.updateStatus(ContestStatusType.ACTIVE)

            assertThat(contest.status).isEqualTo(ContestStatusType.ACTIVE)
        }
    }

    @Nested
    @DisplayName("reconstitute 메서드")
    inner class Reconstitute {

        @Test
        @DisplayName("저장된 데이터로 Contest를 복원한다")
        fun `복원 - 정상 케이스`() {
            val now = LocalDateTime.now()
            val startedAt = now.plusDays(1)
            val expiredAt = now.plusDays(7)

            val contest = Contest.reconstitute(
                id = ContestId(1L),
                title = "복원된 콘테스트",
                description = "설명",
                startedAt = startedAt,
                expiredAt = expiredAt,
                thumbnailUrl = "https://example.com/thumbnail.jpg",
                status = ContestStatusType.EXPIRED,
                createdAt = now,
                updatedAt = now
            )

            assertThat(contest.id.value).isEqualTo(1L)
            assertThat(contest.title).isEqualTo("복원된 콘테스트")
            assertThat(contest.status).isEqualTo(ContestStatusType.EXPIRED)
        }

        @Test
        @DisplayName("다양한 상태로 복원할 수 있다")
        fun `복원 - 다양한 상태`() {
            val now = LocalDateTime.now()

            val activeContest = Contest.reconstitute(
                id = ContestId(1L),
                title = "활성 콘테스트",
                description = null,
                startedAt = now,
                expiredAt = now.plusDays(7),
                thumbnailUrl = "https://example.com/thumbnail.jpg",
                status = ContestStatusType.ACTIVE,
                createdAt = now,
                updatedAt = now
            )

            val winnerSelectedContest = Contest.reconstitute(
                id = ContestId(2L),
                title = "수상자 선정 완료 콘테스트",
                description = null,
                startedAt = now,
                expiredAt = now.plusDays(7),
                thumbnailUrl = "https://example.com/thumbnail.jpg",
                status = ContestStatusType.WINNER_SELECTED,
                createdAt = now,
                updatedAt = now
            )

            assertThat(activeContest.status).isEqualTo(ContestStatusType.ACTIVE)
            assertThat(winnerSelectedContest.status).isEqualTo(ContestStatusType.WINNER_SELECTED)
        }
    }

    @Nested
    @DisplayName("상수 검증")
    inner class Constants {

        @Test
        @DisplayName("IMAGE_MAX_COUNT가 10이다")
        fun `상수 검증 - IMAGE_MAX_COUNT`() {
            assertThat(Contest.IMAGE_MAX_COUNT).isEqualTo(10)
        }

        @Test
        @DisplayName("TAG_MAX_COUNT가 7이다")
        fun `상수 검증 - TAG_MAX_COUNT`() {
            assertThat(Contest.TAG_MAX_COUNT).isEqualTo(7)
        }
    }

    @Nested
    @DisplayName("상태 계산")
    inner class ResolveStatus {

        @Test
        @DisplayName("만료 시각이 남아있으면 ACTIVE다")
        fun `상태 계산 - 진행 중`() {
            val now = LocalDateTime.now()
            val contest = createContest(expiredAt = now.plusDays(1))

            assertThat(contest.resolveStatus(now)).isEqualTo(ContestStatusType.ACTIVE)
        }

        @Test
        @DisplayName("만료 시각과 같은 시각이면 EXPIRED다")
        fun `상태 계산 - 만료 경계`() {
            val now = LocalDateTime.now()
            val contest = createContest(expiredAt = now)

            assertThat(contest.resolveStatus(now)).isEqualTo(ContestStatusType.EXPIRED)
        }

        @Test
        @DisplayName("만료 시각이 지나면 EXPIRED다")
        fun `상태 계산 - 만료`() {
            val now = LocalDateTime.now()
            val contest = createContest(expiredAt = now.minusDays(1))

            assertThat(contest.resolveStatus(now)).isEqualTo(ContestStatusType.EXPIRED)
        }

        @Test
        @DisplayName("수상자가 선정되면 만료 여부와 무관하게 WINNER_SELECTED를 유지한다")
        fun `상태 계산 - 수상자 선정 완료`() {
            val now = LocalDateTime.now()
            val contest = createContest(expiredAt = now.minusDays(1))
            contest.updateStatus(ContestStatusType.WINNER_SELECTED)

            assertThat(contest.resolveStatus(now)).isEqualTo(ContestStatusType.WINNER_SELECTED)
        }
    }

    private fun createContest(): Contest {
        return Contest.create(
            title = "테스트 콘테스트",
            description = "콘테스트 설명",
            startedAt = LocalDateTime.now().plusDays(1),
            expiredAt = LocalDateTime.now().plusDays(7),
            thumbnailUrl = "https://example.com/thumbnail.jpg"
        )
    }

    private fun createContest(expiredAt: LocalDateTime): Contest {
        return Contest.create(
            title = "테스트 콘테스트",
            description = "콘테스트 설명",
            startedAt = LocalDateTime.now().minusDays(1),
            expiredAt = expiredAt,
            thumbnailUrl = "https://example.com/thumbnail.jpg"
        )
    }
}
