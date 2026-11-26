package com.example.mykku.contract.error

import com.example.mykku.BaseControllerTest
import com.example.mykku.contract.matrix.ExactContentType
import com.example.mykku.contract.matrix.MatrixBodies
import com.example.mykku.contract.matrix.MatrixFixtures
import com.example.mykku.contract.support.ContractGolden
import com.example.mykku.contract.support.ContractJson
import com.example.mykku.contract.support.HttpSnapshot
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
import org.springframework.boot.autoconfigure.web.servlet.MultipartProperties
import javax.sql.DataSource

@DisplayName("프레임워크 에러 실제 요청 계약")
class FrameworkErrorHttpContractTest : BaseControllerTest() {

    @Autowired
    private lateinit var dataSource: DataSource

    @Autowired
    private lateinit var multipartProperties: MultipartProperties

    @BeforeEach
    fun loadMatrixFixtures() {
        SqlFixtureLoader(dataSource).load(MatrixFixtures.MEMBERS)
    }

    @Test
    fun `경로·메서드·미디어 타입·바인딩·multipart 오류의 실제 응답은 골든과 같다`() {
        val cases = routingCases() + mediaTypeCases() + bindingCases() + checkIdBodyCases() + typedBodyCases() +
            multipartCases()

        verify(ErrorGoldens.path(ErrorGoldens.FRAMEWORK_HTTP), cases)
    }

    @Test
    fun `지금 409 C302로 나가는 비정상 입력의 실제 응답은 gray-zone 골든과 같다`() {
        val longDeviceId = "d".repeat(FCM_DEVICE_ID_COLUMN_LENGTH + 1)
        val title = "POST /api/v1/fcm-tokens deviceId ${longDeviceId.length}자(컬럼 ${FCM_DEVICE_ID_COLUMN_LENGTH}자 초과)"
        val cases = listOf(
            HttpCase(title) {
                member().json("""{"token":"matrix-fcm-token","deviceId":"$longDeviceId"}""").post("/api/v1/fcm-tokens")
            }
        )

        verify(ErrorGoldens.grayZonePath(ErrorGoldens.GRAY_ZONE_FRAMEWORK_HTTP), cases)
    }

    @Test
    fun `검증 제약이 여러 개 실패하면 C101 message는 실패 메시지를 쉼표로 이은 순열 중 하나다`() {
        val oneField = given().json("""{"memberId":"invalid_id_longer_than_16!"}""").post(CHECK_ID)
        val twoFields = member().json("""{"memberId":"invalid_id!","nickname":"!!"}""").post(SETUP_PROFILE)

        assertC101Permutation(oneField, listOf("아이디는 최대 16자까지 가능합니다", MEMBER_ID_PATTERN_MESSAGE))
        assertC101Permutation(twoFields, listOf(MEMBER_ID_PATTERN_MESSAGE, "닉네임은 한글, 영문, 숫자만 사용 가능합니다"))
    }

    private fun assertC101Permutation(response: Response, messages: List<String>) {
        val body = ContractJson.parse(ContractJson.decodeUtf8(response.asByteArray()))

        assertThat(response.statusCode).isEqualTo(400)
        assertThat(body.path("code").asText()).isEqualTo("C101")
        assertThat(body.path("message").asText()).isIn(joinedPermutations(messages))
    }

    private fun joinedPermutations(messages: List<String>): List<String> {
        if (messages.size <= 1) {
            return messages
        }
        return messages.flatMap { first -> joinedPermutations(messages - first).map { "$first, $it" } }
    }

    private fun routingCases(): List<HttpCase> = listOf(
        HttpCase("GET /api/v1/not-exist") { given().get("/api/v1/not-exist") },
        HttpCase("GET /api/v1/boards/ (끝 슬래시)") { given().get("/api/v1/boards/") },
        HttpCase("PUT /api/v1/members/me/member-id (PATCH만 있는 경로)") {
            member().json("""{"memberId":"matrixchanged"}""").put("/api/v1/members/me/member-id")
        }
    )

    private fun mediaTypeCases(): List<HttpCase> = listOf(
        HttpCase("POST /api/v1/members/check-id Content-Type: text/plain") {
            given().plainText("memberId=matrix").post(CHECK_ID)
        },
        HttpCase("PATCH /api/v1/members/me Content-Type: text/plain") {
            member().plainText("nickname=matrix").patch(MY_PROFILE)
        },
        HttpCase("GET /api/v1/boards Accept: application/xml") {
            given().accept(ExactContentType.APPLICATION_XML).get("/api/v1/boards")
        }
    )

