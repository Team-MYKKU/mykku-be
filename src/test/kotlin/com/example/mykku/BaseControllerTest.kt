package com.example.mykku

import com.example.mykku.auth.application.port.output.TokenProvider
import com.example.mykku.auth.config.JwtProperties
import com.example.mykku.config.TestAsyncConfig
import com.example.mykku.config.TestEmailConfig
import com.example.mykku.config.TestEmailSenderConfig
import com.example.mykku.config.TestImageUploadConfig
import com.example.mykku.config.TestImageUploadService
import com.example.mykku.contract.support.ContractManifestExtension
import com.example.mykku.feed.adapter.output.persistence.FeedImageJpaRepository
import com.example.mykku.feed.adapter.output.persistence.entity.FeedImageJpaEntity
import com.example.mykku.feed.adapter.output.persistence.entity.FeedJpaEntity
import com.example.mykku.member.adapter.output.persistence.entity.MemberJpaEntity
import com.example.mykku.util.DatabaseCleaner
import com.example.mykku.util.JwtTestTokens
import io.restassured.RestAssured
import io.restassured.http.ContentType
import io.restassured.response.ValidatableResponse
import org.hamcrest.Matchers.matchesPattern
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.sql.Connection
import javax.sql.DataSource

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestEmailConfig::class, TestEmailSenderConfig::class, TestImageUploadConfig::class, TestAsyncConfig::class)
@ExtendWith(DatabaseCleaner::class, ContractManifestExtension::class)
abstract class BaseControllerTest : BaseFixtureTest() {

    @LocalServerPort
    protected var port: Int = 0

    @Autowired
    private lateinit var serverTokenProvider: TokenProvider

    @Autowired
    private lateinit var serverJwtProperties: JwtProperties

    protected val jwtTestTokens: JwtTestTokens by lazy { JwtTestTokens(serverJwtProperties, serverTokenProvider) }

    @Autowired
    protected lateinit var testImageUploadService: TestImageUploadService

    @Autowired
    private lateinit var feedImageFixtureRepository: FeedImageJpaRepository

    @Autowired
    private lateinit var connectionPoolDataSource: DataSource

    @BeforeEach
    fun baseSetUp() {
        RestAssured.port = port
        testImageUploadService.reset()
    }

    protected fun fillConnectionPool(connections: Int) {
        val borrowed = mutableListOf<Connection>()
        try {
            repeat(connections) { borrowed.add(connectionPoolDataSource.connection) }
        } finally {
            borrowed.forEach { it.close() }
        }
    }

    protected fun withdraw(member: MemberJpaEntity) {
        memberJpaRepository.deleteById(member.id)
    }

    protected fun createAndSaveFeedImages(feed: FeedJpaEntity, vararg names: String): List<String> {
        return names.map { name ->
            val url = "https://test-bucket.s3.amazonaws.com/feed-images/$name"
            feedImageFixtureRepository.save(FeedImageJpaEntity(url = url, width = 10, height = 10, feed = feed)).url
        }
    }

    protected fun getBearerToken(memberPk: Long): String {
        return JwtTestTokens.bearer(jwtTestTokens.access(memberPk))
    }

    protected fun createAuthHeaders(memberPk: Long): Map<String, String> {
        return mapOf("Authorization" to getBearerToken(memberPk))
    }

    protected fun requestNewRoles(memberPk: Long): ValidatableResponse {
        return RestAssured.given()
            .contentType(ContentType.JSON)
            .headers(createAuthHeaders(memberPk))
            .`when`()
            .get("/api/v1/roles/me/new")
            .then()
            .statusCode(200)
    }

    protected fun getAdminSessionId(): String {
        return RestAssured
            .given()
                .formParam("token", "test-admin-token")
                .redirects().follow(false)
            .`when`()
                .post("/admin/api/login")
            .then()
                .statusCode(302)
                .header("Location", matchesPattern("http://[^/]+/admin(;jsessionid=[0-9A-F]+)?"))
                .extract()
                .sessionId()
    }
}
