package com.example.mykku.event.adapter.input.web

import com.example.mykku.BaseControllerTest
import com.example.mykku.event.adapter.output.persistence.entity.EventJpaEntity
import com.example.mykku.event.adapter.output.persistence.entity.EventParticipationJpaEntity
import com.example.mykku.event.adapter.output.persistence.entity.EventWinnerAnnouncementJpaEntity
import com.example.mykku.event.adapter.output.persistence.entity.EventWinnerJpaEntity
import com.example.mykku.event.adapter.output.persistence.repository.EventJpaRepository
import com.example.mykku.event.adapter.output.persistence.repository.EventParticipationJpaRepository
import com.example.mykku.event.adapter.output.persistence.repository.EventWinnerAnnouncementJpaRepository
import com.example.mykku.event.adapter.output.persistence.repository.EventWinnerJpaRepository
import com.example.mykku.event.domain.vo.EventStatusType
import com.example.mykku.event.domain.vo.EventType
import com.example.mykku.member.adapter.output.persistence.entity.MemberJpaEntity
import com.example.mykku.util.ResponseMaps
import io.restassured.RestAssured
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.notNullValue
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("EventWinnerController 통합 테스트")
class EventWinnerControllerTest : BaseControllerTest() {

    @Autowired
    private lateinit var eventJpaRepository: EventJpaRepository

    @Autowired
    private lateinit var eventParticipationJpaRepository: EventParticipationJpaRepository

    @Autowired
    private lateinit var eventWinnerJpaRepository: EventWinnerJpaRepository

    @Autowired
    private lateinit var eventWinnerAnnouncementJpaRepository: EventWinnerAnnouncementJpaRepository

    @Test
    @DisplayName("이벤트 당첨자 목록 조회 - 정상 케이스")
    fun `getEventWinners - 정상적으로 당첨자 목록을 조회한다`() {
        val member = createAndSaveMember()
        val event = createAndSaveEvent(status = EventStatusType.WINNER_SELECTED)
        val participation = createAndSaveParticipation(member, event)
        createAndSaveWinner(event, participation)

        RestAssured.given()
            .`when`()
            .get("/api/v1/events/{eventId}/winners", event.id)
            .then()
            .statusCode(200)
            .body("message", equalTo("이벤트 당첨자 목록을 성공적으로 조회했습니다."))
            .body("data.eventId", equalTo(event.id!!.toInt()))
            .body("data.winners[0].memberId", equalTo(member.memberId))
    }

    @Test
    @DisplayName("이벤트 당첨자 목록 조회 - 당첨자가 없으면 빈 목록을 반환한다")
    fun `getEventWinners - 당첨자가 없으면 빈 목록을 반환한다`() {
        val event = createAndSaveEvent(status = EventStatusType.WINNER_SELECTED)

        RestAssured.given()
            .`when`()
            .get("/api/v1/events/{eventId}/winners", event.id)
            .then()
            .statusCode(200)
            .body("data.winners.size()", equalTo(0))
    }

    @Test
    @DisplayName("당첨 여부 조회 - 당첨자인 경우")
    fun `getMyWinnerStatus - 당첨자인 경우 true를 반환한다`() {
        val member = createAndSaveMember()
        val authHeader = getBearerToken(member.id)
        val event = createAndSaveEvent(status = EventStatusType.WINNER_SELECTED)
        val participation = createAndSaveParticipation(member, event)
        val winner = createAndSaveWinner(event, participation)

        RestAssured.given()
            .header("Authorization", authHeader)
            .`when`()
            .get("/api/v1/events/{eventId}/my-winner-status", event.id)
            .then()
            .statusCode(200)
            .body("message", equalTo("당첨 여부를 성공적으로 조회했습니다."))
            .body("data.isWinner", equalTo(true))
            .body("data.winnerId", equalTo(winner.id!!.toInt()))
    }

