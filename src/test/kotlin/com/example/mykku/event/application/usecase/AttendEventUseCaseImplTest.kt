package com.example.mykku.event.application.usecase

import com.example.mykku.event.application.dto.AttendEventCommand
import com.example.mykku.event.application.dto.AttendanceStatusResult
import com.example.mykku.event.application.dto.MyEventStatusResult
import com.example.mykku.event.application.port.output.EventAttendanceRepository
import com.example.mykku.event.application.port.output.EventRepository
import com.example.mykku.event.domain.entity.Event
import com.example.mykku.event.domain.entity.EventAttendance
import com.example.mykku.event.domain.vo.EventId
import com.example.mykku.event.domain.vo.EventStatusType
import com.example.mykku.event.domain.vo.EventType
import com.example.mykku.event.exception.EventErrorCode
import com.example.mykku.event.exception.EventException
import com.example.mykku.member.domain.entity.Member
import com.example.mykku.member.domain.vo.SocialProvider
import com.example.mykku.member.exception.MemberErrorCode
import com.example.mykku.member.exception.MemberException
import com.example.mykku.util.DefaultTimeZone
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

@ExtendWith(MockitoExtension::class)
@DisplayName("출석 UseCase 단위 테스트")
class AttendEventUseCaseImplTest {

    @Mock
    private lateinit var eventRepository: EventRepository

    @Mock
    private lateinit var eventAttendanceRepository: EventAttendanceRepository

    @Nested
    @DisplayName("오늘 날짜(UTC 존 고정 시계)")
    inner class Today {

        @ParameterizedTest(name = "{displayName} [{0} → {1}, JVM {2}]")
        @CsvSource(
            "2026-11-28T14:59:59.999999Z, 2026-11-28, Asia/Seoul",
            "2026-11-28T15:00:00Z, 2026-11-29, Asia/Seoul",
            "2026-11-28T14:59:59.999999Z, 2026-11-28, UTC",
            "2026-11-28T15:00:00Z, 2026-11-29, UTC",
            "2026-11-28T15:00:00Z, 2026-11-29, America/Los_Angeles"
        )
        @DisplayName("시계의 순간을 KST 날짜로 바꿔 출석 날짜로 쓴다(KST 자정 = UTC 전날 15:00, 시계 존·JVM 기본 시간대 무관)")
        fun `KST 자정`(instant: String, today: LocalDate, jvmZone: String) {
            val event = givenEvent(attendanceEvent())
            givenAttendable(event, today)

            val result = DefaultTimeZone.with(jvmZone) { attendAt(Instant.parse(instant)) }

            assertThat(result.attendance?.today).isEqualTo(today)
            assertThat(savedDates()).containsExactly(today)
        }

        @ParameterizedTest(name = "{displayName} [{0} ~ {1}, {2} → {3}]")
        @CsvSource(
            "2026-12-01T00:00, 2026-12-08T00:00, 2026-11-30T23:59:59.999, EV102",
            "2026-12-01T00:00, 2026-12-08T00:00, 2026-12-01T00:00:00.000, 2026-12-01",
            "2026-12-01T00:00, 2026-12-08T00:00, 2026-12-07T23:59:59.999, 2026-12-07",
            "2026-12-01T00:00, 2026-12-08T00:00, 2026-12-08T00:00:00.000, EV102",
            "2026-12-01T00:00, 2026-12-07T23:59:59, 2026-12-07T23:59:58.999, 2026-12-07",
            "2026-12-01T00:00, 2026-12-07T23:59:59, 2026-12-07T23:59:59.000, EV102",
            "2026-12-01T10:00, 2026-12-07T18:00, 2026-12-01T09:59, EV102",
            "2026-12-01T10:00, 2026-12-07T18:00, 2026-12-07T17:59:59, 2026-12-07",
            "2026-12-01T10:00, 2026-12-07T18:00, 2026-12-07T18:00:00, EV102",
            "2026-12-01T10:00, 2026-12-01T10:00, 2026-12-01T09:00, EV102",
            "2026-12-01T10:00, 2026-12-01T10:00, 2026-12-01T10:00, EV102",
            "2026-12-01T10:00, 2026-12-01T10:00, 2026-12-01T11:00, EV102"
        )
        @DisplayName("명세 5.2.3 시간 경계: 기간 밖 KST 시각은 EV102이고, 기간 안은 그 KST 날짜로 출석한다")
        fun `시간 경계`(startedAt: String, expiredAt: String, requestedAt: String, expected: String) {
            val event = givenEvent(attendanceEvent(startedAt, expiredAt))
            if (expected == EVENT_NOT_ACTIVE) {
                assertEventError(EventErrorCode.EVENT_NOT_ACTIVE) { attendAtKst(requestedAt) }
                verifyNoInteractions(eventAttendanceRepository)
                return
            }
            givenAttendable(event, LocalDate.parse(expected))
            val result = attendAtKst(requestedAt)
            assertThat(result.attendance?.today).isEqualTo(LocalDate.parse(expected))
            assertThat(savedDates()).containsExactly(LocalDate.parse(expected))
        }

        @Test
        @DisplayName("KST 자정 직전에 출석하고 자정 직후에 다시 요청하면 날짜가 달라 둘 다 출석한다")
        fun `자정을 사이에 둔 두 요청`() {
            val event = givenEvent(attendanceEvent("2026-12-01T00:00", "2026-12-08T00:00"))
            givenAttendable(event, LocalDate.parse("2026-12-03"))
            givenNotAttended(LocalDate.parse("2026-12-04"))

            attendAtKst("2026-12-03T23:59:59.900")
            attendAtKst("2026-12-04T00:00:00.100")

            assertThat(savedDates()).containsExactly(LocalDate.parse("2026-12-03"), LocalDate.parse("2026-12-04"))
        }

        @Test
        @DisplayName("시계는 요청마다 한 번만 읽는다: 읽을 때마다 1ms 가는 시계를 KST 자정 0.5ms 전에 두어도 모든 날짜가 그날이다")
        fun `시계는 한 번만 읽는다`() {
            val today = LocalDate.parse("2026-11-28")
            givenAttendable(givenEvent(attendanceEvent()), today)
            val clock = TickingClock(Instant.parse("2026-11-28T14:59:59.9995Z"))
            val useCase = AttendEventUseCaseImpl(clock, eventRepository, eventAttendanceRepository)

            val result = useCase.execute(COMMAND, member())

            assertThat(clock.reads).isOne()
            assertThat(result.attendance?.today).isEqualTo(today)
            assertThat(savedDates()).containsExactly(today)
            verify(eventAttendanceRepository).existsByEventIdAndMemberIdAndDate(EventId(EVENT_ID), MEMBER_PK, today)
        }
    }

