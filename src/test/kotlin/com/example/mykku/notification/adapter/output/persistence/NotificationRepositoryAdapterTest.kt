package com.example.mykku.notification.adapter.output.persistence

import com.example.mykku.BaseRepositoryTest
import com.example.mykku.notification.application.port.output.NotificationRepository
import com.example.mykku.notification.domain.entity.Notification
import com.example.mykku.notification.domain.vo.NotificationId
import com.example.mykku.notification.domain.vo.NotificationType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort

@DisplayName("NotificationRepositoryAdapter 통합 테스트")
class NotificationRepositoryAdapterTest : BaseRepositoryTest() {

    @Autowired
    private lateinit var notificationRepository: NotificationRepository

    private var receiverId: Long = 0
    private var senderId: Long = 0

    @BeforeEach
    fun setUp() {
        val receiver = createAndSaveMember(
            memberId = "receiver1",
            nickname = "수신자",
            email = "receiver@example.com",
            socialId = "receiver123"
        )
        val sender = createAndSaveMember(
            memberId = "sender1",
            nickname = "발신자",
            email = "sender@example.com",
            socialId = "sender123"
        )
        receiverId = receiver.id
        senderId = sender.id
    }

    @Nested
    @DisplayName("save 메서드")
    inner class Save {

        @Test
        @DisplayName("알림을 저장하면 ID가 부여된다")
        fun `알림 저장 - 정상 케이스`() {
            val notification = createNotification()

            val saved = notificationRepository.save(notification)

            assertThat(saved.id).isNotNull()
            assertThat(saved.id!!.value).isGreaterThan(0)
            assertThat(saved.type).isEqualTo(NotificationType.FEED_LIKE)
            assertThat(saved.receiverId).isEqualTo(receiverId)
            assertThat(saved.content).isEqualTo("테스트 알림 내용")
        }

        @Test
        @DisplayName("senderId가 null인 알림을 저장할 수 있다")
        fun `알림 저장 - senderId 없음`() {
            val notification = Notification.create(
                type = NotificationType.FEED_LIKE,
                senderId = null,
                receiverId = receiverId,
                content = "알림 내용"
            )

            val saved = notificationRepository.save(notification)

            assertThat(saved.id).isNotNull()
            assertThat(saved.senderId).isNull()
            assertThat(saved.type).isEqualTo(NotificationType.FEED_LIKE)
        }

        @Test
        @DisplayName("연관 리소스 정보를 포함한 알림을 저장할 수 있다")
        fun `알림 저장 - 연관 리소스 포함`() {
            val notification = Notification.create(
                type = NotificationType.FEED_COMMENT,
                senderId = senderId,
                receiverId = receiverId,
                content = "새로운 댓글이 달렸습니다",
                relatedResourceId = 100L,
                relatedResourceType = "FEED"
            )

            val saved = notificationRepository.save(notification)

            assertThat(saved.relatedResourceId).isEqualTo(100L)
            assertThat(saved.relatedResourceType).isEqualTo("FEED")
        }
    }

    @Nested
    @DisplayName("findById 메서드")
    inner class FindById {

        @Test
        @DisplayName("ID로 알림을 조회할 수 있다")
        fun `ID로 조회 - 정상 케이스`() {
            val saved = notificationRepository.save(createNotification())

            val found = notificationRepository.findById(saved.id!!)

            assertThat(found).isNotNull()
            assertThat(found!!.id).isEqualTo(saved.id)
            assertThat(found.content).isEqualTo("테스트 알림 내용")
        }

        @Test
        @DisplayName("존재하지 않는 ID로 조회하면 null을 반환한다")
        fun `ID로 조회 - 존재하지 않는 ID`() {
            val found = notificationRepository.findById(NotificationId(999999L))

            assertThat(found).isNull()
        }
    }

