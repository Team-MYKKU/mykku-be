package com.example.mykku.admin.adapter.input.web

import com.example.mykku.BaseControllerTest
import com.example.mykku.admin.exception.AdminErrorCode
import com.example.mykku.common.exception.CommonErrorCode
import com.example.mykku.event.adapter.output.persistence.entity.EventJpaEntity
import com.example.mykku.event.adapter.output.persistence.repository.EventJpaRepository
import com.example.mykku.event.domain.vo.EventStatusType
import com.example.mykku.event.exception.EventErrorCode
import com.example.mykku.util.MultipartParts.image
import com.example.mykku.util.MultipartParts.text
import io.restassured.RestAssured
import io.restassured.response.ValidatableResponse
import io.restassured.specification.MultiPartSpecification
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import java.time.LocalDateTime

@DisplayName("AdminEventApiController 수정 통합 테스트")
class AdminEventUpdateApiControllerTest : BaseControllerTest() {

    @Autowired
    private lateinit var eventJpaRepository: EventJpaRepository

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Test
    @DisplayName("텍스트와 기간을 바꾸고, 표시값과 기존 URL을 모두 보내면 이미지는 그대로다")
    fun `update - 텍스트와 기간 수정, 이미지 유지`() {
        val event = saveEvent()
        val urls = saveImages(event, 2)

        putEvent(
            event.id!!,
            basicParts(title = "새 이벤트", startedAt = "2026-01-01T10:00:00", expiredAt = "2026-01-31T10:00") +
                text("subTitle", "새 부제") + keepParts(urls)
        ).statusCode(200).body("message", equalTo("이벤트가 수정되었습니다"))

        val saved = eventJpaRepository.findById(event.id!!).get()
        assertThat(saved.title).isEqualTo("새 이벤트")
        assertThat(saved.subTitle).isEqualTo("새 부제")
        assertThat(saved.startedAt).isEqualTo(LocalDateTime.of(2026, 1, 1, 10, 0))
        assertThat(saved.expiredAt).isEqualTo(LocalDateTime.of(2026, 1, 31, 10, 0))
        assertThat(imageUrlsOf(event.id!!)).containsExactlyElementsOf(urls)
    }

    @Test
    @DisplayName("뺀 이미지는 지우고, 남긴 이미지 뒤에 새 이미지를 붙이고, 새 썸네일로 바꾼다")
    fun `update - 이미지 교체`() {
        val event = saveEvent()
        val urls = saveImages(event, 2)

        putEvent(
            event.id!!,
            basicParts() + keepParts(listOf(urls[1])) + image("newImages") + image("thumbnailImage")
        ).statusCode(200)

        val images = imageUrlsOf(event.id!!)
        assertThat(images).hasSize(2)
        assertThat(images[0]).isEqualTo(urls[1])
        assertThat(images[1]).startsWith("https://test-bucket.s3.amazonaws.com/event-images/image-")
        assertThat(eventJpaRepository.findById(event.id!!).get().thumbnailUrl)
            .startsWith("https://test-bucket.s3.amazonaws.com/event-images/thumbnail-")
    }

    @Test
    @DisplayName("표시값만 보내면(빈 목록) 상세 이미지를 모두 지운다")
    fun `update - 빈 목록이면 이미지 전부 제거`() {
        val event = saveEvent()
        saveImages(event, 2)

        putEvent(event.id!!, basicParts() + keepParts(emptyList())).statusCode(200)

        assertThat(imageUrlsOf(event.id!!)).isEmpty()
    }

    @Test
    @DisplayName("유지 목록 표시값이 없으면 C101")
    fun `update - 표시값 없음`() {
        val event = saveEvent()
        val urls = saveImages(event, 1)

        putEvent(event.id!!, basicParts())
            .statusCode(400)
            .body("code", equalTo(CommonErrorCode.INVALID_INPUT.code))

        assertThat(imageUrlsOf(event.id!!)).containsExactlyElementsOf(urls)
    }

    @Test
    @DisplayName("이 이벤트의 이미지가 아니거나 중복된 URL이면 EV104")
    fun `update - 잘못된 유지 목록`() {
        val event = saveEvent()
        val urls = saveImages(event, 1)

        putEvent(event.id!!, basicParts() + keepParts(listOf("https://other.example.com/x.jpg")))
            .statusCode(400)
            .body("code", equalTo(EventErrorCode.INVALID_KEEP_IMAGE_URLS.code))
        putEvent(event.id!!, basicParts() + keepParts(listOf(urls[0], urls[0])))
            .statusCode(400)
            .body("code", equalTo(EventErrorCode.INVALID_KEEP_IMAGE_URLS.code))
    }

    @Test
    @DisplayName("남긴 이미지와 새 이미지가 합쳐 10장을 넘으면 EV101")
    fun `update - 11장`() {
        val event = saveEvent()
        val urls = saveImages(event, 2)

        putEvent(event.id!!, basicParts() + keepParts(urls) + (1..9).map { image("newImages", "n$it.png") })
            .statusCode(400)
            .body("code", equalTo(EventErrorCode.EVENT_IMAGE_LIMIT_EXCEEDED.code))
    }

