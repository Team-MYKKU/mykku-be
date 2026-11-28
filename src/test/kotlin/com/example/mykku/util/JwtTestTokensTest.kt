package com.example.mykku.util

import com.example.mykku.BaseControllerTest
import com.example.mykku.auth.application.port.output.TokenProvider
import com.example.mykku.auth.config.JwtProperties
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import io.jsonwebtoken.security.SignatureException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant
import java.util.Base64

@DisplayName("JwtTestTokens 토큰 헬퍼")
class JwtTestTokensTest : BaseControllerTest() {

    @Autowired
    private lateinit var tokenProvider: TokenProvider

    @Autowired
    private lateinit var jwtProperties: JwtProperties

    private val objectMapper = ObjectMapper()

    @Test
    fun `getBearerToken과 createAuthHeaders는 운영 TokenProvider가 받는 액세스 토큰을 Bearer 형식으로 준다`() {
        val bearerToken = getBearerToken(MEMBER_PK)
        val token = bearerToken.removePrefix(BEARER_PREFIX)

        assertThat(bearerToken).startsWith(BEARER_PREFIX)
        assertThat(token).doesNotContain(" ")
        assertThat(tokenProvider.validateToken(token)).isTrue()
        assertThat(tokenProvider.isAccessToken(token)).isTrue()
        assertThat(tokenProvider.getMemberIdFromToken(token)).isEqualTo(MEMBER_PK)
        assertThat(createAuthHeaders(MEMBER_PK).keys).containsExactly("Authorization")
        assertThat(createAuthHeaders(MEMBER_PK).getValue("Authorization")).startsWith(BEARER_PREFIX)
    }

    @Test
    fun `액세스 토큰은 키 길이에 맞는 HS 헤더와 sub·email·tokenType·iat·exp 클레임, 1일 만료를 가진다`() {
        val token = getBearerToken(MEMBER_PK).removePrefix(BEARER_PREFIX)

        assertThat(headerOf(token).toString()).isEqualTo("""{"alg":"${expectedAlgorithm()}"}""")
        assertAccessClaims(payloadOf(token), ONE_DAY_SECONDS)
    }

    @Test
    fun `리프레시 토큰은 운영 생성기와 같은 클레임과 14일 만료를 가진다`() {
        val token = jwtTestTokens.refresh(MEMBER_PK)

        assertThat(headerOf(token).toString()).isEqualTo("""{"alg":"${expectedAlgorithm()}"}""")
        assertRefreshClaims(payloadOf(token), FOURTEEN_DAYS_SECONDS)
        assertThat(tokenProvider.validateToken(token)).isTrue()
        assertThat(tokenProvider.isRefreshToken(token)).isTrue()
        assertThat(tokenProvider.isAccessToken(token)).isFalse()
    }

    @Test
    fun `만료본은 서버 키 서명과 클레임이 같고 만료만 지나 운영 TokenProvider가 거부한다`() {
        val access = jwtTestTokens.expiredAccess(MEMBER_PK)
        val refresh = jwtTestTokens.expiredRefresh(MEMBER_PK)

        assertThat(headerOf(access).toString()).isEqualTo("""{"alg":"${expectedAlgorithm()}"}""")
        assertAccessClaims(payloadOf(access), EXPIRED_SECONDS)
        assertRefreshClaims(payloadOf(refresh), EXPIRED_SECONDS)
        assertThat(payloadOf(access)["exp"].asLong()).isLessThan(Instant.now().epochSecond)
        listOf(access, refresh).forEach {
            assertThatThrownBy { parseWithServerKey(it) }.isInstanceOf(ExpiredJwtException::class.java)
            assertRejected(it)
        }
    }

    @Test
    fun `다른 secret으로 서명한 토큰은 헤더·클레임이 같고 운영 TokenProvider가 거부한다`() {
        val access = jwtTestTokens.otherSecretAccess(MEMBER_PK)
        val refresh = jwtTestTokens.otherSecretRefresh(MEMBER_PK)

        assertThat(headerOf(access).toString()).isEqualTo("""{"alg":"${expectedAlgorithm()}"}""")
        assertAccessClaims(payloadOf(access), ONE_DAY_SECONDS)
        assertRefreshClaims(payloadOf(refresh), FOURTEEN_DAYS_SECONDS)
        assertThat(signatureOf(access)).isNotEqualTo(signatureOf(jwtTestTokens.access(MEMBER_PK)))
        listOf(access, refresh).forEach {
            assertThatThrownBy { parseWithServerKey(it) }.isInstanceOf(SignatureException::class.java)
            assertRejected(it)
        }
    }

    private fun assertAccessClaims(payload: JsonNode, lifetimeSeconds: Long) {
        assertThat(claimNamesOf(payload)).containsExactly("sub", "email", "tokenType", "iat", "exp")
        assertThat(payload["sub"].asText()).isEqualTo(MEMBER_PK.toString())
        assertThat(payload["email"].asText()).isEqualTo(JwtTestTokens.DEFAULT_EMAIL)
        assertThat(payload["tokenType"].asText()).isEqualTo("access")
        assertThat(payload["exp"].asLong() - payload["iat"].asLong()).isEqualTo(lifetimeSeconds)
    }

    private fun assertRefreshClaims(payload: JsonNode, lifetimeSeconds: Long) {
        assertThat(claimNamesOf(payload)).containsExactly("sub", "tokenType", "iat", "exp")
        assertThat(payload["sub"].asText()).isEqualTo(MEMBER_PK.toString())
        assertThat(payload["tokenType"].asText()).isEqualTo("refresh")
        assertThat(payload["exp"].asLong() - payload["iat"].asLong()).isEqualTo(lifetimeSeconds)
    }

    private fun assertRejected(token: String) {
        assertThat(tokenProvider.validateToken(token)).isFalse()
        assertThat(tokenProvider.isAccessToken(token)).isFalse()
        assertThat(tokenProvider.isRefreshToken(token)).isFalse()
    }

    private fun parseWithServerKey(token: String) =
        Jwts.parser().verifyWith(serverKey()).build().parseSignedClaims(token)

    private fun serverKey() = Keys.hmacShaKeyFor(jwtProperties.secret.toByteArray())

    private fun expectedAlgorithm(): String =
        "HS" + serverKey().algorithm.removePrefix("HmacSHA")

    private fun claimNamesOf(payload: JsonNode): List<String> = payload.fieldNames().asSequence().toList()

    private fun headerOf(token: String): JsonNode = decode(token.split('.')[0])

    private fun payloadOf(token: String): JsonNode = decode(token.split('.')[1])

    private fun signatureOf(token: String): String = token.split('.')[2]

    private fun decode(part: String): JsonNode = objectMapper.readTree(Base64.getUrlDecoder().decode(part))

    companion object {
        private const val MEMBER_PK = 7L
        private const val BEARER_PREFIX = "Bearer "
        private const val ONE_DAY_SECONDS = 86_400L
        private const val FOURTEEN_DAYS_SECONDS = 1_209_600L
        private const val EXPIRED_SECONDS = JwtTestTokens.EXPIRED_MILLIS / 1000
    }
}
