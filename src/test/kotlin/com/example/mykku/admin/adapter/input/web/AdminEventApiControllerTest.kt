package com.example.mykku.admin.adapter.input.web

import com.example.mykku.BaseControllerTest
import com.example.mykku.admin.exception.AdminErrorCode
import com.example.mykku.common.exception.CommonErrorCode
import com.example.mykku.event.adapter.output.persistence.entity.EventJpaEntity
import com.example.mykku.event.adapter.output.persistence.entity.EventParticipationJpaEntity
import com.example.mykku.event.adapter.output.persistence.repository.EventJpaRepository
import com.example.mykku.event.adapter.output.persistence.repository.EventParticipationJpaRepository
import com.example.mykku.event.domain.vo.EventStatusType
import com.example.mykku.member.adapter.output.persistence.entity.MemberJpaEntity
import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.hamcrest.Matchers.containsInAnyOrder
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.notNullValue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime

@DisplayName("AdminEventApiController 통합 테스트")
class AdminEventApiControllerTest : BaseControllerTest() {

    @Autowired
    private lateinit var eventJpaRepository: EventJpaRepository

    @Autowired
    private lateinit var eventParticipationJpaRepository: EventParticipationJpaRepository

    @Test
    @DisplayName("이벤트 생성 - 부제목을 함께 저장한다")
    fun `create - 부제목과 함께 이벤트를 생성한다`() {
        val adminSessionId = getAdminSessionId()

        RestAssured.given()
            .sessionId(adminSessionId)
            .contentType("multipart/form-data")
            .multiPart("title", "New Event")
            .multiPart("subTitle", "Event Sub Title")
            .multiPart("description", "Event Description")
            .multiPart("startedAt", "2026-08-01T00:00:00")
            .multiPart("expiredAt", "2026-09-01T00:00:00")
            .multiPart("thumbnailImage", "thumbnail.jpg", ByteArray(10), "image/jpeg")
            .`when`()
            .post("/admin/api/v1/events")
            .then()
            .statusCode(200)
            .body("data.title", equalTo("New Event"))
            .body("data.subTitle", equalTo("Event Sub Title"))
    }

    @Test
    @DisplayName("이벤트 생성 - 부제목이 255자를 초과하면 실패한다")
    fun `create - 부제목 길이 초과 시 400을 반환한다`() {
        val adminSessionId = getAdminSessionId()

        RestAssured.given()
            .sessionId(adminSessionId)
            .contentType("multipart/form-data")
            .multiPart("title", "New Event")
            .multiPart("subTitle", "a".repeat(256))
            .multiPart("startedAt", "2026-08-01T00:00:00")
            .multiPart("expiredAt", "2026-09-01T00:00:00")
            .multiPart("thumbnailImage", "thumbnail.jpg", ByteArray(10), "image/jpeg")
            .`when`()
            .post("/admin/api/v1/events")
            .then()
            .statusCode(400)
            .body("code", equalTo(CommonErrorCode.INVALID_INPUT.code))
    }

    @Test
    @DisplayName("당첨자 선정 - 정상 케이스")
    fun `setWinners - 관리자가 정상적으로 당첨자를 선정한다`() {
        val adminSessionId = getAdminSessionId()
        val member = createAndSaveMember()
        val event = createAndSaveEvent(expiredAt = LocalDateTime.now().minusDays(1))
        val participation = createAndSaveParticipation(member, event)

        val request = mapOf("participationIds" to listOf(participation.id))

        RestAssured.given()
            .sessionId(adminSessionId)
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .post("/admin/api/v1/events/{eventId}/winners", event.id)
            .then()
            .statusCode(200)
            .body("message", equalTo("당첨자가 성공적으로 선정되었습니다."))
            .body("data.eventId", equalTo(event.id!!.toInt()))
            .body("data.winners", notNullValue())
    }

    @Test
    @DisplayName("당첨자 선정 - 기존 당첨자를 일부 유지한 채 다시 선정할 수 있다")
    fun `setWinners - 기존 당첨자를 포함해 재선정한다`() {
        val adminSessionId = getAdminSessionId()
        val memberA = createAndSaveMember(memberId = "memberA", email = "a@example.com", socialId = "a1")
        val memberB = createAndSaveMember(memberId = "memberB", email = "b@example.com", socialId = "b1")
        val memberC = createAndSaveMember(memberId = "memberC", email = "c@example.com", socialId = "c1")
        val event = createAndSaveEvent(expiredAt = LocalDateTime.now().minusDays(1))
        val participationA = createAndSaveParticipation(memberA, event)
        val participationB = createAndSaveParticipation(memberB, event)
        val participationC = createAndSaveParticipation(memberC, event)

        selectEventWinners(adminSessionId, event.id!!, listOf(participationA.id!!, participationB.id!!))

        RestAssured.given()
            .sessionId(adminSessionId)
            .contentType(ContentType.JSON)
            .body(mapOf("participationIds" to listOf(participationA.id, participationC.id)))
            .`when`()
            .post("/admin/api/v1/events/{eventId}/winners", event.id)
            .then()
            .statusCode(200)
            .body("data.winners.memberId", containsInAnyOrder("memberA", "memberC"))
    }

