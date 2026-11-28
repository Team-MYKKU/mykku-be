package com.example.mykku.event.domain

import com.example.mykku.event.domain.entity.Event
import com.example.mykku.event.domain.vo.EventContent
import com.example.mykku.event.domain.vo.EventId
import com.example.mykku.event.domain.vo.EventPhase
import com.example.mykku.event.domain.vo.EventStatusType
import com.example.mykku.event.domain.vo.EventType
import com.example.mykku.event.exception.EventErrorCode
import com.example.mykku.event.exception.EventException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("Event 도메인 엔티티 테스트")
class EventTest {

    @Nested
    @DisplayName("create 메서드")
    inner class Create {

        @Test
        @DisplayName("정상적으로 이벤트를 생성한다")
        fun `이벤트 생성 - 정상 케이스`() {
            val startedAt = LocalDateTime.now().plusDays(1)
            val expiredAt = LocalDateTime.now().plusDays(7)

            val event = Event.create(
                title = "테스트 이벤트",
                subTitle = null,
                description = "이벤트 설명",
                startedAt = startedAt,
                expiredAt = expiredAt,
                thumbnailUrl = "https://example.com/thumbnail.jpg",
                type = EventType.GENERAL,
                googleFormUrl = null
            )

            assertThat(event.id.value).isEqualTo(0L)
            assertThat(event.title).isEqualTo("테스트 이벤트")
            assertThat(event.description).isEqualTo("이벤트 설명")
            assertThat(event.startedAt).isEqualTo(startedAt)
            assertThat(event.expiredAt).isEqualTo(expiredAt)
        }

        @Test
        @DisplayName("이벤트 생성시 status가 ACTIVE이다")
        fun `이벤트 생성 - 기본 상태 검증`() {
            val event = createEvent()

            assertThat(event.status).isEqualTo(EventStatusType.ACTIVE)
        }

        @Test
        @DisplayName("이벤트 생성시 createdAt과 updatedAt이 설정된다")
        fun `이벤트 생성 - 시간 설정 검증`() {
            val event = createEvent()

            assertThat(event.createdAt).isNotNull()
            assertThat(event.updatedAt).isNotNull()
        }

        @Test
        @DisplayName("설명 없이 이벤트를 생성할 수 있다")
        fun `이벤트 생성 - 설명 없음`() {
            val event = Event.create(
                title = "테스트 이벤트",
                subTitle = null,
                description = null,
                startedAt = LocalDateTime.now().plusDays(1),
                expiredAt = LocalDateTime.now().plusDays(7),
                thumbnailUrl = "https://example.com/thumbnail.jpg",
                type = EventType.GENERAL,
                googleFormUrl = null
            )

            assertThat(event.description).isNull()
        }
    }

    @Nested
    @DisplayName("updateStatus 메서드")
    inner class UpdateStatus {

        @Test
        @DisplayName("이벤트 상태를 EXPIRED로 변경할 수 있다")
        fun `상태 변경 - EXPIRED`() {
            val event = createEvent()

            event.updateStatus(EventStatusType.EXPIRED)

            assertThat(event.status).isEqualTo(EventStatusType.EXPIRED)
        }

        @Test
        @DisplayName("이벤트 상태를 WINNER_SELECTING으로 변경할 수 있다")
        fun `상태 변경 - WINNER_SELECTING`() {
            val event = createEvent()

            event.updateStatus(EventStatusType.WINNER_SELECTING)

            assertThat(event.status).isEqualTo(EventStatusType.WINNER_SELECTING)
        }

        @Test
        @DisplayName("이벤트 상태를 WINNER_SELECTED로 변경할 수 있다")
        fun `상태 변경 - WINNER_SELECTED`() {
            val event = createEvent()

            event.updateStatus(EventStatusType.WINNER_SELECTED)

            assertThat(event.status).isEqualTo(EventStatusType.WINNER_SELECTED)
        }

        @Test
        @DisplayName("EXPIRED 상태에서 ACTIVE로 다시 변경할 수 있다")
        fun `상태 변경 - ACTIVE 복원`() {
            val event = createEvent()
            event.updateStatus(EventStatusType.EXPIRED)

            event.updateStatus(EventStatusType.ACTIVE)

            assertThat(event.status).isEqualTo(EventStatusType.ACTIVE)
        }
    }

