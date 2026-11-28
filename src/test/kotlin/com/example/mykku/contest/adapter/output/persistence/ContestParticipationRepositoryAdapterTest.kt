package com.example.mykku.contest.adapter.output.persistence

import com.example.mykku.BaseRepositoryTest
import com.example.mykku.contest.adapter.output.persistence.entity.ContestJpaEntity
import com.example.mykku.contest.adapter.output.persistence.entity.ContestParticipationJpaEntity
import com.example.mykku.contest.adapter.output.persistence.repository.ContestParticipationJpaRepository
import com.example.mykku.contest.application.port.output.ContestParticipationRepository
import com.example.mykku.contest.domain.entity.ContestParticipation
import com.example.mykku.contest.domain.vo.ContestId
import com.example.mykku.contest.domain.vo.ContestParticipationId
import com.example.mykku.feed.adapter.output.persistence.FeedJpaRepository
import com.example.mykku.feed.adapter.output.persistence.entity.FeedJpaEntity
import com.example.mykku.member.adapter.output.persistence.entity.MemberJpaEntity
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest

@DisplayName("ContestParticipationRepositoryAdapter 통합 테스트")
class ContestParticipationRepositoryAdapterTest : BaseRepositoryTest() {

    @Autowired
    private lateinit var contestParticipationRepository: ContestParticipationRepository

    @Autowired
    private lateinit var contestParticipationJpaRepository: ContestParticipationJpaRepository

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

    @Nested
    @DisplayName("save 메서드")
    inner class SaveTest {

        @Test
        @DisplayName("콘테스트 참여를 저장할 수 있다")
        fun saveContestParticipation() {
            val member = createAndSaveMember()
            val contest = createAndSaveContest()
            val feed = createAndSaveFeed(member = member)
            val participation = ContestParticipation.create(
                contestId = ContestId(contest.id!!),
                feedId = feed.id!!,
                memberId = member.id
            )

            val saved = contestParticipationRepository.save(participation)

            assertThat(saved.id.value).isGreaterThan(0)
            assertThat(saved.contestId.value).isEqualTo(contest.id)
            assertThat(saved.feedId).isEqualTo(feed.id)
        }
    }

    @Nested
    @DisplayName("findById 메서드")
    inner class FindByIdTest {

        @Test
        @DisplayName("ID로 참여를 조회할 수 있다")
        fun findById() {
            val member = createAndSaveMember()
            val contest = createAndSaveContest()
            val feed = createAndSaveFeed(member = member)
            val savedParticipation = createAndSaveParticipation(contest, member, feed)

            val found = contestParticipationRepository.findById(
                ContestParticipationId(savedParticipation.id!!)
            )

            assertThat(found).isNotNull
            assertThat(found!!.contestId.value).isEqualTo(contest.id)
        }

        @Test
        @DisplayName("존재하지 않는 ID로 조회하면 null을 반환한다")
        fun findByIdNotFound() {
            val found = contestParticipationRepository.findById(ContestParticipationId(999999L))

            assertThat(found).isNull()
        }
    }

    @Nested
    @DisplayName("findAllByIdIn 메서드")
    inner class FindAllByIdInTest {

        @Test
        @DisplayName("ID 목록으로 참여 목록을 조회할 수 있다")
        fun findAllByIdIn() {
            val member = createAndSaveMember()
            val contest = createAndSaveContest()
            val feed1 = createAndSaveFeed(title = "피드1", member = member)
            val feed2 = createAndSaveFeed(title = "피드2", member = member)
            val participation1 = createAndSaveParticipation(contest, member, feed1)
            val participation2 = createAndSaveParticipation(contest, member, feed2)

            val found = contestParticipationRepository.findAllByIdIn(
                listOf(
                    ContestParticipationId(participation1.id!!),
                    ContestParticipationId(participation2.id!!)
                )
            )

            assertThat(found).hasSize(2)
        }

        @Test
        @DisplayName("빈 ID 목록으로 조회하면 빈 리스트를 반환한다")
        fun findAllByIdInEmpty() {
            val found = contestParticipationRepository.findAllByIdIn(emptyList())

            assertThat(found).isEmpty()
        }
    }

