package com.example.mykku.notification.adapter.input.web

import com.example.mykku.BaseControllerTest
import com.example.mykku.member.adapter.output.persistence.entity.MemberJpaEntity
import com.example.mykku.notification.adapter.output.persistence.entity.FcmTokenJpaEntity
import com.example.mykku.notification.adapter.output.persistence.repository.FcmTokenJpaRepository
import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@DisplayName("FcmTokenController 통합 테스트")
class FcmTokenControllerTest : BaseControllerTest() {

    @Autowired
    private lateinit var fcmTokenJpaRepository: FcmTokenJpaRepository

    private fun createFcmToken(
        member: MemberJpaEntity,
        token: String,
        deviceId: String,
        deviceType: String? = null
    ): FcmTokenJpaEntity {
        return fcmTokenJpaRepository.save(
            FcmTokenJpaEntity(
                member = member,
                token = token,
                deviceId = deviceId,
                deviceType = deviceType
            )
        )
    }

    @Test
    @DisplayName("FCM 토큰 등록 - 정상 케이스")
    fun `registerToken - 정상적으로 FCM 토큰을 등록한다`() {
        val member = createAndSaveMember(
            nickname = "TestUser",
            email = "test@example.com"
        )
        val authHeader = getBearerToken(member.id)

        val request = mapOf(
            "token" to "test_fcm_token_123",
            "deviceId" to "device_001",
            "deviceType" to "ANDROID"
        )

        RestAssured.given()
            .header("Authorization", authHeader)
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .post("/api/v1/fcm-tokens")
            .then()
            .statusCode(200)
            .body("message", equalTo("FCM 토큰이 성공적으로 등록되었습니다."))
            .body("data.deviceId", equalTo("device_001"))
            .body("data.deviceType", equalTo("ANDROID"))
    }

    @Test
    @DisplayName("FCM 토큰 등록 - 이미 존재하는 deviceId는 토큰을 갱신한다")
    fun `registerToken - 이미 존재하는 deviceId는 토큰을 갱신한다`() {
        val member = createAndSaveMember(
            nickname = "TestUser",
            email = "test@example.com"
        )
        val existingToken = createFcmToken(
            member = member,
            token = "old_token",
            deviceId = "device_001",
            deviceType = "ANDROID"
        )
        val authHeader = getBearerToken(member.id)

        val request = mapOf(
            "token" to "new_token_updated",
            "deviceId" to "device_001",
            "deviceType" to "ANDROID"
        )

        RestAssured.given()
            .header("Authorization", authHeader)
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .post("/api/v1/fcm-tokens")
            .then()
            .statusCode(200)
            .body("message", equalTo("FCM 토큰이 성공적으로 등록되었습니다."))

        val updatedToken = fcmTokenJpaRepository.findById(existingToken.id!!).get()
        assert(updatedToken.token == "new_token_updated")
    }

    @Test
    @DisplayName("FCM 토큰 등록 - 인증되지 않은 사용자")
    fun `registerToken - 인증되지 않은 사용자는 등록할 수 없다`() {
        val request = mapOf(
            "token" to "test_fcm_token",
            "deviceId" to "device_001"
        )

        RestAssured.given()
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .post("/api/v1/fcm-tokens")
            .then()
            .statusCode(401)
    }

    @Test
    @DisplayName("FCM 토큰 등록 - 필수 필드 누락")
    fun `registerToken - 필수 필드가 누락되면 실패한다`() {
        val member = createAndSaveMember()
        val authHeader = getBearerToken(member.id)

        val request = mapOf(
            "deviceId" to "device_001"
        )

        RestAssured.given()
            .header("Authorization", authHeader)
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .post("/api/v1/fcm-tokens")
            .then()
            .statusCode(400)
    }

    @Test
    @DisplayName("FCM 토큰 목록 조회 - 정상 케이스")
    fun `getTokens - 정상적으로 FCM 토큰 목록을 조회한다`() {
        val member = createAndSaveMember()
        createFcmToken(
            member = member,
            token = "token1",
            deviceId = "device_001",
            deviceType = "ANDROID"
        )
        createFcmToken(
            member = member,
            token = "token2",
            deviceId = "device_002",
            deviceType = "IOS"
        )
        val authHeader = getBearerToken(member.id)

        RestAssured.given()
            .header("Authorization", authHeader)
            .`when`()
            .get("/api/v1/fcm-tokens")
            .then()
            .statusCode(200)
            .body("message", equalTo("FCM 토큰 목록을 성공적으로 조회했습니다."))
            .body("data", hasSize<Any>(2))
    }

    @Test
    @DisplayName("FCM 토큰 목록 조회 - 인증되지 않은 사용자")
    fun `getTokens - 인증되지 않은 사용자는 조회할 수 없다`() {
        RestAssured.given()
            .`when`()
            .get("/api/v1/fcm-tokens")
            .then()
            .statusCode(401)
    }

    @Test
    @DisplayName("FCM 토큰 삭제 - 정상 케이스")
    fun `deleteToken - 정상적으로 FCM 토큰을 삭제한다`() {
        val member = createAndSaveMember()
        createFcmToken(
            member = member,
            token = "token1",
            deviceId = "device_001"
        )
        val authHeader = getBearerToken(member.id)

        RestAssured.given()
            .header("Authorization", authHeader)
            .`when`()
            .delete("/api/v1/fcm-tokens/device_001")
            .then()
            .statusCode(200)
            .body("message", equalTo("FCM 토큰이 삭제되었습니다."))

        assert(!fcmTokenJpaRepository.existsByMemberIdAndDeviceId(member.id, "device_001"))
    }

    @Test
    @DisplayName("FCM 토큰 삭제 - 인증되지 않은 사용자")
    fun `deleteToken - 인증되지 않은 사용자는 삭제할 수 없다`() {
        RestAssured.given()
            .`when`()
            .delete("/api/v1/fcm-tokens/device_001")
            .then()
            .statusCode(401)
    }
}