    @Nested
    @DisplayName("reconstitute 메서드")
    inner class Reconstitute {

        @Test
        @DisplayName("저장된 데이터로 Event를 복원한다")
        fun `복원 - 정상 케이스`() {
            val now = LocalDateTime.now()
            val startedAt = now.plusDays(1)
            val expiredAt = now.plusDays(7)

            val event = Event.reconstitute(
                id = EventId(1L),
                title = "복원된 이벤트",
                subTitle = null,
                description = "설명",
                startedAt = startedAt,
                expiredAt = expiredAt,
                thumbnailUrl = "https://example.com/thumbnail.jpg",
                type = EventType.GENERAL,
                googleFormUrl = null,
                status = EventStatusType.EXPIRED,
                createdAt = now,
                updatedAt = now
            )

            assertThat(event.id.value).isEqualTo(1L)
            assertThat(event.title).isEqualTo("복원된 이벤트")
            assertThat(event.status).isEqualTo(EventStatusType.EXPIRED)
        }

        @Test
        @DisplayName("다양한 상태로 복원할 수 있다")
        fun `복원 - 다양한 상태`() {
            val now = LocalDateTime.now()

            val activeEvent = Event.reconstitute(
                id = EventId(1L),
                title = "활성 이벤트",
                subTitle = null,
                description = null,
                startedAt = now,
                expiredAt = now.plusDays(7),
                thumbnailUrl = "https://example.com/thumbnail.jpg",
                type = EventType.GENERAL,
                googleFormUrl = null,
                status = EventStatusType.ACTIVE,
                createdAt = now,
                updatedAt = now
            )

            val winnerSelectedEvent = Event.reconstitute(
                id = EventId(2L),
                title = "수상자 선정 완료 이벤트",
                subTitle = null,
                description = null,
                startedAt = now,
                expiredAt = now.plusDays(7),
                thumbnailUrl = "https://example.com/thumbnail.jpg",
                type = EventType.GENERAL,
                googleFormUrl = null,
                status = EventStatusType.WINNER_SELECTED,
                createdAt = now,
                updatedAt = now
            )

            assertThat(activeEvent.status).isEqualTo(EventStatusType.ACTIVE)
            assertThat(winnerSelectedEvent.status).isEqualTo(EventStatusType.WINNER_SELECTED)
        }

        @Test
        @DisplayName("설명이 null인 Event를 복원할 수 있다")
        fun `복원 - 설명 없음`() {
            val now = LocalDateTime.now()

            val event = Event.reconstitute(
                id = EventId(1L),
                title = "설명 없는 이벤트",
                subTitle = null,
                description = null,
                startedAt = now,
                expiredAt = now.plusDays(7),
                thumbnailUrl = "https://example.com/thumbnail.jpg",
                type = EventType.GENERAL,
                googleFormUrl = null,
                status = EventStatusType.ACTIVE,
                createdAt = now,
                updatedAt = now
            )

            assertThat(event.description).isNull()
        }
    }

    @Nested
    @DisplayName("상수 검증")
    inner class Constants {

        @Test
        @DisplayName("IMAGE_MAX_COUNT가 10이다")
        fun `상수 검증 - IMAGE_MAX_COUNT`() {
            assertThat(Event.IMAGE_MAX_COUNT).isEqualTo(10)
        }
    }

    @Nested
    @DisplayName("상태 계산")
    inner class ResolveStatus {

        @Test
        @DisplayName("만료 시각이 남아있으면 ACTIVE다")
        fun `상태 계산 - 진행 중`() {
            val now = LocalDateTime.now()
            val event = createEvent(expiredAt = now.plusDays(1))

            assertThat(event.resolveStatus(now)).isEqualTo(EventStatusType.ACTIVE)
        }

        @Test
        @DisplayName("만료 시각과 같은 시각이면 EXPIRED다")
        fun `상태 계산 - 만료 경계`() {
            val now = LocalDateTime.now()
            val event = createEvent(expiredAt = now)

            assertThat(event.resolveStatus(now)).isEqualTo(EventStatusType.EXPIRED)
        }

        @Test
        @DisplayName("만료 시각이 지나면 EXPIRED다")
        fun `상태 계산 - 만료`() {
            val now = LocalDateTime.now()
            val event = createEvent(expiredAt = now.minusDays(1))

            assertThat(event.resolveStatus(now)).isEqualTo(EventStatusType.EXPIRED)
        }

        @Test
        @DisplayName("당첨자가 선정되면 만료 여부와 무관하게 WINNER_SELECTED를 유지한다")
        fun `상태 계산 - 당첨자 선정 완료`() {
            val now = LocalDateTime.now()
            val event = createEvent(expiredAt = now.minusDays(1))
            event.updateStatus(EventStatusType.WINNER_SELECTED)

            assertThat(event.resolveStatus(now)).isEqualTo(EventStatusType.WINNER_SELECTED)
        }
    }