    @Nested
    @DisplayName("검증 순서(EV001 → EV108 → EV102 → MB201 → EV304)")
    inner class Order {

        @Test
        @DisplayName("이벤트가 없으면 프로필보다 먼저 EV001이고 출석 기록을 읽지 않는다")
        fun `없는 이벤트`() {
            whenever(eventRepository.findById(EventId(EVENT_ID))).thenReturn(null)

            assertEventError(EventErrorCode.EVENT_NOT_FOUND) { attendAt(IN_PROGRESS_AT, member(null)) }

            verifyNoInteractions(eventAttendanceRepository)
        }

        @Test
        @DisplayName("유형이 ATTENDANCE가 아니면 기간·프로필보다 먼저 EV108")
        fun `유형 불일치`() {
            givenEvent(attendanceEvent(type = EventType.GENERAL))

            assertEventError(EventErrorCode.EVENT_TYPE_MISMATCH) { attendAt(ENDED_AT, member(null)) }

            verifyNoInteractions(eventAttendanceRepository)
        }

        @Test
        @DisplayName("참여 가능 구간 밖이면 프로필보다 먼저 EV102")
        fun `기간 밖`() {
            givenEvent(attendanceEvent())

            assertEventError(EventErrorCode.EVENT_NOT_ACTIVE) { attendAt(ENDED_AT, member(null)) }

            verifyNoInteractions(eventAttendanceRepository)
        }

        @ParameterizedTest(name = "{displayName} [memberId={0}, nickname={1}]")
        @CsvSource(", 출석왕", "attender, ", ", ")
        @DisplayName("아이디나 닉네임이 없으면 MB201이고 출석 기록을 읽지 않는다")
        fun `프로필 미완성`(memberId: String?, nickname: String?) {
            givenEvent(attendanceEvent())

            val exception = assertThrows<MemberException> { attendAt(IN_PROGRESS_AT, member(memberId, nickname)) }

            assertThat(exception.errorCode).isEqualTo(MemberErrorCode.PROFILE_NOT_COMPLETED)
            verifyNoInteractions(eventAttendanceRepository)
        }

        @Test
        @DisplayName("오늘 출석 기록이 있으면 EV304이고 저장하지 않는다")
        fun `오늘 이미 출석`() {
            givenEvent(attendanceEvent())
            whenever(eventAttendanceRepository.existsByEventIdAndMemberIdAndDate(EventId(EVENT_ID), MEMBER_PK, TODAY))
                .thenReturn(true)

            assertEventError(EventErrorCode.ALREADY_ATTENDED_TODAY) { attendAt(IN_PROGRESS_AT) }

            verify(eventAttendanceRepository, never()).save(any())
        }

        @Test
        @DisplayName("저장이 경합으로 EV304를 던지면 그대로 던지고 출석 날짜를 조회하지 않는다")
        fun `저장 경합`() {
            givenEvent(attendanceEvent())
            givenNotAttended(TODAY)
            whenever(eventAttendanceRepository.save(any())).thenThrow(EventException.alreadyAttendedToday())

            assertEventError(EventErrorCode.ALREADY_ATTENDED_TODAY) { attendAt(IN_PROGRESS_AT) }

            verify(eventAttendanceRepository).existsByEventIdAndMemberIdAndDate(EventId(EVENT_ID), MEMBER_PK, TODAY)
            verify(eventAttendanceRepository).save(any())
            verifyNoMoreInteractions(eventAttendanceRepository)
        }
    }

