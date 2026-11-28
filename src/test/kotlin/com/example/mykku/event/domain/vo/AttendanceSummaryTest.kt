package com.example.mykku.event.domain.vo

import com.example.mykku.event.domain.entity.Event
import com.example.mykku.util.DefaultTimeZone
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertTimeoutPreemptively
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

@DisplayName("AttendanceSummary 출석 일수 계산 테스트")
class AttendanceSummaryTest {

    @Nested
    @DisplayName("명세 5.2.2 예시 1: 2026-12-01T00:00 ~ 2026-12-08T00:00")
    inner class MidnightPeriod {

        private val event = eventBetween("2026-12-01T00:00", "2026-12-08T00:00")

        @Test
        @DisplayName("12-01 출석 직후: 1일, 연속 1일, 오늘 출석함, 출석 불가")
        fun `예시1 - 첫 출석 직후`() {
            assertSummary(summaryAt(event, "2026-12-01T10:00", "2026-12-01"), Expected(1, 1, true, false))
        }

        @Test
        @DisplayName("12-02 출석 직후: 2일, 연속 2일")
        fun `예시1 - 이틀 연속 출석 직후`() {
            assertSummary(summaryAt(event, "2026-12-02T10:00", "2026-12-01", "2026-12-02"), Expected(2, 2, true, false))
        }

        @Test
        @DisplayName("12-03 아직 미출석이면 어제까지 이어진 2일을 보여 주고 출석할 수 있다")
        fun `예시1 - 오늘 미출석이면 살아 있는 연속`() {
            assertSummary(summaryAt(event, "2026-12-03T10:00", "2026-12-01", "2026-12-02"), Expected(2, 2, false, true))
        }

        @Test
        @DisplayName("12-03을 빠진 채 12-04에 조회하면 연속은 0이다")
        fun `예시1 - 하루 빠지면 연속 0`() {
            assertSummary(summaryAt(event, "2026-12-04T10:00", "2026-12-01", "2026-12-02"), Expected(2, 0, false, true))
        }

        @Test
        @DisplayName("끊긴 뒤 12-04에 다시 출석하면 3일, 연속 1일이다")
        fun `예시1 - 끊긴 뒤 다시 출석`() {
            val summary = summaryAt(event, "2026-12-04T10:00", "2026-12-01", "2026-12-02", "2026-12-04")

            assertSummary(summary, Expected(3, 1, true, false))
        }

        @Test
        @DisplayName("종료 뒤 12-09에 조회하면 마지막 날(12-07)을 빠졌으므로 연속 0이고 출석할 수 없다")
        fun `예시1 - 종료 뒤 마지막 날 미출석`() {
            val summary = summaryAt(event, "2026-12-09T10:00", "2026-12-01", "2026-12-02", "2026-12-04")

            assertSummary(summary, Expected(3, 0, false, false))
        }

        @Test
        @DisplayName("종료 뒤 연속은 마지막 날부터 빈 날 전까지만 센다(12-01·02·06·07 출석이면 2일)")
        fun `예시1 확장 - 종료 뒤 빈 날에서 끊김`() {
            val summary = summaryAt(event, "2026-12-09T10:00", "2026-12-01", "2026-12-02", "2026-12-06", "2026-12-07")

            assertSummary(summary, Expected(4, 2, false, false))
        }

        @Test
        @DisplayName("오늘 미출석이면 어제부터 빈 날 전까지만 센다(12-01·03 출석, 12-04 조회면 1일)")
        fun `예시1 확장 - 어제부터 빈 날에서 끊김`() {
            val summary = summaryAt(event, "2026-12-04T10:00", "2026-12-01", "2026-12-03")

            assertSummary(summary, Expected(2, 1, false, true))
        }
    }