    @Test
    @DisplayName("당첨자 선정 - 관리자 인증 없이 접근 불가")
    fun `setWinners - 관리자 인증이 없으면 당첨자를 선정할 수 없다`() {
        val member = createAndSaveMember()
        val event = createAndSaveEvent(expiredAt = LocalDateTime.now().minusDays(1))
        val participation = createAndSaveParticipation(member, event)

        val request = mapOf("participationIds" to listOf(participation.id))

        RestAssured.given()
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .post("/admin/api/v1/events/{eventId}/winners", event.id)
            .then()
            .statusCode(401)
            .body("code", equalTo(AdminErrorCode.UNAUTHORIZED.code))
    }

    @Test
    @DisplayName("당첨자 선정 - 종료되지 않은 이벤트는 선정할 수 없다")
    fun `setWinners - 기간이 만료되지 않은 이벤트는 당첨자를 선정할 수 없다`() {
        val adminSessionId = getAdminSessionId()
        val member = createAndSaveMember()
        val event = createAndSaveEvent(expiredAt = LocalDateTime.now().plusDays(7))
        val participation = createAndSaveParticipation(member, event)

        val request = mapOf("participationIds" to listOf(participation.id))

        RestAssured.given()
            .sessionId(adminSessionId)
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .post("/admin/api/v1/events/{eventId}/winners", event.id)
            .then()
            .statusCode(400)
    }

    @Test
    @DisplayName("당첨자 발표 공지 저장 - 정상 케이스(신규 생성)")
    fun `upsertWinnerAnnouncement - 관리자가 공지를 신규 저장한다`() {
        val adminSessionId = getAdminSessionId()
        val event = createAndSaveEvent()

        val request = mapOf(
            "title" to "[봄맞이 이벤트] 수상자 발표",
            "content" to "참여해 주신 모든 분들께 감사드립니다.\n대상: OOO",
            "announcedAt" to "2025-10-10"
        )

        RestAssured.given()
            .sessionId(adminSessionId)
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .put("/admin/api/v1/events/{eventId}/winner-announcement", event.id)
            .then()
            .statusCode(200)
            .body("message", equalTo("당첨자 발표 공지가 성공적으로 저장되었습니다."))
            .body("data.eventId", equalTo(event.id!!.toInt()))
            .body("data.title", equalTo("[봄맞이 이벤트] 수상자 발표"))
            .body("data.announcedAt", equalTo("2025-10-10"))
    }

    @Test
    @DisplayName("당첨자 발표 공지 저장 - 재호출 시 수정(upsert)")
    fun `upsertWinnerAnnouncement - 재호출하면 기존 공지를 수정한다`() {
        val adminSessionId = getAdminSessionId()
        val event = createAndSaveEvent()

        RestAssured.given()
            .sessionId(adminSessionId)
            .contentType(ContentType.JSON)
            .body(mapOf("title" to "초안", "content" to "초안 본문", "announcedAt" to "2025-10-10"))
            .`when`()
            .put("/admin/api/v1/events/{eventId}/winner-announcement", event.id)
            .then()
            .statusCode(200)

        RestAssured.given()
            .sessionId(adminSessionId)
            .contentType(ContentType.JSON)
            .body(mapOf("title" to "수정본", "content" to "수정 본문", "announcedAt" to "2025-10-11"))
            .`when`()
            .put("/admin/api/v1/events/{eventId}/winner-announcement", event.id)
            .then()
            .statusCode(200)
            .body("data.title", equalTo("수정본"))
            .body("data.content", equalTo("수정 본문"))
            .body("data.announcedAt", equalTo("2025-10-11"))
    }

    @Test
    @DisplayName("당첨자 발표 공지 저장 - 관리자 인증 없이 접근 불가")
    fun `upsertWinnerAnnouncement - 관리자 인증이 없으면 저장할 수 없다`() {
        val event = createAndSaveEvent()

        val request = mapOf(
            "title" to "제목",
            "content" to "본문",
            "announcedAt" to "2025-10-10"
        )

        RestAssured.given()
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .put("/admin/api/v1/events/{eventId}/winner-announcement", event.id)
            .then()
            .statusCode(401)
            .body("code", equalTo(AdminErrorCode.UNAUTHORIZED.code))
    }

    private fun createAndSaveEvent(
        title: String = "테스트 이벤트",
        expiredAt: LocalDateTime = LocalDateTime.now().minusDays(1)
    ): EventJpaEntity {
        val event = EventJpaEntity(
            title = title,
            description = "테스트 이벤트 설명",
            startedAt = LocalDateTime.now().minusDays(30),
            expiredAt = expiredAt,
            status = EventStatusType.ACTIVE,
            thumbnailUrl = "https://example.com/thumbnail.jpg"
        )
        return eventJpaRepository.save(event)
    }

    private fun selectEventWinners(adminSessionId: String, eventId: Long, participationIds: List<Long>) {
        RestAssured.given()
            .sessionId(adminSessionId)
            .contentType(ContentType.JSON)
            .body(mapOf("participationIds" to participationIds))
            .`when`()
            .post("/admin/api/v1/events/{eventId}/winners", eventId)
            .then()
            .statusCode(200)
    }

    private fun createAndSaveParticipation(
        member: MemberJpaEntity,
        event: EventJpaEntity
    ): EventParticipationJpaEntity {
        return eventParticipationJpaRepository.save(
            EventParticipationJpaEntity(member = member, event = event)
        )
    }
}