    @Nested
    @DisplayName("결과 매핑")
    inner class ResultMapping {

        @Test
        @DisplayName("출석한 뒤의 상태를 공통 상태 응답 모양으로 돌려준다")
        fun `출석 뒤 상태`() {
            val event = givenEvent(attendanceEvent())
            givenAttendable(event, TODAY, listOf(TODAY.minusDays(2), TODAY.minusDays(1), TODAY))

            val result = attendAt(IN_PROGRESS_AT)

            assertThat(result).isEqualTo(
                MyEventStatusResult(
                    eventId = EVENT_ID,
                    type = EventType.ATTENDANCE,
                    isInProgress = true,
                    attendance = attendanceResult(hasAttendedToday = true, total = 3, consecutive = 3),
                    entry = null
                )
            )
        }

        @Test
        @DisplayName("하루를 빠졌으면 총 일수는 그대로 세고 연속 일수는 오늘부터 다시 센다")
        fun `하루 빠진 뒤 출석`() {
            val event = givenEvent(attendanceEvent())
            givenAttendable(event, TODAY, listOf(TODAY.minusDays(2), TODAY))

            val result = attendAt(IN_PROGRESS_AT)

            val expected = attendanceResult(hasAttendedToday = true, total = 2, consecutive = 1)
            assertThat(result.attendance).isEqualTo(expected)
        }

        @Test
        @DisplayName("출석은 이벤트 id·회원 PK·오늘로 저장하고, 기간 날짜의 양 끝으로 출석 날짜를 조회한다")
        fun `포트 인자`() {
            val event = givenEvent(attendanceEvent())
            givenAttendable(event, TODAY)

            attendAt(IN_PROGRESS_AT)

            val saved = argumentCaptor<EventAttendance>().apply { verify(eventAttendanceRepository).save(capture()) }
            assertThat(saved.firstValue.eventId).isEqualTo(EventId(EVENT_ID))
            assertThat(saved.firstValue.memberId).isEqualTo(MEMBER_PK)
            assertThat(saved.firstValue.attendanceDate).isEqualTo(TODAY)
            verify(eventAttendanceRepository)
                .findDates(EventId(EVENT_ID), MEMBER_PK, PERIOD_START_DATE, PERIOD_END_DATE)
        }

        private fun attendanceResult(hasAttendedToday: Boolean, total: Int, consecutive: Int): AttendanceStatusResult {
            return AttendanceStatusResult(
                today = TODAY,
                periodStartDate = PERIOD_START_DATE,
                periodEndDate = PERIOD_END_DATE,
                hasAttendedToday = hasAttendedToday,
                isAttendable = !hasAttendedToday,
                totalAttendanceDays = total,
                consecutiveAttendanceDays = consecutive
            )
        }
    }

    private fun givenEvent(event: Event): Event {
        whenever(eventRepository.findById(EventId(EVENT_ID))).thenReturn(event)
        return event
    }