    @Nested
    @DisplayName("명세 5.2.2 예시 2: 자정이 아닌 종료 2026-12-01T10:00 ~ 2026-12-07T18:00, 12-01~12-06 매일 출석")
    inner class NonMidnightEnd {

        private val event = eventBetween("2026-12-01T10:00", "2026-12-07T18:00")
        private val firstSixDays = daysFrom("2026-12-01", 6)

        @Test
        @DisplayName("12-07T17:00 미출석이면 진행 중이라 6일, 연속 6일이고 출석할 수 있다")
        fun `예시2 - 마지막 날 종료 전`() {
            val summary = summaryAt(event, "2026-12-07T17:00", *firstSixDays)

            assertSummary(summary, Expected(6, 6, false, true))
        }

        @ParameterizedTest(name = "{displayName} [{0}]")
        @CsvSource("2026-12-07T20:00", "2026-12-08T00:00:01")
        @DisplayName("종료 뒤에는 마지막 날(12-07)을 빠졌으므로 연속 0이고, 자정이 지나도 값이 같다")
        fun `예시2 - 종료 뒤 마지막 날 미출석`(now: String) {
            assertSummary(summaryAt(event, now, *firstSixDays), Expected(6, 0, false, false))
        }

        @ParameterizedTest(name = "{displayName} [{0}]")
        @CsvSource("2026-12-07T20:00, true", "2026-12-08T00:00:01, false", "2026-12-08T10:00, false")
        @DisplayName("12-07T17:30에 출석했다면 종료 뒤 7일, 연속 7일이고 자정이 지나도 같다")
        fun `예시2 - 종료 뒤 마지막 날 출석`(now: String, attendedToday: Boolean) {
            val summary = summaryAt(event, now, *daysFrom("2026-12-01", 7))

            assertSummary(summary, Expected(7, 7, attendedToday, false))
        }
    }

    @Nested
    @DisplayName("명세 5.2.2 예시 3: 시작 = 종료(2026-12-01T10:00)이면 기간 날짜가 비어 있다")
    inner class EmptyPeriod {

        @ParameterizedTest(name = "{displayName} [{0}]")
        @CsvSource("2026-12-01T09:00", "2026-12-01T10:00", "2026-12-01T23:59:59", "2026-12-02T10:00")
        @DisplayName("언제 조회해도 0일, 연속 0일이고 출석할 수 없다")
        fun `예시3 - 언제든 0`(now: String) {
            val event = eventBetween("2026-12-01T10:00", "2026-12-01T10:00")

            assertThat(event.attendancePeriod().isEmpty).isTrue()
            assertSummary(summaryAt(event, now, "2026-12-01"), Expected(0, 0, false, false))
        }
    }

    @Nested
    @DisplayName("명세 5.2.3 시간 경계: 200은 진행 중이고 출석 가능, EV102는 진행 중이 아님")
    inner class TimeBoundaries {

        @ParameterizedTest(name = "{displayName} [{0} ~ {1} @ {2}]")
        @CsvSource(
            "2026-12-01T00:00, 2026-12-08T00:00, 2026-11-30T23:59:59.999, BEFORE, 2026-11-30",
            "2026-12-01T00:00, 2026-12-08T00:00, 2026-12-01T00:00:00.000, IN_PROGRESS, 2026-12-01",
            "2026-12-01T00:00, 2026-12-08T00:00, 2026-12-07T23:59:59.999, IN_PROGRESS, 2026-12-07",
            "2026-12-01T00:00, 2026-12-08T00:00, 2026-12-08T00:00:00.000, ENDED, 2026-12-08",
            "2026-12-01T00:00, 2026-12-07T23:59:59, 2026-12-07T23:59:58.999, IN_PROGRESS, 2026-12-07",
            "2026-12-01T00:00, 2026-12-07T23:59:59, 2026-12-07T23:59:59.000, ENDED, 2026-12-07",
            "2026-12-01T10:00, 2026-12-07T18:00, 2026-12-01T09:59, BEFORE, 2026-12-01",
            "2026-12-01T10:00, 2026-12-07T18:00, 2026-12-07T17:59:59, IN_PROGRESS, 2026-12-07",
            "2026-12-01T10:00, 2026-12-07T18:00, 2026-12-07T18:00:00, ENDED, 2026-12-07",
            "2026-12-01T10:00, 2026-12-01T10:00, 2026-12-01T10:00, ENDED, 2026-12-01"
        )
        @DisplayName("요청 시각의 진행 상태와 오늘 날짜")
        fun `시간 경계 - 진행 상태와 오늘`(startedAt: String, expiredAt: String, now: String, phase: EventPhase, today: String) {
            val event = eventBetween(startedAt, expiredAt)
            val summary = summaryAt(event, now)

            assertThat(event.phaseAt(at(now))).isEqualTo(phase)
            assertThat(at(now).toLocalDate()).isEqualTo(LocalDate.parse(today))
            assertThat(summary.isAttendable).isEqualTo(phase == EventPhase.IN_PROGRESS)
        }

        @Test
        @DisplayName("기존 관례(종료 23:59:59)도 기간 날짜는 12-01~12-07이다")
        fun `시간 경계 - 23시59분59초 종료의 기간 날짜`() {
            val period = eventBetween("2026-12-01T00:00", "2026-12-07T23:59:59").attendancePeriod()

            assertThat(period.startDate).isEqualTo(LocalDate.parse("2026-12-01"))
            assertThat(period.endDate).isEqualTo(LocalDate.parse("2026-12-07"))
        }

        @Test
        @DisplayName("12-03T23:59:59.900에 출석하고 12-04T00:00:00.100에 다시 요청하면 날짜가 달라 다시 출석할 수 있다")
        fun `시간 경계 - 자정 직전 출석 뒤 자정 직후 재요청`() {
            val event = eventBetween("2026-12-01T00:00", "2026-12-08T00:00")
            val beforeMidnight = summaryAt(event, "2026-12-03T23:59:59.900")
            val afterMidnight = summaryAt(event, "2026-12-04T00:00:00.100", "2026-12-03")

            assertSummary(beforeMidnight, Expected(0, 0, false, true))
            assertSummary(afterMidnight, Expected(1, 1, false, true))
        }
    }

