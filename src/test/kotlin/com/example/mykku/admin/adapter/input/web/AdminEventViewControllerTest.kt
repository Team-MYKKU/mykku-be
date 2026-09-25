package com.example.mykku.admin.adapter.input.web

import com.example.mykku.BaseControllerTest
import com.example.mykku.event.adapter.output.persistence.entity.EventJpaEntity
import com.example.mykku.event.adapter.output.persistence.repository.EventJpaRepository
import com.example.mykku.event.domain.vo.EventStatusType
import io.restassured.RestAssured
import io.restassured.http.ContentType
import io.restassured.response.ValidatableResponse
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.endsWith
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime

@DisplayName("AdminEventViewController 통합 테스트")
class AdminEventViewControllerTest : BaseControllerTest() {

    @Autowired
    private lateinit var eventJpaRepository: EventJpaRepository

    @Test
    @DisplayName("status를 생략하면 전체 탭이 활성화되고 모든 이벤트가 보인다")
    fun `listPage - 기본 ALL`() {
        val adminSessionId = getAdminSessionId()
        createAndSaveEvent(title = "종료된 이벤트", expiredAt = LocalDateTime.now().minusDays(1))
        createAndSaveEvent(title = "진행중 이벤트", expiredAt = LocalDateTime.now().plusDays(7))

        RestAssured.given()
            .sessionId(adminSessionId)
            .`when`()
            .get("/admin/event")
            .then()
            .statusCode(200)
            .body(containsString("종료된 이벤트"))
            .body(containsString("진행중 이벤트"))
            .body(containsString(activeTab("ALL")))
            .body(not(containsString(activeTab("ACTIVE"))))
            .body(containsString("class=\"badge badge-active\""))
            .body(containsString("class=\"badge badge-expired\""))
    }

    @Test
    @DisplayName("PENDING_SELECTION 필터는 종료됐고 선정하지 않은 이벤트만 보여 준다")
    fun `listPage - 선정 대기 필터`() {
        val adminSessionId = getAdminSessionId()
        val past = LocalDateTime.now().minusDays(1)
        createAndSaveEvent(title = "선정 대기 이벤트", expiredAt = past)
        createAndSaveEvent(title = "선정 완료 이벤트", expiredAt = past, status = EventStatusType.WINNER_SELECTED)
        createAndSaveEvent(title = "진행중 이벤트", expiredAt = LocalDateTime.now().plusDays(7))

        RestAssured.given()
            .sessionId(adminSessionId)
            .queryParam("status", "PENDING_SELECTION")
            .`when`()
            .get("/admin/event")
            .then()
            .statusCode(200)
            .body(containsString("선정 대기 이벤트"))
            .body(not(containsString("선정 완료 이벤트")))
            .body(not(containsString("진행중 이벤트")))
            .body(containsString(activeTab("PENDING_SELECTION")))
            .body(containsString("class=\"badge badge-expired\""))
    }

    @Test
    @DisplayName("WINNER_SELECTED 필터는 선정 완료 이벤트만 보여 준다")
    fun `listPage - 선정 완료 필터`() {
        val adminSessionId = getAdminSessionId()
        val past = LocalDateTime.now().minusDays(1)
        createAndSaveEvent(title = "선정 완료 이벤트", expiredAt = past, status = EventStatusType.WINNER_SELECTED)
        createAndSaveEvent(title = "선정 대기 이벤트", expiredAt = past)

        RestAssured.given()
            .sessionId(adminSessionId)
            .queryParam("status", "WINNER_SELECTED")
            .`when`()
            .get("/admin/event")
            .then()
            .statusCode(200)
            .body(containsString("선정 완료 이벤트"))
            .body(not(containsString("선정 대기 이벤트")))
            .body(containsString("class=\"badge badge-winner-selected\""))
            .body(not(containsString("class=\"badge badge-expired\"")))
    }