    private fun givenAttendable(event: Event, today: LocalDate, attendedDates: List<LocalDate> = listOf(today)) {
        val period = event.attendancePeriod()
        givenNotAttended(today)
        whenever(eventAttendanceRepository.save(any())).thenAnswer { it.getArgument<EventAttendance>(0) }
        val from = period.startDate!!
        val to = period.endDate!!
        whenever(eventAttendanceRepository.findDates(EventId(EVENT_ID), MEMBER_PK, from, to)).thenReturn(attendedDates)
    }

    private fun givenNotAttended(today: LocalDate) {
        whenever(eventAttendanceRepository.existsByEventIdAndMemberIdAndDate(EventId(EVENT_ID), MEMBER_PK, today))
            .thenReturn(false)
    }

    private fun savedDates(): List<LocalDate> {
        val captor = argumentCaptor<EventAttendance>()
        verify(eventAttendanceRepository, atLeastOnce()).save(captor.capture())
        return captor.allValues.map { it.attendanceDate }
    }

    private fun useCaseAt(instant: Instant): AttendEventUseCaseImpl {
        return AttendEventUseCaseImpl(Clock.fixed(instant, ZoneOffset.UTC), eventRepository, eventAttendanceRepository)
    }

    private fun attendAt(instant: Instant, member: Member = member()): MyEventStatusResult {
        return useCaseAt(instant).execute(COMMAND, member)
    }

    private fun attendAtKst(kst: String): MyEventStatusResult = attendAt(LocalDateTime.parse(kst).toInstant(KST_OFFSET))

    private fun assertEventError(errorCode: EventErrorCode, action: () -> Unit) {
        val exception = assertThrows<EventException> { action() }

        assertThat(exception.errorCode).isEqualTo(errorCode)
    }

    private fun attendanceEvent(
        startedAt: String = PERIOD_START,
        expiredAt: String = PERIOD_END,
        type: EventType = EventType.ATTENDANCE
    ): Event {
        return Event.reconstitute(
            id = EventId(EVENT_ID),
            title = "출석 이벤트",
            subTitle = null,
            description = null,
            startedAt = LocalDateTime.parse(startedAt),
            expiredAt = LocalDateTime.parse(expiredAt),
            thumbnailUrl = "https://example.com/thumbnail.jpg",
            type = type,
            googleFormUrl = null,
            status = EventStatusType.ACTIVE,
            createdAt = LocalDateTime.parse(startedAt),
            updatedAt = LocalDateTime.parse(startedAt)
        )
    }

    private fun member(memberId: String? = "attender", nickname: String? = "출석왕"): Member {
        return Member.reconstitute(
            id = MEMBER_PK,
            memberId = memberId,
            nickname = nickname,
            roleId = null,
            profileImage = "",
            provider = SocialProvider.GOOGLE,
            socialId = "attender-social",
            email = "attender@example.com",
            password = null,
            emailVerified = true,
            createdAt = LocalDateTime.parse(PERIOD_START),
            updatedAt = LocalDateTime.parse(PERIOD_START)
        )
    }

    private class TickingClock(private var next: Instant) : Clock() {
        var reads = 0
            private set

        override fun getZone(): ZoneId = ZoneOffset.UTC

        override fun withZone(zone: ZoneId?): Clock = this

        override fun instant(): Instant {
            reads++
            return next.also { next = it.plusMillis(1) }
        }
    }

    companion object {
        private const val EVENT_ID = 12L
        private const val MEMBER_PK = 7L
        private const val EVENT_NOT_ACTIVE = "EV102"
        private const val PERIOD_START = "2026-11-25T00:00"
        private const val PERIOD_END = "2026-12-02T00:00"
        private val COMMAND = AttendEventCommand(eventId = EVENT_ID)
        private val KST_OFFSET: ZoneOffset = ZoneOffset.ofHours(9)
        private val TODAY: LocalDate = LocalDate.parse("2026-11-28")
        private val PERIOD_START_DATE: LocalDate = LocalDate.parse("2026-11-25")
        private val PERIOD_END_DATE: LocalDate = LocalDate.parse("2026-12-01")
        private val IN_PROGRESS_AT: Instant = Instant.parse("2026-11-28T01:00:00Z")
        private val ENDED_AT: Instant = Instant.parse("2026-12-01T15:00:00Z")
    }
}
