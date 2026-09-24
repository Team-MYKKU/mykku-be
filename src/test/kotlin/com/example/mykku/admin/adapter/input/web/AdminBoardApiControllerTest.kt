package com.example.mykku.admin.adapter.input.web

import com.example.mykku.BaseControllerTest
import com.example.mykku.admin.exception.AdminErrorCode
import io.restassured.RestAssured
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.notNullValue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("AdminBoardApiController 통합 테스트")
class AdminBoardApiControllerTest : BaseControllerTest() {

    private lateinit var adminSessionId: String

    @BeforeEach
    fun setUp() {
        adminSessionId = getAdminSessionId()
    }

    @Test
    @DisplayName("게시판 생성 - 정상 케이스")
    fun `create - 정상적으로 게시판을 생성한다`() {
        RestAssured.given()
            .sessionId(adminSessionId)
            .contentType("multipart/form-data")
            .multiPart("title", "FreeBoard")
            .multiPart("logo", "logo.png", "fake-image".toByteArray(), "image/png")
            .`when`()
            .post("/admin/api/v1/boards")
            .then()
            .statusCode(200)
            .body("message", equalTo("게시판이 생성되었습니다"))
            .body("data", notNullValue())
            .body("data.title", equalTo("FreeBoard"))
            .body("data.logo", notNullValue())

        val boards = boardJpaRepository.findAll()
        assertThat(boards).hasSize(1)
        assertThat(boards.first().title).isEqualTo("FreeBoard")
    }

    @Test
    @DisplayName("게시판 생성 - 관리자 세션 없이 요청 시 실패")
    fun `create - 관리자 세션 없이 요청하면 실패한다`() {
        RestAssured.given()
            .contentType("multipart/form-data")
            .multiPart("title", "자유게시판")
            .multiPart("logo", "logo.png", "fake-image".toByteArray(), "image/png")
            .`when`()
            .post("/admin/api/v1/boards")
            .then()
            .statusCode(401)
            .body("code", equalTo(AdminErrorCode.UNAUTHORIZED.code))
    }
}
