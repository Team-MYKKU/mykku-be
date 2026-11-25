package com.example.mykku.common.exception

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.example.mykku.BaseControllerTest
import io.restassured.RestAssured
import io.restassured.http.ContentType
import io.restassured.path.json.JsonPath
import io.restassured.response.Response
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean
import org.springframework.boot.autoconfigure.web.servlet.MultipartProperties
import org.springframework.core.io.ClassPathResource
import org.springframework.util.unit.DataSize
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.SequenceInputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.Collections

@DisplayName("전역 예외 처리 통합 테스트")
class GlobalExceptionHandlingTest : BaseControllerTest() {

    @Autowired
    private lateinit var multipartProperties: MultipartProperties

    @Test
    @DisplayName("존재하지 않는 API 경로는 404를 반환한다")
    fun `존재하지 않는 경로는 404를 반환한다`() {
        RestAssured.given()
            .contentType(ContentType.JSON)
            .`when`()
            .get("/api/v1/not-exist-endpoint")
            .then()
            .statusCode(404)
            .body("code", equalTo(CommonErrorCode.ENDPOINT_NOT_FOUND.code))
    }

    @Test
    @DisplayName("지원하지 않는 HTTP 메서드는 405를 반환한다")
    fun `지원하지 않는 메서드는 405를 반환한다`() {
        val member = createAndSaveMember(
            memberId = "methodcheck",
            nickname = "메서드확인",
            email = "methodcheck@example.com",
            socialId = "methodcheck"
        )

        RestAssured.given()
            .header("Authorization", getBearerToken(member.id))
            .contentType(ContentType.JSON)
            .body(mapOf("memberId" to "changedid"))
            .`when`()
            .put("/api/v1/members/me/member-id")
            .then()
            .statusCode(405)
            .body("code", equalTo(CommonErrorCode.METHOD_NOT_ALLOWED.code))
    }

    @Test
    @DisplayName("지원하지 않는 Content-Type은 415를 반환한다")
    fun `지원하지 않는 컨텐츠 타입은 415를 반환한다`() {
        RestAssured.given()
            .contentType(ContentType.TEXT)
            .body("memberId=testid")
            .`when`()
            .post("/api/v1/members/check-id")
            .then()
            .statusCode(415)
            .body("code", equalTo(CommonErrorCode.UNSUPPORTED_MEDIA_TYPE.code))
    }

    @Test
    @DisplayName("필수 쿼리 파라미터가 없으면 400을 반환한다")
    fun `필수 쿼리 파라미터가 없으면 400을 반환한다`() {
        RestAssured.given()
            .`when`()
            .get("/api/v1/daily-messages")
            .then()
            .statusCode(400)
            .body("code", equalTo(CommonErrorCode.MISSING_REQUEST_PARAMETER.code))
    }

    @Test
    @DisplayName("쿼리 파라미터 타입이 맞지 않으면 400을 반환한다")
    fun `쿼리 파라미터 타입이 맞지 않으면 400을 반환한다`() {
        RestAssured.given()
            .queryParam("date", "not-a-date")
            .`when`()
            .get("/api/v1/daily-messages")
            .then()
            .statusCode(400)
            .body("code", equalTo(CommonErrorCode.INVALID_PARAMETER_TYPE.code))
    }

    @Test
    @DisplayName("프로필 수정에 JSON도 multipart도 아닌 Content-Type을 보내면 415를 반환한다")
    fun `프로필 수정에 지원하지 않는 컨텐츠 타입은 415를 반환한다`() {
        val member = createAndSaveMember(
            memberId = "mediatype",
            nickname = "미디어타입",
            email = "mediatype@example.com",
            socialId = "mediatype"
        )

        RestAssured.given()
            .header("Authorization", getBearerToken(member.id))
            .contentType(ContentType.TEXT)
            .body("nickname=newname")
            .`when`()
            .patch("/api/v1/members/me")
            .then()
            .statusCode(415)
            .body("code", equalTo(CommonErrorCode.UNSUPPORTED_MEDIA_TYPE.code))
    }

