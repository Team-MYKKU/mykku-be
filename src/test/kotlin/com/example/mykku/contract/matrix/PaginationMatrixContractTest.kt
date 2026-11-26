package com.example.mykku.contract.matrix

import com.example.mykku.contract.support.Endpoint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.core.ResolvableType

@DisplayName("페이지 매트릭스 계약")
class PaginationMatrixContractTest : BaseMatrixContractTest() {

    override val fixtures = listOf(MatrixFixtures.MEMBERS, MatrixFixtures.PARENTS)

    @Test
    fun `앱 페이지 엔드포인트의 page·size 기본값·경계·형식 오류 응답은 골든과 같다`() {
        assertThat(appEndpoints.filter { returnsSpringPage(it) }.map { MatrixRules.keyOf(it) })
            .`as`("PageImpl 키 순서 정규화 대상은 Spring Page를 그대로 반환하는 앱 엔드포인트와 같다")
            .containsExactlyInAnyOrderElementsOf(MatrixRules.PAGE_IMPL_ENDPOINTS)
        val authorization = memberAuthorization()

        val lines = appEndpoints.filter { it.paged }.flatMap { endpoint ->
            val request = parentRequest(endpoint).copy(authorization = authorization)
            PAGE_VARIANTS.map { variant -> requestLine(endpoint, variant.applyTo(request)) }
        }

        verify(MatrixGoldens.PAGINATION, lines)
    }

    private fun parentRequest(endpoint: Endpoint): MatrixRequest =
        MatrixRequests.of(endpoint) { MatrixFixtures.parentId(it.name) }

    private fun returnsSpringPage(endpoint: Endpoint): Boolean =
        ResolvableType.forMethodReturnType(endpoint.handlerMethod.method).toString().contains(SPRING_PAGE)

    private class PageVariant(private val parameters: List<Pair<String, String>>) {
        fun applyTo(request: MatrixRequest): MatrixRequest =
            parameters.fold(request) { current, parameter -> current.withQuery(parameter.first, parameter.second) }
    }

    private companion object {
        const val SPRING_PAGE = "org.springframework.data.domain.Page<"
        val PAGE_VARIANTS = listOf(
            PageVariant(emptyList()),
            PageVariant(listOf("page" to "-1")),
            PageVariant(listOf("size" to "0")),
            PageVariant(listOf("size" to "1001")),
            PageVariant(listOf("page" to "abc")),
            PageVariant(listOf("size" to "1000"))
        )
    }
}
