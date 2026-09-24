package com.example.mykku.admin.adapter.input.web

import com.example.mykku.BaseControllerTest
import com.example.mykku.event.adapter.output.persistence.entity.EventJpaEntity
import com.example.mykku.event.adapter.output.persistence.repository.EventJpaRepository
import com.example.mykku.event.domain.vo.EventStatusType
import io.restassured.RestAssured
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
