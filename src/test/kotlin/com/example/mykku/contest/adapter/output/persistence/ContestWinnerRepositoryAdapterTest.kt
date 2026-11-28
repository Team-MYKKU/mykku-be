package com.example.mykku.contest.adapter.output.persistence

import com.example.mykku.BaseRepositoryTest
import com.example.mykku.contest.adapter.output.persistence.entity.ContestJpaEntity
import com.example.mykku.contest.adapter.output.persistence.entity.ContestParticipationJpaEntity
import com.example.mykku.contest.adapter.output.persistence.entity.ContestWinnerJpaEntity
import com.example.mykku.contest.adapter.output.persistence.repository.ContestParticipationJpaRepository
import com.example.mykku.contest.adapter.output.persistence.repository.ContestWinnerJpaRepository
import com.example.mykku.contest.application.port.output.ContestWinnerRepository
import com.example.mykku.contest.domain.entity.ContestWinner
import com.example.mykku.contest.domain.vo.ContestId
import com.example.mykku.contest.domain.vo.ContestParticipationId
import com.example.mykku.contest.domain.vo.ContestWinnerId
import com.example.mykku.contest.exception.ContestException
import com.example.mykku.feed.adapter.output.persistence.FeedJpaRepository
import com.example.mykku.feed.adapter.output.persistence.entity.FeedJpaEntity
import com.example.mykku.member.adapter.output.persistence.entity.MemberJpaEntity
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@DisplayName("ContestWinnerRepositoryAdapter 통합 테스트")
class ContestWinnerRepositoryAdapterTest : BaseRepositoryTest() {

    @Autowired
    private lateinit var contestWinnerRepository: ContestWinnerRepository

    @Autowired
    private lateinit var contestParticipationJpaRepository: ContestParticipationJpaRepository

    @Autowired
    private lateinit var contestWinnerJpaRepository: ContestWinnerJpaRepository

    @Autowired
    private lateinit var feedJpaRepository: FeedJpaRepository

    private fun createAndSaveFeed(
        title: String = "테스트 피드",
        content: String = "테스트 내용",
        member: MemberJpaEntity
    ): FeedJpaEntity {
        val board = createAndSaveBoard()
        val feed = FeedJpaEntity(
            title = title,
            content = content,
            board = board,
            member = member
        )
        return feedJpaRepository.save(feed)
    }

    private fun createAndSaveParticipation(
        contest: ContestJpaEntity,
        member: MemberJpaEntity,
        feed: FeedJpaEntity
    ): ContestParticipationJpaEntity {
        val participation = ContestParticipationJpaEntity(
            contest = contest,
            member = member,
            feed = feed
        )
        return contestParticipationJpaRepository.save(participation)
    }

    private fun createAndSaveWinner(
        contest: ContestJpaEntity,
        participation: ContestParticipationJpaEntity,
        winnerRank: Int = 1,
        description: String = "1등 상품"
    ): ContestWinnerJpaEntity {
        val winner = ContestWinnerJpaEntity(
            winnerRank = winnerRank,
            description = description,
            contest = contest,
            participation = participation
        )
        return contestWinnerJpaRepository.save(winner)
    }

    @Nested
    @DisplayName("save 메서드")
    inner class SaveTest {

        @Test
        @DisplayName("콘테스트 우승자를 저장할 수 있다")
        fun saveContestWinner() {
            val member = createAndSaveMember()
            val contest = createAndSaveContest()
            val feed = createAndSaveFeed(member = member)
            val participation = createAndSaveParticipation(contest, member, feed)

            val winner = ContestWinner.create(
                winnerRank = 1,
                awardTitle = "최우수상",
                description = "1등 상품",
                contestId = ContestId(contest.id!!),
                participationId = ContestParticipationId(participation.id!!)
            )

            val saved = contestWinnerRepository.save(winner)

            assertThat(saved.id.value).isGreaterThan(0)
            assertThat(saved.winnerRank).isEqualTo(1)
            assertThat(saved.awardTitle).isEqualTo("최우수상")
            assertThat(saved.description).isEqualTo("1등 상품")
            assertThat(saved.contestId.value).isEqualTo(contest.id)
            assertThat(saved.participationId.value).isEqualTo(participation.id)
        }

        @Test
        @DisplayName("존재하지 않는 콘테스트에 우승자 저장시 예외가 발생한다")
        fun saveWinnerWithInvalidContest() {
            val member = createAndSaveMember()
            val contest = createAndSaveContest()
            val feed = createAndSaveFeed(member = member)
            val participation = createAndSaveParticipation(contest, member, feed)

            val winner = ContestWinner.create(
                winnerRank = 1,
                awardTitle = "최우수상",
                description = "1등 상품",
                contestId = ContestId(999999L),
                participationId = ContestParticipationId(participation.id!!)
            )

            assertThatThrownBy { contestWinnerRepository.save(winner) }
                .isInstanceOf(ContestException::class.java)
        }

        @Test
        @DisplayName("존재하지 않는 참여에 우승자 저장시 예외가 발생한다")
        fun saveWinnerWithInvalidParticipation() {
            val contest = createAndSaveContest()

            val winner = ContestWinner.create(
                winnerRank = 1,
                awardTitle = "최우수상",
                description = "1등 상품",
                contestId = ContestId(contest.id!!),
                participationId = ContestParticipationId(999999L)
            )

            assertThatThrownBy { contestWinnerRepository.save(winner) }
                .isInstanceOf(ContestException::class.java)
        }
    }

