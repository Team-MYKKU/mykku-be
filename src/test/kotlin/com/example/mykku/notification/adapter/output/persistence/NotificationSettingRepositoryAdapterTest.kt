package com.example.mykku.notification.adapter.output.persistence

import com.example.mykku.BaseRepositoryTest
import com.example.mykku.notification.application.port.output.NotificationSettingRepository
import com.example.mykku.notification.domain.entity.NotificationSetting
import com.example.mykku.notification.domain.vo.NotificationSettingId
import com.example.mykku.notification.domain.vo.NotificationType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@DisplayName("NotificationSettingRepositoryAdapter 통합 테스트")
class NotificationSettingRepositoryAdapterTest : BaseRepositoryTest() {

    @Autowired
    private lateinit var notificationSettingRepository: NotificationSettingRepository

    private var memberId: Long = 0

    @BeforeEach
    fun setUp() {
        val member = createAndSaveMember(
            memberId = "ns_member",
            nickname = "테스트유저",
            email = "test@example.com",
            socialId = "test123"
        )
        memberId = member.id
    }

    @Nested
    @DisplayName("save 메서드")
    inner class Save {

        @Test
        @DisplayName("알림 설정을 저장하면 ID가 부여된다")
        fun `설정 저장 - 정상 케이스`() {
            val setting = createNotificationSetting()

            val saved = notificationSettingRepository.save(setting)

            assertThat(saved.id).isNotNull()
            assertThat(saved.id!!.value).isGreaterThan(0)
            assertThat(saved.memberId).isEqualTo(memberId)
            assertThat(saved.notificationType).isEqualTo(NotificationType.FEED_LIKE)
            assertThat(saved.isEnabled).isTrue()
        }

        @Test
        @DisplayName("비활성화된 알림 설정을 저장할 수 있다")
        fun `설정 저장 - 비활성화 상태`() {
            val setting = NotificationSetting.create(
                memberId = memberId,
                notificationType = NotificationType.FEED_COMMENT,
                isEnabled = false
            )

            val saved = notificationSettingRepository.save(setting)

            assertThat(saved.isEnabled).isFalse()
        }

        @Test
        @DisplayName("설정을 업데이트할 수 있다")
        fun `설정 저장 - 업데이트`() {
            val saved = notificationSettingRepository.save(createNotificationSetting())
            val domain = notificationSettingRepository.findById(saved.id!!)!!
            domain.disable()

            val updated = notificationSettingRepository.save(domain)
            flushAndClear()

            assertThat(updated.isEnabled).isFalse()
            val reloaded = notificationSettingRepository.findById(saved.id!!)!!
            assertThat(reloaded.isEnabled).isFalse()
            assertThat(reloaded.notificationType).isEqualTo(NotificationType.FEED_LIKE)
        }
    }

    @Nested
    @DisplayName("saveAll 메서드")
    inner class SaveAll {

        @Test
        @DisplayName("여러 알림 설정을 한 번에 저장할 수 있다")
        fun `일괄 저장 - 정상 케이스`() {
            val settings = NotificationSetting.createDefaultSettings(memberId)

            val savedList = notificationSettingRepository.saveAll(settings)

            assertThat(savedList).hasSize(NotificationType.entries.size)
            savedList.forEach { setting ->
                assertThat(setting.id).isNotNull()
                assertThat(setting.memberId).isEqualTo(memberId)
                assertThat(setting.isEnabled).isTrue()
            }
        }

        @Test
        @DisplayName("빈 리스트를 저장하면 빈 리스트를 반환한다")
        fun `일괄 저장 - 빈 리스트`() {
            val savedList = notificationSettingRepository.saveAll(emptyList())

            assertThat(savedList).isEmpty()
        }
    }

    @Nested
    @DisplayName("findById 메서드")
    inner class FindById {

        @Test
        @DisplayName("ID로 알림 설정을 조회할 수 있다")
        fun `ID로 조회 - 정상 케이스`() {
            val saved = notificationSettingRepository.save(createNotificationSetting())

            val found = notificationSettingRepository.findById(saved.id!!)

            assertThat(found).isNotNull()
            assertThat(found!!.id).isEqualTo(saved.id)
            assertThat(found.notificationType).isEqualTo(NotificationType.FEED_LIKE)
        }

        @Test
        @DisplayName("존재하지 않는 ID로 조회하면 null을 반환한다")
        fun `ID로 조회 - 존재하지 않는 ID`() {
            val found = notificationSettingRepository.findById(NotificationSettingId(999999L))

            assertThat(found).isNull()
        }
    }

