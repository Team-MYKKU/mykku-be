package com.example.mykku.event.adapter.output.persistence

import com.example.mykku.BaseRepositoryTest
import com.example.mykku.event.adapter.output.persistence.entity.EventJpaEntity
import com.example.mykku.event.adapter.output.persistence.repository.EventJpaRepository
import com.example.mykku.event.application.port.output.EventRepository
import com.example.mykku.event.domain.entity.Event
import com.example.mykku.event.domain.vo.EventId
import com.example.mykku.event.domain.vo.EventListFilter
import com.example.mykku.event.domain.vo.EventSortType
import com.example.mykku.event.domain.vo.EventStatusType
import com.example.mykku.event.domain.vo.EventType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.jdbc.core.JdbcTemplate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@DisplayName("EventRepositoryAdapter 통합 테스트")
class EventRepositoryAdapterTest : BaseRepositoryTest() {

    @Autowired
    private lateinit var eventRepository: EventRepository

    @Autowired
    private lateinit var eventJpaRepository: EventJpaRepository

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Nested
    @DisplayName("save 메서드")
    inner class Save {

        @Test
        @DisplayName("이벤트를 저장하고 ID가 생성된다")
        fun `이벤트 저장 - 정상 케이스`() {
            val event = createEvent()

            val savedEvent = eventRepository.save(event)

            assertThat(savedEvent.id.value).isGreaterThan(0)
            assertThat(savedEvent.title).isEqualTo(event.title)
            assertThat(savedEvent.description).isEqualTo(event.description)
            assertThat(savedEvent.status).isEqualTo(EventStatusType.ACTIVE)
        }

        @Test
        @DisplayName("description이 null인 이벤트를 저장할 수 있다")
        fun `이벤트 저장 - description null`() {
            val event = Event.create(
                title = "테스트 이벤트",
                subTitle = null,
                description = null,
                startedAt = LocalDateTime.now(),
                expiredAt = LocalDateTime.now().plusDays(7),
                thumbnailUrl = "https://example.com/thumbnail.jpg",
                type = EventType.GENERAL,
                googleFormUrl = null
            )

            val savedEvent = eventRepository.save(event)

            assertThat(savedEvent.description).isNull()
        }

        @Test
        @DisplayName("기존 이벤트를 업데이트하면 createdAt이 보존되고 행이 추가되지 않는다")
        fun `이벤트 업데이트 - createdAt 보존`() {
            val rowsBefore = eventJpaRepository.count()
            val saved = eventRepository.save(createEvent())
            eventJpaRepository.flush()
            Thread.sleep(5)
            saved.updateStatus(EventStatusType.WINNER_SELECTED)

            val updated = eventRepository.save(saved)
            flushAndClear()

            assertThat(updated.id).isEqualTo(saved.id)
            assertThat(updated.status).isEqualTo(EventStatusType.WINNER_SELECTED)
            assertThat(updated.createdAt).isEqualTo(saved.createdAt)
            assertThat(eventJpaRepository.count()).isEqualTo(rowsBefore + 1)
            assertPersistedUpdate(saved.id, saved.createdAt)
        }

        private fun assertPersistedUpdate(id: EventId, originalCreatedAt: LocalDateTime) {
            val persisted = eventRepository.findById(id)!!
            assertThat(persisted.status).isEqualTo(EventStatusType.WINNER_SELECTED)
            assertThat(persisted.createdAt).isCloseTo(originalCreatedAt, within(1, ChronoUnit.MICROS))
        }
    }

    @Nested
    @DisplayName("findById 메서드")
    inner class FindById {

        @Test
        @DisplayName("존재하는 이벤트를 ID로 조회한다")
        fun `이벤트 조회 - 정상 케이스`() {
            val savedEvent = eventRepository.save(createEvent())

            val foundEvent = eventRepository.findById(savedEvent.id)

            assertThat(foundEvent).isNotNull
            assertThat(foundEvent!!.id).isEqualTo(savedEvent.id)
            assertThat(foundEvent.title).isEqualTo(savedEvent.title)
        }

        @Test
        @DisplayName("존재하지 않는 ID로 조회하면 null을 반환한다")
        fun `이벤트 조회 - 존재하지 않는 ID`() {
            val nonExistentId = EventId(999999L)

            val foundEvent = eventRepository.findById(nonExistentId)

            assertThat(foundEvent).isNull()
        }
    }