    @Nested
    @DisplayName("KST 자정 경계: UTC 인스턴트를 KST로 바꾼 입력, 계산은 JVM 기본 시간대를 읽지 않아 UTC에서도 같다")
    inner class KstMidnight {

        private val event = eventBetween("2026-12-01T00:00", "2026-12-08T00:00")
        private val attended = arrayOf("2026-12-01", "2026-12-02", "2026-12-03")

        @Test
        @DisplayName("UTC 2026-12-03T15:00Z는 UTC 날짜로는 12-03이지만 KST로는 12-04 00:00이다")
        fun `KST 자정 - 시험 입력의 전제`() {
            assertThat(LocalDateTime.ofInstant(Instant.parse("2026-12-03T15:00:00Z"), ZoneOffset.UTC).toLocalDate())
                .isEqualTo(LocalDate.parse("2026-12-03"))
            assertThat(kst("2026-12-03T15:00:00Z")).isEqualTo(at("2026-12-04T00:00:00"))
            assertThat(kst("2026-12-03T14:59:59.999Z")).isEqualTo(at("2026-12-03T23:59:59.999"))
        }

        @ParameterizedTest(name = "{displayName} [{0}]")
        @CsvSource("UTC", "Asia/Seoul")
        @DisplayName("KST 23:59:59.999에는 오늘(12-03) 출석함, 00:00:00에는 오늘(12-04) 미출석이고 어제까지 3일 연속이다")
        fun `KST 자정 - 오늘이 바뀐다`(jvmZone: String) {
            DefaultTimeZone.with(jvmZone) {
                assertSummary(summaryAt(event, kst("2026-12-03T14:59:59.999Z"), *attended), Expected(3, 3, true, false))
                assertSummary(summaryAt(event, kst("2026-12-03T15:00:00Z"), *attended), Expected(3, 3, false, true))
            }
        }

        @ParameterizedTest(name = "{displayName} [{0}]")
        @CsvSource("UTC", "Asia/Seoul")
        @DisplayName("종료 경계 KST 12-07 23:59:59.999는 진행 중, 12-08 00:00:00은 종료다")
        fun `KST 자정 - 종료 경계`(jvmZone: String) {
            DefaultTimeZone.with(jvmZone) {
                assertThat(event.phaseAt(kst("2026-12-07T14:59:59.999Z"))).isEqualTo(EventPhase.IN_PROGRESS)
                assertThat(event.phaseAt(kst("2026-12-07T15:00:00Z"))).isEqualTo(EventPhase.ENDED)
                assertThat(event.attendancePeriod().endDate).isEqualTo(LocalDate.parse("2026-12-07"))
            }
        }

        @ParameterizedTest(name = "{displayName} [{0}]")
        @CsvSource("UTC", "Asia/Seoul")
        @DisplayName("시작 경계 KST 11-30 23:59:59.999는 시작 전, 12-01 00:00:00은 진행 중이다")
        fun `KST 자정 - 시작 경계`(jvmZone: String) {
            DefaultTimeZone.with(jvmZone) {
                assertThat(event.phaseAt(kst("2026-11-30T14:59:59.999Z"))).isEqualTo(EventPhase.BEFORE)
                assertThat(event.phaseAt(kst("2026-11-30T15:00:00Z"))).isEqualTo(EventPhase.IN_PROGRESS)
                assertThat(event.attendancePeriod().startDate).isEqualTo(LocalDate.parse("2026-12-01"))
            }
        }
    }