    @Test
    @DisplayName("multipart 필수 파트가 누락되면 400을 반환한다")
    fun `multipart 필수 파트가 누락되면 400을 반환한다`() {
        val member = createAndSaveMember(
            memberId = "partmissing",
            nickname = "파트누락",
            email = "partmissing@example.com",
            socialId = "partmissing"
        )

        RestAssured.given()
            .header("Authorization", getBearerToken(member.id))
            .contentType(ContentType.MULTIPART)
            .multiPart("images", "dummy.jpg", "image data".toByteArray(), "image/jpeg")
            .`when`()
            .post("/api/v1/feeds")
            .then()
            .statusCode(400)
            .body("code", equalTo(CommonErrorCode.MISSING_REQUEST_PARAMETER.code))
    }

    @Test
    @DisplayName("boundary가 없는 multipart 요청은 400을 반환한다")
    fun `boundary가 없는 multipart 요청은 400을 반환한다`() {
        val member = createAndSaveMember(
            memberId = "brokenpart",
            nickname = "깨진멀티",
            email = "brokenpart@example.com",
            socialId = "brokenpart"
        )

        val request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:$port/api/v1/members/me"))
            .header("Authorization", getBearerToken(member.id))
            .header("Content-Type", "multipart/form-data")
            .method("PATCH", HttpRequest.BodyPublishers.ofString("boundary-less-multipart-body"))
            .build()

        val response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString())