    @Nested
    @DisplayName("findByExpiredAtAfter 메서드")
    inner class FindByExpiredAtAfter {

        @Test
        @DisplayName("만료일이 지정된 시간 이후인 이벤트들을 조회한다")
        fun `만료일 이후 이벤트 조회 - 정상 케이스`() {
            val now = LocalDateTime.now()
            val activeEvent = createEventWithExpiredAt(now.plusDays(7))
            val expiredEvent = createEventWithExpiredAt(now.minusDays(1))
            val savedActiveEvent = eventRepository.save(activeEvent)
            eventRepository.save(expiredEvent)

            val events = eventRepository.findByExpiredAtAfter(now)

            assertThat(events.map { it.id }).containsExactly(savedActiveEvent.id)
            assertThat(events[0].expiredAt).isAfter(now)
        }

        @Test
        @DisplayName("만료일이 지정된 시간 이후인 이벤트가 없으면 빈 리스트를 반환한다")
        fun `만료일 이후 이벤트 조회 - 결과 없음`() {
            val now = LocalDateTime.now()
            val expiredEvent = createEventWithExpiredAt(now.minusDays(1))
            eventRepository.save(expiredEvent)

            val events = eventRepository.findByExpiredAtAfter(now)

            assertThat(events).isEmpty()
        }

        @Test
        @DisplayName("여러 개의 활성 이벤트를 조회한다")
        fun `만료일 이후 이벤트 조회 - 여러 개`() {
            val now = LocalDateTime.now()
            val event1 = eventRepository.save(createEventWithExpiredAt(now.plusDays(1)))
            val event2 = eventRepository.save(createEventWithExpiredAt(now.plusDays(3)))
            val event3 = eventRepository.save(createEventWithExpiredAt(now.plusDays(7)))

            val events = eventRepository.findByExpiredAtAfter(now)

            assertThat(events.map { it.id }).containsExactlyInAnyOrder(event1.id, event2.id, event3.id)
        }
    }