    @Test
    @DisplayName("당첨자 선정 뒤에는 같은 기간으로 제목만 바꿀 수 있고, 기간을 바꾸면 EV105")
    fun `update - 선정 후 기간 잠금`() {
        val event = saveEvent(status = EventStatusType.WINNER_SELECTED)

        putEvent(event.id!!, basicParts(title = "제목만 변경") + keepParts(emptyList())).statusCode(200)
        putEvent(event.id!!, basicParts(expiredAt = "2026-02-28T00:00:00") + keepParts(emptyList()))
            .statusCode(400)
            .body("code", equalTo(EventErrorCode.PERIOD_LOCKED_AFTER_WINNER_SELECTED.code))

        val saved = eventJpaRepository.findById(event.id!!).get()
        assertThat(saved.title).isEqualTo("제목만 변경")
        assertThat(saved.expiredAt).isEqualTo(EXPIRED_AT)
    }

    @Test
    @DisplayName("시작일이 종료일보다 늦으면 수정과 생성 모두 EV107")
    fun `update create - 기간 역전`() {
        val event = saveEvent()

        putEvent(event.id!!, basicParts(startedAt = "2026-02-01T00:00:00") + keepParts(emptyList()))
            .statusCode(400)
            .body("code", equalTo(EventErrorCode.INVALID_EVENT_PERIOD.code))
        RestAssured.given()
            .sessionId(getAdminSessionId())
            .contentType("multipart/form-data")
            .multiPart(text("title", "역전 이벤트"))
            .multiPart(text("startedAt", "2026-02-01T00:00"))
            .multiPart(text("expiredAt", "2026-01-01T00:00"))
            .multiPart(image("thumbnailImage"))
            .`when`()
            .post("/admin/api/v1/events")
            .then()
            .statusCode(400)
            .body("code", equalTo(EventErrorCode.INVALID_EVENT_PERIOD.code))
    }

    @Test
    @DisplayName("없는 이벤트는 EV001, 관리자 세션이 없으면 401")
    fun `update - 없는 이벤트와 미인증`() {
        putEvent(999999L, basicParts() + keepParts(emptyList()))
            .statusCode(404)
            .body("code", equalTo(EventErrorCode.EVENT_NOT_FOUND.code))
        RestAssured.given()
            .contentType("multipart/form-data")
            .multiPart(text("title", "제목"))
            .`when`()
            .put("/admin/api/v1/events/{eventId}", 1L)
            .then()
            .statusCode(401)
            .body("code", equalTo(AdminErrorCode.UNAUTHORIZED.code))
    }

    private fun putEvent(eventId: Long, parts: List<MultiPartSpecification>): ValidatableResponse {
        var spec = RestAssured.given()
            .sessionId(getAdminSessionId())
            .contentType("multipart/form-data")
        parts.forEach { spec = spec.multiPart(it) }
        return spec.`when`().put("/admin/api/v1/events/{eventId}", eventId).then()
    }

    private fun basicParts(
        title: String = "이벤트",
        startedAt: String = "2026-01-01T00:00:00",
        expiredAt: String = "2026-01-31T00:00:00"
    ): List<MultiPartSpecification> {
        return listOf(text("title", title), text("startedAt", startedAt), text("expiredAt", expiredAt))
    }

    private fun keepParts(urls: List<String>): List<MultiPartSpecification> {
        return listOf(text("_keepImageUrls", "on")) + urls.map { text("keepImageUrls", it) }
    }

    private fun saveEvent(status: EventStatusType = EventStatusType.ACTIVE): EventJpaEntity {
        return eventJpaRepository.save(
            EventJpaEntity(
                title = "이벤트",
                startedAt = STARTED_AT,
                expiredAt = EXPIRED_AT,
                status = status,
                thumbnailUrl = "https://test-bucket.s3.amazonaws.com/event-images/old-thumbnail.jpg"
            )
        )
    }

    private fun saveImages(event: EventJpaEntity, count: Int): List<String> {
        return (0 until count).map { index ->
            val url = "https://test-bucket.s3.amazonaws.com/event-images/old-$index.jpg"
            jdbcTemplate.update(
                "INSERT INTO event_image (url, order_index, event_id, created_at, updated_at) " +
                    "VALUES (?, ?, ?, NOW(), NOW())",
                url,
                index,
                event.id
            )
            url
        }
    }

    private fun imageUrlsOf(eventId: Long): List<String> {
        return jdbcTemplate.queryForList(
            "SELECT url FROM event_image WHERE event_id = ? ORDER BY order_index",
            String::class.java,
            eventId
        )
    }

    companion object {
        private val STARTED_AT = LocalDateTime.of(2026, 1, 1, 0, 0)
        private val EXPIRED_AT = LocalDateTime.of(2026, 1, 31, 0, 0)
    }
}
