package com.example.mykku.member.adapter.input.web

import com.example.mykku.BaseControllerTest
import com.example.mykku.event.adapter.output.persistence.entity.EventJpaEntity
import com.example.mykku.event.adapter.output.persistence.entity.EventParticipationJpaEntity
import com.example.mykku.event.adapter.output.persistence.entity.EventWinnerJpaEntity
import com.example.mykku.event.adapter.output.persistence.repository.EventJpaRepository
import com.example.mykku.event.adapter.output.persistence.repository.EventParticipationJpaRepository
import com.example.mykku.event.adapter.output.persistence.repository.EventWinnerJpaRepository
import com.example.mykku.event.domain.vo.EventStatusType
import com.example.mykku.event.domain.vo.EventType
import com.example.mykku.member.adapter.output.persistence.entity.MemberJpaEntity
import com.example.mykku.util.ResponseMaps
import io.restassured.RestAssured
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.notNullValue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime

@DisplayName("MemberEventController 통합 테스트")
class MemberEventControllerTest : BaseControllerTest() {

    @Autowired
    private lateinit var eventJpaRepository: EventJpaRepository

    @Autowired
    private lateinit var eventParticipationJpaRepository: EventParticipationJpaRepository

    @Autowired
    private lateinit var eventWinnerJpaRepository: EventWinnerJpaRepository

    @Test
    @DisplayName("내가 참여한 이벤트 목록 조회 - 당첨된 이벤트는 winnerStatus가 WON이다")
    fun `getMyParticipatedEvents - 당첨된 이벤트는 winnerStatus가 WON이다`() {
        val member = createAndSaveMember()
        val authHeader = getBearerToken(member.id)

        val event = eventJpaRepository.save(
            EventJpaEntity(
                title = "당첨 이벤트",
                startedAt = LocalDateTime.now().minusDays(7),
                expiredAt = LocalDateTime.now().minusDays(1),
                status = EventStatusType.WINNER_SELECTED,
                thumbnailUrl = "https://example.com/thumbnail.jpg"
            )
        )
        val participation = eventParticipationJpaRepository.save(
            EventParticipationJpaEntity(member = member, event = event)
        )
        eventWinnerJpaRepository.save(EventWinnerJpaEntity(event = event, participation = participation))

        RestAssured.given()
            .header("Authorization", authHeader)
            .queryParam("page", 0)
            .queryParam("size", 20)
        .`when`()
            .get("/api/v1/members/me/events")
        .then()
            .statusCode(200)
            .body("data.content[0].id", equalTo(event.id!!.toInt()))
            .body("data.content[0].winnerStatus", equalTo("WON"))
    }

    @Test
    @DisplayName("내가 참여한 이벤트 목록 조회 - 발표 완료됐지만 미당첨이면 winnerStatus가 LOST이다")
    fun `getMyParticipatedEvents - 발표 완료 미당첨이면 winnerStatus가 LOST이다`() {
        val member = createAndSaveMember()
        val authHeader = getBearerToken(member.id)

        val event = eventJpaRepository.save(
            EventJpaEntity(
                title = "낙첨 이벤트",
                startedAt = LocalDateTime.now().minusDays(7),
                expiredAt = LocalDateTime.now().minusDays(1),
                status = EventStatusType.WINNER_SELECTED,
                thumbnailUrl = "https://example.com/thumbnail.jpg"
            )
        )
        eventParticipationJpaRepository.save(EventParticipationJpaEntity(member = member, event = event))

        RestAssured.given()
            .header("Authorization", authHeader)
            .queryParam("page", 0)
            .queryParam("size", 20)
        .`when`()
            .get("/api/v1/members/me/events")
        .then()
            .statusCode(200)
            .body("data.content[0].winnerStatus", equalTo("LOST"))
    }

    @Test
    @DisplayName("내가 참여한 이벤트 목록 조회 - 발표 전이면 winnerStatus가 PENDING이다")
    fun `getMyParticipatedEvents - 발표 전이면 winnerStatus가 PENDING이다`() {
        val member = createAndSaveMember()
        val authHeader = getBearerToken(member.id)

        val event = eventJpaRepository.save(
            EventJpaEntity(
                title = "진행중 이벤트",
                startedAt = LocalDateTime.now().minusDays(1),
                expiredAt = LocalDateTime.now().plusDays(7),
                status = EventStatusType.ACTIVE,
                thumbnailUrl = "https://example.com/thumbnail.jpg"
            )
        )
        eventParticipationJpaRepository.save(EventParticipationJpaEntity(member = member, event = event))

        RestAssured.given()
            .header("Authorization", authHeader)
            .queryParam("page", 0)
            .queryParam("size", 20)
        .`when`()
            .get("/api/v1/members/me/events")
        .then()
            .statusCode(200)
            .body("data.content[0].winnerStatus", equalTo("PENDING"))
    }