    @Nested
    @DisplayName("findWithPagination 메서드")
    inner class FindWithPagination {

        @Nested
        @DisplayName("ACTIVE 상태 조회")
        inner class ActiveStatus {

            @Test
            @DisplayName("LATEST 정렬로 활성 이벤트를 조회한다")
            fun `활성 이벤트 조회 - LATEST 정렬`() {
                val now = LocalDateTime.now()
                val event1 = eventRepository.save(createEventWithExpiredAt(now.plusDays(7)))
                Thread.sleep(10)
                val event2 = eventRepository.save(createEventWithExpiredAt(now.plusDays(7)))
                val pageable = PageRequest.of(0, 10)

                val page = eventRepository.findWithPagination(
                    filter = EventListFilter.ACTIVE,
                    sortType = EventSortType.LATEST,
                    pageable = pageable,
                    currentTime = now
                )

                assertThat(page.content.map { it.id }).containsExactlyInAnyOrder(event1.id, event2.id)
                assertThat(page.content[0].id).isEqualTo(event2.id)
            }

            @Test
            @DisplayName("OLDEST 정렬로 활성 이벤트를 조회한다")
            fun `활성 이벤트 조회 - OLDEST 정렬`() {
                val now = LocalDateTime.now()
                val event1 = eventRepository.save(createEventWithExpiredAt(now.plusDays(7)))
                Thread.sleep(10)
                val event2 = eventRepository.save(createEventWithExpiredAt(now.plusDays(7)))
                val pageable = PageRequest.of(0, 10)

                val page = eventRepository.findWithPagination(
                    filter = EventListFilter.ACTIVE,
                    sortType = EventSortType.OLDEST,
                    pageable = pageable,
                    currentTime = now
                )

                assertThat(page.content.map { it.id }).containsExactlyInAnyOrder(event1.id, event2.id)
                assertThat(page.content[0].id).isEqualTo(event1.id)
            }

            @Test
            @DisplayName("POPULAR 정렬로 활성 이벤트를 조회한다")
            fun `활성 이벤트 조회 - POPULAR 정렬`() {
                val now = LocalDateTime.now()
                val event1 = eventRepository.save(createEventWithExpiredAt(now.plusDays(7)))
                Thread.sleep(10)
                val event2 = eventRepository.save(createEventWithExpiredAt(now.plusDays(7)))
                val pageable = PageRequest.of(0, 10)

                val page = eventRepository.findWithPagination(
                    filter = EventListFilter.ACTIVE,
                    sortType = EventSortType.POPULAR,
                    pageable = pageable,
                    currentTime = now
                )

                assertThat(page.content.map { it.id }).containsExactlyInAnyOrder(event1.id, event2.id)
                assertThat(page.content[0].id).isEqualTo(event2.id)
            }
        }

        @Nested
        @DisplayName("EXPIRED 상태 조회")
        inner class ExpiredStatus {

            @Test
            @DisplayName("만료된 이벤트를 조회한다")
            fun `만료된 이벤트 조회`() {
                val now = LocalDateTime.now()
                eventRepository.save(createEventWithExpiredAt(now.plusDays(7)))
                val expiredEvent = eventRepository.save(createEventWithExpiredAt(now.minusDays(1)))
                val pageable = PageRequest.of(0, 10)

                val page = eventRepository.findWithPagination(
                    filter = EventListFilter.EXPIRED,
                    sortType = EventSortType.LATEST,
                    pageable = pageable,
                    currentTime = now
                )

                assertThat(page.content.map { it.id }).containsExactly(expiredEvent.id)
                assertThat(page.content[0].expiredAt).isBefore(now)
            }
        }

        @Nested
        @DisplayName("ALL 상태 조회")
        inner class AllStatus {

            @Test
            @DisplayName("모든 이벤트를 조회한다")
            fun `모든 이벤트 조회`() {
                val now = LocalDateTime.now()
                val activeEvent = eventRepository.save(createEventWithExpiredAt(now.plusDays(7)))
                val expiredEvent = eventRepository.save(createEventWithExpiredAt(now.minusDays(1)))
                val pageable = PageRequest.of(0, 10)

                val page = eventRepository.findWithPagination(
                    filter = EventListFilter.ALL,
                    sortType = EventSortType.LATEST,
                    pageable = pageable,
                    currentTime = now
                )

                assertThat(page.content.map { it.id }).containsExactlyInAnyOrder(activeEvent.id, expiredEvent.id)
            }
        }

        @Nested
        @DisplayName("WINNER_SELECTED 상태 조회")
        inner class WinnerSelectedStatus {

            @Test
            @DisplayName("저장 상태가 WINNER_SELECTED인 이벤트만 조회한다")
            fun `수상자 선정 완료 이벤트만 조회`() {
                val now = LocalDateTime.now()
                val selected = eventRepository.save(createEventWithExpiredAt(now.minusDays(1)))
                selected.updateStatus(EventStatusType.WINNER_SELECTED)
                eventRepository.save(selected)
                eventRepository.save(createEventWithExpiredAt(now.minusDays(1)))

                val page = eventRepository.findWithPagination(
                    filter = EventListFilter.WINNER_SELECTED,
                    sortType = EventSortType.LATEST,
                    pageable = PageRequest.of(0, 10),
                    currentTime = now
                )

                assertThat(page.content.map { it.id }).containsExactly(selected.id)
            }

            @Test
            @DisplayName("PENDING_SELECTION은 만료됐고 당첨자를 선정하지 않은 이벤트만 조회한다")
            fun `당첨자 선정 대기 이벤트만 조회`() {
                val now = LocalDateTime.now()
                val selected = eventRepository.save(createEventWithExpiredAt(now.minusDays(1)))
                selected.updateStatus(EventStatusType.WINNER_SELECTED)
                eventRepository.save(selected)
                val pending = eventRepository.save(createEventWithExpiredAt(now.minusDays(1)))
                eventRepository.save(createEventWithExpiredAt(now.plusDays(7)))

                val page = eventRepository.findWithPagination(
                    filter = EventListFilter.PENDING_SELECTION,
                    sortType = EventSortType.LATEST,
                    pageable = PageRequest.of(0, 10),
                    currentTime = now
                )

                assertThat(page.content.map { it.id }).containsExactly(pending.id)
            }
        }

        @Nested
        @DisplayName("페이징 기능")
        inner class Paging {

            @Test
            @DisplayName("페이지 크기만큼 조회한다")
            fun `페이징 - 페이지 크기`() {
                val now = LocalDateTime.now()
                repeat(5) {
                    eventRepository.save(createEventWithExpiredAt(now.plusDays(7)))
                }
                val pageable = PageRequest.of(0, 3)

                val page = eventRepository.findWithPagination(
                    filter = EventListFilter.ALL,
                    sortType = EventSortType.LATEST,
                    pageable = pageable,
                    currentTime = now
                )

                assertThat(page.content).hasSize(3)
                assertThat(page.totalElements).isEqualTo(5)
                assertThat(page.totalPages).isEqualTo(2)
            }

            @Test
            @DisplayName("두 번째 페이지를 조회한다")
            fun `페이징 - 두 번째 페이지`() {
                val now = LocalDateTime.now()
                val savedIds = List(5) {
                    eventRepository.save(createEventWithExpiredAt(now.plusDays(7))).id
                }

                val firstPage = findAllSortedByLatest(PageRequest.of(0, 3), now)
                val secondPage = findAllSortedByLatest(PageRequest.of(1, 3), now)

                assertThat(firstPage.content).hasSize(3)
                assertThat((firstPage.content + secondPage.content).map { it.id })
                    .containsExactlyInAnyOrderElementsOf(savedIds)
                assertThat(secondPage.number).isEqualTo(1)
            }

            private fun findAllSortedByLatest(pageable: Pageable, currentTime: LocalDateTime): Page<Event> {
                return eventRepository.findWithPagination(
                    filter = EventListFilter.ALL,
                    sortType = EventSortType.LATEST,
                    pageable = pageable,
                    currentTime = currentTime
                )
            }
        }
    }

