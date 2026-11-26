package com.example.mykku.contract.snapshot

import com.example.mykku.contract.snapshot.SnapshotFixtures.EMAIL
import com.example.mykku.contract.snapshot.SnapshotFixtures.EMAIL_MEMBER
import com.example.mykku.contract.snapshot.SnapshotFixtures.GOOGLE
import com.example.mykku.contract.snapshot.SnapshotFixtures.PASSWORD
import com.example.mykku.contract.snapshot.SnapshotFixtures.jsonPart
import com.example.mykku.contract.snapshot.SnapshotMask.Companion.jvmTime
import com.example.mykku.contract.snapshot.SnapshotMask.Companion.jwt
import com.example.mykku.contract.snapshot.SnapshotFixtures.verifiedSignup
import io.restassured.response.Response
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@DisplayName("실경로 스냅샷: 지금 500·버그 동작 gray-zone")
class GrayZoneSnapshotTest : ContractSnapshotTest(), GrayZoneSession {

    override val fixtures = emptyList<String>()

    override val memo: MutableMap<String, String> = mutableMapOf()

    @ParameterizedTest(name = "{0}")
    @MethodSource("cases")
    fun `지금 동작은 gray-zone 골든과 같다`(case: GrayZoneCase) {
        loadFixtures(case.fixtures)
        val snapshots = case.steps.map { step -> render(step.call(this), step.rules) }

        golden.verify(SnapshotGoldens.grayZone(case.golden), case.header + snapshots.joinToString("\n"))
    }

