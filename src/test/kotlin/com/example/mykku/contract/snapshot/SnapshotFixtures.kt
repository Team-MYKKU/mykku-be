package com.example.mykku.contract.snapshot

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.restassured.builder.MultiPartSpecBuilder
import io.restassured.response.Response
import io.restassured.specification.MultiPartSpecification
import java.nio.charset.StandardCharsets.UTF_8
import java.time.LocalDateTime

object SnapshotFixtures {
    const val BASE = "snapshot-base.sql"

    const val GOOGLE_MEMBER = 1L
    const val EMAIL_MEMBER = 2L
    const val INCOMPLETE_MEMBER = 4L
    const val LEAVING_MEMBER = 6L

    const val EMAIL = "snap-email@example.com"
    const val LEAVING_EMAIL = "snap-leave@example.com"
    const val PASSWORD = "contract-login-1!"

    val GOOGLE = Caller.Member(GOOGLE_MEMBER)

    private const val OK = 200
    private const val SIGNUP = "SIGNUP"
    private val objectMapper = jacksonObjectMapper()

    fun jsonPart(name: String, body: Any): MultiPartSpecification =
        MultiPartSpecBuilder(objectMapper.writeValueAsString(body))
            .controlName(name)
            .mimeType("application/json")
            .charset(UTF_8)
            .build()

    fun imagePart(): MultiPartSpecification =
        MultiPartSpecBuilder("fake-image".toByteArray())
            .controlName("images")
            .fileName("image.png")
            .mimeType("image/png")
            .build()

    fun SnapshotSession.verifiedSignup(email: String, password: String): Response {
        val code = issuedVerificationCode(email, SIGNUP)
        val verification = mapOf("email" to email, "code" to code, "purpose" to SIGNUP)
        requireStatus(OK, send("POST", "/api/v1/email-auth/verify-code", json = verification))
        return send("POST", "/api/v1/email-auth/signup", json = mapOf("email" to email, "password" to password))
    }

    fun SnapshotSession.movePopularFeedsIntoWindow() {
        setFeedTime(6, fixtureDate.minusDays(2).atTime(12, 0, 0, 123_456_000))
        setFeedTime(7, fixtureDate.minusDays(3).atTime(12, 0))
        setFeedTime(8, fixtureDate.minusDays(1).atTime(9, 0, 0, 120_000_000))
    }

    fun SnapshotSession.setFeedTime(feedId: Long, createdAt: LocalDateTime) {
        sql("UPDATE feed SET created_at = ?, updated_at = ? WHERE id = ?", createdAt, createdAt, feedId)
    }
}
