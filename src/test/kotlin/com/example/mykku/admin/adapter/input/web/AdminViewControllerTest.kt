package com.example.mykku.admin.adapter.input.web

import com.example.mykku.BaseControllerTest
import io.restassured.RestAssured
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.endsWith
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Admin 뷰 통합 테스트")
class AdminViewControllerTest : BaseControllerTest() {

    @Test
    @DisplayName("대시보드 페이지는 사이드바에서 대시보드 메뉴가 활성화된다")
    fun `index - 사이드바 대시보드 활성`() {
        val adminSessionId = getAdminSessionId()

        RestAssured.given()
            .sessionId(adminSessionId)
            .`when`()
            .get("/admin")
            .then()
            .statusCode(200)
            .contentType(containsString("text/html"))
            .body(containsString("class=\"nav-link active\" href=\"/admin\""))
            .body(containsString("href=\"/admin/board\""))
            .body(containsString("window.adminFetch"))
            .body(not(containsString("class=\"nav-link active\" href=\"/admin/event\"")))
    }

    @Test
    @DisplayName("이벤트 목록 페이지는 사이드바에서 이벤트 메뉴가 활성화된다")
    fun `event list - 사이드바 이벤트 활성`() {
        val adminSessionId = getAdminSessionId()

        RestAssured.given()
            .sessionId(adminSessionId)
            .`when`()
            .get("/admin/event")
            .then()
            .statusCode(200)
            .contentType(containsString("text/html"))
            .body(containsString("class=\"nav-link active\" href=\"/admin/event\""))
            .body(not(containsString("class=\"nav-link active\" href=\"/admin\"")))
    }

    @Test
    @DisplayName("잘못된 토큰으로 로그인하면 에러 표시와 함께 로그인 페이지로 돌아간다")
    fun `login - 토큰이 틀리면 로그인 페이지로 리다이렉트`() {
        RestAssured.given()
            .formParam("token", "wrong-token")
            .redirects().follow(false)
            .`when`()
            .post("/admin/api/login")
            .then()
            .statusCode(302)
            .header("Location", endsWith("/admin/login?error"))
    }

    @Test
    @DisplayName("로그인하면 세션 ID가 새 ID로 바뀌고 이전 ID는 더 이상 쓸 수 없다")
    fun `login - 세션 ID 교체`() {
        val previousSessionId = getAdminSessionId()

        val newSessionId = RestAssured.given()
            .sessionId(previousSessionId)
            .formParam("token", "test-admin-token")
            .redirects().follow(false)
            .`when`()
            .post("/admin/api/login")
            .then()
            .statusCode(302)
            .extract()
            .sessionId()

        assertThat(newSessionId).isNotNull().isNotEqualTo(previousSessionId)
        RestAssured.given()
            .sessionId(previousSessionId)
            .redirects().follow(false)
            .`when`()
            .get("/admin")
            .then()
            .statusCode(302)
    }

    @Test
    @DisplayName("세션 없이 뷰에 접근하면 로그인으로 리다이렉트된다")
    fun `index - 미인증이면 302`() {
        RestAssured.given()
            .redirects().follow(false)
            .`when`()
            .get("/admin")
            .then()
            .statusCode(302)
            .header("Location", endsWith("/admin/login"))
    }
}
