package com.example.mykku.contract.matrix

import com.example.mykku.BaseControllerTest
import com.example.mykku.contract.support.AuthLevel
import com.example.mykku.contract.support.ContractGolden
import com.example.mykku.contract.support.Endpoint
import com.example.mykku.contract.support.EndpointArea
import com.example.mykku.contract.support.HandlerCatalog
import com.example.mykku.contract.support.SqlFixtureLoader
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import javax.sql.DataSource

abstract class BaseMatrixContractTest : BaseControllerTest() {

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private lateinit var handlerMapping: RequestMappingHandlerMapping

    @Autowired
    private lateinit var handlerAdapter: RequestMappingHandlerAdapter

    @Autowired
    private lateinit var dataSource: DataSource

    protected abstract val fixtures: List<String>

    protected val appEndpoints: List<Endpoint> by lazy {
        HandlerCatalog(handlerMapping, handlerAdapter).inArea(EndpointArea.APP)
    }

    @BeforeEach
    fun loadMatrixFixtures() {
        SqlFixtureLoader(dataSource).load(*fixtures.toTypedArray())
    }

    protected fun endpointsWith(auth: AuthLevel): List<Endpoint> = appEndpoints.filter { it.auth == auth }

    protected fun memberAuthorization(): String = getBearerToken(MatrixFixtures.MEMBER_PK)

    protected fun outcome(endpoint: Endpoint, request: MatrixRequest, context: String = request.describe()): String {
        val response = request.send()
        return MatrixLine.outcome(response, MatrixRules.of(endpoint, response), context)
    }

    protected fun requestLine(endpoint: Endpoint, request: MatrixRequest): String =
        MatrixLine.of(request.describe(), outcome(endpoint, request))

    protected fun excludedLine(endpoint: Endpoint): String =
        MatrixLine.of(EXCLUDED, "${endpoint.method} ${endpoint.pattern}", ERROR_CODE_GOLDEN)

    protected fun isErrorCodeApi(endpoint: Endpoint): Boolean = endpoint.pattern.startsWith(ERROR_CODE_API)

    protected fun verify(fileName: String, lines: List<String>) {
        ContractGolden.DEFAULT.verify(MatrixGoldens.path(fileName), lines.joinToString("\n", postfix = "\n"))
    }

    private companion object {
        const val EXCLUDED = "excluded"
        const val ERROR_CODE_API = "/api/error-codes"
        const val ERROR_CODE_GOLDEN = "wire/error-codes 골든이 고정"
    }
}