    @Nested
    @DisplayName("findAllByMemberId 메서드")
    inner class FindAllByMemberId {

        @Test
        @DisplayName("회원의 모든 알림 설정을 조회할 수 있다")
        fun `회원별 조회 - 정상 케이스`() {
            notificationSettingRepository.saveAll(
                NotificationSetting.createDefaultSettings(memberId)
            )

            val settings = notificationSettingRepository.findAllByMemberId(memberId)

            assertThat(settings).hasSize(NotificationType.entries.size)
        }

        @Test
        @DisplayName("다른 회원의 설정은 조회되지 않는다")
        fun `회원별 조회 - 다른 회원`() {
            notificationSettingRepository.save(createNotificationSetting())
            val otherMember = createAndSaveMember(
                memberId = "ns_other",
                nickname = "다른유저",
                email = "other@example.com",
                socialId = "other123"
            )

            val settings = notificationSettingRepository.findAllByMemberId(otherMember.id)

            assertThat(settings).isEmpty()
        }

        @Test
        @DisplayName("설정이 없으면 빈 리스트를 반환한다")
        fun `회원별 조회 - 설정 없음`() {
            val settings = notificationSettingRepository.findAllByMemberId(memberId)

            assertThat(settings).isEmpty()
        }
    }

    @Nested
    @DisplayName("findByMemberIdAndNotificationType 메서드")
    inner class FindByMemberIdAndNotificationType {

        @Test
        @DisplayName("회원 ID와 알림 타입으로 설정을 조회할 수 있다")
        fun `회원ID_타입으로 조회 - 정상 케이스`() {
            notificationSettingRepository.saveAll(
                NotificationSetting.createDefaultSettings(memberId)
            )

            val found = notificationSettingRepository.findByMemberIdAndNotificationType(
                memberId,
                NotificationType.FEED_LIKE
            )

            assertThat(found).isNotNull()
            assertThat(found!!.memberId).isEqualTo(memberId)
            assertThat(found.notificationType).isEqualTo(NotificationType.FEED_LIKE)
        }

        @Test
        @DisplayName("일치하는 설정이 없으면 null을 반환한다")
        fun `회원ID_타입으로 조회 - 없는 경우`() {
            val found = notificationSettingRepository.findByMemberIdAndNotificationType(
                memberId,
                NotificationType.FEED_LIKE
            )

            assertThat(found).isNull()
        }

        @Test
        @DisplayName("다른 회원의 동일 타입 설정은 조회되지 않는다")
        fun `회원ID_타입으로 조회 - 다른 회원`() {
            notificationSettingRepository.save(createNotificationSetting())
            val otherMember = createAndSaveMember(
                memberId = "ns_other",
                nickname = "다른유저",
                email = "other@example.com",
                socialId = "other123"
            )

            val found = notificationSettingRepository.findByMemberIdAndNotificationType(
                otherMember.id,
                NotificationType.FEED_LIKE
            )

            assertThat(found).isNull()
        }
    }

    @Nested
    @DisplayName("existsByMemberIdAndNotificationType 메서드")
    inner class ExistsByMemberIdAndNotificationType {

        @Test
        @DisplayName("회원 ID와 알림 타입으로 설정 존재 여부를 확인할 수 있다")
        fun `존재 여부 확인 - 존재하는 경우`() {
            notificationSettingRepository.save(createNotificationSetting())

            val exists = notificationSettingRepository.existsByMemberIdAndNotificationType(
                memberId,
                NotificationType.FEED_LIKE
            )

            assertThat(exists).isTrue()
        }

        @Test
        @DisplayName("설정이 없으면 false를 반환한다")
        fun `존재 여부 확인 - 없는 경우`() {
            val exists = notificationSettingRepository.existsByMemberIdAndNotificationType(
                memberId,
                NotificationType.FEED_LIKE
            )

            assertThat(exists).isFalse()
        }

        @Test
        @DisplayName("다른 타입의 설정만 있으면 false를 반환한다")
        fun `존재 여부 확인 - 다른 타입`() {
            notificationSettingRepository.save(
                NotificationSetting.create(
                    memberId = memberId,
                    notificationType = NotificationType.FEED_COMMENT
                )
            )

            val exists = notificationSettingRepository.existsByMemberIdAndNotificationType(
                memberId,
                NotificationType.FEED_LIKE
            )

            assertThat(exists).isFalse()
        }

        @Test
        @DisplayName("다른 회원의 동일 타입 설정은 false를 반환한다")
        fun `존재 여부 확인 - 다른 회원`() {
            notificationSettingRepository.save(createNotificationSetting())
            val otherMember = createAndSaveMember(
                memberId = "ns_other",
                nickname = "다른유저",
                email = "other@example.com",
                socialId = "other123"
            )

            val exists = notificationSettingRepository.existsByMemberIdAndNotificationType(
                otherMember.id,
                NotificationType.FEED_LIKE
            )

            assertThat(exists).isFalse()
        }
    }

