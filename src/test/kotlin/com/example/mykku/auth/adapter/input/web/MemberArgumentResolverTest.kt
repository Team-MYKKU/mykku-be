package com.example.mykku.auth.adapter.input.web

import com.example.mykku.BaseControllerTest
import com.example.mykku.auth.application.port.output.TokenProvider
import com.example.mykku.auth.exception.AuthErrorCode
import io.restassured.RestAssured
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@DisplayName("MemberArgumentResolver 통합 테스트")
class MemberArgumentResolverTest : BaseControllerTest() {

    @Autowired
    private lateinit var tokenProvider: TokenProvider

    @Test
    fun `리프레시 토큰을 Authorization 헤더로 보내면 401을 반환한다`() {
        val member = createAndSaveMember(memberId = "refreshasbearer", email = "refreshasbearer@example.com")
        val refreshToken = tokenProvider.generateRefreshToken(member.id)

        RestAssured.given()
            .header("Authorization", "Bearer $refreshToken")
            .`when`()
            .get("/api/v1/members/me")
            .then()
            .statusCode(401)
            .body("code", equalTo(AuthErrorCode.UNAUTHORIZED.code))
    }

    @Test
    fun `액세스 토큰을 Authorization 헤더로 보내면 200을 반환한다`() {
        val member = createAndSaveMember(memberId = "accessasbearer", email = "accessasbearer@example.com")
        val accessToken = tokenProvider.generateAccessToken(member.id, member.email)

        RestAssured.given()
            .header("Authorization", "Bearer $accessToken")
            .`when`()
            .get("/api/v1/members/me")
            .then()
            .statusCode(200)
            .body("data.memberId", equalTo(member.memberId))
    }
}