    @Nested
    @DisplayName("기간 밖 날짜와 입력 형태")
    inner class OutOfPeriodAndInputShape {

        @Test
        @DisplayName("기간 시작을 늦추면 새 시작일 전 출석은 총·연속 일수에서 빠진다(D13)")
        fun `기간 수정 - 시작 전으로 밀려난 날짜 제외`() {
            val event = eventBetween("2026-12-01T00:00", "2026-12-08T00:00")
            val dates = arrayOf("2026-11-28", "2026-11-29", "2026-11-30", "2026-12-01")

            assertSummary(summaryAt(event, "2026-12-02T10:00", *dates), Expected(1, 1, false, true))
        }

        @Test
        @DisplayName("기간 끝을 당기면 새 마지막 날 뒤 출석은 빠지고, 종료 뒤 연속은 새 마지막 날 기준이다(D13)")
        fun `기간 수정 - 끝 뒤로 밀려난 날짜 제외`() {
            val event = eventBetween("2026-12-01T00:00", "2026-12-04T00:00")
            val dates = daysFrom("2026-12-01", 5)

            assertSummary(summaryAt(event, "2026-12-06T10:00", *dates), Expected(3, 3, false, false))
        }

        @Test
        @DisplayName("정렬되지 않고 중복된 입력도 날짜 집합으로 센다")
        fun `입력 - 정렬 안 됨과 중복`() {
            val event = eventBetween("2026-12-01T00:00", "2026-12-08T00:00")
            val dates = arrayOf("2026-12-03", "2026-12-01", "2026-12-02", "2026-12-03", "2026-12-01")

            assertSummary(summaryAt(event, "2026-12-03T10:00", *dates), Expected(3, 3, true, false))
        }

        @Test
        @DisplayName("시작 전(BEFORE)에는 기간 안에 남은 행이 있어도 연속 0이고 출석할 수 없다")
        fun `시작 전 - 남은 행`() {
            val event = eventBetween("2026-12-03T12:00", "2026-12-10T00:00")
            val summary = summaryAt(event, "2026-12-03T09:00", "2026-12-01", "2026-12-02", "2026-12-03")

            assertThat(event.phaseAt(at("2026-12-03T09:00"))).isEqualTo(EventPhase.BEFORE)
            assertSummary(summary, Expected(1, 0, true, false))
        }

        @Test
        @DisplayName("시작이 종료보다 늦게 저장된 기간은 그날 행이 있어도 모두 0이고 출석할 수 없다")
        fun `뒤집힌 기간 - 모두 0`() {
            val event = eventBetween("2026-12-01T10:00", "2026-12-01T09:00")

            assertSummary(summaryAt(event, "2026-12-01T12:00", "2026-12-01"), Expected(0, 0, false, false))
        }

        @Test
        @DisplayName("시작 전(BEFORE)에 기간 밖 행만 있으면 모두 0이다")
        fun `시작 전 - 기간 밖 행만`() {
            val event = eventBetween("2026-12-05T00:00", "2026-12-10T00:00")
            val summary = summaryAt(event, "2026-12-04T10:00", "2026-12-01", "2026-12-02")

            assertSummary(summary, Expected(0, 0, false, false))
        }
    }

