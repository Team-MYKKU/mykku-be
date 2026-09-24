package com.example.mykku.auth.adapter.output.persistence

import com.example.mykku.auth.config.JwtProperties
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.Date

@DisplayName("JwtTokenProviderAdapter 단위 테스트")
class JwtTokenProviderAdapterTest {

    private val secret = "unit-test-secret-key-that-is-long-enough-for-hmac-sha256-signing"
    private val adapter = JwtTokenProviderAdapter(JwtProperties(secret = secret))

    @Test
    fun `액세스 토큰은 isAccessToken만 참이다`() {
        val token = adapter.generateAccessToken(1L, "user@example.com")

        assertThat(adapter.isAccessToken(token)).isTrue()
        assertThat(adapter.isRefreshToken(token)).isFalse()
    }

    @Test
    fun `리프레시 토큰은 isRefreshToken만 참이다`() {
        val token = adapter.generateRefreshToken(1L)

        assertThat(adapter.isRefreshToken(token)).isTrue()
        assertThat(adapter.isAccessToken(token)).isFalse()
    }

    @Test
    fun `tokenType 클레임이 없는 토큰은 액세스 토큰도 리프레시 토큰도 아니다`() {
        val token = Jwts.builder()
            .subject("1")
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + 60_000))
            .signWith(Keys.hmacShaKeyFor(secret.toByteArray()))
            .compact()

        assertThat(adapter.validateToken(token)).isTrue()
        assertThat(adapter.isAccessToken(token)).isFalse()
        assertThat(adapter.isRefreshToken(token)).isFalse()
    }

    @Test
    fun `형식이 잘못된 토큰은 검증과 타입 판별 모두 거짓이다`() {
        val token = "not.a.jwt"

        assertThat(adapter.validateToken(token)).isFalse()
        assertThat(adapter.isAccessToken(token)).isFalse()
        assertThat(adapter.isRefreshToken(token)).isFalse()
    }
}
