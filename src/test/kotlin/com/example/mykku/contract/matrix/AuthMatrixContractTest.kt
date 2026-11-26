package com.example.mykku.contract.matrix

import com.example.mykku.contract.support.AuthLevel
import com.example.mykku.contract.support.Endpoint
import com.example.mykku.contract.support.LoginTokens
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("인증 매트릭스 계약")
class AuthMatrixContractTest : BaseMatrixContractTest() {

    override val fixtures = listOf(MatrixFixtures.MEMBERS, MatrixFixtures.PARENTS, MatrixFixtures.FAN_NOTE)

    @Test
    fun `인증 필수·선택 엔드포인트의 토큰 변형 응답과 회원보다 먼저 받는 인자의 우선순위는 골든과 같다`() {
        assertThat(statusOfMyProfile(memberAuthorization())).`as`("토큰 변형의 원본 액세스 토큰은 유효하다").isEqualTo(200)
        assertThat(statusOfMyProfile(refreshAuthorization())).`as`("리프레시 토큰으로는 회원을 조회할 수 없다").isEqualTo(401)
        val variants = tokenVariants()

        val lines = requiredLines(variants) + optionalLines(variants) + precedenceLines()

        verify(MatrixGoldens.AUTH, lines)
    }

    private fun tokenVariants(): List<TokenVariant> {
        val access = memberAuthorization()
        return listOf(
            TokenVariant(ANONYMOUS, null),
            TokenVariant("bearer-x", "Bearer x"),
            TokenVariant("lowercase-bearer", access.replaceFirst(BEARER_PREFIX, "bearer ")),
            TokenVariant("double-space", access.replaceFirst(BEARER_PREFIX, "Bearer  ")),
            TokenVariant("refresh-token", refreshAuthorization()),
            TokenVariant("withdrawn-member", withdrawnMemberAuthorization())
        )
    }

    private fun refreshAuthorization(): String =
        BEARER_PREFIX + LoginTokens.emailLogin(MatrixFixtures.LOGIN_EMAIL, MatrixFixtures.LOGIN_PASSWORD).refreshToken

    private fun withdrawnMemberAuthorization(): String {
        val authorization = getBearerToken(MatrixFixtures.WITHDRAWN_PK)
        val withdrawal = MatrixRequest(DELETE, MY_PROFILE, authorization = authorization).send()
        assertThat(withdrawal.statusCode).`as`("탈퇴 응답 상태").isEqualTo(204)
        return authorization
    }

    private fun requiredLines(variants: List<TokenVariant>): List<String> =
        endpointsWith(AuthLevel.REQUIRED).flatMap { endpoint ->
            val request = MatrixRequests.of(endpoint) { MatrixFixtures.ABSENT_ID }
            variants.map { line(REQUIRED, it.label, endpoint, request.copy(authorization = it.authorization)) }
        }

    private fun optionalLines(variants: List<TokenVariant>): List<String> =
        endpointsWith(AuthLevel.OPTIONAL).flatMap { endpoint ->
            val request = MatrixRequests.of(endpoint) { MatrixFixtures.parentId(it.name) }
            val outcomes = variants.associateWith { optionalOutcome(endpoint, request, it) }
            val anonymous = outcomes.getValue(variants.single { it.authorization == null })
            outcomes.map { (variant, outcome) ->
                optionalLine(variant, request, againstAnonymous(variant, outcome, anonymous))
            }
        }

    private fun optionalOutcome(endpoint: Endpoint, request: MatrixRequest, variant: TokenVariant): String {
        val context = "$OPTIONAL ${variant.label} ${request.describe()}"
        return outcome(endpoint, request.copy(authorization = variant.authorization), context)
    }

    private fun optionalLine(variant: TokenVariant, request: MatrixRequest, outcome: String): String =
        MatrixLine.of(OPTIONAL, variant.label, request.describe(), outcome)

    private fun againstAnonymous(variant: TokenVariant, outcome: String, anonymous: String): String =
        if (variant.authorization != null && outcome == anonymous) "= $ANONYMOUS" else outcome

    private fun precedenceLines(): List<String> = endpointsWith(AuthLevel.REQUIRED).flatMap { endpoint ->
        MatrixRequests.precedenceCases(endpoint).map { (label, request) -> line(PRECEDENCE, label, endpoint, request) }
    }

    private fun line(section: String, label: String, endpoint: Endpoint, request: MatrixRequest): String {
        val context = "$section $label ${request.describe()}"
        return MatrixLine.of(section, label, request.describe(), outcome(endpoint, request, context))
    }

    private fun statusOfMyProfile(authorization: String): Int =
        MatrixRequest(GET, MY_PROFILE, authorization = authorization).send().statusCode

    private data class TokenVariant(val label: String, val authorization: String?)

    private companion object {
        const val REQUIRED = "required"
        const val OPTIONAL = "optional"
        const val PRECEDENCE = "precedence"
        const val ANONYMOUS = "none"
        const val BEARER_PREFIX = "Bearer "
        const val GET = "GET"
        const val DELETE = "DELETE"
        const val MY_PROFILE = "/api/v1/members/me"
    }
}
