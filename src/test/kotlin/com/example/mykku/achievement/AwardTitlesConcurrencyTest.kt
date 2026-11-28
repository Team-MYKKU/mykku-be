package com.example.mykku.achievement

import com.example.mykku.BaseControllerTest
import com.example.mykku.achievement.adapter.output.persistence.repository.MemberActivityCountJpaRepository
import com.example.mykku.achievement.application.port.input.AwardTitlesUseCase
import com.example.mykku.achievement.domain.TitleThreshold
import com.example.mykku.achievement.domain.vo.ActivityType
import com.example.mykku.role.adapter.output.persistence.entity.RoleJpaEntity
import com.example.mykku.role.adapter.output.persistence.repository.MemberRoleJpaRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.PessimisticLockingFailureException
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@DisplayName("칭호 자동 부여 동시성 통합 테스트")
class AwardTitlesConcurrencyTest : BaseControllerTest() {

    companion object {
        private const val SAME_ACTIVITY_THREAD_COUNT = 8
        private const val TIMEOUT_SECONDS = 30L
        private val DISTINCT_ACTIVITIES = listOf(
            ActivityType.FEED_UPLOAD,
            ActivityType.COMMENT_CREATE,
            ActivityType.LIKE_PRESS,
            ActivityType.FANNOTE_VIEW,
            ActivityType.DAILYMESSAGE_VIEW,
            ActivityType.CONTEST_PARTICIPATE
        )
    }

    @Autowired
    private lateinit var awardTitlesUseCase: AwardTitlesUseCase

    @Autowired
    private lateinit var memberRoleJpaRepository: MemberRoleJpaRepository

    @Autowired
    private lateinit var memberActivityCountJpaRepository: MemberActivityCountJpaRepository

    @BeforeEach
    fun seedTitles() {
        TitleThreshold.entries.forEach {
            roleJpaRepository.save(RoleJpaEntity(name = it.roleName, description = it.roleName))
        }
    }

    @Test
    @DisplayName("같은 활동의 칭호 부여가 동시에 실행되어도 칭호가 중복 부여되지 않는다")
    fun concurrentSameActivityAwardsCreateNoDuplicates() {
        // given
        val member = createAndSaveMember(memberId = "awardsamekind", nickname = "테스터", role = null)
        val activities = List(SAME_ACTIVITY_THREAD_COUNT) { ActivityType.FEED_UPLOAD }

        // when
        val outcomes = awardConcurrently(member.id, activities)

        // then
        assertOnlyLockFailures(outcomes)
        val committedCount = outcomes.count { it == null }
        assertThat(committedCount).isPositive()
        assertActivityCount(member.id, ActivityType.FEED_UPLOAD, committedCount.toLong())
        assertAwardedTitles(member.id, expectedTitles(ActivityType.FEED_UPLOAD, committedCount.toLong()))
    }

    @Test
    @DisplayName("서로 다른 활동의 칭호 부여가 동시에 실행되어도 각 칭호가 한 번만 부여된다")
    fun concurrentDistinctActivityAwardsCreateNoDuplicates() {
        // given
        val member = createAndSaveMember(memberId = "awarddistinct", nickname = "테스터", role = null)

        // when
        val outcomes = awardConcurrently(member.id, DISTINCT_ACTIVITIES)

        // then
        assertOnlyLockFailures(outcomes)
        val committed = DISTINCT_ACTIVITIES.filterIndexed { index, _ -> outcomes[index] == null }
        assertThat(committed).isNotEmpty()
        committed.forEach { assertActivityCount(member.id, it, 1) }
        assertAwardedTitles(member.id, committed.flatMap { expectedTitles(it, 1) })
    }

    private fun assertOnlyLockFailures(outcomes: List<Throwable?>) {
        assertThat(outcomes.filterNotNull()).allMatch { it is PessimisticLockingFailureException }
    }

    private fun assertActivityCount(memberPk: Long, activityType: ActivityType, expected: Long) {
        val row = memberActivityCountJpaRepository.findByMemberIdAndActivityType(memberPk, activityType)
        assertThat(row?.count).isEqualTo(expected)
    }

    private fun assertAwardedTitles(memberPk: Long, expected: List<String>) {
        val awarded = memberRoleJpaRepository.findByMemberIdWithRole(memberPk)
        assertThat(awarded.map { it.role.name }).doesNotHaveDuplicates()
        assertThat(awarded.map { it.role.name }).containsExactlyInAnyOrderElementsOf(expected)
        assertThat(memberJpaRepository.findById(memberPk).get().role?.id).isIn(awarded.map { it.role.id })
    }

    private fun expectedTitles(activityType: ActivityType, count: Long): List<String> =
        TitleThreshold.forActivity(activityType).filter { it.threshold <= count }.map { it.roleName }

    private fun awardConcurrently(memberPk: Long, activities: List<ActivityType>): List<Throwable?> {
        fillConnectionPool(activities.size)
        val executor = Executors.newFixedThreadPool(activities.size)
        val start = CountDownLatch(1)
        try {
            val futures = activities.map { executor.submit(awardTask(memberPk, it, start)) }
            start.countDown()
            return futures.map { it.get(TIMEOUT_SECONDS, TimeUnit.SECONDS) }
        } finally {
            executor.shutdownNow()
        }
    }

    private fun awardTask(memberPk: Long, activityType: ActivityType, start: CountDownLatch) =
        Callable<Throwable?> {
            start.await()
            runCatching { awardTitlesUseCase.handleActivity(memberPk, activityType) }.exceptionOrNull()
        }
}