    @Nested
    @DisplayName("saveAll 메서드")
    inner class SaveAllTest {

        @Test
        @DisplayName("여러 우승자를 한번에 저장할 수 있다")
        fun saveAllWinners() {
            val member1 = createAndSaveMember(memberId = "member1")
            val member2 = createAndSaveMember(memberId = "member2")
            val contest = createAndSaveContest()
            val feed1 = createAndSaveFeed(title = "피드1", member = member1)
            val feed2 = createAndSaveFeed(title = "피드2", member = member2)
            val participation1 = createAndSaveParticipation(contest, member1, feed1)
            val participation2 = createAndSaveParticipation(contest, member2, feed2)

            val winners = listOf(
                ContestWinner.create(
                    winnerRank = 1,
                    awardTitle = "최우수상",
                    description = "1등 상품",
                    contestId = ContestId(contest.id!!),
                    participationId = ContestParticipationId(participation1.id!!)
                ),
                ContestWinner.create(
                    winnerRank = 2,
                    awardTitle = "우수상",
                    description = "2등 상품",
                    contestId = ContestId(contest.id!!),
                    participationId = ContestParticipationId(participation2.id!!)
                )
            )

            val savedWinners = contestWinnerRepository.saveAll(winners)

            assertThat(savedWinners).hasSize(2)
            assertThat(savedWinners.map { it.winnerRank }).containsExactlyInAnyOrder(1, 2)
            assertThat(savedWinners.map { it.awardTitle }).containsExactlyInAnyOrder("최우수상", "우수상")
        }

        @Test
        @DisplayName("빈 리스트로 저장하면 빈 리스트를 반환한다")
        fun saveAllEmptyList() {
            val savedWinners = contestWinnerRepository.saveAll(emptyList())

            assertThat(savedWinners).isEmpty()
        }
    }

    @Nested
    @DisplayName("findById 메서드")
    inner class FindByIdTest {

        @Test
        @DisplayName("ID로 우승자를 조회할 수 있다")
        fun findById() {
            val member = createAndSaveMember()
            val contest = createAndSaveContest()
            val feed = createAndSaveFeed(member = member)
            val participation = createAndSaveParticipation(contest, member, feed)
            val savedWinner = createAndSaveWinner(contest, participation)

            val found = contestWinnerRepository.findById(ContestWinnerId(savedWinner.id!!))

            assertThat(found).isNotNull
            assertThat(found!!.winnerRank).isEqualTo(1)
        }

        @Test
        @DisplayName("존재하지 않는 ID로 조회하면 null을 반환한다")
        fun findByIdNotFound() {
            val found = contestWinnerRepository.findById(ContestWinnerId(999999L))

            assertThat(found).isNull()
        }
    }

    @Nested
    @DisplayName("findByContestId 메서드")
    inner class FindByContestIdTest {

        @Test
        @DisplayName("콘테스트 ID로 우승자 목록을 조회할 수 있다")
        fun findByContestId() {
            val member1 = createAndSaveMember(memberId = "member1")
            val member2 = createAndSaveMember(memberId = "member2")
            val contest = createAndSaveContest()
            val feed1 = createAndSaveFeed(title = "피드1", member = member1)
            val feed2 = createAndSaveFeed(title = "피드2", member = member2)
            val participation1 = createAndSaveParticipation(contest, member1, feed1)
            val participation2 = createAndSaveParticipation(contest, member2, feed2)
            createAndSaveWinner(contest, participation1, winnerRank = 1)
            createAndSaveWinner(contest, participation2, winnerRank = 2)

            val winners = contestWinnerRepository.findByContestId(ContestId(contest.id!!))

            assertThat(winners).hasSize(2)
        }

        @Test
        @DisplayName("존재하지 않는 콘테스트 ID로 조회하면 빈 리스트를 반환한다")
        fun findByContestIdNotFound() {
            val winners = contestWinnerRepository.findByContestId(ContestId(999999L))

            assertThat(winners).isEmpty()
        }
    }

