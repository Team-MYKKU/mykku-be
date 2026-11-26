package com.example.mykku.contract.support

import com.example.mykku.BaseControllerTest
import io.restassured.RestAssured
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import javax.sql.DataSource

@DisplayName("로그인 토큰 헬퍼")
class LoginTokensTest : BaseControllerTest() {

    @Autowired
    private lateinit var dataSource: DataSource

    @BeforeEach
    fun loadFixture() {
        SqlFixtureLoader(dataSource).load("support-selftest.sql")
    }

    @Test
    fun `픽스처 EMAIL 회원의 이메일 로그인 응답에서 액세스·리프레시 토큰을 꺼낸다`() {
        val tokens = LoginTokens.emailLogin(FIXTURE_EMAIL, FIXTURE_PASSWORD)

        assertThat(listOf(tokens.accessToken, tokens.refreshToken)).allMatch { JWT.matches(it) }
        assertThat(tokens.accessToken).isNotEqualTo(tokens.refreshToken)
        assertThat(statusOfMyProfile(tokens.accessToken)).isEqualTo(200)
        assertThat(statusOfMyProfile(tokens.refreshToken)).isEqualTo(401)
    }

    @Test
    fun `로그인이 실패하면 상태를 담아 실패한다`() {
        assertThatThrownBy { LoginTokens.emailLogin(FIXTURE_EMAIL, "wrong-password") }
            .isInstanceOf(AssertionError::class.java)
            .hasMessageContaining("401")
    }

    private fun statusOfMyProfile(token: String): Int =
        RestAssured.given().header("Authorization", "Bearer $token").get("/api/v1/members/me").statusCode

    private companion object {
        const val FIXTURE_EMAIL = "contract-login@example.com"
        const val FIXTURE_PASSWORD = "contract-login-1!"
        val JWT = Regex("[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+")
    }
}
