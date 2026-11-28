package com.example.mykku.notification.adapter.input.web

import com.example.mykku.BaseControllerTest
import com.example.mykku.member.adapter.output.persistence.entity.MemberJpaEntity
import com.example.mykku.notification.adapter.output.persistence.entity.NotificationJpaEntity
import com.example.mykku.notification.adapter.output.persistence.repository.NotificationJpaRepository
import com.example.mykku.notification.domain.vo.NotificationType
import io.restassured.RestAssured
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@DisplayName("NotificationController 통합 테스트")
class NotificationControllerTest : BaseControllerTest() {

    @Autowired
    private lateinit var notificationJpaRepository: NotificationJpaRepository

    private fun createNotification(
        type: NotificationType,
        sender: MemberJpaEntity,
        receiver: MemberJpaEntity,
        content: String
    ): NotificationJpaEntity {
        return notificationJpaRepository.save(
            NotificationJpaEntity(
                type = type,
                sender = sender,
                receiver = receiver,
                content = content
            )
        )
    }

    @Test
    @DisplayName("알림 목록 조회 - 정상 케이스")
    fun `getNotifications - 정상적으로 알림 목록을 조회한다`() {
        val sender = createAndSaveMember(memberId = "sender", nickname = "Sender", email = "sender@example.com", socialId = "sender1")
        val receiver = createAndSaveMember(memberId = "receiver", nickname = "Receiver", email = "receiver@example.com", socialId = "receiver1")

        createNotification(
            type = NotificationType.FEED_LIKE,
            sender = sender,
            receiver = receiver,
            content = "sender1님이 회원님의 피드를 좋아합니다"
        )
        createNotification(
            type = NotificationType.FEED_COMMENT,
            sender = sender,
            receiver = receiver,
            content = "sender1님이 회원님의 피드에 댓글을 남겼습니다"
        )

        val authHeader = getBearerToken(receiver.id)

        RestAssured.given()
            .header("Authorization", authHeader)
            .queryParam("page", 0)
            .queryParam("size", 10)
            .`when`()
            .get("/api/v1/notifications")
            .then()
            .statusCode(200)
            .body("message", equalTo("알림 목록을 성공적으로 조회했습니다."))
            .body("data.content", hasSize<Any>(2))
    }

    @Test
    @DisplayName("알림 목록 조회 - 인증되지 않은 사용자")
    fun `getNotifications - 인증되지 않은 사용자는 조회할 수 없다`() {
        RestAssured.given()
            .`when`()
            .get("/api/v1/notifications")
            .then()
            .statusCode(401)
    }

    @Test
    @DisplayName("읽지 않은 알림 목록 조회 - 정상 케이스")
    fun `getUnreadNotifications - 읽지 않은 알림만 조회한다`() {
        val sender = createAndSaveMember(memberId = "sender", email = "sender@example.com", socialId = "sender1")
        val receiver = createAndSaveMember(memberId = "receiver", email = "receiver@example.com", socialId = "receiver1")

        val readNotification = NotificationJpaEntity(
            type = NotificationType.FEED_LIKE,
            sender = sender,
            receiver = receiver,
            content = "읽은 알림"
        )
        readNotification.markAsRead()
        notificationJpaRepository.save(readNotification)

        createNotification(
            type = NotificationType.FEED_COMMENT,
            sender = sender,
            receiver = receiver,
            content = "읽지 않은 알림"
        )

        val authHeader = getBearerToken(receiver.id)

        RestAssured.given()
            .header("Authorization", authHeader)
            .`when`()
            .get("/api/v1/notifications/unread")
            .then()
            .statusCode(200)
            .body("message", equalTo("읽지 않은 알림 목록을 성공적으로 조회했습니다."))
            .body("data.content", hasSize<Any>(1))
    }

    @Test
    @DisplayName("읽지 않은 알림 개수 조회 - 정상 케이스")
    fun `getUnreadCount - 읽지 않은 알림 개수를 조회한다`() {
        val sender = createAndSaveMember(memberId = "sender", email = "sender@example.com", socialId = "sender1")
        val receiver = createAndSaveMember(memberId = "receiver", email = "receiver@example.com", socialId = "receiver1")

        createNotification(
            type = NotificationType.FEED_LIKE,
            sender = sender,
            receiver = receiver,
            content = "알림1"
        )
        createNotification(
            type = NotificationType.FEED_COMMENT,
            sender = sender,
            receiver = receiver,
            content = "알림2"
        )

        val authHeader = getBearerToken(receiver.id)

        RestAssured.given()
            .header("Authorization", authHeader)
            .`when`()
            .get("/api/v1/notifications/unread/count")
            .then()
            .statusCode(200)
            .body("message", equalTo("읽지 않은 알림 개수를 성공적으로 조회했습니다."))
            .body("data", equalTo(2))
    }

