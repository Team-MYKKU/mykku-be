package com.example.mykku.util

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import java.util.Date

object TestTokenGenerator {

    private const val SECRET_KEY = "your-secret-key-should-be-very-long-and-secure-at-least-32-characters-long"
    private const val EXPIRATION_TIME = 86400000L // 24 hours

    private val secretKey = Keys.hmacShaKeyFor(SECRET_KEY.toByteArray())

    fun generateToken(memberPk: Long, email: String = "test@example.com"): String {
        val now = Date()
        val expiryDate = Date(now.time + EXPIRATION_TIME)

        return Jwts.builder()
            .subject(memberPk.toString())
            .claim("email", email)
            .claim("tokenType", "access")
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(secretKey)
            .compact()
    }

    fun getBearerToken(memberPk: Long): String {
        val token = generateToken(memberPk)
        return "Bearer $token"
    }

    fun createAuthHeaders(memberPk: Long): Map<String, String> {
        return mapOf("Authorization" to getBearerToken(memberPk))
    }
}