    @Nested
    @DisplayName("findByContestIds 메서드")
    inner class FindByContestIdsTest {

        @Test
        @DisplayName("여러 콘테스트 ID로 우승자 목록을 조회할 수 있다")
        fun findByContestIds() {
            val member = createAndSaveMember()
            val contest1 = createAndSaveContest(title = "콘테스트1")
            val contest2 = createAndSaveContest(title = "콘테스트2")
            val feed1 = createAndSaveFeed(title = "피드1", member = member)
            val feed2 = createAndSaveFeed(title = "피드2", member = member)
            val participation1 = createAndSaveParticipation(contest1, member, feed1)
            val participation2 = createAndSaveParticipation(contest2, member, feed2)
            createAndSaveWinner(contest1, participation1)
            createAndSaveWinner(contest2, participation2)

            val winners = contestWinnerRepository.findByContestIds(
                listOf(ContestId(contest1.id!!), ContestId(contest2.id!!))
            )

            assertThat(winners).hasSize(2)
        }

        @Test
        @DisplayName("빈 콘테스트 ID 목록으로 조회하면 빈 리스트를 반환한다")
        fun findByContestIdsEmpty() {
            val winners = contestWinnerRepository.findByContestIds(emptyList())

            assertThat(winners).isEmpty()
        }
    }

    @Nested
    @DisplayName("existsByContestId 메서드")
    inner class ExistsByContestIdTest {

        @Test
        @DisplayName("콘테스트에 우승자가 있는지 확인할 수 있다")
        fun existsByContestId() {
            val member = createAndSaveMember()
            val contest = createAndSaveContest()
            val feed = createAndSaveFeed(member = member)
            val participation = createAndSaveParticipation(contest, member, feed)
            createAndSaveWinner(contest, participation)

            val exists = contestWinnerRepository.existsByContestId(ContestId(contest.id!!))

            assertThat(exists).isTrue()
        }

        @Test
        @DisplayName("우승자가 없는 콘테스트는 false를 반환한다")
        fun existsByContestIdNotFound() {
            val contest = createAndSaveContest()

            val exists = contestWinnerRepository.existsByContestId(ContestId(contest.id!!))

            assertThat(exists).isFalse()
        }
    }

    @Nested
    @DisplayName("deleteAllByContestId 메서드")
    inner class DeleteAllByContestIdTest {

        @Test
        @DisplayName("콘테스트의 모든 우승자를 삭제할 수 있다")
        fun deleteAllByContestId() {
            val member1 = createAndSaveMember(memberId = "member1")
            val member2 = createAndSaveMember(memberId = "member2")
            val contest = createAndSaveContest()
            val feed1 = createAndSaveFeed(title = "피드1", member = member1)
            val feed2 = createAndSaveFeed(title = "피드2", member = member2)
            val participation1 = createAndSaveParticipation(contest, member1, feed1)
            val participation2 = createAndSaveParticipation(contest, member2, feed2)
            createAndSaveWinner(contest, participation1, winnerRank = 1)
            createAndSaveWinner(contest, participation2, winnerRank = 2)

            contestWinnerRepository.deleteAllByContestId(ContestId(contest.id!!))

            val exists = contestWinnerRepository.existsByContestId(ContestId(contest.id!!))
            assertThat(exists).isFalse()
        }

        @Test
        @DisplayName("존재하지 않는 콘테스트 ID로 삭제해도 예외가 발생하지 않는다")
        fun deleteAllByContestIdNotFound() {
            contestWinnerRepository.deleteAllByContestId(ContestId(999999L))
        }
    }

    @Nested
    @DisplayName("deleteAllByParticipationIds 메서드")
    inner class DeleteAllByParticipationIdsTest {

        @Test
        @DisplayName("참여 ID 목록으로 우승자를 삭제할 수 있다")
        fun deleteAllByParticipationIds() {
            val member1 = createAndSaveMember(memberId = "member1")
            val member2 = createAndSaveMember(memberId = "member2")
            val contest = createAndSaveContest()
            val feed1 = createAndSaveFeed(title = "피드1", member = member1)
            val feed2 = createAndSaveFeed(title = "피드2", member = member2)
            val participation1 = createAndSaveParticipation(contest, member1, feed1)
            val participation2 = createAndSaveParticipation(contest, member2, feed2)
            createAndSaveWinner(contest, participation1, winnerRank = 1)
            createAndSaveWinner(contest, participation2, winnerRank = 2)

            contestWinnerRepository.deleteAllByParticipationIds(
                listOf(ContestParticipationId(participation1.id!!))
            )

            val winners = contestWinnerRepository.findByContestId(ContestId(contest.id!!))
            assertThat(winners).hasSize(1)
            assertThat(winners[0].participationId.value).isEqualTo(participation2.id)
        }

        @Test
        @DisplayName("빈 참여 ID 목록으로 삭제해도 예외가 발생하지 않는다")
        fun deleteAllByParticipationIdsEmpty() {
            contestWinnerRepository.deleteAllByParticipationIds(emptyList())
        }
    }
}