    @Test
    @DisplayName("알림 읽음 처리 - 정상 케이스")
    fun `markAsRead - 알림을 읽음 처리한다`() {
        val sender = createAndSaveMember(memberId = "sender", email = "sender@example.com", socialId = "sender1")
        val receiver = createAndSaveMember(memberId = "receiver", email = "receiver@example.com", socialId = "receiver1")

        val notification = createNotification(
            type = NotificationType.FEED_LIKE,
            sender = sender,
            receiver = receiver,
            content = "알림"
        )

        val authHeader = getBearerToken(receiver.id)

        RestAssured.given()
            .header("Authorization", authHeader)
            .`when`()
            .patch("/api/v1/notifications/${notification.id}/read")
            .then()
            .statusCode(200)
            .body("message", equalTo("알림을 읽음 처리했습니다."))

        val updatedNotification = notificationJpaRepository.findById(notification.id!!).get()
        assertThat(updatedNotification.isRead).isTrue()
    }

    @Test
    @DisplayName("알림 읽음 처리 - 권한 없음")
    fun `markAsRead - 다른 사용자의 알림은 읽음 처리할 수 없다`() {
        val sender = createAndSaveMember(memberId = "sender", email = "sender@example.com", socialId = "sender1")
        val receiver = createAndSaveMember(memberId = "receiver", email = "receiver@example.com", socialId = "receiver1")
        val other = createAndSaveMember(memberId = "other", email = "other@example.com", socialId = "other")

        val notification = createNotification(
            type = NotificationType.FEED_LIKE,
            sender = sender,
            receiver = receiver,
            content = "알림"
        )

        val authHeader = getBearerToken(other.id)

        RestAssured.given()
            .header("Authorization", authHeader)
            .`when`()
            .patch("/api/v1/notifications/${notification.id}/read")
            .then()
            .statusCode(403)
    }

    @Test
    @DisplayName("모든 알림 읽음 처리 - 정상 케이스")
    fun `markAllAsRead - 모든 읽지 않은 알림을 읽음 처리한다`() {
        val sender = createAndSaveMember(memberId = "sender", email = "sender@example.com", socialId = "sender1")
        val receiver = createAndSaveMember(memberId = "receiver", email = "receiver@example.com", socialId = "receiver1")

        createNotification(
            type = NotificationType.FEED_LIKE,
            sender = sender,
            receiver = receiver,
            content = "알림1"
        )
        createNotification(
            type = NotificationType.FEED_COMMENT,
            sender = sender,
            receiver = receiver,
            content = "알림2"
        )

        val authHeader = getBearerToken(receiver.id)

        RestAssured.given()
            .header("Authorization", authHeader)
            .`when`()
            .patch("/api/v1/notifications/read-all")
            .then()
            .statusCode(200)
            .body("message", equalTo("모든 알림을 읽음 처리했습니다."))

        val unreadCount = notificationJpaRepository.countByReceiverIdAndIsRead(receiver.id, false)
        assertThat(unreadCount).isEqualTo(0L)
    }

    @Test
    @DisplayName("알림 삭제 - 정상 케이스")
    fun `deleteNotification - 알림을 삭제한다`() {
        val sender = createAndSaveMember(memberId = "sender", email = "sender@example.com", socialId = "sender1")
        val receiver = createAndSaveMember(memberId = "receiver", email = "receiver@example.com", socialId = "receiver1")

        val notification = createNotification(
            type = NotificationType.FEED_LIKE,
            sender = sender,
            receiver = receiver,
            content = "알림"
        )

        val authHeader = getBearerToken(receiver.id)

        RestAssured.given()
            .header("Authorization", authHeader)
            .`when`()
            .delete("/api/v1/notifications/${notification.id}")
            .then()
            .statusCode(200)
            .body("message", equalTo("알림을 삭제했습니다."))

        assertThat(notificationJpaRepository.existsById(notification.id!!)).isFalse()
    }

    @Test
    @DisplayName("알림 삭제 - 권한 없음")
    fun `deleteNotification - 다른 사용자의 알림은 삭제할 수 없다`() {
        val sender = createAndSaveMember(memberId = "sender", email = "sender@example.com", socialId = "sender1")
        val receiver = createAndSaveMember(memberId = "receiver", email = "receiver@example.com", socialId = "receiver1")
        val other = createAndSaveMember(memberId = "other", email = "other@example.com", socialId = "other")

        val notification = createNotification(
            type = NotificationType.FEED_LIKE,
            sender = sender,
            receiver = receiver,
            content = "알림"
        )

        val authHeader = getBearerToken(other.id)

        RestAssured.given()
            .header("Authorization", authHeader)
            .`when`()
            .delete("/api/v1/notifications/${notification.id}")
            .then()
            .statusCode(403)
    }
}
