package com.example.mykku.notification.adapter.input.web

import com.example.mykku.BaseControllerTest
import com.example.mykku.member.adapter.output.persistence.entity.MemberJpaEntity
import com.example.mykku.notification.adapter.output.persistence.entity.NotificationSettingJpaEntity
import com.example.mykku.notification.adapter.output.persistence.repository.NotificationSettingJpaRepository
import com.example.mykku.notification.domain.vo.NotificationType
import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@DisplayName("NotificationSettingController 통합 테스트")
class NotificationSettingControllerTest : BaseControllerTest() {

    @Autowired
    private lateinit var notificationSettingJpaRepository: NotificationSettingJpaRepository

    private fun createNotificationSetting(
        member: MemberJpaEntity,
        notificationType: NotificationType,
        isEnabled: Boolean
    ): NotificationSettingJpaEntity {
        return notificationSettingJpaRepository.save(
            NotificationSettingJpaEntity(
                member = member,
                notificationType = notificationType,
                isEnabled = isEnabled
            )
        )
    }

    @Test
    @DisplayName("알림 설정 목록 조회 - 기존 유저 설정 보충 (1개 → 2개)")
    fun `getSettings - 기존 유저의 누락된 설정을 보충한다`() {
        val member = createAndSaveMember()

        createNotificationSetting(
            member = member,
            notificationType = NotificationType.FEED_LIKE,
            isEnabled = true
        )

        val authHeader = getBearerToken(member.id)

        RestAssured.given()
            .header("Authorization", authHeader)
            .`when`()
            .get("/api/v1/notification-settings")
            .then()
            .statusCode(200)
            .body("message", equalTo("알림 설정을 성공적으로 조회했습니다."))
            .body("data", hasSize<Any>(2))
    }

    @Test
    @DisplayName("알림 설정 목록 조회 - 설정이 없으면 기본 설정 생성 후 반환")
    fun `getSettings - 설정이 없으면 기본 설정을 생성한다`() {
        val member = createAndSaveMember()

        val authHeader = getBearerToken(member.id)

        RestAssured.given()
            .header("Authorization", authHeader)
            .`when`()
            .get("/api/v1/notification-settings")
            .then()
            .statusCode(200)
            .body("message", equalTo("알림 설정을 성공적으로 조회했습니다."))
            .body("data", hasSize<Any>(2))
    }

    @Test
    @DisplayName("알림 설정 목록 조회 - 인증되지 않은 사용자")
    fun `getSettings - 인증되지 않은 사용자는 조회할 수 없다`() {
        RestAssured.given()
            .`when`()
            .get("/api/v1/notification-settings")
            .then()
            .statusCode(401)
    }

    @Test
    @DisplayName("알림 설정 변경 - 정상 케이스 (기존 설정 업데이트)")
    fun `updateSetting - 기존 설정을 업데이트한다`() {
        val member = createAndSaveMember()

        createNotificationSetting(
            member = member,
            notificationType = NotificationType.FEED_LIKE,
            isEnabled = true
        )

        val request = UpdateNotificationSettingRequest(
            notificationType = NotificationType.FEED_LIKE,
            isEnabled = false
        )

        val authHeader = getBearerToken(member.id)

        RestAssured.given()
            .header("Authorization", authHeader)
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .patch("/api/v1/notification-settings")
            .then()
            .statusCode(200)
            .body("message", equalTo("알림 설정이 성공적으로 변경되었습니다."))
            .body("data.notificationType", equalTo("FEED_LIKE"))
            .body("data.isEnabled", equalTo(false))
    }

    @Test
    @DisplayName("알림 설정 변경 - 정상 케이스 (새 설정 생성)")
    fun `updateSetting - 새 설정을 생성한다`() {
        val member = createAndSaveMember()

        val request = UpdateNotificationSettingRequest(
            notificationType = NotificationType.FEED_COMMENT,
            isEnabled = false
        )

        val authHeader = getBearerToken(member.id)

        RestAssured.given()
            .header("Authorization", authHeader)
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .patch("/api/v1/notification-settings")
            .then()
            .statusCode(200)
            .body("message", equalTo("알림 설정이 성공적으로 변경되었습니다."))
            .body("data.notificationType", equalTo("FEED_COMMENT"))
            .body("data.isEnabled", equalTo(false))
    }

    @Test
    @DisplayName("알림 설정 변경 - 인증되지 않은 사용자")
    fun `updateSetting - 인증되지 않은 사용자는 변경할 수 없다`() {
        val request = UpdateNotificationSettingRequest(
            notificationType = NotificationType.FEED_LIKE,
            isEnabled = false
        )

        RestAssured.given()
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .patch("/api/v1/notification-settings")
            .then()
            .statusCode(401)
    }

    @Test
    @DisplayName("알림 설정 변경 - 유효하지 않은 요청")
    fun `updateSetting - 유효하지 않은 요청은 거부된다`() {
        val member = createAndSaveMember()

        val authHeader = getBearerToken(member.id)

        RestAssured.given()
            .header("Authorization", authHeader)
            .contentType(ContentType.JSON)
            .body("{}")
            .`when`()
            .patch("/api/v1/notification-settings")
            .then()
            .statusCode(400)
    }
}