    @Nested
    @DisplayName("유형 설정 검사")
    inner class ValidateTypeSetting {

        @Test
        @DisplayName("GOOGLE_FORM인데 구글폼 링크가 없으면 EV109")
        fun `유형 설정 - 구글폼 링크 누락`() {
            assertInvalidTypeSetting { Event.validateTypeSetting(EventType.GOOGLE_FORM, null) }
        }

        @Test
        @DisplayName("GOOGLE_FORM이고 링크가 응답 링크 규칙에 맞으면 통과한다")
        fun `유형 설정 - 구글폼 링크 정상`() {
            assertThatCode { Event.validateTypeSetting(EventType.GOOGLE_FORM, FORM_URL) }.doesNotThrowAnyException()
        }

        @Test
        @DisplayName("GOOGLE_FORM이어도 링크가 응답 링크 규칙에 맞지 않으면 EV109")
        fun `유형 설정 - 구글폼 링크 규칙 위반`() {
            assertInvalidTypeSetting { Event.validateTypeSetting(EventType.GOOGLE_FORM, EDIT_FORM_URL) }
        }

        @ParameterizedTest(name = "{displayName} [{0}]")
        @EnumSource(value = EventType::class, names = ["GOOGLE_FORM"], mode = EnumSource.Mode.EXCLUDE)
        @DisplayName("GOOGLE_FORM이 아닌 유형은 구글폼 링크가 없으면 통과한다")
        fun `유형 설정 - 다른 유형 링크 없음`(type: EventType) {
            assertThatCode { Event.validateTypeSetting(type, null) }.doesNotThrowAnyException()
        }

        @ParameterizedTest(name = "{displayName} [{0}]")
        @EnumSource(value = EventType::class, names = ["GOOGLE_FORM"], mode = EnumSource.Mode.EXCLUDE)
        @DisplayName("GOOGLE_FORM이 아닌 유형에 구글폼 링크가 있으면 EV109")
        fun `유형 설정 - 다른 유형에 링크`(type: EventType) {
            assertInvalidTypeSetting { Event.validateTypeSetting(type, FORM_URL) }
        }
    }

    @Nested
    @DisplayName("유형별 생성")
    inner class CreateWithType {

        @Test
        @DisplayName("유형과 구글폼 링크를 담아 생성한다")
        fun `생성 - 구글폼 유형`() {
            val event = createEvent(EventType.GOOGLE_FORM, FORM_URL)

            assertThat(event.type).isEqualTo(EventType.GOOGLE_FORM)
            assertThat(event.googleFormUrl).isEqualTo(FORM_URL)
        }

        @Test
        @DisplayName("생성할 때도 유형 설정을 검사한다")
        fun `생성 - 유형 설정 위반`() {
            assertInvalidTypeSetting { createEvent(EventType.GOOGLE_FORM, null) }
            assertInvalidTypeSetting { createEvent(EventType.GENERAL, FORM_URL) }
        }
    }