    @Nested
    @DisplayName("findByContestId 메서드")
    inner class FindByContestIdTest {

        @Test
        @DisplayName("콘테스트 ID로 참여 목록을 페이지네이션으로 조회할 수 있다")
        fun findByContestId() {
            val member = createAndSaveMember()
            val contest = createAndSaveContest()
            val feed1 = createAndSaveFeed(title = "피드1", member = member)
            val feed2 = createAndSaveFeed(title = "피드2", member = member)
            createAndSaveParticipation(contest, member, feed1)
            createAndSaveParticipation(contest, member, feed2)

            val page = contestParticipationRepository.findByContestId(
                ContestId(contest.id!!),
                PageRequest.of(0, 10)
            )

            assertThat(page.content).hasSize(2)
            assertThat(page.totalElements).isEqualTo(2)
        }
    }

    @Nested
    @DisplayName("findByFeedId 메서드")
    inner class FindByFeedIdTest {

        @Test
        @DisplayName("피드 ID로 참여 목록을 조회할 수 있다")
        fun findByFeedId() {
            val member = createAndSaveMember()
            val contest1 = createAndSaveContest(title = "콘테스트1")
            val contest2 = createAndSaveContest(title = "콘테스트2")
            val feed = createAndSaveFeed(member = member)
            createAndSaveParticipation(contest1, member, feed)
            createAndSaveParticipation(contest2, member, feed)

            val participations = contestParticipationRepository.findByFeedId(feed.id!!)

            assertThat(participations).hasSize(2)
        }

        @Test
        @DisplayName("존재하지 않는 피드 ID로 조회하면 빈 리스트를 반환한다")
        fun findByFeedIdNotFound() {
            val participations = contestParticipationRepository.findByFeedId(999999L)

            assertThat(participations).isEmpty()
        }
    }

    @Nested
    @DisplayName("findContestsByMemberId 메서드")
    inner class FindContestsByMemberIdTest {

        @Test
        @DisplayName("회원 ID로 참여한 콘테스트 목록을 조회할 수 있다")
        fun findContestsByMemberId() {
            val member = createAndSaveMember()
            val contest1 = createAndSaveContest(title = "콘테스트1")
            val contest2 = createAndSaveContest(title = "콘테스트2")
            val feed1 = createAndSaveFeed(title = "피드1", member = member)
            val feed2 = createAndSaveFeed(title = "피드2", member = member)
            createAndSaveParticipation(contest1, member, feed1)
            createAndSaveParticipation(contest2, member, feed2)

            val page = contestParticipationRepository.findContestsByMemberId(
                member.id,
                PageRequest.of(0, 10)
            )

            assertThat(page.content).hasSize(2)
        }

        @Test
        @DisplayName("존재하지 않는 회원 ID로 조회하면 빈 페이지를 반환한다")
        fun findContestsByMemberIdNotFound() {
            val page = contestParticipationRepository.findContestsByMemberId(
                999999L,
                PageRequest.of(0, 10)
            )

            assertThat(page.content).isEmpty()
        }
    }

    @Nested
    @DisplayName("findByMemberIdAndContestIds 메서드")
    inner class FindByMemberIdAndContestIdsTest {

        @Test
        @DisplayName("회원 ID와 콘테스트 ID 목록으로 참여를 조회할 수 있다")
        fun findByMemberIdAndContestIds() {
            val member = createAndSaveMember()
            val contest1 = createAndSaveContest(title = "콘테스트1")
            val contest2 = createAndSaveContest(title = "콘테스트2")
            val feed1 = createAndSaveFeed(title = "피드1", member = member)
            val feed2 = createAndSaveFeed(title = "피드2", member = member)
            createAndSaveParticipation(contest1, member, feed1)
            createAndSaveParticipation(contest2, member, feed2)

            val participations = contestParticipationRepository.findByMemberIdAndContestIds(
                member.id,
                listOf(ContestId(contest1.id!!), ContestId(contest2.id!!))
            )

            assertThat(participations).hasSize(2)
        }

        @Test
        @DisplayName("빈 콘테스트 ID 목록으로 조회하면 빈 리스트를 반환한다")
        fun findByMemberIdAndContestIdsEmpty() {
            val member = createAndSaveMember()

            val participations = contestParticipationRepository.findByMemberIdAndContestIds(
                member.id,
                emptyList()
            )

            assertThat(participations).isEmpty()
        }
    }