    @Nested
    @DisplayName("findAllByReceiverId 메서드")
    inner class FindAllByReceiverId {

        @Test
        @DisplayName("수신자 ID로 알림 목록을 페이징 조회할 수 있다")
        fun `수신자별 조회 - 정상 케이스`() {
            repeat(5) {
                notificationRepository.save(createNotification())
            }
            val pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"))

            val page = notificationRepository.findAllByReceiverId(receiverId, pageable)

            assertThat(page.content).hasSize(5)
            assertThat(page.totalElements).isEqualTo(5)
        }

        @Test
        @DisplayName("다른 수신자의 알림은 조회되지 않는다")
        fun `수신자별 조회 - 다른 수신자`() {
            notificationRepository.save(createNotification())
            val otherReceiver = createAndSaveMember(
                memberId = "other",
                nickname = "다른유저",
                email = "other@example.com",
                socialId = "other123"
            )
            val pageable = PageRequest.of(0, 10)

            val page = notificationRepository.findAllByReceiverId(otherReceiver.id, pageable)

            assertThat(page.content).isEmpty()
        }

        @Test
        @DisplayName("페이징이 정상적으로 동작한다")
        fun `수신자별 조회 - 페이징`() {
            repeat(15) {
                notificationRepository.save(createNotification())
            }
            val pageable = PageRequest.of(1, 10)

            val page = notificationRepository.findAllByReceiverId(receiverId, pageable)

            assertThat(page.content).hasSize(5)
            assertThat(page.totalElements).isEqualTo(15)
            assertThat(page.totalPages).isEqualTo(2)
        }
    }

    @Nested
    @DisplayName("findAllByReceiverIdAndIsRead 메서드")
    inner class FindAllByReceiverIdAndIsRead {

        @Test
        @DisplayName("읽지 않은 알림만 조회할 수 있다")
        fun `읽음 상태별 조회 - 읽지 않은 알림`() {
            val unread = notificationRepository.save(createNotification())
            val read = notificationRepository.save(createNotification())
            val readDomain = notificationRepository.findById(read.id!!)!!
            readDomain.markAsRead()
            notificationRepository.save(readDomain)
            val pageable = PageRequest.of(0, 10)

            val page = notificationRepository.findAllByReceiverIdAndIsRead(receiverId, false, pageable)

            assertThat(page.content).hasSize(1)
            assertThat(page.content[0].id).isEqualTo(unread.id)
        }

        @Test
        @DisplayName("읽은 알림만 조회할 수 있다")
        fun `읽음 상태별 조회 - 읽은 알림`() {
            notificationRepository.save(createNotification())
            val read = notificationRepository.save(createNotification())
            val readDomain = notificationRepository.findById(read.id!!)!!
            readDomain.markAsRead()
            notificationRepository.save(readDomain)
            val pageable = PageRequest.of(0, 10)

            val page = notificationRepository.findAllByReceiverIdAndIsRead(receiverId, true, pageable)

            assertThat(page.content).hasSize(1)
            assertThat(page.content[0].isRead).isTrue()
        }
    }

    @Nested
    @DisplayName("findAllByReceiverIdAndType 메서드")
    inner class FindAllByReceiverIdAndType {

        @Test
        @DisplayName("알림 타입별로 조회할 수 있다")
        fun `타입별 조회 - 정상 케이스`() {
            notificationRepository.save(
                Notification.create(
                    type = NotificationType.FEED_LIKE,
                    senderId = senderId,
                    receiverId = receiverId,
                    content = "좋아요 알림"
                )
            )
            notificationRepository.save(
                Notification.create(
                    type = NotificationType.FEED_COMMENT,
                    senderId = senderId,
                    receiverId = receiverId,
                    content = "댓글 알림"
                )
            )
            val pageable = PageRequest.of(0, 10)

            val page = notificationRepository.findAllByReceiverIdAndType(
                receiverId,
                NotificationType.FEED_LIKE,
                pageable
            )

            assertThat(page.content).hasSize(1)
            assertThat(page.content[0].type).isEqualTo(NotificationType.FEED_LIKE)
        }

        @Test
        @DisplayName("해당 타입의 알림이 없으면 빈 페이지를 반환한다")
        fun `타입별 조회 - 없는 타입`() {
            notificationRepository.save(
                Notification.create(
                    type = NotificationType.FEED_LIKE,
                    senderId = senderId,
                    receiverId = receiverId,
                    content = "좋아요 알림"
                )
            )
            val pageable = PageRequest.of(0, 10)

            val page = notificationRepository.findAllByReceiverIdAndType(
                receiverId,
                NotificationType.FEED_COMMENT,
                pageable
            )

            assertThat(page.content).isEmpty()
        }
    }