    @Test
    @DisplayName("당첨 여부 조회 - 당첨자가 아닌 경우")
    fun `getMyWinnerStatus - 당첨자가 아닌 경우 false를 반환한다`() {
        val winnerMember = createAndSaveMember(memberId = "winner")
        val nonWinner = createAndSaveMember(
            memberId = "nonwinner",
            nickname = "비당첨자",
            email = "nonwinner@example.com",
            socialId = "99999"
        )
        val authHeader = getBearerToken(nonWinner.id)
        val event = createAndSaveEvent(status = EventStatusType.WINNER_SELECTED)
        val participation = createAndSaveParticipation(winnerMember, event)
        createAndSaveWinner(event, participation)

        RestAssured.given()
            .header("Authorization", authHeader)
            .`when`()
            .get("/api/v1/events/{eventId}/my-winner-status", event.id)
            .then()
            .statusCode(200)
            .body("data.isWinner", equalTo(false))
            .body("data.winnerId", nullValue())
    }

    @Test
    @DisplayName("당첨 여부 조회 - 당첨자가 발표되지 않은 이벤트")
    fun `getMyWinnerStatus - 당첨자 미발표 이벤트는 예외가 발생한다`() {
        val member = createAndSaveMember()
        val authHeader = getBearerToken(member.id)
        val event = createAndSaveEvent(status = EventStatusType.ACTIVE)

        RestAssured.given()
            .header("Authorization", authHeader)
            .`when`()
            .get("/api/v1/events/{eventId}/my-winner-status", event.id)
            .then()
            .statusCode(400)
    }

    @Test
    @DisplayName("당첨 여부 조회 - 인증되지 않은 사용자")
    fun `getMyWinnerStatus - 인증되지 않은 사용자는 조회할 수 없다`() {
        val event = createAndSaveEvent(status = EventStatusType.WINNER_SELECTED)

        RestAssured.given()
            .`when`()
            .get("/api/v1/events/{eventId}/my-winner-status", event.id)
            .then()
            .statusCode(401)
    }

    @Test
    @DisplayName("내 당첨 이벤트 목록 조회 - 정상 케이스")
    fun `getMyAwardEvents - 정상적으로 내 당첨 이벤트 목록을 조회한다`() {
        val member = createAndSaveMember()
        val authHeader = getBearerToken(member.id)
        val event = createAndSaveEvent(status = EventStatusType.WINNER_SELECTED)
        val participation = createAndSaveParticipation(member, event)
        createAndSaveWinner(event, participation)

        RestAssured.given()
            .header("Authorization", authHeader)
            .`when`()
            .get("/api/v1/events/my-awards")
            .then()
            .statusCode(200)
            .body("message", equalTo("내 당첨 이벤트 목록을 성공적으로 조회했습니다."))
            .body("data.content[0].eventId", equalTo(event.id!!.toInt()))
    }

    @Test
    @DisplayName("내 당첨 이벤트 목록 조회 - 인증되지 않은 사용자")
    fun `getMyAwardEvents - 인증되지 않은 사용자는 조회할 수 없다`() {
        RestAssured.given()
            .`when`()
            .get("/api/v1/events/my-awards")
            .then()
            .statusCode(401)
    }

    @Test
    @DisplayName("당첨자 발표 공지 조회 - 정상 케이스")
    fun `getEventWinnerAnnouncement - 저장된 공지를 조회한다`() {
        val event = createAndSaveEvent(status = EventStatusType.WINNER_SELECTED)
        eventWinnerAnnouncementJpaRepository.save(
            EventWinnerAnnouncementJpaEntity(
                event = event,
                title = "[봄맞이 이벤트] 수상자 발표",
                content = "감사합니다.\n대상: OOO",
                announcedAt = LocalDate.of(2025, 10, 10)
            )
        )

        RestAssured.given()
            .`when`()
            .get("/api/v1/events/{eventId}/winner-announcement", event.id)
            .then()
            .statusCode(200)
            .body("message", equalTo("당첨자 발표 공지를 성공적으로 조회했습니다."))
            .body("data.eventId", equalTo(event.id!!.toInt()))
            .body("data.title", equalTo("[봄맞이 이벤트] 수상자 발표"))
            .body("data.content", equalTo("감사합니다.\n대상: OOO"))
            .body("data.announcedAt", equalTo("2025-10-10"))
    }

