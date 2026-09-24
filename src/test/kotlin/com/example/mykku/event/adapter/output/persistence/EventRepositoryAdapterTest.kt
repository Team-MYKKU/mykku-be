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
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import java.time.LocalDateTime

@DisplayName("EventRepositoryAdapter 통합 테스트")
class EventRepositoryAdapterTest : BaseRepositoryTest() {

    @Autowired
    private lateinit var eventRepository: EventRepository

    @Autowired
    private lateinit var eventJpaRepository: EventJpaRepository

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
                thumbnailUrl = "https://example.com/thumbnail.jpg"
            )

            val savedEvent = eventRepository.save(event)

            assertThat(savedEvent.description).isNull()
        }

        @Test
        @DisplayName("기존 이벤트를 업데이트하면 createdAt이 보존되고 행이 추가되지 않는다")
        fun `이벤트 업데이트 - createdAt 보존`() {
            val saved = eventRepository.save(createEvent())
            eventJpaRepository.flush()
            Thread.sleep(5)
            saved.updateStatus(EventStatusType.WINNER_SELECTED)

            val updated = eventRepository.save(saved)

            assertThat(updated.id).isEqualTo(saved.id)
            assertThat(updated.status).isEqualTo(EventStatusType.WINNER_SELECTED)
            assertThat(updated.createdAt).isEqualTo(saved.createdAt)
            assertThat(eventJpaRepository.count()).isEqualTo(1)
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
            eventRepository.save(activeEvent)
            eventRepository.save(expiredEvent)

            val events = eventRepository.findByExpiredAtAfter(now)

            assertThat(events).hasSize(1)
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
            eventRepository.save(createEventWithExpiredAt(now.plusDays(1)))
            eventRepository.save(createEventWithExpiredAt(now.plusDays(3)))
            eventRepository.save(createEventWithExpiredAt(now.plusDays(7)))

            val events = eventRepository.findByExpiredAtAfter(now)

            assertThat(events).hasSize(3)
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

                assertThat(page.content).hasSize(2)
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

                assertThat(page.content).hasSize(2)
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

                assertThat(page.content).hasSize(2)
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
                eventRepository.save(createEventWithExpiredAt(now.minusDays(1)))
                val pageable = PageRequest.of(0, 10)

                val page = eventRepository.findWithPagination(
                    filter = EventListFilter.EXPIRED,
                    sortType = EventSortType.LATEST,
                    pageable = pageable,
                    currentTime = now
                )

                assertThat(page.content).hasSize(1)
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
                eventRepository.save(createEventWithExpiredAt(now.plusDays(7)))
                eventRepository.save(createEventWithExpiredAt(now.minusDays(1)))
                val pageable = PageRequest.of(0, 10)

                val page = eventRepository.findWithPagination(
                    filter = EventListFilter.ALL,
                    sortType = EventSortType.LATEST,
                    pageable = pageable,
                    currentTime = now
                )

                assertThat(page.content).hasSize(2)
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

                assertThat(page.content).hasSize(1)
                assertThat(page.content[0].id).isEqualTo(selected.id)
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

                assertThat(page.content).hasSize(1)
                assertThat(page.content[0].id).isEqualTo(pending.id)
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
                repeat(5) {
                    eventRepository.save(createEventWithExpiredAt(now.plusDays(7)))
                }
                val pageable = PageRequest.of(1, 3)

                val page = eventRepository.findWithPagination(
                    filter = EventListFilter.ALL,
                    sortType = EventSortType.LATEST,
                    pageable = pageable,
                    currentTime = now
                )

                assertThat(page.content).hasSize(2)
                assertThat(page.number).isEqualTo(1)
            }
        }
    }

    private fun createEvent(): Event {
        return Event.create(
            title = "테스트 이벤트",
            subTitle = null,
            description = "테스트 이벤트 설명",
            startedAt = LocalDateTime.now(),
            expiredAt = LocalDateTime.now().plusDays(7),
            thumbnailUrl = "https://example.com/thumbnail.jpg"
        )
    }

    private fun createEventWithExpiredAt(expiredAt: LocalDateTime): Event {
        return Event.create(
            title = "테스트 이벤트",
            subTitle = null,
            description = "테스트 이벤트 설명",
            startedAt = LocalDateTime.now().minusDays(1),
            expiredAt = expiredAt,
            thumbnailUrl = "https://example.com/thumbnail.jpg"
        )
    }

}