        assertThat(response.statusCode()).isEqualTo(400)
        assertThat(response.body()).contains(CommonErrorCode.INVALID_MULTIPART_REQUEST.code)
    }

    @Test
    @DisplayName("test 프로필 서블릿 업로드 한도는 개발 서버 설정과 같다 (파일 10MB, 요청 50MB)")
    fun `test 프로필 서블릿 업로드 한도는 개발 서버 설정과 같다`() {
        assertThat(multipartProperties.maxFileSize).isEqualTo(devProfileMultipartLimit("max-file-size"))
        assertThat(multipartProperties.maxRequestSize).isEqualTo(devProfileMultipartLimit("max-request-size"))
        assertThat(multipartProperties.maxFileSize).isEqualTo(DataSize.ofMegabytes(10))
        assertThat(multipartProperties.maxRequestSize).isEqualTo(DataSize.ofMegabytes(50))
    }

    @Test
    @DisplayName("서블릿 업로드 한도 초과는 C205")
    fun `서블릿 업로드 한도 초과는 C205`() {
        val member = createAndSaveMember(
            memberId = "toolarge",
            nickname = "용량초과",
            email = "toolarge@example.com",
            socialId = "toolarge"
        )
        val board = createAndSaveBoard()

        postFeedWithImage(member.id, board.id!!, ByteArray(maxFileSizeBytes() + 1))
            .then()
            .statusCode(413)
            .body("code", equalTo(CommonErrorCode.PAYLOAD_TOO_LARGE.code))
    }

    @Test
    @DisplayName("파일 크기가 서블릿 파일 한도와 같으면 413 없이 처리된다")
    fun `파일 크기가 서블릿 파일 한도와 같으면 413 없이 처리된다`() {
        val member = createAndSaveMember(
            memberId = "exactlimit",
            nickname = "한도일치",
            email = "exactlimit@example.com",
            socialId = "exactlimit"
        )
        val board = createAndSaveBoard()

        postFeedWithImage(member.id, board.id!!, ByteArray(maxFileSizeBytes()))
            .then()
            .statusCode(200)
    }

    @Test
    @DisplayName("요청 전체가 서블릿 요청 한도를 넘으면 C205 (개발 서버에서는 앞단 nginx client_max_body_size 50M이 먼저 413을 응답)")
    fun `요청 전체가 서블릿 요청 한도를 넘으면 C205`() {
        val member = createAndSaveMember(
            memberId = "requestlarge",
            nickname = "요청초과",
            email = "requestlarge@example.com",
            socialId = "requestlarge"
        )
        val board = createAndSaveBoard()

        val response = postChunkedFeed(member.id, board.id!!, imageSizesFillingRequestLimit())

        assertThat(response.statusCode()).isEqualTo(413)
        assertThat(JsonPath.from(response.body()).getString("code")).isEqualTo(CommonErrorCode.PAYLOAD_TOO_LARGE.code)
    }

    @Test
    @DisplayName("존재하지 않는 경로 요청은 WARN 레벨로 스택트레이스 없이 기록한다")
    fun `존재하지 않는 경로 요청은 WARN 레벨로 스택트레이스 없이 기록한다`() {
        val logger = LoggerFactory.getLogger(BaseExceptionHandler::class.java) as Logger
        val appender = ListAppender<ILoggingEvent>().apply { start() }
        logger.addAppender(appender)

        try {
            RestAssured.given()
                .`when`()
                .get("/api/v1/not-exist-endpoint")
                .then()
                .statusCode(404)

            val event = appender.list.single { it.formattedMessage.contains("NoResourceFoundException") }
            assertThat(event.level).isEqualTo(Level.WARN)
            assertThat(event.throwableProxy).isNull()
        } finally {
            logger.detachAppender(appender)
        }
    }

    private fun devProfileMultipartLimit(key: String): DataSize {
        val devProfile = YamlPropertiesFactoryBean().apply { setResources(ClassPathResource("application-dev.yml")) }
        return DataSize.parse(devProfile.getObject()!!.getProperty("spring.servlet.multipart.$key"))
    }

    private fun maxFileSizeBytes(): Int {
        return multipartProperties.maxFileSize.toBytes().toInt()
    }

    private fun imageSizesFillingRequestLimit(): List<Int> {
        val maxFileSize = multipartProperties.maxFileSize.toBytes()
        val maxRequestSize = multipartProperties.maxRequestSize.toBytes()
        val count = (maxRequestSize + maxFileSize - 1) / maxFileSize
        return (0 until count).map { index -> minOf(maxFileSize, maxRequestSize - index * maxFileSize).toInt() }
    }

    private fun postFeedWithImage(memberPk: Long, boardId: Long, image: ByteArray): Response {
        return RestAssured.given()
            .header("Authorization", getBearerToken(memberPk))
            .contentType(ContentType.MULTIPART)
            .multiPart("request", feedRequestJson(boardId), "application/json")
            .multiPart("images", "upload.jpg", image, "image/jpeg")
            .`when`()
            .post("/api/v1/feeds")
    }

    private fun postChunkedFeed(memberPk: Long, boardId: Long, imageSizes: List<Int>): HttpResponse<String> {
        val zeros = ByteArray(imageSizes.max())
        val request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:$port/api/v1/feeds"))
            .version(HttpClient.Version.HTTP_1_1)
            .timeout(Duration.ofSeconds(60))
            .header("Authorization", getBearerToken(memberPk))
            .header("Content-Type", "multipart/form-data; boundary=$MULTIPART_BOUNDARY")
            .POST(HttpRequest.BodyPublishers.ofInputStream { multipartBody(boardId, imageSizes, zeros) })
            .build()
        return HttpClient.newHttpClient().use { it.send(request, HttpResponse.BodyHandlers.ofString()) }
    }

    private fun multipartBody(boardId: Long, imageSizes: List<Int>, zeros: ByteArray): InputStream {
        val requestPart = partHeader("name=\"request\"", "application/json") + feedRequestJson(boardId) + CRLF
        val imageParts = imageSizes.flatMapIndexed { index, size -> imagePart(index, zeros, size) }
        val closing = "--$MULTIPART_BOUNDARY--$CRLF"
        val streams = listOf<InputStream>(requestPart.byteInputStream()) + imageParts + closing.byteInputStream()
        return SequenceInputStream(Collections.enumeration(streams))
    }

    private fun imagePart(index: Int, zeros: ByteArray, size: Int): List<InputStream> {
        val header = partHeader("name=\"images\"; filename=\"image-$index.jpg\"", "image/jpeg")
        return listOf(header.byteInputStream(), ByteArrayInputStream(zeros, 0, size), CRLF.byteInputStream())
    }

    private fun partHeader(disposition: String, contentType: String): String {
        return "--$MULTIPART_BOUNDARY$CRLF" +
            "Content-Disposition: form-data; $disposition$CRLF" +
            "Content-Type: $contentType$CRLF$CRLF"
    }

    private fun feedRequestJson(boardId: Long): String {
        return """{"title":"제목","content":"내용","boardId":$boardId,"tags":[]}"""
    }

    companion object {
        private const val MULTIPART_BOUNDARY = "servlet-request-limit-boundary"
        private const val CRLF = "\r\n"
    }
}