    @Nested
    @DisplayName("유형과 구글폼 링크")
    inner class TypeAndGoogleFormUrl {

        @Test
        @DisplayName("유형과 구글폼 링크를 저장하고 영속성 컨텍스트를 비운 뒤 다시 읽는다")
        fun `유형 저장 - 다시 읽기`() {
            val googleForm = eventRepository.save(createTypedEvent(EventType.GOOGLE_FORM, FORM_URL))
            val attendance = eventRepository.save(createTypedEvent(EventType.ATTENDANCE, null))
            flushAndClear()

            val foundGoogleForm = eventRepository.findById(googleForm.id)!!
            val foundAttendance = eventRepository.findById(attendance.id)!!

            assertThat(foundGoogleForm.type).isEqualTo(EventType.GOOGLE_FORM)
            assertThat(foundGoogleForm.googleFormUrl).isEqualTo(FORM_URL)
            assertThat(foundAttendance.type).isEqualTo(EventType.ATTENDANCE)
            assertThat(foundAttendance.googleFormUrl).isNull()
        }

        @Test
        @DisplayName("type 컬럼 없이 넣은 기존 형태의 행은 DB 기본값으로 GENERAL, 링크 없음이 된다")
        fun `유형 저장 - DB 기본값`() {
            val id = insertLegacyEventRow()

            val found = eventRepository.findById(id)!!

            assertThat(found.type).isEqualTo(EventType.GENERAL)
            assertThat(found.googleFormUrl).isNull()
            assertThat(storedTypeColumns(id)).isEqualTo("GENERAL" to null)
        }

        @Test
        @DisplayName("유형 설정 규칙에 맞지 않는 저장값도 검사 없이 그대로 읽는다")
        fun `유형 저장 - 규칙 밖 저장값`() {
            val withoutUrl = insertTypedEventRow("GOOGLE_FORM", null)
            val strayUrl = insertTypedEventRow("GENERAL", "http://example.com/not-a-form")

            val foundWithoutUrl = eventRepository.findById(withoutUrl)!!
            val foundStrayUrl = eventRepository.findById(strayUrl)!!

            assertThat(foundWithoutUrl.type).isEqualTo(EventType.GOOGLE_FORM)
            assertThat(foundWithoutUrl.googleFormUrl).isNull()
            assertThat(foundStrayUrl.type).isEqualTo(EventType.GENERAL)
            assertThat(foundStrayUrl.googleFormUrl).isEqualTo("http://example.com/not-a-form")
        }

        @Test
        @DisplayName("기존 이벤트를 다시 저장하면 구글폼 링크만 반영하고 저장된 유형은 바꾸지 않는다")
        fun `유형 저장 - 수정 저장은 유형을 바꾸지 않는다`() {
            val saved = eventRepository.save(createTypedEvent(EventType.GENERAL, null))
            flushAndClear()

            eventRepository.save(reconstituteWithType(saved, EventType.GOOGLE_FORM, FORM_URL))
            flushAndClear()

            assertThat(storedTypeColumns(saved.id)).isEqualTo("GENERAL" to FORM_URL)
            assertThat(eventRepository.findById(saved.id)!!.type).isEqualTo(EventType.GENERAL)
        }

        @Test
        @DisplayName("findTypesByIds는 있는 이벤트의 유형만 id별로 돌려준다")
        fun `유형 일괄 조회`() {
            val saved = EventType.entries.associateWith { type ->
                eventRepository.save(createTypedEvent(type, FORM_URL.takeIf { type == EventType.GOOGLE_FORM })).id
            }
            eventRepository.save(createTypedEvent(EventType.ATTENDANCE, null))

            val types = eventRepository.findTypesByIds(saved.values.toList() + EventId(999999L))

            assertThat(types).isEqualTo(saved.entries.associate { (type, id) -> id.value to type })
        }

        @Test
        @DisplayName("findTypesByIds에 빈 목록을 주면 빈 맵을 돌려준다")
        fun `유형 일괄 조회 - 빈 목록`() {
            eventRepository.save(createTypedEvent(EventType.ATTENDANCE, null))

            assertThat(eventRepository.findTypesByIds(emptyList())).isEmpty()
        }

        private fun reconstituteWithType(event: Event, type: EventType, googleFormUrl: String?): Event {
            return Event.reconstitute(
                id = event.id,
                title = event.title,
                subTitle = event.subTitle,
                description = event.description,
                startedAt = event.startedAt,
                expiredAt = event.expiredAt,
                thumbnailUrl = event.thumbnailUrl,
                type = type,
                googleFormUrl = googleFormUrl,
                status = event.status,
                createdAt = event.createdAt,
                updatedAt = event.updatedAt
            )
        }

        private fun insertLegacyEventRow(): EventId {
            jdbcTemplate.update(
                "INSERT INTO event (title, started_at, expired_at, status, thumbnail_url, created_at, updated_at) " +
                    "VALUES ('기존 형태 이벤트', '2026-12-01 00:00:00', '2026-12-07 23:59:59', 'ACTIVE', ?, " +
                    "NOW(6), NOW(6))",
                THUMBNAIL_URL
            )
            return EventId(lastInsertId())
        }

        private fun insertTypedEventRow(type: String, googleFormUrl: String?): EventId {
            jdbcTemplate.update(
                "INSERT INTO event (title, started_at, expired_at, status, thumbnail_url, created_at, updated_at, " +
                    "type, google_form_url) VALUES ('저장값 이벤트', '2026-12-01 00:00:00', '2026-12-07 23:59:59', " +
                    "'ACTIVE', ?, NOW(6), NOW(6), ?, ?)",
                THUMBNAIL_URL,
                type,
                googleFormUrl
            )
            return EventId(lastInsertId())
        }

        private fun lastInsertId(): Long {
            return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long::class.java)!!
        }

