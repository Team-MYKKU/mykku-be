package com.example.mykku.contract.snapshot

import com.example.mykku.contract.snapshot.SnapshotFixtures.GOOGLE
import com.example.mykku.contract.snapshot.SnapshotFixtures.verifiedSignup
import com.example.mykku.contract.snapshot.SnapshotMask.Companion.dbTime
import io.restassured.response.Response
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@DisplayName("실경로 스냅샷: 덕담·덕질노트·알림·칭호·신고")
class ContentSnapshotTest : ContractSnapshotTest() {

    override val fixtures = listOf(SnapshotFixtures.BASE)

    @ParameterizedTest(name = "{0}")
    @MethodSource("cases")
    fun `실경로 응답은 골든과 같다`(case: SnapshotCase) = verify(case)

    companion object {
        private const val DAILY_MESSAGES = "/api/v1/daily-messages"
        private const val SIGNED_UP_MEMBER = 7L
        private const val OK = 200
        private val PAGE = SnapshotRules.PAGE

        @JvmStatic
        fun cases(): List<SnapshotCase> = dailyMessages() + fanNotesAndNotifications() + rolesAndReports()

        private fun dailyMessages(): List<SnapshotCase> = listOf(
            SnapshotCase("daily-message/list", PAGE) { get(DAILY_MESSAGES, query = mapOf("date" to "2001-01-02")) },
            SnapshotCase("daily-message/list-today", PAGE) {
                get(DAILY_MESSAGES, query = mapOf("date" to fixtureDate))
            },
            SnapshotCase("daily-message/detail") { get("$DAILY_MESSAGES/1") },
            SnapshotCase("daily-message/comments-anonymous") { get("$DAILY_MESSAGES/1/comments") },
            SnapshotCase("daily-message/comments-member") { get("$DAILY_MESSAGES/1/comments", GOOGLE) }
        )

        private fun fanNotesAndNotifications(): List<SnapshotCase> = listOf(
            SnapshotCase("fan-note/list", PAGE) { get("/api/v1/fan-notes") },
            SnapshotCase("fan-note/detail") { get("/api/v1/fan-notes/1") },
            SnapshotCase("notification/list", PAGE) { get("/api/v1/notifications", GOOGLE) },
            SnapshotCase("notification/unread", PAGE) { get("/api/v1/notifications/unread", GOOGLE) },
            SnapshotCase("notification/unread-count") { get("/api/v1/notifications/unread/count", GOOGLE) }
        )

        private fun rolesAndReports(): List<SnapshotCase> = listOf(
            SnapshotCase("role/list") { get("/api/v1/roles", GOOGLE) },
            SnapshotCase("role/mine") { get("/api/v1/roles/me", GOOGLE) },
            SnapshotCase("role/mine-after-signup-award", SnapshotRules.masks(dbTime("/data/0/earnedAt"))) {
                rolesAfterSignup()
            },
            SnapshotCase("report/reasons") { get("/api/v1/reports/reasons") }
        )

        private fun SnapshotSession.rolesAfterSignup(): Response {
            requireStatus(OK, verifiedSignup("snap-new@example.com", "Snapshot1!"))
            return get("/api/v1/roles/me", Caller.Member(SIGNED_UP_MEMBER))
        }
    }
}