    @Test
    @DisplayName("ACTIVE 필터에서 시작 전 이벤트는 예정, 시작된 이벤트는 진행중 배지로 보인다")
    fun `listPage - 시작 전이면 예정 배지`() {
        val adminSessionId = getAdminSessionId()
        createAndSaveEvent(
            title = "예정 이벤트",
            startedAt = LocalDateTime.now().plusDays(1),
            expiredAt = LocalDateTime.now().plusDays(7)
        )
        createAndSaveEvent(title = "시작된 이벤트", expiredAt = LocalDateTime.now().plusDays(7))

        RestAssured.given()
            .sessionId(adminSessionId)
            .queryParam("status", "ACTIVE")
            .`when`()
            .get("/admin/event")
            .then()
            .statusCode(200)
            .body(containsString("class=\"badge badge-upcoming\">예정</span>"))
            .body(containsString("class=\"badge badge-active\">진행중</span>"))
    }

    @Test
    @DisplayName("페이지 링크는 현재 status를 유지한다")
    fun `listPage - 페이지 링크 status 유지`() {
        val adminSessionId = getAdminSessionId()
        repeat(21) { index ->
            createAndSaveEvent(title = "종료 이벤트 $index", expiredAt = LocalDateTime.now().minusDays(1))
        }

        RestAssured.given()
            .sessionId(adminSessionId)
            .queryParam("status", "PENDING_SELECTION")
            .`when`()
            .get("/admin/event")
            .then()
            .statusCode(200)
            .body(containsString("page=1&amp;status=PENDING_SELECTION"))
    }

    @Test
    @DisplayName("세션 없이 접근하면 로그인으로 리다이렉트된다")
    fun `listPage - 미인증이면 302`() {
        RestAssured.given()
            .redirects().follow(false)
            .`when`()
            .get("/admin/event")
            .then()
            .statusCode(302)
            .header("Location", endsWith("/admin/login"))
    }

    @Test
    @DisplayName("목록의 각 행에 당첨자 화면 링크가 있다")
    fun `listPage - 당첨자 링크`() {
        val adminSessionId = getAdminSessionId()
        val event = createAndSaveEvent(title = "링크 이벤트", expiredAt = LocalDateTime.now().minusDays(1))

        getPage(adminSessionId, "/admin/event")
            .statusCode(200)
            .body(containsString("href=\"/admin/event/${event.id}/winners\""))
    }

    @Test
    @DisplayName("당첨자 화면은 현재 당첨자 memberId를 줄마다 채우고, 공지가 없으면 빈 폼을 띄운다")
    fun `winnersPage - 현재 당첨자 미리 채움과 빈 공지 폼`() {
        val adminSessionId = getAdminSessionId()
        createAndSaveMember(memberId = "memberA", email = "a@example.com", socialId = "a1")
        createAndSaveMember(memberId = "memberB", email = "b@example.com", socialId = "b1")
        val event = createAndSaveEvent(title = "당첨 이벤트", expiredAt = LocalDateTime.now().minusDays(1))
        putWinners(adminSessionId, event.id!!, listOf("memberA", "memberB"))

        getPage(adminSessionId, "/admin/event/${event.id}/winners")
            .statusCode(200)
            .body(containsString("class=\"nav-link active\" href=\"/admin/event\""))
            .body(containsString("memberA\nmemberB</textarea>"))
            .body(containsString("id=\"announcement-empty\""))
            .body(not(containsString("id=\"withdrawn-winner-notice\"")))
            .body(not(containsString("id=\"not-selectable-notice\"")))
    }

