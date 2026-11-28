package com.example.mykku.event.adapter.output.persistence

import com.example.mykku.BaseRepositoryTest
import com.example.mykku.event.adapter.output.persistence.entity.EventJpaEntity
import com.example.mykku.event.adapter.output.persistence.repository.EventAttendanceJpaRepository
import com.example.mykku.event.adapter.output.persistence.repository.EventJpaRepository
import com.example.mykku.event.application.port.output.EventAttendanceRepository
import com.example.mykku.event.application.port.output.EventRepository
import com.example.mykku.event.domain.entity.EventAttendance
import com.example.mykku.event.domain.vo.EventId
import com.example.mykku.event.domain.vo.EventType
import com.example.mykku.event.exception.EventErrorCode
import com.example.mykku.event.exception.EventException
import com.example.mykku.member.adapter.output.persistence.entity.MemberJpaEntity
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.jdbc.core.JdbcTemplate
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("EventAttendanceRepositoryAdapter 통합 테스트")
class EventAttendanceRepositoryAdapterTest : BaseRepositoryTest() {

    @Autowired
    private lateinit var eventAttendanceRepository: EventAttendanceRepository

    @Autowired
    private lateinit var eventAttendanceJpaRepository: EventAttendanceJpaRepository

    @Autowired
    private lateinit var eventRepository: EventRepository

    @Autowired
    private lateinit var eventJpaRepository: EventJpaRepository

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    private lateinit var event: EventJpaEntity
    private lateinit var member: MemberJpaEntity

    @BeforeEach
    fun setUp() {
        event = saveEvent("출석 이벤트")
        saveMember("padding")
        member = saveMember("attendee")
        assertThat(member.id).`as`("이벤트 id와 회원 id가 달라야 두 인자를 뒤바꾼 조회를 잡는다").isNotEqualTo(event.id)
    }

    @Nested
    @DisplayName("save")
    inner class Save {

        @Test
        @DisplayName("출석을 저장하면 id가 생기고, 다시 읽어도 이벤트·회원·날짜가 그대로다")
        fun `저장 - 다시 읽기`() {
            val saved = eventAttendanceRepository.save(attendance(event, member, DECEMBER_1))
            flushAndClear()

            val stored = eventAttendanceJpaRepository.findById(saved.id.value).orElseThrow()
            assertThat(saved.id.value).isPositive()
            assertThat(stored.eventId).isEqualTo(event.id)
            assertThat(stored.memberId).isEqualTo(member.id)
            assertThat(stored.attendanceDate).isEqualTo(DECEMBER_1)
            assertSameAttendance(stored.toDomain(), saved)
        }

        @Test
        @DisplayName("같은 이벤트·회원·날짜를 다시 저장하면 EV304다")
        fun `저장 - 같은 날짜 중복은 EV304`() {
            eventAttendanceRepository.save(attendance(event, member, DECEMBER_1))

            val exception = assertThrows<EventException> {
                eventAttendanceRepository.save(attendance(event, member, DECEMBER_1))
            }

            assertThat(exception.errorCode).isEqualTo(EventErrorCode.ALREADY_ATTENDED_TODAY)
        }

        @Test
        @DisplayName("다음 날짜, 다른 회원, 다른 이벤트는 같은 날 출석과 겹치지 않는다")
        fun `저장 - 다음 날짜와 다른 회원과 다른 이벤트는 허용`() {
            val otherMember = saveMember("other")
            val otherEvent = saveEvent("다른 이벤트")
            eventAttendanceRepository.save(attendance(event, member, DECEMBER_1))

            eventAttendanceRepository.save(attendance(event, member, DECEMBER_1.plusDays(1)))
            eventAttendanceRepository.save(attendance(event, otherMember, DECEMBER_1))
            eventAttendanceRepository.save(attendance(otherEvent, member, DECEMBER_1))
            flushAndClear()

            assertThat(eventAttendanceJpaRepository.count()).isEqualTo(4)
        }

        @Test
        @DisplayName("없는 이벤트를 가리키는 외래 키 위반은 EV304로 바꾸지 않고 그대로 던진다")
        fun `저장 - 이벤트 외래 키 위반은 그대로`() {
            val orphan = EventAttendance.create(EventId(MISSING_ID), member.id, DECEMBER_1)

            val exception = assertThrows<DataIntegrityViolationException> { eventAttendanceRepository.save(orphan) }

            assertThat(exception).isNotInstanceOf(EventException::class.java)
        }

        @Test
        @DisplayName("없는 회원을 가리키는 외래 키 위반도 EV304로 바꾸지 않는다")
        fun `저장 - 회원 외래 키 위반은 그대로`() {
            val orphan = EventAttendance.create(EventId(event.id!!), MISSING_ID, DECEMBER_1)

            assertThrows<DataIntegrityViolationException> { eventAttendanceRepository.save(orphan) }
        }
    }