    @Nested
    @DisplayName("existsByMemberIdAndContestId 메서드")
    inner class ExistsByMemberIdAndContestIdTest {

        @Test
        @DisplayName("회원이 콘테스트에 참여했는지 확인할 수 있다")
        fun existsByMemberIdAndContestId() {
            val member = createAndSaveMember()
            val contest = createAndSaveContest()
            val feed = createAndSaveFeed(member = member)
            createAndSaveParticipation(contest, member, feed)

            val exists = contestParticipationRepository.existsByMemberIdAndContestId(
                member.id,
                ContestId(contest.id!!)
            )

            assertThat(exists).isTrue()
        }

        @Test
        @DisplayName("참여하지 않은 경우 false를 반환한다")
        fun existsByMemberIdAndContestIdNotFound() {
            val member = createAndSaveMember()
            val contest = createAndSaveContest()

            val exists = contestParticipationRepository.existsByMemberIdAndContestId(
                member.id,
                ContestId(contest.id!!)
            )

            assertThat(exists).isFalse()
        }
    }

    @Nested
    @DisplayName("existsByMemberIdAndContestIdAndFeedId 메서드")
    inner class ExistsByMemberIdAndContestIdAndFeedIdTest {

        @Test
        @DisplayName("특정 피드로 참여했는지 확인할 수 있다")
        fun existsByMemberIdAndContestIdAndFeedId() {
            val member = createAndSaveMember()
            val contest = createAndSaveContest()
            val feed = createAndSaveFeed(member = member)
            createAndSaveParticipation(contest, member, feed)

            val exists = contestParticipationRepository.existsByMemberIdAndContestIdAndFeedId(
                member.id,
                ContestId(contest.id!!),
                feed.id!!
            )

            assertThat(exists).isTrue()
        }

        @Test
        @DisplayName("해당 피드로 참여하지 않은 경우 false를 반환한다")
        fun existsByMemberIdAndContestIdAndFeedIdNotFound() {
            val member = createAndSaveMember()
            val contest = createAndSaveContest()
            val feed = createAndSaveFeed(member = member)

            val exists = contestParticipationRepository.existsByMemberIdAndContestIdAndFeedId(
                member.id,
                ContestId(contest.id!!),
                feed.id!!
            )

            assertThat(exists).isFalse()
        }
    }

    @Nested
    @DisplayName("countByContestId 메서드")
    inner class CountByContestIdTest {

        @Test
        @DisplayName("콘테스트의 참여 수를 조회할 수 있다")
        fun countByContestId() {
            val member1 = createAndSaveMember(memberId = "member1", email = "member1@test.com", socialId = "member1_s")
            val member2 = createAndSaveMember(memberId = "member2", email = "member2@test.com", socialId = "member2_s")
            val contest = createAndSaveContest()
            val feed1 = createAndSaveFeed(title = "피드1", member = member1)
            val feed2 = createAndSaveFeed(title = "피드2", member = member2)
            createAndSaveParticipation(contest, member1, feed1)
            createAndSaveParticipation(contest, member2, feed2)

            val count = contestParticipationRepository.countByContestId(ContestId(contest.id!!))

            assertThat(count).isEqualTo(2)
        }

        @Test
        @DisplayName("존재하지 않는 콘테스트의 참여 수는 0을 반환한다")
        fun countByContestIdNotFound() {
            val count = contestParticipationRepository.countByContestId(ContestId(999999L))

            assertThat(count).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("deleteAll 메서드")
    inner class DeleteAllTest {

        @Test
        @DisplayName("참여 목록을 삭제할 수 있다")
        fun deleteAll() {
            val member = createAndSaveMember()
            val contest = createAndSaveContest()
            val feed1 = createAndSaveFeed(title = "피드1", member = member)
            val feed2 = createAndSaveFeed(title = "피드2", member = member)
            val participation1 = createAndSaveParticipation(contest, member, feed1)
            val participation2 = createAndSaveParticipation(contest, member, feed2)

            val participations = listOf(
                participation1.toDomain(),
                participation2.toDomain()
            )

            contestParticipationRepository.deleteAll(participations)

            val count = contestParticipationRepository.countByContestId(ContestId(contest.id!!))
            assertThat(count).isEqualTo(0)
        }

        @Test
        @DisplayName("빈 목록으로 삭제해도 예외가 발생하지 않는다")
        fun deleteAllEmpty() {
            contestParticipationRepository.deleteAll(emptyList())
        }
    }
}