    @Nested
    @DisplayName("delete 메서드")
    inner class Delete {

        @Test
        @DisplayName("알림 설정을 삭제할 수 있다")
        fun `삭제 - 정상 케이스`() {
            val saved = notificationSettingRepository.save(createNotificationSetting())

            notificationSettingRepository.delete(saved)

            val found = notificationSettingRepository.findById(saved.id!!)
            assertThat(found).isNull()
            flushAndClear()
            assertThat(notificationSettingRepository.findById(saved.id!!)).isNull()
        }
    }

    @Nested
    @DisplayName("deleteAllByMemberId 메서드")
    inner class DeleteAllByMemberId {

        @Test
        @DisplayName("회원의 모든 알림 설정을 삭제할 수 있다")
        fun `회원별 전체 삭제 - 정상 케이스`() {
            notificationSettingRepository.saveAll(
                NotificationSetting.createDefaultSettings(memberId)
            )

            notificationSettingRepository.deleteAllByMemberId(memberId)

            val remaining = notificationSettingRepository.findAllByMemberId(memberId)
            assertThat(remaining).isEmpty()
        }

        @Test
        @DisplayName("다른 회원의 설정은 삭제되지 않는다")
        fun `회원별 전체 삭제 - 다른 회원`() {
            val otherMember = createAndSaveMember(
                memberId = "ns_other",
                nickname = "다른유저",
                email = "other@example.com",
                socialId = "other123"
            )
            notificationSettingRepository.saveAll(
                NotificationSetting.createDefaultSettings(otherMember.id)
            )
            notificationSettingRepository.saveAll(
                NotificationSetting.createDefaultSettings(memberId)
            )

            notificationSettingRepository.deleteAllByMemberId(memberId)

            val otherSettings = notificationSettingRepository.findAllByMemberId(otherMember.id)
            assertThat(otherSettings).hasSize(NotificationType.entries.size)
        }

        @Test
        @DisplayName("설정이 없는 회원에 대해 삭제해도 에러가 발생하지 않는다")
        fun `회원별 전체 삭제 - 설정 없음`() {
            notificationSettingRepository.deleteAllByMemberId(memberId)

            val remaining = notificationSettingRepository.findAllByMemberId(memberId)
            assertThat(remaining).isEmpty()
        }
    }

    @Nested
    @DisplayName("알림 설정 활성화/비활성화 통합 테스트")
    inner class EnableDisableIntegration {

        @Test
        @DisplayName("저장된 설정을 비활성화하고 다시 조회하면 비활성화 상태이다")
        fun `설정 비활성화 후 조회`() {
            val saved = notificationSettingRepository.save(createNotificationSetting())
            val domain = notificationSettingRepository.findById(saved.id!!)!!
            domain.disable()
            notificationSettingRepository.save(domain)

            val found = notificationSettingRepository.findByMemberIdAndNotificationType(
                memberId,
                NotificationType.FEED_LIKE
            )

            assertThat(found!!.isEnabled).isFalse()
            flushAndClear()
            assertThat(notificationSettingRepository.findById(saved.id!!)!!.isEnabled).isFalse()
        }

        @Test
        @DisplayName("비활성화된 설정을 다시 활성화할 수 있다")
        fun `설정 활성화 후 조회`() {
            val saved = notificationSettingRepository.save(createNotificationSetting(isEnabled = false))
            val domain = notificationSettingRepository.findById(saved.id!!)!!
            domain.enable()
            notificationSettingRepository.save(domain)

            val found = notificationSettingRepository.findByMemberIdAndNotificationType(
                memberId,
                NotificationType.FEED_LIKE
            )

            assertThat(found!!.isEnabled).isTrue()
            flushAndClear()
            assertThat(notificationSettingRepository.findById(saved.id!!)!!.isEnabled).isTrue()
        }
    }

    private fun createNotificationSetting(isEnabled: Boolean = true): NotificationSetting {
        return NotificationSetting.create(
            memberId = memberId,
            notificationType = NotificationType.FEED_LIKE,
            isEnabled = isEnabled
        )
    }
}
