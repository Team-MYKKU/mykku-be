package com.example.mykku.contract.matrix

import com.example.mykku.contract.support.Endpoint
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("미존재 매트릭스 계약")
class NotFoundMatrixContractTest : BaseMatrixContractTest() {

    override val fixtures = listOf(MatrixFixtures.MEMBERS)

    @Test
    fun `경로 변수에 없는 값 999999를 넣은 앱 엔드포인트의 응답은 골든과 같다`() {
        val authorization = memberAuthorization()

        val lines = appEndpoints.filter { it.pathArguments.isNotEmpty() }.map { endpoint ->
            if (isErrorCodeApi(endpoint)) excludedLine(endpoint) else line(endpoint, authorization)
        }

        verify(MatrixGoldens.NOT_FOUND, lines)
    }

    private fun line(endpoint: Endpoint, authorization: String): String {
        val request = MatrixRequests.of(endpoint) { MatrixFixtures.ABSENT_ID }
        return requestLine(endpoint, request.copy(authorization = authorization))
    }
}