    @Nested
    @DisplayName("update 메서드")
    inner class Update {

        @Test
        @DisplayName("GOOGLE_FORM 이벤트의 구글폼 링크를 바꾼다")
        fun `수정 - 구글폼 링크 변경`() {
            val event = createEvent(EventType.GOOGLE_FORM, FORM_URL)

            event.update(contentOf(event, OTHER_FORM_URL), thumbnailUrl = null)

            assertThat(event.googleFormUrl).isEqualTo(OTHER_FORM_URL)
            assertThat(event.type).isEqualTo(EventType.GOOGLE_FORM)
        }

        @Test
        @DisplayName("GOOGLE_FORM 이벤트에서 링크를 빼거나 규칙 밖 링크로 바꾸면 EV109이고 값은 그대로다")
        fun `수정 - 구글폼 링크 누락과 규칙 위반`() {
            val event = createEvent(EventType.GOOGLE_FORM, FORM_URL)

            assertInvalidTypeSetting { event.update(contentOf(event, null, title = "새 제목"), thumbnailUrl = null) }
            assertInvalidTypeSetting { event.update(contentOf(event, EDIT_FORM_URL, title = "새 제목"), null) }

            assertThat(event.googleFormUrl).isEqualTo(FORM_URL)
            assertThat(event.title).isEqualTo("테스트 이벤트")
        }

        @Test
        @DisplayName("GOOGLE_FORM이 아닌 이벤트에 구글폼 링크를 넣으면 EV109")
        fun `수정 - 다른 유형에 링크`() {
            val event = createEvent(EventType.ATTENDANCE, null)

            assertInvalidTypeSetting { event.update(contentOf(event, FORM_URL), thumbnailUrl = null) }

            assertThat(event.googleFormUrl).isNull()
        }

        @Test
        @DisplayName("수정 내용에는 유형이 없어 수정해도 유형은 그대로다")
        fun `수정 - 유형 불변`() {
            val event = createEvent(EventType.ATTENDANCE, null)

            event.update(contentOf(event, null, title = "새 제목"), thumbnailUrl = null)

            assertThat(event.title).isEqualTo("새 제목")
            assertThat(event.type).isEqualTo(EventType.ATTENDANCE)
        }
    }

    @Nested
    @DisplayName("유형 설정 저장값 복원")
    inner class ReconstituteTypeSetting {

        @Test
        @DisplayName("유형 설정 규칙에 맞지 않는 저장값도 검사 없이 그대로 복원한다")
        fun `복원 - 규칙 밖 저장값`() {
            val withoutUrl = reconstitute(EventType.GOOGLE_FORM, null)
            val strayUrl = reconstitute(EventType.GENERAL, "http://example.com/not-a-form")

            assertThat(withoutUrl.type).isEqualTo(EventType.GOOGLE_FORM)
            assertThat(withoutUrl.googleFormUrl).isNull()
            assertThat(strayUrl.type).isEqualTo(EventType.GENERAL)
            assertThat(strayUrl.googleFormUrl).isEqualTo("http://example.com/not-a-form")
        }

        private fun reconstitute(type: EventType, googleFormUrl: String?): Event {
            return Event.reconstitute(
                id = EventId(1L),
                title = "복원된 이벤트",
                subTitle = null,
                description = null,
                startedAt = STARTED_AT,
                expiredAt = EXPIRED_AT,
                thumbnailUrl = "https://example.com/thumbnail.jpg",
                type = type,
                googleFormUrl = googleFormUrl,
                status = EventStatusType.ACTIVE,
                createdAt = STARTED_AT,
                updatedAt = STARTED_AT
            )
        }
    }

