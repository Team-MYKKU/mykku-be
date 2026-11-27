package com.example.mykku.util

import com.example.mykku.auth.adapter.output.persistence.JwtTokenProviderAdapter
import com.example.mykku.auth.application.port.output.TokenProvider
import com.example.mykku.auth.config.JwtProperties

class JwtTestTokens(
    private val properties: JwtProperties,
    private val tokenProvider: TokenProvider = JwtTokenProviderAdapter(properties)
) {

    fun access(memberPk: Long, email: String = DEFAULT_EMAIL): String =
        tokenProvider.generateAccessToken(memberPk, email)

    fun refresh(memberPk: Long): String = tokenProvider.generateRefreshToken(memberPk)

    fun expiredAccess(memberPk: Long, email: String = DEFAULT_EMAIL): String =
        expiredProvider().generateAccessToken(memberPk, email)

    fun expiredRefresh(memberPk: Long): String = expiredProvider().generateRefreshToken(memberPk)

    fun otherSecretAccess(memberPk: Long, email: String = DEFAULT_EMAIL): String =
        otherSecretProvider().generateAccessToken(memberPk, email)

    fun otherSecretRefresh(memberPk: Long): String = otherSecretProvider().generateRefreshToken(memberPk)

    private fun expiredProvider(): TokenProvider = JwtTokenProviderAdapter(
        properties.copy(accessTokenExpiration = EXPIRED_MILLIS, refreshTokenExpiration = EXPIRED_MILLIS)
    )

    private fun otherSecretProvider(): TokenProvider =
        JwtTokenProviderAdapter(properties.copy(secret = otherSecretOf(properties.secret)))

    companion object {
        const val DEFAULT_EMAIL = "test@example.com"
        const val EXPIRED_MILLIS = -60_000L

        fun bearer(token: String): String = "Bearer $token"

        private fun otherSecretOf(secret: String): String {
            val bytes = secret.toByteArray()
            return String(CharArray(bytes.size) { if (bytes[it] == 'a'.code.toByte()) 'b' else 'a' })
        }
    }
}
