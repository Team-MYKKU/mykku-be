package com.example.mykku.contract.snapshot

import com.example.mykku.BaseControllerTest
import com.example.mykku.contract.support.ContractGolden
import com.example.mykku.contract.support.ContractJson
import com.example.mykku.contract.support.ContractViolationError
import com.example.mykku.contract.support.HttpSnapshot
import com.example.mykku.contract.support.IssuedTokens
import com.example.mykku.contract.support.JsonPath
import com.example.mykku.contract.support.LoginTokens
import com.example.mykku.contract.support.SqlFixtureLoader
import com.example.mykku.email.tool.RedisVerificationCodeManager
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.restassured.RestAssured
import io.restassured.http.ContentType
import io.restassured.response.Response
import io.restassured.specification.MultiPartSpecification
import io.restassured.specification.RequestSpecification
import org.junit.jupiter.api.BeforeEach
import org.redisson.api.RedissonClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import java.time.LocalDate
import java.time.LocalDateTime
import javax.sql.DataSource

abstract class ContractSnapshotTest : BaseControllerTest(), SnapshotSession, SnapshotClock {

    @Autowired
    private lateinit var dataSource: DataSource

    @Autowired
    protected lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var redissonClient: RedissonClient

    @Autowired
    private lateinit var verificationCodes: RedisVerificationCodeManager

    final override var fixtureDate: LocalDate = LocalDate.now()
        private set

    private var requestLine: String = ""

    protected val golden: ContractGolden = ContractGolden.DEFAULT

    protected abstract val fixtures: List<String>

    @BeforeEach
    fun loadSnapshotFixtures() {
        redissonClient.keys.deleteByPattern(VERIFICATION_KEYS)
        fixtureDate = LocalDate.now()
        loadFixtures(fixtures)
    }

    protected fun loadFixtures(names: List<String>) {
        SqlFixtureLoader(dataSource).load(*names.toTypedArray())
    }

    protected fun verify(case: SnapshotCase) {
        val response = case.call(this)
        val violations = listOfNotNull(
            golden.violationOf(SnapshotGoldens.response(case.golden), render(response, case.rules)),
            claimsOf(case, response)?.let { golden.violationOf(SnapshotGoldens.claims(case.golden), it) }
        )
        if (violations.isNotEmpty()) {
            throw ContractViolationError(violations.joinToString("\n\n"))
        }
    }

    protected fun render(response: Response, rules: SnapshotRules = SnapshotRules.NONE): String =
        HttpSnapshot.of(requestLine, response, rules.bind(this)).render()

    protected fun lastRequestLine(): String = requestLine

    protected fun jsonOf(response: Response): JsonNode =
        ContractJson.parse(ContractJson.decodeUtf8(response.asByteArray()))

    override fun get(path: String, caller: Caller, query: Map<String, Any>): Response {
        val spec = given(caller)
        query.forEach { spec.queryParam(it.key, it.value.toString()) }
        requestLine = withTodayToken("GET $path${RequestTitles.queryString(query)} [${caller.label}]")
        return spec.get(path)
    }

    override fun send(method: String, path: String, caller: Caller, json: Any?): Response {
        val spec = given(caller)
        val body = json?.let { objectMapper.writeValueAsString(it) }
        body?.let { spec.contentType(ContentType.JSON).body(it) }
        requestLine = withTodayToken("$method $path [${caller.label}]" + (body?.let { " ${describe(it)}" } ?: ""))
        return spec.request(method, path)
    }

    override fun multipart(
        method: String,
        path: String,
        caller: Caller,
        parts: List<MultiPartSpecification>
    ): Response {
        val spec = given(caller).contentType(ContentType.MULTIPART)
        parts.forEach { spec.multiPart(it) }
        val described = parts.joinToString(" ") { describe(it) }
        requestLine = withTodayToken("$method $path [${caller.label}] multipart $described")
        return spec.request(method, path)
    }

    override fun login(email: String, password: String): IssuedTokens = LoginTokens.emailLogin(email, password)

    override fun issuedVerificationCode(email: String, purpose: String): String {
        requireStatus(OK, send("POST", SEND_CODE, json = mapOf("email" to email, "purpose" to purpose)))
        return verificationCodes.getVerificationCode(email, purpose)
            ?: throw ContractViolationError("[API 계약] 인증 코드가 Redis에 없습니다: $requestLine")
    }

    override fun requireStatus(status: Int, response: Response): Response {
        if (response.statusCode != status) {
            throw ContractViolationError(
                "[API 계약] 준비 요청이 HTTP $status 가 아닙니다: $requestLine -> ${response.statusCode} ${response.asString()}"
            )
        }
        return response
    }

    override fun sql(statement: String, vararg args: Any) {
        jdbcTemplate.update(statement, *args)
    }

    override fun jvmNow(): LocalDateTime = LocalDateTime.now()

    override fun dbNow(): LocalDateTime = jdbcTemplate.queryForObject("SELECT NOW(6)", LocalDateTime::class.java)!!

    private fun claimsOf(case: SnapshotCase, response: Response): String? {
        if (case.tokens.isEmpty()) {
            return null
        }
        val body = jsonOf(response)
        return case.tokens.joinToString("") { path ->
            JwtClaims.render("$requestLine $path", JsonPath.select(body, path).single().asText())
        }
    }

    private fun given(caller: Caller): RequestSpecification {
        val spec = RestAssured.given()
        if (caller is Caller.Member) {
            spec.headers(createAuthHeaders(caller.pk))
        }
        return spec
    }

    private fun withTodayToken(line: String): String = line.replace(fixtureDate.toString(), TODAY_TOKEN)

    private fun describe(json: String): String =
        VERIFICATION_CODE.replace(JWT.replace(json, JWT_TOKEN), CODE_TOKEN)

    private fun describe(part: MultiPartSpecification): String =
        if (part.fileName != null) {
            "${part.controlName}=file(${part.fileName}, ${part.mimeType})"
        } else {
            "${part.controlName}=${part.mimeType}:${part.content}"
        }

    private companion object {
        const val OK = 200
        const val SEND_CODE = "/api/v1/email-auth/send-code"
        const val VERIFICATION_KEYS = "email:verification:*"
        const val JWT_TOKEN = "<JWT>"
        const val TODAY_TOKEN = "<TODAY>"
        const val CODE_TOKEN = "\"code\":\"<CODE>\""
        val VERIFICATION_CODE = Regex("\"code\":\"\\d{6}\"")
        val JWT = Regex("[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{10,}")
        val objectMapper = jacksonObjectMapper()
    }
}