    @Nested
    @DisplayName("exists·findDates")
    inner class Read {

        @Test
        @DisplayName("오늘 출석 여부는 이벤트·회원·날짜가 모두 같을 때만 true다")
        fun `조회 - 날짜별 출석 여부`() {
            val otherMember = saveMember("other")
            eventAttendanceRepository.save(attendance(event, member, DECEMBER_1))
            flushAndClear()

            assertThat(exists(event, member, DECEMBER_1)).isTrue()
            assertThat(exists(event, member, DECEMBER_1.plusDays(1))).isFalse()
            assertThat(exists(event, otherMember, DECEMBER_1)).isFalse()
            assertThat(exists(saveEvent("다른 이벤트"), member, DECEMBER_1)).isFalse()
        }

        @Test
        @DisplayName("기간 날짜 조회는 양 끝을 포함하고 날짜 오름차순이며, 다른 회원·이벤트 행은 뺀다")
        fun `조회 - 날짜 범위 양 끝 포함`() {
            listOf(6L, 4L, -1L, 0L, 2L).forEach { eventAttendanceRepository.save(attendance(event, member, day(it))) }
            eventAttendanceRepository.save(attendance(event, saveMember("other"), day(1)))
            eventAttendanceRepository.save(attendance(saveEvent("다른 이벤트"), member, day(1)))
            flushAndClear()

            val dates = eventAttendanceRepository.findDates(EventId(event.id!!), member.id, day(0), day(4))

            assertThat(dates).containsExactly(day(0), day(2), day(4))
        }

        @Test
        @DisplayName("범위가 하루면 그날만 읽는다")
        fun `조회 - 하루 범위`() {
            listOf(0L, 1L, 2L).forEach { eventAttendanceRepository.save(attendance(event, member, day(it))) }
            flushAndClear()

            val dates = eventAttendanceRepository.findDates(EventId(event.id!!), member.id, day(1), day(1))

            assertThat(dates).containsExactly(day(1))
        }
    }

    @Nested
    @DisplayName("회원 탈퇴·이벤트 삭제")
    inner class Deletion {

        @Test
        @DisplayName("회원을 지우면 출석 행은 member_id NULL로 남고, 같은 날 탈퇴자 행이 여럿이어도 된다")
        fun `탈퇴 - member_id NULL로 보존`() {
            val otherMember = saveMember("other")
            eventAttendanceRepository.save(attendance(event, member, DECEMBER_1))
            eventAttendanceRepository.save(attendance(event, otherMember, DECEMBER_1))
            flushAndClear()

            memberJpaRepository.deleteById(member.id)
            memberJpaRepository.deleteById(otherMember.id)
            flushAndClear()

            val rows = eventAttendanceJpaRepository.findAll()
            assertThat(rows).hasSize(2).allSatisfy { assertThat(it.memberId).isNull() }
            assertThat(rows.map { it.toDomain().memberId }).containsOnlyNulls()
            assertThat(rows.map { it.attendanceDate }).containsOnly(DECEMBER_1)
            assertThat(eventAttendanceRepository.countByEventIds(listOf(eventId(event)))).containsEntry(event.id!!, 2)
        }

        @Test
        @DisplayName("이벤트를 JPQL로 지우면 그 이벤트의 출석 행만 CASCADE로 사라진다")
        fun `이벤트 삭제 - CASCADE`() {
            val keptEvent = saveEvent("남는 이벤트")
            eventAttendanceRepository.save(attendance(event, member, DECEMBER_1))
            eventAttendanceRepository.save(attendance(keptEvent, member, DECEMBER_1))
            flushAndClear()

            eventRepository.deleteById(EventId(event.id!!))
            flushAndClear()

            assertThat(rowsOf(event)).isZero()
            assertThat(rowsOf(keptEvent)).isOne()
        }
    }

