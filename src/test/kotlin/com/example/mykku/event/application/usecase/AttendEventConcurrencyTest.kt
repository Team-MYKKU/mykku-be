package com.example.mykku.event.application.usecase

import com.example.mykku.BaseControllerTest
import com.example.mykku.event.adapter.output.persistence.entity.EventJpaEntity
import com.example.mykku.event.adapter.output.persistence.repository.EventJpaRepository
import com.example.mykku.event.application.dto.AttendEventCommand
import com.example.mykku.event.application.dto.MyEventStatusResult
import com.example.mykku.event.application.port.input.AttendEventUseCase
import com.example.mykku.event.domain.vo.EventType
import com.example.mykku.event.exception.EventErrorCode
import com.example.mykku.event.exception.EventException
import com.example.mykku.member.domain.entity.Member
import com.example.mykku.util.KstDayGuard
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.PessimisticLockingFailureException
import org.springframework.jdbc.core.JdbcTemplate
import java.time.Clock
import java.time.LocalDate
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@DisplayName("출석 API 동시성 통합 테스트")
class AttendEventConcurrencyTest : BaseControllerTest() {

    @Autowired
    private lateinit var attendEventUseCase: AttendEventUseCase

    @Autowired
    private lateinit var eventJpaRepository: EventJpaRepository

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var clock: Clock

    private val day: KstDayGuard by lazy { KstDayGuard(clock) }

    @BeforeEach
    fun separateMemberIdsFromEventIds() {
        createAndSaveMember(memberId = "idspacer", email = "idspacer@example.com", socialId = "idspacer-social")
    }

    @Test
    @DisplayName("같은 회원이 동시에 8번 출석하면 1번만 성공하고 나머지는 EV304(드물게 C301)이며 기록은 1행이다")
    fun `같은 회원 동시 출석`() {
        val member = racer("racer")
        val today = day.today()
        val event = saveEvent(today)

        val tally = Tally(attendConcurrently(event, List(THREAD_COUNT) { member }))

        day.assumeStillToday(today)
        log.info(
            "같은 회원 동시 출석: 성공 {}, EV304 {}(INSERT 충돌 {}), C301 {}",
            tally.attended.size, tally.duplicates, tally.insertCollisions, tally.lockConflicts
        )
        assertSingleAttendance(tally, event)
        assumeTrue(tally.insertCollisions > 0) { "경합이 INSERT까지 가지 않아 어댑터의 EV304 변환 경로를 지나지 않았다" }
    }

    @Test
    @DisplayName("서로 다른 회원 8명이 동시에 출석하면 모두 성공하고 기록은 8행이다")
    fun `다른 회원 동시 출석`() {
        val members = (1..THREAD_COUNT).map { racer("racer$it") }
        val today = day.today()
        val event = saveEvent(today)

        val tally = Tally(attendConcurrently(event, members))

        day.assumeStillToday(today)
        assertThat(tally.failures).isEmpty()
        assertThat(tally.attended.map { it.attendance?.totalAttendanceDays }).containsOnly(1).hasSize(THREAD_COUNT)
        assertThat(rowsOf(event)).isEqualTo(THREAD_COUNT)
    }

    private fun assertSingleAttendance(tally: Tally, event: EventJpaEntity) {
        assertThat(tally.unexpected).`as`("EV304·C301 밖의 실패").isEmpty()
        assertThat(tally.attended).`as`("성공 수 (EV304 ${tally.duplicates}건, C301 ${tally.lockConflicts}건)")
            .hasSize(1)
        assertThat(tally.attended.single().attendance?.totalAttendanceDays).isOne()
        assertThat(tally.duplicates + tally.lockConflicts).isEqualTo(THREAD_COUNT - 1)
        assertThat(rowsOf(event)).isOne()
    }

    private fun attendConcurrently(event: EventJpaEntity, members: List<Member>): List<Result<MyEventStatusResult>> {
        assertThat(members.map { it.id.value }).doesNotContain(event.id)
        fillConnectionPool(members.size)
        val executor = Executors.newFixedThreadPool(members.size)
        val start = CountDownLatch(1)
        try {
            val futures = members.map { executor.submit(attendTask(event, it, start)) }
            start.countDown()
            return futures.map { it.get(TIMEOUT_SECONDS, TimeUnit.SECONDS) }
        } finally {
            executor.shutdownNow()
        }
    }

    private fun attendTask(event: EventJpaEntity, member: Member, start: CountDownLatch) =
        Callable {
            start.await()
            runCatching { attendEventUseCase.execute(AttendEventCommand(eventId = event.id!!), member) }
        }

    private fun racer(memberId: String): Member {
        return createAndSaveMember(memberId = memberId, email = "$memberId@example.com", socialId = "$memberId-social")
            .toDomain()
    }

    private fun saveEvent(today: LocalDate): EventJpaEntity {
        return eventJpaRepository.save(
            EventJpaEntity(
                title = "출석 동시성 이벤트",
                startedAt = today.minusDays(3).atStartOfDay(),
                expiredAt = today.plusDays(4).atStartOfDay(),
                thumbnailUrl = "https://example.com/thumbnail.jpg",
                type = EventType.ATTENDANCE
            )
        )
    }

    private fun rowsOf(event: EventJpaEntity): Int {
        return jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM event_attendance WHERE event_id = ?",
            Int::class.java,
            event.id
        )!!
    }

    private class Tally(outcomes: List<Result<MyEventStatusResult>>) {
        val failures = outcomes.mapNotNull { it.exceptionOrNull() }
        val attended = outcomes.mapNotNull { it.getOrNull() }
        val duplicates = failures.count { isDuplicate(it) }
        val insertCollisions = failures.count { isDuplicate(it) && it.cause is DataIntegrityViolationException }
        val lockConflicts = failures.count { it is PessimisticLockingFailureException }
        val unexpected = failures.filterNot { isDuplicate(it) || it is PessimisticLockingFailureException }

        private fun isDuplicate(failure: Throwable): Boolean {
            return failure is EventException && failure.errorCode == EventErrorCode.ALREADY_ATTENDED_TODAY
        }
    }

    companion object {
        private const val THREAD_COUNT = 8
        private const val TIMEOUT_SECONDS = 30L
        private val log = LoggerFactory.getLogger(AttendEventConcurrencyTest::class.java)
    }
}