    companion object {
        private val CREATED_AT = SnapshotRules.masks(jvmTime("/data/createdAt"))
        private val PASSWORD_72 = "Aa1!" + "a".repeat(68)
        private val PASSWORD_73 = "Aa1!" + "a".repeat(69)
        private val EMAIL_CALLER = Caller.Member(EMAIL_MEMBER)
        private const val CODE = "code"
        private const val RESET = "PASSWORD_RESET"
        private const val OVERFLOW_PAGE_SIZE = 1000
        private const val LARGEST_SAFE_PAGE = 2_147_483
        private const val FIRST_OVERFLOW_PAGE = 2_147_484
        private const val WRAPPED_TO_ZERO_PAGE = 16_777_216
        private const val WRAPPED_TO_ZERO_SIZE = 256
        private val PREFERENCES = listOf(
            Triple("genre", "genreTypes", "KPOP"),
            Triple("goods", "goodsTypes", "PHOTOCARD_HOLDER"),
            Triple("mood", "moodTypes", "COZY")
        )

        @JvmStatic
        fun cases(): List<GrayZoneCase> = listOf(
            pageOffsetOverflow(), passwordOverLimit(), feedTagsNullElement(), notificationIdNotPositive(),
            duplicateEmail(), fcmTokenDuplicate(), preferenceNullElement(), feedTitleTooLong(),
            awardFeedsPageOverflow(), duplicateWinnerRows()
        )

        private fun pageOffsetOverflow(): GrayZoneCase = case(
            "bug-11-page-offset-overflow", "BUG-11",
            "page×size가 Int 최대값을 넘는 목록 요청(지금 500 C402, 바로 아래 page는 200)",
            overflowSteps("/api/v1/fan-notes", emptyMap(), SnapshotRules.PAGE) +
                overflowSteps("/api/v1/feeds/1/comments", emptyMap()) +
                overflowSteps("/api/v1/boards/1/feeds", emptyMap()) +
                overflowSteps("/api/v1/search/feeds/boards/1", mapOf("keyword" to "스냅샷"))
        )

        private fun passwordOverLimit(): GrayZoneCase = case(
            "bug-16-password-over-72-bytes", "BUG-16",
            "72바이트를 넘는 비밀번호(지금 500 C402, 재설정은 코드를 먼저 폐기해 같은 코드 재시도는 400 EM101)",
            listOf(
                GrayZoneStep(SnapshotRules.masks(jwt("/data/accessToken"), jwt("/data/refreshToken"))) {
                    verifiedSignup("snap-b16-72@example.com", PASSWORD_72)
                },
                GrayZoneStep { verifiedSignup("snap-b16-73@example.com", PASSWORD_73) },
                GrayZoneStep { resetPassword(issuedVerificationCode(EMAIL, RESET).also { memo[CODE] = it }) },
                GrayZoneStep { resetPassword(memo.getValue(CODE)) },
                GrayZoneStep { changePassword(PASSWORD_73) }
            )
        )

        private fun feedTagsNullElement(): GrayZoneCase = case(
            "bug-19-feed-tags-null-element", "BUG-19", "피드 tags 배열의 null 원소(지금 500 C402)",
            listOf(
                GrayZoneStep { createFeed(feed("널 태그", listOf("덕질", null))) },
                GrayZoneStep { updateFeed(mapOf("tags" to listOf("덕질", null))) }
            )
        )

        private fun notificationIdNotPositive(): GrayZoneCase = case(
            "bug-33-notification-id-not-positive", "BUG-33", "0 이하 알림 ID(지금 500 C402)",
            listOf(
                GrayZoneStep { send("PATCH", "/api/v1/notifications/0/read", GOOGLE) },
                GrayZoneStep { send("DELETE", "/api/v1/notifications/-1", GOOGLE) }
            )
        )

        private fun duplicateEmail(): GrayZoneCase = case(
            "bug-02-duplicate-email", "BUG-02", "같은 이메일 회원 2명(지금 이메일 로그인·임시 비밀번호 발급 500 C402)",
            listOf(
                GrayZoneStep { send("POST", EMAIL_LOGIN, json = mapOf("email" to EMAIL, "password" to PASSWORD)) },
                GrayZoneStep { send("POST", TEMPORARY_PASSWORD, json = mapOf("email" to EMAIL)) }
            ),
            "snapshot-gray-duplicate-email.sql"
        )

        private fun fcmTokenDuplicate(): GrayZoneCase = case(
            "bug-31-fcm-token-duplicate", "BUG-31",
            "같은 FCM 토큰을 다른 deviceId로 등록(지금 중복 행이 생긴 뒤 등록 500 C402)",
            listOf(
                GrayZoneStep(CREATED_AT) { registerFcm("snap-device-1", "ANDROID") },
                GrayZoneStep(CREATED_AT) { registerFcm("snap-device-2", null) },
                GrayZoneStep { registerFcm("snap-device-3", null) },
                GrayZoneStep(SnapshotRules.masks(jvmTime("/data/1/createdAt"), jvmTime("/data/2/createdAt"))) {
                    get("/api/v1/fcm-tokens", GOOGLE)
                }
            )
        )

        private fun preferenceNullElement(): GrayZoneCase = case(
            "bug-35-preference-null-element", "BUG-35", "취향 배열의 null 원소(지금 500 C402)",
            PREFERENCES.map { (kind, field, value) ->
                GrayZoneStep {
                    send("POST", "/api/v1/preferences/$kind", EMAIL_CALLER, mapOf(field to listOf(value, null)))
                }
            }
        )

        private fun feedTitleTooLong(): GrayZoneCase = case(
            "bug-37-feed-title-too-long", "BUG-37", "256자 피드 제목(지금 409 C302, 255자는 200)",
            listOf(
                GrayZoneStep(CREATED_AT) { createFeed(feed("가".repeat(255))) },
                GrayZoneStep { createFeed(feed("가".repeat(256))) },
                GrayZoneStep { updateFeed(mapOf("title" to "가".repeat(256))) }
            )
        )

        private fun awardFeedsPageOverflow(): GrayZoneCase = case(
            "bug-45-award-feeds-page-overflow", "BUG-45",
            "수상 이력이 있는 회원의 내 수상 피드에 큰 page(지금 500 C402, Int 래핑된 첫 페이지, 빈 목록에 hasNext=true)",
            listOf(
                FIRST_OVERFLOW_PAGE to OVERFLOW_PAGE_SIZE,
                WRAPPED_TO_ZERO_PAGE to WRAPPED_TO_ZERO_SIZE,
                LARGEST_SAFE_PAGE to OVERFLOW_PAGE_SIZE
            ).map { (page, size) ->
                GrayZoneStep { get("/api/v1/contests/my-awards/feeds", GOOGLE, mapOf("page" to page, "size" to size)) }
            }
        )

        private fun duplicateWinnerRows(): GrayZoneCase = case(
            "bug-59-duplicate-winner-rows", "BUG-59", "같은 회원의 같은 콘테스트 수상 행 2건(지금 my-winner-status 500 C402)",
            listOf(GrayZoneStep { get("/api/v1/contests/1/my-winner-status", GOOGLE) }),
            "snapshot-gray-duplicate-winner.sql"
        )

        private fun case(
            golden: String,
            reportId: String,
            summary: String,
            steps: List<GrayZoneStep>,
            extraFixture: String? = null
        ): GrayZoneCase =
            GrayZoneCase(golden, reportId, summary, listOfNotNull(SnapshotFixtures.BASE, extraFixture), steps)

        private fun overflowSteps(
            path: String,
            query: Map<String, Any>,
            safePageRules: SnapshotRules = SnapshotRules.NONE
        ): List<GrayZoneStep> =
            listOf(LARGEST_SAFE_PAGE to safePageRules, FIRST_OVERFLOW_PAGE to SnapshotRules.NONE).map { (page, rules) ->
                GrayZoneStep(rules) { get(path, query = query + mapOf("page" to page, "size" to OVERFLOW_PAGE_SIZE)) }
            }

        private const val EMAIL_LOGIN = "/api/v1/email-auth/login"
        private const val TEMPORARY_PASSWORD = "/api/v1/email-auth/send-temporary-password"

        private fun feed(title: String, tags: List<String?>? = null): Map<String, Any?> {
            val request = mapOf("title" to title, "content" to "gray-zone 본문", "boardId" to 1)
            return if (tags == null) request else request + ("tags" to tags)
        }

        private fun SnapshotSession.resetPassword(code: String): Response = send(
            "POST", "/api/v1/email-auth/reset-password",
            json = mapOf("email" to EMAIL, "code" to code, "newPassword" to PASSWORD_73)
        )

        private fun SnapshotSession.changePassword(newPassword: String): Response = send(
            "PUT", "/api/v1/members/password", EMAIL_CALLER,
            mapOf("currentPassword" to PASSWORD, "newPassword" to newPassword)
        )

        private fun SnapshotSession.createFeed(request: Map<String, Any?>): Response =
            multipart("POST", "/api/v1/feeds", GOOGLE, listOf(jsonPart("request", request)))

        private fun SnapshotSession.updateFeed(request: Map<String, Any?>): Response =
            multipart("PATCH", "/api/v1/feeds/1", GOOGLE, listOf(jsonPart("request", request)))

        private fun SnapshotSession.registerFcm(deviceId: String, deviceType: String?): Response = send(
            "POST", "/api/v1/fcm-tokens", GOOGLE,
            mapOf("token" to "snap-fcm-token", "deviceId" to deviceId, "deviceType" to deviceType)
        )
    }
}