    @Nested
    @DisplayName("countByEventIds")
    inner class CountByEventIds {

        @Test
        @DisplayName("이벤트별 출석 행 수를 세고, 출석이 없는 이벤트는 맵에 넣지 않는다")
        fun `건수 - 이벤트별`() {
            val other = saveEvent("다른 이벤트")
            val empty = saveEvent("출석 없는 이벤트")
            eventAttendanceRepository.save(attendance(event, member, day(0)))
            eventAttendanceRepository.save(attendance(event, member, day(1)))
            eventAttendanceRepository.save(attendance(event, saveMember("other"), day(0)))
            eventAttendanceRepository.save(attendance(other, member, day(0)))

            val counts = eventAttendanceRepository.countByEventIds(listOf(event, other, empty).map { EventId(it.id!!) })

            assertThat(counts).containsExactlyInAnyOrderEntriesOf(mapOf(event.id!! to 3, other.id!! to 1))
        }

        @Test
        @DisplayName("빈 목록이면 조회하지 않고 빈 맵을 준다")
        fun `건수 - 빈 목록`() {
            assertThat(eventAttendanceRepository.countByEventIds(emptyList())).isEmpty()
        }
    }

    private fun attendance(event: EventJpaEntity, member: MemberJpaEntity, date: LocalDate): EventAttendance {
        return EventAttendance.create(eventId(event), member.id, date)
    }

    private fun eventId(event: EventJpaEntity): EventId = EventId(event.id!!)

    private fun assertSameAttendance(reloaded: EventAttendance, saved: EventAttendance) {
        assertThat(reloaded.id).isEqualTo(saved.id)
        assertThat(reloaded.eventId).isEqualTo(saved.eventId).isEqualTo(eventId(event))
        assertThat(reloaded.memberId).isEqualTo(saved.memberId).isEqualTo(member.id)
        assertThat(reloaded.attendanceDate).isEqualTo(saved.attendanceDate).isEqualTo(DECEMBER_1)
    }

    private fun exists(event: EventJpaEntity, member: MemberJpaEntity, date: LocalDate): Boolean {
        return eventAttendanceRepository.existsByEventIdAndMemberIdAndDate(EventId(event.id!!), member.id, date)
    }

    private fun rowsOf(event: EventJpaEntity): Int {
        return jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM event_attendance WHERE event_id = ?",
            Int::class.java,
            event.id
        )!!
    }

    private fun saveMember(name: String): MemberJpaEntity {
        return createAndSaveMember(memberId = name, email = "$name@example.com", socialId = "$name-social")
    }

    private fun saveEvent(title: String): EventJpaEntity {
        return eventJpaRepository.save(
            EventJpaEntity(
                title = title,
                startedAt = LocalDateTime.of(2026, 12, 1, 0, 0),
                expiredAt = LocalDateTime.of(2026, 12, 8, 0, 0),
                thumbnailUrl = "https://example.com/thumbnail.jpg",
                type = EventType.ATTENDANCE
            )
        )
    }

    private fun day(offset: Long): LocalDate = DECEMBER_1.plusDays(offset)

    companion object {
        private val DECEMBER_1: LocalDate = LocalDate.of(2026, 12, 1)
        private const val MISSING_ID = 999_999L
    }
}