    @Nested
    @DisplayName("출석 기간 날짜")
    inner class AttendancePeriodOfEvent {

        @Test
        @DisplayName("종료가 00:00이면 기간 날짜는 전날까지다")
        fun `기간 날짜 - 자정 종료`() {
            val period = eventBetween("2026-12-01T00:00", "2026-12-08T00:00").attendancePeriod()

            assertThat(period.isEmpty).isFalse()
            assertThat(period.startDate).isEqualTo(LocalDate.of(2026, 12, 1))
            assertThat(period.endDate).isEqualTo(LocalDate.of(2026, 12, 7))
        }

        @ParameterizedTest(name = "{displayName} [{0}]")
        @ValueSource(strings = ["2026-12-07T18:00", "2026-12-07T23:59:59", "2026-12-07T00:00:00.000000001"])
        @DisplayName("종료가 그날 00:00 뒤면 기간 날짜는 그날까지다")
        fun `기간 날짜 - 자정이 아닌 종료`(expiredAt: String) {
            val period = eventBetween("2026-12-01T10:00", expiredAt).attendancePeriod()

            assertThat(period.startDate).isEqualTo(LocalDate.of(2026, 12, 1))
            assertThat(period.endDate).isEqualTo(LocalDate.of(2026, 12, 7))
        }

        @ParameterizedTest(name = "{displayName} [{0}]")
        @ValueSource(strings = ["2026-12-01T00:00", "2026-12-01T10:00"])
        @DisplayName("시작과 종료가 같으면 00:00이든 10:00이든 기간 날짜가 비어 있다")
        fun `기간 날짜 - 시작과 종료가 같으면 비어 있음`(startedAt: String) {
            val period = eventBetween(startedAt, startedAt).attendancePeriod()

            assertThat(period.isEmpty).isTrue()
            assertThat(period.startDate).isNull()
            assertThat(period.endDate).isNull()
            assertThat(period.contains(LocalDate.of(2026, 12, 1))).isFalse()
        }

        @ParameterizedTest(name = "{displayName} [{0} ~ {1}]")
        @CsvSource("2026-12-01T10:00, 2026-12-01T09:00", "2026-12-05T00:00, 2026-12-03T00:00")
        @DisplayName("시작이 종료보다 늦게 저장된 기간(검증 없는 복원)도 기간 날짜가 비어 있다")
        fun `기간 날짜 - 시작이 종료보다 늦으면 비어 있음`(startedAt: String, expiredAt: String) {
            val period = eventBetween(startedAt, expiredAt).attendancePeriod()

            assertThat(period.isEmpty).isTrue()
            assertThat(period.startDate).isNull()
            assertThat(period.contains(LocalDate.of(2026, 12, 1))).isFalse()
            assertThat(period.contains(LocalDate.of(2026, 12, 4))).isFalse()
        }

        @Test
        @DisplayName("마지막 날은 종료 시각에서 1나노초를 뺀 날짜다")
        fun `기간 날짜 - 종료 1나노초 전`() {
            val justAfterMidnight = eventBetween("2026-12-01T00:00", "2026-12-08T00:00:00.000000001")
            val oneNanoLong = eventBetween("2026-12-07T23:59:59.999999999", "2026-12-08T00:00")

            assertThat(justAfterMidnight.attendancePeriod().endDate).isEqualTo(LocalDate.of(2026, 12, 8))
            assertThat(oneNanoLong.attendancePeriod().startDate).isEqualTo(LocalDate.of(2026, 12, 7))
            assertThat(oneNanoLong.attendancePeriod().endDate).isEqualTo(LocalDate.of(2026, 12, 7))
        }

        @Test
        @DisplayName("기간 날짜는 양 끝을 포함한다")
        fun `기간 날짜 - 양 끝 포함`() {
            val period = eventBetween("2026-12-01T10:00", "2026-12-07T18:00").attendancePeriod()

            assertThat(period.contains(LocalDate.of(2026, 11, 30))).isFalse()
            assertThat(period.contains(LocalDate.of(2026, 12, 1))).isTrue()
            assertThat(period.contains(LocalDate.of(2026, 12, 7))).isTrue()
            assertThat(period.contains(LocalDate.of(2026, 12, 8))).isFalse()
        }

        @Test
        @DisplayName("기간을 고치면 기간 날짜도 새 기간으로 다시 계산한다(D13)")
        fun `기간 날짜 - 기간 수정 뒤 다시 계산`() {
            val event = eventBetween("2026-12-01T00:00", "2026-12-08T00:00")
            val content = EventContent("제목", null, null, at("2026-12-03T00:00"), at("2026-12-05T12:00"), null)

            event.update(content, null)

            assertThat(event.attendancePeriod().startDate).isEqualTo(LocalDate.of(2026, 12, 3))
            assertThat(event.attendancePeriod().endDate).isEqualTo(LocalDate.of(2026, 12, 5))
        }
    }