    @Test
    @DisplayName("내가 참여한 이벤트 목록 조회 - 정상 케이스")
    fun `getMyParticipatedEvents - 참여한 이벤트 목록을 조회한다`() {
        // given
        val member = createAndSaveMember()
        val authHeader = getBearerToken(member.id)

        val event1 = eventJpaRepository.save(
            EventJpaEntity(
                title = "이벤트1",
                startedAt = LocalDateTime.now(),
                expiredAt = LocalDateTime.now().plusDays(7),
                thumbnailUrl = "https://example.com/thumbnail.jpg"
            )
        )
        val event2 = eventJpaRepository.save(
            EventJpaEntity(
                title = "이벤트2",
                startedAt = LocalDateTime.now(),
                expiredAt = LocalDateTime.now().plusDays(7),
                thumbnailUrl = "https://example.com/thumbnail.jpg"
            )
        )

        eventParticipationJpaRepository.save(EventParticipationJpaEntity(member = member, event = event1))
        eventParticipationJpaRepository.save(EventParticipationJpaEntity(member = member, event = event2))

        // when & then
        RestAssured.given()
            .header("Authorization", authHeader)
            .queryParam("page", 0)
            .queryParam("size", 20)
        .`when`()
            .get("/api/v1/members/me/events")
        .then()
            .statusCode(200)
            .body("message", equalTo("참여한 이벤트 목록을 성공적으로 조회했습니다."))
            .body("data", notNullValue())
            .body("data.totalElements", equalTo(2))
    }

    @Test
    @DisplayName("내가 참여한 이벤트 목록 조회 - 참여한 이벤트가 없는 경우")
    fun `getMyParticipatedEvents - 참여한 이벤트가 없으면 빈 목록을 반환한다`() {
        // given
        val member = createAndSaveMember()
        val authHeader = getBearerToken(member.id)

        // when & then
        RestAssured.given()
            .header("Authorization", authHeader)
            .queryParam("page", 0)
            .queryParam("size", 20)
        .`when`()
            .get("/api/v1/members/me/events")
        .then()
            .statusCode(200)
            .body("message", equalTo("참여한 이벤트 목록을 성공적으로 조회했습니다."))
            .body("data", notNullValue())
            .body("data.totalElements", equalTo(0))
    }

    @Test
    @DisplayName("내가 참여한 이벤트 목록 조회 - 페이지네이션 동작 확인")
    fun `getMyParticipatedEvents - 페이지네이션이 정상 동작한다`() {
        // given
        val member = createAndSaveMember()
        val authHeader = getBearerToken(member.id)

        repeat(5) { index ->
            val event = eventJpaRepository.save(
                EventJpaEntity(
                    title = "이벤트${index + 1}",
                    startedAt = LocalDateTime.now(),
                    expiredAt = LocalDateTime.now().plusDays(7),
                    thumbnailUrl = "https://example.com/thumbnail.jpg"
                )
            )
            eventParticipationJpaRepository.save(EventParticipationJpaEntity(member = member, event = event))
        }

        // when & then
        RestAssured.given()
            .header("Authorization", authHeader)
            .queryParam("page", 0)
            .queryParam("size", 2)
        .`when`()
            .get("/api/v1/members/me/events")
        .then()
            .statusCode(200)
            .body("data.totalElements", equalTo(5))
            .body("data.totalPages", equalTo(3))
            .body("data.size", equalTo(2))
    }

    @Test
    @DisplayName("내가 참여한 이벤트 목록 조회 - 인증 없이 접근 시 401 에러")
    fun `getMyParticipatedEvents - 인증 없이 접근하면 401 에러가 발생한다`() {
        // when & then
        RestAssured.given()
            .queryParam("page", 0)
            .queryParam("size", 20)
        .`when`()
            .get("/api/v1/members/me/events")
        .then()
            .statusCode(401)
    }

    @ParameterizedTest(name = "{displayName} [{0}]")
    @EnumSource(EventType::class)
    @DisplayName("내가 참여한 이벤트 목록 조회 - 유형이 다른 이벤트도 같은 조건의 일반 이벤트와 같은 필드·값으로 응답한다")
    fun `getMyParticipatedEvents - 유형과 무관하게 같은 응답`(type: EventType) {
        val member = createAndSaveMember()
        val general = createAndSaveWonEvent(member, EventType.GENERAL)
        val typed = createAndSaveWonEvent(member, type)

        val content = getMyEventsContent(member)

        assertThat(content.map { it["id"] }).containsExactlyInAnyOrder(general.id!!.toInt(), typed.id!!.toInt())
        assertThat(content.map { ResponseMaps.masked(it, setOf("id")) }.distinct()).hasSize(1)
    }

    private fun getMyEventsContent(member: MemberJpaEntity): List<Map<String, Any?>> {
        return RestAssured.given()
            .header("Authorization", getBearerToken(member.id))
            .`when`()
            .get("/api/v1/members/me/events")
            .then()
            .statusCode(200)
            .extract()
            .jsonPath()
            .getList("data.content")
    }

    private fun createAndSaveWonEvent(member: MemberJpaEntity, type: EventType): EventJpaEntity {
        val event = createAndSaveTypedEvent(type)
        val participation = eventParticipationJpaRepository.save(
            EventParticipationJpaEntity(member = member, event = event)
        )
        eventWinnerJpaRepository.save(EventWinnerJpaEntity(event = event, participation = participation))
        return event
    }

    private fun createAndSaveTypedEvent(type: EventType): EventJpaEntity {
        return eventJpaRepository.save(
            EventJpaEntity(
                title = "유형 비교 이벤트",
                startedAt = LocalDateTime.of(2020, 1, 1, 0, 0),
                expiredAt = LocalDateTime.of(2020, 1, 31, 23, 59, 59),
                status = EventStatusType.WINNER_SELECTED,
                thumbnailUrl = "https://example.com/thumbnail.jpg",
                type = type,
                googleFormUrl = "https://forms.gle/abc123".takeIf { type == EventType.GOOGLE_FORM }
            )
        )
    }
}
