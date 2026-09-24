package com.example.mykku.admin.adapter.input.web

import com.example.mykku.BaseControllerTest
import com.example.mykku.contest.adapter.output.persistence.entity.ContestJpaEntity
import com.example.mykku.contest.adapter.output.persistence.repository.ContestJpaRepository
import com.example.mykku.contest.domain.vo.ContestStatusType
import io.restassured.RestAssured
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.endsWith
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime

@DisplayName("AdminContestViewController 통합 테스트")
class AdminContestViewControllerTest : BaseControllerTest() {

    @Autowired
    private lateinit var contestJpaRepository: ContestJpaRepository

    @Test
    @DisplayName("status를 생략하면 전체 탭이 활성화되고 모든 콘테스트가 보인다")
    fun `listPage - 기본 ALL`() {
        val adminSessionId = getAdminSessionId()
        createAndSaveContest(title = "종료된 콘테스트", expiredAt = LocalDateTime.now().minusDays(1))
        createAndSaveContest(title = "진행중 콘테스트", expiredAt = LocalDateTime.now().plusDays(7))

        RestAssured.given()
            .sessionId(adminSessionId)
            .`when`()
            .get("/admin/contest")
            .then()
            .statusCode(200)
            .body(containsString("종료된 콘테스트"))
            .body(containsString("진행중 콘테스트"))
            .body(containsString(activeTab("ALL")))
            .body(not(containsString(activeTab("ACTIVE"))))
            .body(containsString("class=\"badge badge-active\""))
            .body(containsString("class=\"badge badge-expired\""))
    }

    @Test
    @DisplayName("PENDING_SELECTION 필터는 종료됐고 선정하지 않은 콘테스트만 보여 준다")
    fun `listPage - 선정 대기 필터`() {
        val adminSessionId = getAdminSessionId()
        val past = LocalDateTime.now().minusDays(1)
        createAndSaveContest(title = "선정 대기 콘테스트", expiredAt = past)
        createAndSaveContest(title = "선정 완료 콘테스트", expiredAt = past, status = ContestStatusType.WINNER_SELECTED)
        createAndSaveContest(title = "진행중 콘테스트", expiredAt = LocalDateTime.now().plusDays(7))

        RestAssured.given()
            .sessionId(adminSessionId)
            .queryParam("status", "PENDING_SELECTION")
            .`when`()
            .get("/admin/contest")
            .then()
            .statusCode(200)
            .body(containsString("선정 대기 콘테스트"))
            .body(not(containsString("선정 완료 콘테스트")))
            .body(not(containsString("진행중 콘테스트")))
            .body(containsString(activeTab("PENDING_SELECTION")))
            .body(containsString("class=\"badge badge-expired\""))
    }

    @Test
    @DisplayName("WINNER_SELECTED 필터는 선정 완료 콘테스트만 보여 준다")
    fun `listPage - 선정 완료 필터`() {
        val adminSessionId = getAdminSessionId()
        val past = LocalDateTime.now().minusDays(1)
        createAndSaveContest(title = "선정 완료 콘테스트", expiredAt = past, status = ContestStatusType.WINNER_SELECTED)
        createAndSaveContest(title = "선정 대기 콘테스트", expiredAt = past)

        RestAssured.given()
            .sessionId(adminSessionId)
            .queryParam("status", "WINNER_SELECTED")
            .`when`()
            .get("/admin/contest")
            .then()
            .statusCode(200)
            .body(containsString("선정 완료 콘테스트"))
            .body(not(containsString("선정 대기 콘테스트")))
            .body(containsString("class=\"badge badge-winner-selected\""))
            .body(not(containsString("class=\"badge badge-expired\"")))
    }

    @Test
    @DisplayName("ACTIVE 필터에서 시작 전 콘테스트는 예정, 시작된 콘테스트는 진행중 배지로 보인다")
    fun `listPage - 시작 전이면 예정 배지`() {
        val adminSessionId = getAdminSessionId()
        createAndSaveContest(
            title = "예정 콘테스트",
            startedAt = LocalDateTime.now().plusDays(1),
            expiredAt = LocalDateTime.now().plusDays(7)
        )
        createAndSaveContest(title = "시작된 콘테스트", expiredAt = LocalDateTime.now().plusDays(7))

        RestAssured.given()
            .sessionId(adminSessionId)
            .queryParam("status", "ACTIVE")
            .`when`()
            .get("/admin/contest")
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
            createAndSaveContest(title = "종료 콘테스트 $index", expiredAt = LocalDateTime.now().minusDays(1))
        }

        RestAssured.given()
            .sessionId(adminSessionId)
            .queryParam("status", "PENDING_SELECTION")
            .`when`()
            .get("/admin/contest")
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
            .get("/admin/contest")
            .then()
            .statusCode(302)
            .header("Location", endsWith("/admin/login"))
    }

    private fun activeTab(status: String): String {
        return "class=\"nav-link active\" href=\"/admin/contest?status=$status\""
    }

    private fun createAndSaveContest(
        title: String,
        startedAt: LocalDateTime = LocalDateTime.now().minusDays(10),
        expiredAt: LocalDateTime,
        status: ContestStatusType = ContestStatusType.ACTIVE
    ): ContestJpaEntity {
        val contest = ContestJpaEntity(
            title = title,
            startedAt = startedAt,
            expiredAt = expiredAt,
            status = status,
            thumbnailUrl = "https://example.com/thumbnail.jpg"
        )
        return contestJpaRepository.save(contest)
    }
}