        private fun storedTypeColumns(id: EventId): Pair<String, String?> {
            return jdbcTemplate.queryForObject(
                "SELECT type, google_form_url FROM event WHERE id = ?",
                { rs, _ -> rs.getString("type") to rs.getString("google_form_url") },
                id.value
            )!!
        }
    }

    private fun createEvent(): Event {
        return Event.create(
            title = "테스트 이벤트",
            subTitle = null,
            description = "테스트 이벤트 설명",
            startedAt = LocalDateTime.now(),
            expiredAt = LocalDateTime.now().plusDays(7),
            thumbnailUrl = "https://example.com/thumbnail.jpg",
            type = EventType.GENERAL,
            googleFormUrl = null
        )
    }

    private fun createEventWithExpiredAt(expiredAt: LocalDateTime): Event {
        return Event.create(
            title = "테스트 이벤트",
            subTitle = null,
            description = "테스트 이벤트 설명",
            startedAt = LocalDateTime.now().minusDays(1),
            expiredAt = expiredAt,
            thumbnailUrl = "https://example.com/thumbnail.jpg",
            type = EventType.GENERAL,
            googleFormUrl = null
        )
    }

    private fun createTypedEvent(type: EventType, googleFormUrl: String?): Event {
        return Event.create(
            title = "유형 이벤트",
            subTitle = null,
            description = null,
            startedAt = LocalDateTime.of(2026, 12, 1, 0, 0),
            expiredAt = LocalDateTime.of(2026, 12, 7, 23, 59, 59),
            thumbnailUrl = THUMBNAIL_URL,
            type = type,
            googleFormUrl = googleFormUrl
        )
    }

    companion object {
        private const val THUMBNAIL_URL = "https://example.com/thumbnail.jpg"
        private const val FORM_URL = "https://docs.google.com/forms/d/e/1FAIpQLSc_example/viewform"
    }
}