    @Nested
    @DisplayName("진행 상태(phase)")
    inner class PhaseAt {

        private val event = eventBetween("2026-12-01T10:00", "2026-12-07T18:00")

        @ParameterizedTest(name = "{displayName} [{0} → {1}]")
        @CsvSource(
            "2026-12-01T09:59:59.999999999, BEFORE",
            "2026-12-01T10:00, IN_PROGRESS",
            "2026-12-07T17:59:59.999999999, IN_PROGRESS",
            "2026-12-07T18:00, ENDED",
            "2026-12-08T00:00, ENDED"
        )
        @DisplayName("시작 시각은 진행 중에 포함하고 종료 시각은 포함하지 않는다")
        fun `진행 상태 - 시작 포함 종료 배타`(now: String, phase: EventPhase) {
            assertThat(event.phaseAt(at(now))).isEqualTo(phase)
        }

        @ParameterizedTest(name = "{displayName} [{0} → {1}]")
        @CsvSource("2026-12-01T09:59:59.999999999, BEFORE", "2026-12-01T10:00, ENDED", "2026-12-02T00:00, ENDED")
        @DisplayName("시작과 종료가 같으면 진행 중인 순간이 없다")
        fun `진행 상태 - 시작과 종료가 같음`(now: String, phase: EventPhase) {
            val instantEvent = eventBetween("2026-12-01T10:00", "2026-12-01T10:00")

            assertThat(instantEvent.phaseAt(at(now))).isEqualTo(phase)
        }

        @ParameterizedTest(name = "{displayName} [{0}]")
        @ValueSource(strings = ["2026-11-30T00:00", "2026-12-01T09:30", "2026-12-01T10:00", "2026-12-04T00:00"])
        @DisplayName("시작이 종료보다 늦게 저장된 기간에는 진행 중인 순간이 없다")
        fun `진행 상태 - 시작이 종료보다 늦음`(now: String) {
            val sameDay = eventBetween("2026-12-01T10:00", "2026-12-01T09:00")
            val acrossDays = eventBetween("2026-12-05T00:00", "2026-12-03T00:00")

            assertThat(sameDay.phaseAt(at(now))).isNotEqualTo(EventPhase.IN_PROGRESS)
            assertThat(acrossDays.phaseAt(at(now))).isNotEqualTo(EventPhase.IN_PROGRESS)
        }

        @Test
        @DisplayName("저장 status가 ACTIVE든 WINNER_SELECTED든 진행 상태는 기간과 시각으로만 정한다")
        fun `진행 상태 - 저장 status와 무관`() {
            val selected = eventBetween("2026-12-01T10:00", "2026-12-07T18:00")
            selected.updateStatus(EventStatusType.WINNER_SELECTED)

            assertThat(selected.phaseAt(at("2026-12-03T10:00"))).isEqualTo(EventPhase.IN_PROGRESS)
        }
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

    private fun at(value: String): LocalDateTime = LocalDateTime.parse(value)

    private fun createEvent(type: EventType, googleFormUrl: String?): Event {
        return Event.create(
            title = "테스트 이벤트",
            subTitle = null,
            description = "이벤트 설명",
            startedAt = STARTED_AT,
            expiredAt = EXPIRED_AT,
            thumbnailUrl = "https://example.com/thumbnail.jpg",
            type = type,
            googleFormUrl = googleFormUrl
        )
    }

    private fun contentOf(event: Event, googleFormUrl: String?, title: String = event.title): EventContent {
        return EventContent(
            title = title,
            subTitle = event.subTitle,
            description = event.description,
            startedAt = event.startedAt,
            expiredAt = event.expiredAt,
            googleFormUrl = googleFormUrl
        )
    }

    private fun assertInvalidTypeSetting(action: () -> Unit) {
        val exception = assertThrows<EventException> { action() }

        assertThat(exception.errorCode).isEqualTo(EventErrorCode.INVALID_EVENT_TYPE_SETTING)
    }

    private fun createEvent(): Event {
        return Event.create(
            title = "테스트 이벤트",
            subTitle = null,
            description = "이벤트 설명",
            startedAt = LocalDateTime.now().plusDays(1),
            expiredAt = LocalDateTime.now().plusDays(7),
            thumbnailUrl = "https://example.com/thumbnail.jpg",
            type = EventType.GENERAL,
            googleFormUrl = null
        )
    }

    private fun createEvent(expiredAt: LocalDateTime): Event {
        return Event.create(
            title = "테스트 이벤트",
            subTitle = null,
            description = "이벤트 설명",
            startedAt = LocalDateTime.now().minusDays(1),
            expiredAt = expiredAt,
            thumbnailUrl = "https://example.com/thumbnail.jpg",
            type = EventType.GENERAL,
            googleFormUrl = null
        )
    }

    companion object {
        private const val FORM_URL = "https://docs.google.com/forms/d/e/1FAIpQLSc_example/viewform"
        private const val OTHER_FORM_URL = "https://forms.gle/abc123"
        private const val EDIT_FORM_URL = "https://docs.google.com/forms/d/1AbC_example/edit"
        private val STARTED_AT = LocalDateTime.of(2026, 12, 1, 0, 0)
        private val EXPIRED_AT = LocalDateTime.of(2026, 12, 7, 23, 59, 59)
    }
}