    @Test
    @DisplayName("탈퇴한 당첨자는 입력칸에서 빠지고 유지된다는 안내를 띄운다")
    fun `winnersPage - 탈퇴 당첨자 안내`() {
        val adminSessionId = getAdminSessionId()
        createAndSaveMember(memberId = "memberA", email = "a@example.com", socialId = "a1")
        val memberB = createAndSaveMember(memberId = "memberB", email = "b@example.com", socialId = "b1")
        val event = createAndSaveEvent(title = "탈퇴 이벤트", expiredAt = LocalDateTime.now().minusDays(1))
        putWinners(adminSessionId, event.id!!, listOf("memberA", "memberB"))
        memberJpaRepository.deleteById(memberB.id)

        getPage(adminSessionId, "/admin/event/${event.id}/winners")
            .statusCode(200)
            .body(containsString(">memberA</textarea>"))
            .body(containsString("탈퇴한 당첨자 1명은"))
    }

    @Test
    @DisplayName("공지가 있으면 폼을 미리 채운다")
    fun `winnersPage - 공지 미리 채움`() {
        val adminSessionId = getAdminSessionId()
        val event = createAndSaveEvent(title = "공지 이벤트", expiredAt = LocalDateTime.now().minusDays(1))
        RestAssured.given()
            .sessionId(adminSessionId)
            .contentType(ContentType.JSON)
            .body(mapOf("title" to "당첨자 발표", "content" to "축하합니다", "announcedAt" to "2025-10-10"))
            .`when`()
            .put("/admin/api/v1/events/{eventId}/winner-announcement", event.id)
            .then()
            .statusCode(200)

        getPage(adminSessionId, "/admin/event/${event.id}/winners")
            .statusCode(200)
            .body(containsString("value=\"당첨자 발표\""))
            .body(containsString(">축하합니다</textarea>"))
            .body(containsString("value=\"2025-10-10\""))
            .body(not(containsString("id=\"announcement-empty\"")))
    }

    @Test
    @DisplayName("종료 전인 이벤트는 확인 버튼을 끄고 안내를 띄운다")
    fun `winnersPage - 종료 전이면 저장 불가 안내`() {
        val adminSessionId = getAdminSessionId()
        val event = createAndSaveEvent(title = "진행중 이벤트", expiredAt = LocalDateTime.now().plusDays(3))

        getPage(adminSessionId, "/admin/event/${event.id}/winners")
            .statusCode(200)
            .body(containsString("id=\"not-selectable-notice\""))
            .body(containsString("id=\"check-winners\" class=\"btn btn-outline-primary me-2\" disabled"))
    }

    @Test
    @DisplayName("세션 없이 당첨자 화면에 접근하면 로그인으로 리다이렉트된다")
    fun `winnersPage - 미인증이면 302`() {
        RestAssured.given()
            .redirects().follow(false)
            .`when`()
            .get("/admin/event/1/winners")
            .then()
            .statusCode(302)
            .header("Location", endsWith("/admin/login"))
    }

    private fun getPage(adminSessionId: String, path: String): ValidatableResponse {
        return RestAssured.given()
            .sessionId(adminSessionId)
            .`when`()
            .get(path)
            .then()
    }

    private fun putWinners(adminSessionId: String, eventId: Long, memberIds: List<String>) {
        RestAssured.given()
            .sessionId(adminSessionId)
            .contentType(ContentType.JSON)
            .body(mapOf("memberIds" to memberIds, "dryRun" to false))
            .`when`()
            .put("/admin/api/v1/events/{eventId}/winners", eventId)
            .then()
            .statusCode(200)
    }

    private fun activeTab(status: String): String {
        return "class=\"nav-link active\" href=\"/admin/event?status=$status\""
    }

    private fun createAndSaveEvent(
        title: String,
        startedAt: LocalDateTime = LocalDateTime.now().minusDays(10),
        expiredAt: LocalDateTime,
        status: EventStatusType = EventStatusType.ACTIVE
    ): EventJpaEntity {
        val event = EventJpaEntity(
            title = title,
            startedAt = startedAt,
            expiredAt = expiredAt,
            status = status,
            thumbnailUrl = "https://example.com/thumbnail.jpg"
        )
        return eventJpaRepository.save(event)
    }
}
