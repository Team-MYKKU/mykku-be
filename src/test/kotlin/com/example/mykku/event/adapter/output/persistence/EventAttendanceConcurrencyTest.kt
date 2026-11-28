package com.example.mykku.event.adapter.output.persistence

import com.example.mykku.BaseControllerTest
import com.example.mykku.event.adapter.output.persistence.entity.EventJpaEntity
import com.example.mykku.event.adapter.output.persistence.repository.EventJpaRepository
import com.example.mykku.event.application.port.output.EventAttendanceRepository
import com.example.mykku.event.domain.entity.EventAttendance
import com.example.mykku.event.domain.vo.EventId
import com.example.mykku.event.domain.vo.EventType
import com.example.mykku.event.exception.EventErrorCode
import com.example.mykku.event.exception.EventException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.PessimisticLockingFailureException
import org.springframework.jdbc.core.JdbcTemplate
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@DisplayName("출석 저장 동시성 통합 테스트")
class EventAttendanceConcurrencyTest : BaseControllerTest() {

    @Autowired
    private lateinit var eventAttendanceRepository: EventAttendanceRepository

    @Autowired
    private lateinit var eventJpaRepository: EventJpaRepository

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Test
    @DisplayName("같은 회원·이벤트·날짜로 동시에 저장하면 1건만 저장되고 나머지는 EV304다")
    fun `같은 회원 동시 저장`() {
        val event = saveEvent()
        val member = createAndSaveMember(memberId = "racer", email = "racer@example.com", socialId = "racer-social")
        val sameDay = List(THREAD_COUNT) { EventAttendance.create(EventId(event.id!!), member.id, DATE) }

        val outcomes = saveConcurrently(sameDay)

        val tally = Tally(outcomes)
        log.info("같은 회원 동시 출석 저장: 성공 {}, EV304 {}, C301 {}", tally.saved, tally.duplicates, tally.lockConflicts)
        assertThat(tally.unexpected).`as`("EV304·C301 밖의 실패").isEmpty()
        assertThat(tally.saved).`as`("성공 수 (EV304 ${tally.duplicates}건, C301 ${tally.lockConflicts}건)").isOne()
        assertThat(tally.duplicates + tally.lockConflicts).isEqualTo(THREAD_COUNT - 1)
        assertThat(rowsOf(event)).isOne()
    }

    @Test
    @DisplayName("서로 다른 회원이 같은 날짜로 동시에 저장하면 모두 저장된다")
    fun `다른 회원 동시 저장`() {
        val event = saveEvent()
        val members = (1..THREAD_COUNT).map {
            createAndSaveMember(memberId = "racer$it", email = "racer$it@example.com", socialId = "racer$it-social")
        }

        val outcomes = saveConcurrently(members.map { EventAttendance.create(EventId(event.id!!), it.id, DATE) })

        assertThat(outcomes).containsOnlyNulls()
        assertThat(rowsOf(event)).isEqualTo(THREAD_COUNT)
    }

    private fun saveConcurrently(attendances: List<EventAttendance>): List<Throwable?> {
        fillConnectionPool(attendances.size)
        val executor = Executors.newFixedThreadPool(attendances.size)
        val start = CountDownLatch(1)
        try {
            val futures = attendances.map { executor.submit(saveTask(it, start)) }
            start.countDown()
            return futures.map { it.get(TIMEOUT_SECONDS, TimeUnit.SECONDS) }
        } finally {
            executor.shutdownNow()
        }
    }

    private fun saveTask(attendance: EventAttendance, start: CountDownLatch) = Callable<Throwable?> {
        start.await()
        runCatching { eventAttendanceRepository.save(attendance) }.exceptionOrNull()
    }

    private fun rowsOf(event: EventJpaEntity): Int {
        return jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM event_attendance WHERE event_id = ?",
            Int::class.java,
            event.id
        )!!
    }

    private fun saveEvent(): EventJpaEntity {
        return eventJpaRepository.save(
            EventJpaEntity(
                title = "출석 동시성 이벤트",
                startedAt = LocalDateTime.of(2026, 12, 1, 0, 0),
                expiredAt = LocalDateTime.of(2026, 12, 8, 0, 0),
                thumbnailUrl = "https://example.com/thumbnail.jpg",
                type = EventType.ATTENDANCE
            )
        )
    }

    private class Tally(outcomes: List<Throwable?>) {
        val saved = outcomes.count { it == null }
        val duplicates = outcomes.count { isDuplicate(it) }
        val lockConflicts = outcomes.count { isLockConflict(it) }
        val unexpected = outcomes.filterNotNull().filterNot { isDuplicate(it) || isLockConflict(it) }

        private fun isDuplicate(outcome: Throwable?): Boolean {
            return outcome is EventException && outcome.errorCode == EventErrorCode.ALREADY_ATTENDED_TODAY
        }

        private fun isLockConflict(outcome: Throwable?): Boolean = outcome is PessimisticLockingFailureException
    }

    companion object {
        private const val THREAD_COUNT = 8
        private const val TIMEOUT_SECONDS = 30L
        private val DATE: LocalDate = LocalDate.of(2026, 12, 1)
        private val log = LoggerFactory.getLogger(EventAttendanceConcurrencyTest::class.java)
    }
}