    @Test
    @DisplayName("당첨자 발표 공지 조회 - 공지 미작성 시 404")
    fun `getEventWinnerAnnouncement - 공지가 없으면 404를 반환한다`() {
        val event = createAndSaveEvent(status = EventStatusType.WINNER_SELECTED)

        RestAssured.given()
            .`when`()
            .get("/api/v1/events/{eventId}/winner-announcement", event.id)
            .then()
            .statusCode(404)
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(EventType::class)
    @DisplayName("유형이 다른 이벤트도 당첨자·발표 공지·내 당첨 여부·내 당첨 목록을 같은 조건의 일반 이벤트와 같은 필드·값으로 응답한다")
    fun `당첨 조회 - 유형과 무관하게 같은 응답`(type: EventType) {
        val member = createAndSaveMember()
        val authHeader = getBearerToken(member.id)
        val general = createAndSaveWonEvent(member, EventType.GENERAL)
        val typed = createAndSaveWonEvent(member, type)

        listOf("winners", "winner-announcement", "my-winner-status").forEach { path ->
            assertThat(getDataWithoutIds(authHeader, "/api/v1/events/${typed.id}/$path")).`as`(path)
                .isEqualTo(getDataWithoutIds(authHeader, "/api/v1/events/${general.id}/$path"))
        }
        val awards = getData(authHeader, "/api/v1/events/my-awards") as Map<*, *>
        val content = awards["content"] as List<*>
        assertThat(content.map { (it as Map<*, *>)["eventId"] })
            .containsExactlyInAnyOrder(general.id!!.toInt(), typed.id!!.toInt())
        assertThat(content.map { ResponseMaps.withoutKeys(it, ID_KEYS) }.distinct()).hasSize(1)
    }

    private fun createAndSaveWonEvent(member: MemberJpaEntity, type: EventType): EventJpaEntity {
        val event = eventJpaRepository.save(
            EventJpaEntity(
                title = "유형 비교 이벤트",
                description = "유형 비교 설명",
                startedAt = LocalDateTime.of(2020, 1, 1, 0, 0),
                expiredAt = LocalDateTime.of(2020, 1, 31, 23, 59, 59),
                status = EventStatusType.WINNER_SELECTED,
                thumbnailUrl = "https://example.com/thumbnail.jpg",
                type = type,
                googleFormUrl = "https://forms.gle/abc123".takeIf { type == EventType.GOOGLE_FORM }
            )
        )
        createAndSaveWinner(event, createAndSaveParticipation(member, event))
        eventWinnerAnnouncementJpaRepository.save(
            EventWinnerAnnouncementJpaEntity(
                event = event,
                title = "당첨자 발표",
                content = "축하합니다",
                announcedAt = LocalDate.of(2020, 2, 1)
            )
        )
        return event
    }

    private fun getDataWithoutIds(authHeader: String, path: String): Any? {
        return ResponseMaps.withoutKeys(getData(authHeader, path), ID_KEYS)
    }

    private fun getData(authHeader: String, path: String): Any? {
        return RestAssured.given()
            .header("Authorization", authHeader)
            .`when`()
            .get(path)
            .then()
            .statusCode(200)
            .extract()
            .jsonPath()
            .get<Any?>("data")
    }

    private fun createAndSaveEvent(
        title: String = "테스트 이벤트",
        status: EventStatusType = EventStatusType.WINNER_SELECTED
    ): EventJpaEntity {
        val event = EventJpaEntity(
            title = title,
            description = "테스트 이벤트 설명",
            startedAt = LocalDateTime.now().minusDays(30),
            expiredAt = LocalDateTime.now().minusDays(1),
            status = status,
            thumbnailUrl = "https://example.com/thumbnail.jpg"
        )
        return eventJpaRepository.save(event)
    }

    private fun createAndSaveParticipation(
        member: MemberJpaEntity,
        event: EventJpaEntity
    ): EventParticipationJpaEntity {
        return eventParticipationJpaRepository.save(
            EventParticipationJpaEntity(member = member, event = event)
        )
    }

    private fun createAndSaveWinner(
        event: EventJpaEntity,
        participation: EventParticipationJpaEntity
    ): EventWinnerJpaEntity {
        return eventWinnerJpaRepository.save(
            EventWinnerJpaEntity(event = event, participation = participation)
        )
    }

    companion object {
        private val ID_KEYS = setOf("eventId", "winnerId")
    }
}