    private fun bindingCases(): List<HttpCase> = listOf(
        HttpCase("GET /api/v1/daily-messages (date 누락)") { given().get(DAILY_MESSAGES) },
        HttpCase("POST /api/v1/feeds multipart (request 파트 누락)") {
            member().multiPart("images", "matrix.jpg", "matrix image".toByteArray(), "image/jpeg").post(FEEDS)
        },
        HttpCase("GET /api/v1/daily-messages?date=not-a-date") {
            given().queryParam("date", "not-a-date").get(DAILY_MESSAGES)
        },
        HttpCase("GET /api/v1/feeds/abc") { given().get("/api/v1/feeds/abc") },
        HttpCase("GET /api/v1/contests?status=active") {
            member().queryParam("status", "active").get("/api/v1/contests")
        }
    )

    private fun checkIdBodyCases(): List<HttpCase> = listOf(
        HttpCase("""POST /api/v1/members/check-id {"memberId":"invalid_id!"}""") {
            given().json("""{"memberId":"invalid_id!"}""").post(CHECK_ID)
        },
        HttpCase("POST /api/v1/members/check-id {}") { given().json("{}").post(CHECK_ID) },
        HttpCase("POST /api/v1/members/check-id {") { given().json("{").post(CHECK_ID) },
        HttpCase("POST /api/v1/members/check-id (빈 본문)") { given().json("").post(CHECK_ID) }
    )

    private fun typedBodyCases(): List<HttpCase> = listOf(
        HttpCase("""POST /api/v1/reports {"targetType":"FEED","targetId":1.5,"reason":"SPAM"}""") {
            member().json("""{"targetType":"FEED","targetId":1.5,"reason":"SPAM"}""").post("/api/v1/reports")
        },
        HttpCase("""POST /api/v1/auth/mobile/login {"provider":"EMAIL"}""") {
            given().json("""{"provider":"EMAIL"}""").post(MOBILE_LOGIN)
        },
        HttpCase("""POST /api/v1/auth/mobile/login {"provider":"GOOGLE"} (accessToken 누락)""") {
            given().json("""{"provider":"GOOGLE"}""").post(MOBILE_LOGIN)
        }
    )

    private fun multipartCases(): List<HttpCase> = listOf(
        HttpCase("PATCH /api/v1/members/me Content-Type: multipart/form-data (boundary 없음)") {
            member().config(ExactContentType.CONFIG).contentType(ExactContentType.MULTIPART_FORM_DATA)
                .body("boundary-less").patch(MY_PROFILE)
        },
        HttpCase("POST /api/v1/feeds images 파일 1개가 max-file-size + 1바이트") {
            member().multiPart("request", MatrixBodies.FEED_CREATE_JSON, "application/json")
                .multiPart("images", "matrix.jpg", ByteArray(maxFileSizeBytes() + 1), "image/jpeg").post(FEEDS)
        }
    )

    private fun verify(goldenPath: String, cases: List<HttpCase>) {
        val snapshots = cases.map { HttpSnapshot.of(it.title, it.send()).render() }
        ContractGolden.DEFAULT.verify(goldenPath, snapshots.joinToString("\n"))
    }

    private fun given(): RequestSpecification = RestAssured.given()

    private fun member(): RequestSpecification =
        RestAssured.given().header("Authorization", getBearerToken(MatrixFixtures.MEMBER_PK))

    private fun RequestSpecification.json(body: String): RequestSpecification = contentType(ContentType.JSON).body(body)

    private fun RequestSpecification.plainText(body: String): RequestSpecification =
        config(ExactContentType.CONFIG).contentType(ExactContentType.TEXT_PLAIN).body(body)

    private fun maxFileSizeBytes(): Int = multipartProperties.maxFileSize.toBytes().toInt()

    private class HttpCase(val title: String, val send: () -> Response)

    private companion object {
        const val CHECK_ID = "/api/v1/members/check-id"
        const val MY_PROFILE = "/api/v1/members/me"
        const val DAILY_MESSAGES = "/api/v1/daily-messages"
        const val FEEDS = "/api/v1/feeds"
        const val MOBILE_LOGIN = "/api/v1/auth/mobile/login"
        const val SETUP_PROFILE = "/api/v1/members/setup-profile"
        const val MEMBER_ID_PATTERN_MESSAGE = "아이디는 영문과 숫자만 사용 가능합니다"
        const val FCM_DEVICE_ID_COLUMN_LENGTH = 100
    }
}
