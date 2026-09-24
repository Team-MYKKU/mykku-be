package com.example.mykku.common.exception

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.example.mykku.BaseControllerTest
import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

@DisplayName("전역 예외 처리 통합 테스트")
class GlobalExceptionHandlingTest : BaseControllerTest() {

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
    @DisplayName("업로드 파일이 최대 크기를 초과하면 413을 반환한다")
    fun `업로드 파일이 최대 크기를 초과하면 413을 반환한다`() {
        val member = createAndSaveMember(
            memberId = "toolarge",
            nickname = "용량초과",
            email = "toolarge@example.com",
            socialId = "toolarge"
        )
        val board = createAndSaveBoard()
        val oversizedImage = ByteArray(OVERSIZED_IMAGE_BYTES)

        RestAssured.given()
            .header("Authorization", getBearerToken(member.id))
            .contentType(ContentType.MULTIPART)
            .multiPart("request", """{"title":"제목","content":"내용","boardId":${board.id},"tags":[]}""", "application/json")
            .multiPart("images", "oversized.jpg", oversizedImage, "image/jpeg")
            .`when`()
            .post("/api/v1/feeds")
            .then()
            .statusCode(413)
            .body("code", equalTo(CommonErrorCode.PAYLOAD_TOO_LARGE.code))
    }

    companion object {
        private const val OVERSIZED_IMAGE_BYTES = 2 * 1024 * 1024
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
}
