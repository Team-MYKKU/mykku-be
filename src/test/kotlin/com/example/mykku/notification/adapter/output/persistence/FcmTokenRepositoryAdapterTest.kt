package com.example.mykku.notification.adapter.output.persistence

import com.example.mykku.BaseRepositoryTest
import com.example.mykku.notification.application.port.output.FcmTokenRepository
import com.example.mykku.notification.domain.entity.FcmToken
import com.example.mykku.notification.domain.vo.FcmTokenId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@DisplayName("FcmTokenRepositoryAdapter 통합 테스트")
class FcmTokenRepositoryAdapterTest : BaseRepositoryTest() {

    @Autowired
    private lateinit var fcmTokenRepository: FcmTokenRepository

    private var memberId: Long = 0

    @BeforeEach
    fun setUp() {
        val member = createAndSaveMember(
            memberId = "fcm_member",
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
        @DisplayName("FCM 토큰을 저장하면 ID가 부여된다")
        fun `토큰 저장 - 정상 케이스`() {
            val fcmToken = createFcmToken()

            val saved = fcmTokenRepository.save(fcmToken)

            assertThat(saved.id).isNotNull()
            assertThat(saved.id!!.value).isGreaterThan(0)
            assertThat(saved.memberId).isEqualTo(memberId)
            assertThat(saved.token).isEqualTo("test-fcm-token")
            assertThat(saved.deviceId).isEqualTo("device-001")
        }

        @Test
        @DisplayName("deviceType이 null인 토큰을 저장할 수 있다")
        fun `토큰 저장 - deviceType null`() {
            val fcmToken = FcmToken.create(
                memberId = memberId,
                token = "test-token",
                deviceId = "device-001",
                deviceType = null
            )

            val saved = fcmTokenRepository.save(fcmToken)

            assertThat(saved.deviceType).isNull()
        }

        @Test
        @DisplayName("deviceType을 포함한 토큰을 저장할 수 있다")
        fun `토큰 저장 - deviceType 포함`() {
            val fcmToken = FcmToken.create(
                memberId = memberId,
                token = "test-token",
                deviceId = "device-001",
                deviceType = "ANDROID"
            )

            val saved = fcmTokenRepository.save(fcmToken)

            assertThat(saved.deviceType).isEqualTo("ANDROID")
        }
    }

    @Nested
    @DisplayName("findById 메서드")
    inner class FindById {

        @Test
        @DisplayName("ID로 FCM 토큰을 조회할 수 있다")
        fun `ID로 조회 - 정상 케이스`() {
            val saved = fcmTokenRepository.save(createFcmToken())

            val found = fcmTokenRepository.findById(saved.id!!)

            assertThat(found).isNotNull()
            assertThat(found!!.id).isEqualTo(saved.id)
            assertThat(found.token).isEqualTo("test-fcm-token")
        }

        @Test
        @DisplayName("존재하지 않는 ID로 조회하면 null을 반환한다")
        fun `ID로 조회 - 존재하지 않는 ID`() {
            val found = fcmTokenRepository.findById(FcmTokenId(999999L))

            assertThat(found).isNull()
        }
    }

    @Nested
    @DisplayName("findAllByMemberId 메서드")
    inner class FindAllByMemberId {

        @Test
        @DisplayName("회원의 모든 FCM 토큰을 조회할 수 있다")
        fun `회원별 조회 - 정상 케이스`() {
            fcmTokenRepository.save(
                FcmToken.create(
                    memberId = memberId,
                    token = "token-1",
                    deviceId = "device-001"
                )
            )
            fcmTokenRepository.save(
                FcmToken.create(
                    memberId = memberId,
                    token = "token-2",
                    deviceId = "device-002"
                )
            )

            val tokens = fcmTokenRepository.findAllByMemberId(memberId)

            assertThat(tokens).hasSize(2)
        }

        @Test
        @DisplayName("다른 회원의 토큰은 조회되지 않는다")
        fun `회원별 조회 - 다른 회원`() {
            fcmTokenRepository.save(createFcmToken())
            val otherMember = createAndSaveMember(
                memberId = "fcm_other",
                nickname = "다른유저",
                email = "other@example.com",
                socialId = "other123"
            )

            val tokens = fcmTokenRepository.findAllByMemberId(otherMember.id)

            assertThat(tokens).isEmpty()
        }

        @Test
        @DisplayName("토큰이 없으면 빈 리스트를 반환한다")
        fun `회원별 조회 - 토큰 없음`() {
            val tokens = fcmTokenRepository.findAllByMemberId(memberId)

            assertThat(tokens).isEmpty()
        }
    }

    @Nested
    @DisplayName("findByMemberIdAndDeviceId 메서드")
    inner class FindByMemberIdAndDeviceId {

        @Test
        @DisplayName("회원 ID와 디바이스 ID로 토큰을 조회할 수 있다")
        fun `회원ID_디바이스ID로 조회 - 정상 케이스`() {
            fcmTokenRepository.save(createFcmToken())

            val found = fcmTokenRepository.findByMemberIdAndDeviceId(memberId, "device-001")

            assertThat(found).isNotNull()
            assertThat(found!!.memberId).isEqualTo(memberId)
            assertThat(found.deviceId).isEqualTo("device-001")
        }

        @Test
        @DisplayName("일치하는 토큰이 없으면 null을 반환한다")
        fun `회원ID_디바이스ID로 조회 - 없는 경우`() {
            fcmTokenRepository.save(createFcmToken())

            val found = fcmTokenRepository.findByMemberIdAndDeviceId(memberId, "non-existent-device")

            assertThat(found).isNull()
        }

        @Test
        @DisplayName("다른 회원의 동일 디바이스는 조회되지 않는다")
        fun `회원ID_디바이스ID로 조회 - 다른 회원`() {
            fcmTokenRepository.save(createFcmToken())
            val otherMember = createAndSaveMember(
                memberId = "fcm_other",
                nickname = "다른유저",
                email = "other@example.com",
                socialId = "other123"
            )

            val found = fcmTokenRepository.findByMemberIdAndDeviceId(otherMember.id, "device-001")

            assertThat(found).isNull()
        }
    }

    @Nested
    @DisplayName("findByToken 메서드")
    inner class FindByToken {

        @Test
        @DisplayName("토큰 값으로 FCM 토큰을 조회할 수 있다")
        fun `토큰으로 조회 - 정상 케이스`() {
            fcmTokenRepository.save(createFcmToken())

            val found = fcmTokenRepository.findByToken("test-fcm-token")

            assertThat(found).isNotNull()
            assertThat(found!!.token).isEqualTo("test-fcm-token")
        }

        @Test
        @DisplayName("존재하지 않는 토큰으로 조회하면 null을 반환한다")
        fun `토큰으로 조회 - 없는 경우`() {
            val found = fcmTokenRepository.findByToken("non-existent-token")

            assertThat(found).isNull()
        }
    }

    @Nested
    @DisplayName("existsByMemberIdAndDeviceId 메서드")
    inner class ExistsByMemberIdAndDeviceId {

        @Test
        @DisplayName("회원 ID와 디바이스 ID로 토큰 존재 여부를 확인할 수 있다")
        fun `존재 여부 확인 - 존재하는 경우`() {
            fcmTokenRepository.save(createFcmToken())

            val exists = fcmTokenRepository.existsByMemberIdAndDeviceId(memberId, "device-001")

            assertThat(exists).isTrue()
        }

        @Test
        @DisplayName("토큰이 없으면 false를 반환한다")
        fun `존재 여부 확인 - 없는 경우`() {
            val exists = fcmTokenRepository.existsByMemberIdAndDeviceId(memberId, "device-001")

            assertThat(exists).isFalse()
        }

        @Test
        @DisplayName("다른 회원의 동일 디바이스는 false를 반환한다")
        fun `존재 여부 확인 - 다른 회원`() {
            fcmTokenRepository.save(createFcmToken())
            val otherMember = createAndSaveMember(
                memberId = "fcm_other",
                nickname = "다른유저",
                email = "other@example.com",
                socialId = "other123"
            )

            val exists = fcmTokenRepository.existsByMemberIdAndDeviceId(otherMember.id, "device-001")

            assertThat(exists).isFalse()
        }
    }

    @Nested
    @DisplayName("delete 메서드")
    inner class Delete {

        @Test
        @DisplayName("FCM 토큰을 삭제할 수 있다")
        fun `삭제 - 정상 케이스`() {
            val saved = fcmTokenRepository.save(createFcmToken())

            fcmTokenRepository.delete(saved)

            val found = fcmTokenRepository.findById(saved.id!!)
            assertThat(found).isNull()
            flushAndClear()
            assertThat(fcmTokenRepository.findById(saved.id!!)).isNull()
        }
    }

    @Nested
    @DisplayName("deleteByMemberIdAndDeviceId 메서드")
    inner class DeleteByMemberIdAndDeviceId {

        @Test
        @DisplayName("회원 ID와 디바이스 ID로 토큰을 삭제할 수 있다")
        fun `회원ID_디바이스ID로 삭제 - 정상 케이스`() {
            fcmTokenRepository.save(createFcmToken())

            fcmTokenRepository.deleteByMemberIdAndDeviceId(memberId, "device-001")

            val found = fcmTokenRepository.findByMemberIdAndDeviceId(memberId, "device-001")
            assertThat(found).isNull()
        }

        @Test
        @DisplayName("다른 디바이스의 토큰은 삭제되지 않는다")
        fun `회원ID_디바이스ID로 삭제 - 다른 디바이스`() {
            fcmTokenRepository.save(
                FcmToken.create(
                    memberId = memberId,
                    token = "token-1",
                    deviceId = "device-001"
                )
            )
            fcmTokenRepository.save(
                FcmToken.create(
                    memberId = memberId,
                    token = "token-2",
                    deviceId = "device-002"
                )
            )

            fcmTokenRepository.deleteByMemberIdAndDeviceId(memberId, "device-001")

            val remaining = fcmTokenRepository.findAllByMemberId(memberId)
            assertThat(remaining).hasSize(1)
            assertThat(remaining[0].deviceId).isEqualTo("device-002")
        }
    }

    @Nested
    @DisplayName("deleteAllByMemberId 메서드")
    inner class DeleteAllByMemberId {

        @Test
        @DisplayName("회원의 모든 FCM 토큰을 삭제할 수 있다")
        fun `회원별 전체 삭제 - 정상 케이스`() {
            fcmTokenRepository.save(
                FcmToken.create(
                    memberId = memberId,
                    token = "token-1",
                    deviceId = "device-001"
                )
            )
            fcmTokenRepository.save(
                FcmToken.create(
                    memberId = memberId,
                    token = "token-2",
                    deviceId = "device-002"
                )
            )

            fcmTokenRepository.deleteAllByMemberId(memberId)

            val remaining = fcmTokenRepository.findAllByMemberId(memberId)
            assertThat(remaining).isEmpty()
        }

        @Test
        @DisplayName("다른 회원의 토큰은 삭제되지 않는다")
        fun `회원별 전체 삭제 - 다른 회원`() {
            val otherMember = createAndSaveMember(
                memberId = "fcm_other",
                nickname = "다른유저",
                email = "other@example.com",
                socialId = "other123"
            )
            fcmTokenRepository.save(
                FcmToken.create(
                    memberId = otherMember.id,
                    token = "other-token",
                    deviceId = "other-device"
                )
            )
            fcmTokenRepository.save(createFcmToken())

            fcmTokenRepository.deleteAllByMemberId(memberId)

            val otherTokens = fcmTokenRepository.findAllByMemberId(otherMember.id)
            assertThat(otherTokens).hasSize(1)
        }
    }

    @Nested
    @DisplayName("deleteByToken 메서드")
    inner class DeleteByToken {

        @Test
        @DisplayName("토큰 값으로 FCM 토큰을 삭제할 수 있다")
        fun `토큰으로 삭제 - 정상 케이스`() {
            fcmTokenRepository.save(createFcmToken())

            fcmTokenRepository.deleteByToken("test-fcm-token")

            val found = fcmTokenRepository.findByToken("test-fcm-token")
            assertThat(found).isNull()
        }

        @Test
        @DisplayName("다른 토큰은 삭제되지 않는다")
        fun `토큰으로 삭제 - 다른 토큰`() {
            fcmTokenRepository.save(
                FcmToken.create(
                    memberId = memberId,
                    token = "token-1",
                    deviceId = "device-001"
                )
            )
            fcmTokenRepository.save(
                FcmToken.create(
                    memberId = memberId,
                    token = "token-2",
                    deviceId = "device-002"
                )
            )

            fcmTokenRepository.deleteByToken("token-1")

            val remaining = fcmTokenRepository.findAllByMemberId(memberId)
            assertThat(remaining).hasSize(1)
            assertThat(remaining[0].token).isEqualTo("token-2")
        }
    }

    private fun createFcmToken(): FcmToken {
        return FcmToken.create(
            memberId = memberId,
            token = "test-fcm-token",
            deviceId = "device-001",
            deviceType = "IOS"
        )
    }
}