    @Nested
    @DisplayName("findAllByReceiverIdAndTypeIn 메서드")
    inner class FindAllByReceiverIdAndTypeIn {

        @Test
        @DisplayName("여러 타입으로 알림을 조회할 수 있다")
        fun `타입 목록별 조회 - 정상 케이스`() {
            notificationRepository.save(
                Notification.create(
                    type = NotificationType.FEED_LIKE,
                    senderId = senderId,
                    receiverId = receiverId,
                    content = "좋아요 알림"
                )
            )
            notificationRepository.save(
                Notification.create(
                    type = NotificationType.FEED_COMMENT,
                    senderId = senderId,
                    receiverId = receiverId,
                    content = "댓글 알림"
                )
            )
            val pageable = PageRequest.of(0, 10)

            val page = notificationRepository.findAllByReceiverIdAndTypeIn(
                receiverId,
                listOf(NotificationType.FEED_LIKE, NotificationType.FEED_COMMENT),
                pageable
            )

            assertThat(page.content).hasSize(2)
            assertThat(page.content.map { it.type }).containsExactlyInAnyOrder(
                NotificationType.FEED_LIKE,
                NotificationType.FEED_COMMENT
            )
        }
    }

    @Nested
    @DisplayName("countByReceiverIdAndIsReadAndTypeIn 메서드")
    inner class CountByReceiverIdAndIsReadAndTypeIn {

        @Test
        @DisplayName("타입 목록으로 읽지 않은 알림 개수를 조회할 수 있다")
        fun `타입별 읽지 않은 알림 개수 조회`() {
            notificationRepository.save(
                Notification.create(
                    type = NotificationType.FEED_LIKE,
                    senderId = senderId,
                    receiverId = receiverId,
                    content = "좋아요 알림"
                )
            )
            notificationRepository.save(
                Notification.create(
                    type = NotificationType.FEED_COMMENT,
                    senderId = senderId,
                    receiverId = receiverId,
                    content = "댓글 알림"
                )
            )

            val count = notificationRepository.countByReceiverIdAndIsReadAndTypeIn(
                receiverId,
                false,
                listOf(NotificationType.FEED_LIKE)
            )

            assertThat(count).isEqualTo(1)
        }
    }

