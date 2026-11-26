package com.example.mykku.contract.matrix

import com.example.mykku.contract.support.Endpoint
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("빈 상태 매트릭스 계약")
class EmptyStateMatrixContractTest : BaseMatrixContractTest() {

    override val fixtures = listOf(MatrixFixtures.MEMBERS)

    @Test
    fun `회원만 있는 DB에서 경로 변수 없는 앱 GET 엔드포인트의 응답은 골든과 같다`() {
        val authorization = memberAuthorization()

        val lines = appEndpoints.filter { it.method == GET && it.pathArguments.isEmpty() }.map { endpoint ->
            if (isErrorCodeApi(endpoint)) excludedLine(endpoint) else line(endpoint, authorization)
        }

        verify(MatrixGoldens.EMPTY_STATE, lines)
    }

    private fun line(endpoint: Endpoint, authorization: String): String {
        val request = MatrixRequests.of(endpoint) { error("경로 변수가 없어야 할 엔드포인트입니다: ${endpoint.pattern}") }
        return requestLine(endpoint, request.copy(authorization = authorization))
    }

    private companion object {
        const val GET = "GET"
    }
}
