package com.example.mykku.contract.snapshot

import com.example.mykku.contract.snapshot.SnapshotFixtures.EMAIL
import com.example.mykku.contract.snapshot.SnapshotFixtures.GOOGLE
import com.example.mykku.contract.snapshot.SnapshotFixtures.INCOMPLETE_MEMBER
import com.example.mykku.contract.snapshot.SnapshotFixtures.LEAVING_EMAIL
import com.example.mykku.contract.snapshot.SnapshotFixtures.LEAVING_MEMBER
import com.example.mykku.contract.snapshot.SnapshotFixtures.PASSWORD
import io.restassured.response.Response
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@DisplayName("실경로 스냅샷: 인증·회원")
class AuthMemberSnapshotTest : ContractSnapshotTest() {

    override val fixtures = listOf(SnapshotFixtures.BASE)

    @ParameterizedTest(name = "{0}")
    @MethodSource("cases")
    fun `실경로 응답은 골든과 같다`(case: SnapshotCase) = verify(case)

    companion object {
        private const val ME = "/api/v1/members/me"
        private const val NO_CONTENT = 204
        private val TOKENS = listOf("/data/accessToken", "/data/refreshToken")
        private val TOKEN_MASKS = SnapshotRules.masks(*TOKENS.map { SnapshotMask.jwt(it) }.toTypedArray())

        @JvmStatic
        fun cases(): List<SnapshotCase> = auth() + member()

        private fun auth(): List<SnapshotCase> = listOf(
            SnapshotCase("auth/login-email", TOKEN_MASKS, TOKENS) { emailLogin(EMAIL, PASSWORD) },
            SnapshotCase("auth/login-email-wrong-password") { emailLogin(EMAIL, "wrong-password-1!") },
            SnapshotCase("auth/refresh", TOKEN_MASKS, TOKENS) { refresh(login(EMAIL, PASSWORD).refreshToken) },
            SnapshotCase("auth/refresh-with-access-token") { refresh(login(EMAIL, PASSWORD).accessToken) },
            SnapshotCase("auth/refresh-invalid-token") { refresh("invalid-token") },
            SnapshotCase("auth/refresh-withdrawn-member") { refreshAfterWithdrawal() }
        )

        private fun member(): List<SnapshotCase> = listOf(
            SnapshotCase("member/me") { get(ME, GOOGLE) },
            SnapshotCase("member/me-profile-incomplete") { get(ME, Caller.Member(INCOMPLETE_MEMBER)) },
            SnapshotCase("member/check-id-available") { checkId("snapnewid") },
            SnapshotCase("member/check-id-duplicate") { checkId("snapgoogle") }
        )

        private fun SnapshotSession.emailLogin(email: String, password: String): Response =
            send("POST", "/api/v1/email-auth/login", json = mapOf("email" to email, "password" to password))

        private fun SnapshotSession.refresh(token: String): Response =
            send("POST", "/api/v1/auth/refresh", json = mapOf("refreshToken" to token))

        private fun SnapshotSession.checkId(memberId: String): Response =
            send("POST", "/api/v1/members/check-id", json = mapOf("memberId" to memberId))

        private fun SnapshotSession.refreshAfterWithdrawal(): Response {
            val tokens = login(LEAVING_EMAIL, PASSWORD)
            requireStatus(NO_CONTENT, send("DELETE", ME, Caller.Member(LEAVING_MEMBER)))
            return refresh(tokens.refreshToken)
        }
    }
}