    @Nested
    @DisplayName("countByReceiverIdAndIsRead 메서드")
    inner class CountByReceiverIdAndIsRead {

        @Test
        @DisplayName("읽지 않은 알림 개수를 조회할 수 있다")
        fun `읽지 않은 알림 개수 조회`() {
            repeat(3) {
                notificationRepository.save(createNotification())
            }
            val read = notificationRepository.save(createNotification())
            val readDomain = notificationRepository.findById(read.id!!)!!
            readDomain.markAsRead()
            notificationRepository.save(readDomain)

            val count = notificationRepository.countByReceiverIdAndIsRead(receiverId, false)

            assertThat(count).isEqualTo(3)
        }

        @Test
        @DisplayName("읽은 알림 개수를 조회할 수 있다")
        fun `읽은 알림 개수 조회`() {
            notificationRepository.save(createNotification())
            val read = notificationRepository.save(createNotification())
            val readDomain = notificationRepository.findById(read.id!!)!!
            readDomain.markAsRead()
            notificationRepository.save(readDomain)

            val count = notificationRepository.countByReceiverIdAndIsRead(receiverId, true)

            assertThat(count).isEqualTo(1)
        }

        @Test
        @DisplayName("알림이 없으면 0을 반환한다")
        fun `알림 개수 조회 - 없는 경우`() {
            val count = notificationRepository.countByReceiverIdAndIsRead(receiverId, false)

            assertThat(count).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("markAllAsReadByReceiverId 메서드")
    inner class MarkAllAsReadByReceiverId {

        @Test
        @DisplayName("수신자의 모든 알림을 읽음 처리할 수 있다")
        fun `전체 읽음 처리 - 정상 케이스`() {
            repeat(5) {
                notificationRepository.save(createNotification())
            }

            val updatedCount = notificationRepository.markAllAsReadByReceiverId(receiverId)

            assertThat(updatedCount).isEqualTo(5)
            val unreadCount = notificationRepository.countByReceiverIdAndIsRead(receiverId, false)
            assertThat(unreadCount).isEqualTo(0)
        }

        @Test
        @DisplayName("이미 읽은 알림은 영향을 받지 않는다")
        fun `전체 읽음 처리 - 이미 읽은 알림`() {
            val notification = notificationRepository.save(createNotification())
            val domain = notificationRepository.findById(notification.id!!)!!
            domain.markAsRead()
            notificationRepository.save(domain)

            val updatedCount = notificationRepository.markAllAsReadByReceiverId(receiverId)

            assertThat(updatedCount).isEqualTo(0)
        }

        @Test
        @DisplayName("다른 수신자의 알림은 영향을 받지 않는다")
        fun `전체 읽음 처리 - 다른 수신자`() {
            val otherReceiver = createAndSaveMember(
                memberId = "other",
                nickname = "다른유저",
                email = "other@example.com",
                socialId = "other123"
            )
            notificationRepository.save(
                Notification.create(
                    type = NotificationType.FEED_LIKE,
                    senderId = senderId,
                    receiverId = otherReceiver.id,
                    content = "다른 사람 알림"
                )
            )
            notificationRepository.save(createNotification())

            notificationRepository.markAllAsReadByReceiverId(receiverId)

            val otherUnreadCount = notificationRepository.countByReceiverIdAndIsRead(otherReceiver.id, false)
            assertThat(otherUnreadCount).isEqualTo(1)
        }
    }

    @Nested
    @DisplayName("markAllAsReadByReceiverIdAndTypeIn 메서드")
    inner class MarkAllAsReadByReceiverIdAndTypeIn {

        @Test
        @DisplayName("특정 타입의 알림만 읽음 처리할 수 있다")
        fun `타입별 전체 읽음 처리 - 정상 케이스`() {
            notificationRepository.save(
                Notification.create(
                    type = NotificationType.FEED_LIKE,
                    senderId = senderId,
                    receiverId = receiverId,
                    content = "좋아요 알림"
                )
            )
            notificationRepository.save(
                Notification.create(
                    type = NotificationType.FEED_COMMENT,
                    senderId = senderId,
                    receiverId = receiverId,
                    content = "댓글 알림"
                )
            )

            val updatedCount = notificationRepository.markAllAsReadByReceiverIdAndTypeIn(
                receiverId,
                listOf(NotificationType.FEED_LIKE)
            )

            assertThat(updatedCount).isEqualTo(1)
            val pageable = PageRequest.of(0, 10)
            val unreadNotifications = notificationRepository.findAllByReceiverIdAndIsRead(receiverId, false, pageable)
            assertThat(unreadNotifications.content).hasSize(1)
            assertThat(unreadNotifications.content[0].type).isEqualTo(NotificationType.FEED_COMMENT)
        }

        @Test
        @DisplayName("해당 타입의 알림이 없으면 0을 반환한다")
        fun `타입별 전체 읽음 처리 - 해당 타입 없음`() {
            notificationRepository.save(
                Notification.create(
                    type = NotificationType.FEED_COMMENT,
                    senderId = senderId,
                    receiverId = receiverId,
                    content = "댓글 알림"
                )
            )

            val updatedCount = notificationRepository.markAllAsReadByReceiverIdAndTypeIn(
                receiverId,
                listOf(NotificationType.FEED_LIKE)
            )

            assertThat(updatedCount).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("delete 메서드")
    inner class Delete {

        @Test
        @DisplayName("알림을 삭제할 수 있다")
        fun `삭제 - 정상 케이스`() {
            val saved = notificationRepository.save(createNotification())

            notificationRepository.delete(saved)

            val found = notificationRepository.findById(saved.id!!)
            assertThat(found).isNull()
            flushAndClear()
            assertThat(notificationRepository.findById(saved.id!!)).isNull()
        }
    }

    @Nested
    @DisplayName("deleteAllByReceiverId 메서드")
    inner class DeleteAllByReceiverId {

        @Test
        @DisplayName("수신자의 모든 알림을 삭제할 수 있다")
        fun `전체 삭제 - 정상 케이스`() {
            repeat(5) {
                notificationRepository.save(createNotification())
            }

            notificationRepository.deleteAllByReceiverId(receiverId)

            val pageable = PageRequest.of(0, 10)
            val page = notificationRepository.findAllByReceiverId(receiverId, pageable)
            assertThat(page.content).isEmpty()
        }

        @Test
        @DisplayName("다른 수신자의 알림은 삭제되지 않는다")
        fun `전체 삭제 - 다른 수신자`() {
            val otherReceiver = createAndSaveMember(
                memberId = "other",
                nickname = "다른유저",
                email = "other@example.com",
                socialId = "other123"
            )
            notificationRepository.save(
                Notification.create(
                    type = NotificationType.FEED_LIKE,
                    senderId = senderId,
                    receiverId = otherReceiver.id,
                    content = "다른 사람 알림"
                )
            )
            notificationRepository.save(createNotification())

            notificationRepository.deleteAllByReceiverId(receiverId)

            val pageable = PageRequest.of(0, 10)
            val otherPage = notificationRepository.findAllByReceiverId(otherReceiver.id, pageable)
            assertThat(otherPage.content).hasSize(1)
        }
    }

    private fun createNotification(): Notification {
        return Notification.create(
            type = NotificationType.FEED_LIKE,
            senderId = senderId,
            receiverId = receiverId,
            content = "테스트 알림 내용"
        )
    }
}