    @Nested
    @DisplayName("긴 기간")
    inner class LongPeriod {

        @Test
        @DisplayName("종료가 9999-12-31이어도 기간을 날짜 목록으로 펼치지 않고 곧바로 계산한다")
        fun `긴 기간 - 9999년 종료`() {
            val event = eventBetween("2026-12-01T00:00", "9999-12-31T23:59:59")
            val period = event.attendancePeriod()
            val dates = dates("2026-12-01", "2026-12-02")

            assertTimeoutPreemptively(Duration.ofSeconds(1)) {
                val inProgress = summaryAt(event, "2026-12-03T10:00", "2026-12-01", "2026-12-02")
                val ended = AttendanceSummary.of(dates, today("2026-12-03"), period, EventPhase.ENDED)

                assertSummary(inProgress, Expected(2, 2, false, true))
                assertSummary(ended, Expected(2, 0, false, false))
            }
            assertThat(period.endDate).isEqualTo(LocalDate.parse("9999-12-31"))
        }

        @Test
        @DisplayName("표현할 수 있는 가장 긴 기간도 곧바로 계산한다")
        fun `긴 기간 - 최대 범위`() {
            val period = AttendancePeriod.of(LocalDateTime.MIN, LocalDateTime.MAX)
            val dates = dates("2026-12-01", "2026-12-02")

            assertTimeoutPreemptively(Duration.ofSeconds(1)) {
                val inProgress = AttendanceSummary.of(dates, today("2026-12-03"), period, EventPhase.IN_PROGRESS)
                val ended = AttendanceSummary.of(dates, today("2026-12-03"), period, EventPhase.ENDED)

                assertSummary(inProgress, Expected(2, 2, false, true))
                assertSummary(ended, Expected(2, 0, false, false))
            }
        }
    }

    private data class Expected(
        val total: Int,
        val consecutive: Int,
        val hasAttendedToday: Boolean,
        val isAttendable: Boolean
    )

    private fun assertSummary(summary: AttendanceSummary, expected: Expected) {
        assertThat(summary.totalAttendanceDays).`as`("totalAttendanceDays").isEqualTo(expected.total)
        assertThat(summary.consecutiveAttendanceDays).`as`("consecutiveAttendanceDays").isEqualTo(expected.consecutive)
        assertThat(summary.hasAttendedToday).`as`("hasAttendedToday").isEqualTo(expected.hasAttendedToday)
        assertThat(summary.isAttendable).`as`("isAttendable").isEqualTo(expected.isAttendable)
    }

    private fun summaryAt(event: Event, now: String, vararg attended: String): AttendanceSummary {
        return summaryAt(event, at(now), *attended)
    }

    private fun summaryAt(event: Event, now: LocalDateTime, vararg attended: String): AttendanceSummary {
        return AttendanceSummary.of(dates(*attended), now.toLocalDate(), event.attendancePeriod(), event.phaseAt(now))
    }

    private fun eventBetween(startedAt: String, expiredAt: String): Event {
        return Event.reconstitute(
            id = EventId(1L),
            title = "출석 이벤트",
            subTitle = null,
            description = null,
            startedAt = at(startedAt),
            expiredAt = at(expiredAt),
            thumbnailUrl = "https://example.com/thumbnail.jpg",
            type = EventType.ATTENDANCE,
            googleFormUrl = null,
            status = EventStatusType.ACTIVE,
            createdAt = at(startedAt),
            updatedAt = at(startedAt)
        )
    }

    private fun daysFrom(first: String, count: Long): Array<String> {
        return Array(count.toInt()) { LocalDate.parse(first).plusDays(it.toLong()).toString() }
    }

    private fun dates(vararg values: String): List<LocalDate> = values.map { LocalDate.parse(it) }

    private fun today(value: String): LocalDate = LocalDate.parse(value)

    private fun at(value: String): LocalDateTime = LocalDateTime.parse(value)

    private fun kst(instant: String): LocalDateTime = LocalDateTime.ofInstant(Instant.parse(instant), KST)

    companion object {
        private val KST: ZoneId = ZoneId.of("Asia/Seoul")
    }
}
