package com.example.mykku.contract.header

import com.example.mykku.BaseControllerTest
import com.example.mykku.contract.matrix.ExactContentType
import com.example.mykku.contract.matrix.MatrixBodies
import com.example.mykku.contract.matrix.MatrixFixtures
import com.example.mykku.contract.matrix.MatrixLine
import com.example.mykku.contract.support.ContractGolden
import com.example.mykku.contract.support.ContractViolationError
import com.example.mykku.contract.support.SqlFixtureLoader
import io.restassured.RestAssured
import io.restassured.http.ContentType
import io.restassured.response.Response
import io.restassured.specification.RequestSpecification
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.web.servlet.MultipartProperties
import javax.sql.DataSource

@DisplayName("응답 헤더 계약")
class ResponseHeaderContractTest : BaseControllerTest() {

    @Autowired
    private lateinit var dataSource: DataSource

    @Autowired
    private lateinit var multipartProperties: MultipartProperties

    @Value("\${admin.token}")
    private lateinit var adminToken: String

    @BeforeEach
    fun loadMatrixFixtures() {
        SqlFixtureLoader(dataSource).load(MatrixFixtures.MEMBERS)
    }

    @Test
    fun `앱 성공·에러 응답과 어드민 대표 응답의 헤더 이름·Content-Type·X-Request-ID 규칙은 골든과 같다`() {
        assertThat(MatrixFixtures.HEADER_WITHDRAWAL_PKS).`as`("X-Request-ID 변형마다 탈퇴할 회원").hasSameSizeAs(REQUEST_IDS)
        val appLines = (successScenarios() + clientErrorScenarios() + protocolErrorScenarios()).flatMap { scenario ->
            REQUEST_IDS.mapIndexed { index, requestId -> appLine(scenario, index, requestId) }
        }

        val lines = appLines + adminScenarios().map { adminLine(it) }

        val golden = HeaderGoldens.path(HeaderGoldens.RESPONSE_HEADERS)
        ContractGolden.DEFAULT.verify(golden, lines.joinToString("\n", postfix = "\n"))
    }

    private fun appLine(scenario: HeaderScenario, index: Int, requestId: RequestId): String {
        val spec = requestId.value?.let { given().header(REQUEST_ID_HEADER, it) } ?: given()
        val variant = "X-Request-ID 요청 ${requestId.label}"
        val summary = summary(scenario.send(spec, index), requestId, "${scenario.label} / $variant")
        return MatrixLine.of(scenario.label, variant, summary)
    }

    private fun summary(response: Response, requestId: RequestId, context: String): String = try {
        HeaderSummary.of(response, requestId.value)
    } catch (violation: ContractViolationError) {
        throw ContractViolationError("$context: ${violation.message}", violation)
    }

    private fun adminLine(scenario: HeaderScenario): String = appLine(scenario, 0, REQUEST_IDS.first())

    private fun successScenarios(): List<HeaderScenario> = listOf(
        HeaderScenario("GET /api/v1/boards") { spec, _ -> spec.get(BOARDS) },
        HeaderScenario("POST /api/v1/blocks/keywords") { spec, index ->
            spec.member().json("""{"keyword":"headerkeyword$index"}""").post("/api/v1/blocks/keywords")
        },
        HeaderScenario("DELETE /api/v1/members/me") { spec, index ->
            spec.header(AUTHORIZATION, getBearerToken(MatrixFixtures.HEADER_WITHDRAWAL_PKS[index])).delete(MY_PROFILE)
        },
        HeaderScenario("GET /api/error-codes") { spec, _ -> spec.get("/api/error-codes") }
    )

    private fun clientErrorScenarios(): List<HeaderScenario> = listOf(
        HeaderScenario("""POST /api/v1/members/check-id {"memberId":"invalid_id!"}""") { spec, _ ->
            spec.json("""{"memberId":"invalid_id!"}""").post(CHECK_ID)
        },
        HeaderScenario("GET /api/v1/members/me (토큰 없음)") { spec, _ -> spec.get(MY_PROFILE) },
        HeaderScenario("GET /api/v1/not-exist") { spec, _ -> spec.get("/api/v1/not-exist") },
        HeaderScenario("GET /api/v1/feeds/999999") { spec, _ -> spec.get("/api/v1/feeds/999999") }
    )

    private fun protocolErrorScenarios(): List<HeaderScenario> = listOf(
        HeaderScenario("PUT /api/v1/members/me/member-id") { spec, _ ->
            spec.member().json("""{"memberId":"matrixchanged"}""").put("/api/v1/members/me/member-id")
        },
        HeaderScenario("GET /api/v1/boards Accept: application/xml") { spec, _ ->
            spec.accept(ExactContentType.APPLICATION_XML).get(BOARDS)
        },
        HeaderScenario("POST /api/v1/feeds images 파일 1개가 max-file-size + 1바이트") { spec, _ -> oversizedFeed(spec) },
        HeaderScenario("POST /api/v1/members/check-id Content-Type: text/plain") { spec, _ ->
            spec.config(ExactContentType.CONFIG).contentType(ExactContentType.TEXT_PLAIN).body("memberId=matrix")
                .post(CHECK_ID)
        }
    )

    private fun adminScenarios(): List<HeaderScenario> = listOf(
        HeaderScenario("admin GET /admin/login") { spec, _ -> spec.get("/admin/login") },
        HeaderScenario("admin GET /admin/api/v1/roles (세션 없음)") { spec, _ -> spec.get("/admin/api/v1/roles") },
        HeaderScenario("admin POST /admin/api/login (올바른 관리자 토큰)") { spec, _ ->
            spec.formParam("token", adminToken).redirects().follow(false).post("/admin/api/login")
        }
    )

    private fun oversizedFeed(spec: RequestSpecification): Response {
        val image = ByteArray(multipartProperties.maxFileSize.toBytes().toInt() + 1)
        return spec.member().multiPart("request", MatrixBodies.FEED_CREATE_JSON, "application/json")
            .multiPart("images", "header.jpg", image, "image/jpeg").post("/api/v1/feeds")
    }

    private fun given(): RequestSpecification = RestAssured.given()

    private fun RequestSpecification.member(): RequestSpecification =
        header(AUTHORIZATION, getBearerToken(MatrixFixtures.MEMBER_PK))

    private fun RequestSpecification.json(body: String): RequestSpecification = contentType(ContentType.JSON).body(body)

    private class HeaderScenario(val label: String, val send: (RequestSpecification, Int) -> Response)

    private class RequestId(val label: String, val value: String?)

    private companion object {
        const val REQUEST_ID_HEADER = "X-Request-ID"
        const val AUTHORIZATION = "Authorization"
        const val BOARDS = "/api/v1/boards"
        const val MY_PROFILE = "/api/v1/members/me"
        const val CHECK_ID = "/api/v1/members/check-id"
        val REQUEST_IDS = listOf(
            RequestId("없음", null),
            RequestId("abc-123", "abc-123"),
            RequestId("ASCII 100자", "r".repeat(100)),
            RequestId("빈 값", ""),
            RequestId("공백 포함 'abc def'", "abc def")
        )
    }
}
