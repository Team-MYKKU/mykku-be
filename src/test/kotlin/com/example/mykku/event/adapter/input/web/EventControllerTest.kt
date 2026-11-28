package com.example.mykku.event.adapter.input.web

import com.example.mykku.BaseControllerTest
import com.example.mykku.common.exception.CommonErrorCode
import com.example.mykku.event.adapter.output.persistence.entity.EventJpaEntity
import com.example.mykku.event.adapter.output.persistence.repository.EventJpaRepository
import com.example.mykku.event.domain.vo.EventStatusType
import com.example.mykku.event.domain.vo.EventType
import com.example.mykku.util.ResponseMaps
import io.restassured.RestAssured
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.notNullValue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime

@DisplayName("EventController 통합 테스트")
class EventControllerTest : BaseControllerTest() {

    @Autowired
    private lateinit var eventJpaRepository: EventJpaRepository

    @Test
    @DisplayName("이벤트 목록 조회 - 정상 케이스")
    fun `getEvents - 정상적으로 이벤트 목록을 조회한다`() {
        val member = createAndSaveMember()
        val authHeader = getBearerToken(member.id)

        createAndSaveEvent(
            title = "진행중인 이벤트",
            startedAt = LocalDateTime.now().minusDays(1),
            expiredAt = LocalDateTime.now().plusDays(30)
        )

        RestAssured.given()
            .header("Authorization", authHeader)
            .param("status", "ACTIVE")
            .param("sortType", "LATEST")
            .param("page", 0)
            .param("size", 20)
            .`when`()
            .get("/api/v1/events")
            .then()
            .statusCode(200)
            .body("message", equalTo("이벤트 목록을 성공적으로 조회했습니다."))
            .body("data.content", notNullValue())
            .body("data.content[0].description", equalTo("테스트 이벤트 설명"))
            .body("data.content[0].subTitle", equalTo("테스트 부제목"))
    }

    @Test
    @DisplayName("이벤트 목록 조회 - 종료된 이벤트는 EXPIRED로 응답한다")
    fun `getEvents - 종료된 이벤트의 상태는 EXPIRED다`() {
        val member = createAndSaveMember()
        val authHeader = getBearerToken(member.id)

        createAndSaveEvent(
            title = "종료된 이벤트",
            startedAt = LocalDateTime.now().minusDays(10),
            expiredAt = LocalDateTime.now().minusDays(1)
        )

        RestAssured.given()
            .header("Authorization", authHeader)
            .param("status", "EXPIRED")
            .`when`()
            .get("/api/v1/events")
            .then()
            .statusCode(200)
            .body("data.content[0].status", equalTo("EXPIRED"))
    }

    @Test
    @DisplayName("이벤트 상세 조회 - 정상 케이스")
    fun `getEventDetail - 정상적으로 이벤트 상세를 조회한다`() {
        val member = createAndSaveMember()
        val authHeader = getBearerToken(member.id)

        val event = createAndSaveEvent(
            title = "상세 조회 테스트 이벤트",
            startedAt = LocalDateTime.now().minusDays(1),
            expiredAt = LocalDateTime.now().plusDays(30)
        )

        RestAssured.given()
            .header("Authorization", authHeader)
            .`when`()
            .get("/api/v1/events/{eventId}", event.id)
            .then()
            .statusCode(200)
            .body("message", equalTo("이벤트 상세 정보를 성공적으로 조회했습니다."))
            .body("data.id", equalTo(event.id!!.toInt()))
            .body("data.title", equalTo("상세 조회 테스트 이벤트"))
            .body("data.subTitle", equalTo("테스트 부제목"))
            .body("data.description", equalTo("테스트 이벤트 설명"))
    }

    @Test
    @DisplayName("이벤트 목록 조회 - ACTIVE, EXPIRED, ALL 이외의 status는 400을 반환한다")
    fun `getEvents - WINNER_SELECTED 필터는 400을 반환한다`() {
        val member = createAndSaveMember()
        val authHeader = getBearerToken(member.id)

        RestAssured.given()
            .header("Authorization", authHeader)
            .param("status", "WINNER_SELECTED")
            .`when`()
            .get("/api/v1/events")
            .then()
            .statusCode(400)
            .body("code", equalTo(CommonErrorCode.INVALID_PARAMETER_TYPE.code))
    }

    @Test
    @DisplayName("이벤트 목록 조회 - 인증되지 않은 사용자")
    fun `getEvents - 인증되지 않은 사용자는 목록을 조회할 수 없다`() {
        RestAssured.given()
            .param("status", "ACTIVE")
            .`when`()
            .get("/api/v1/events")
            .then()
            .statusCode(401)
    }

    @Test
    @DisplayName("이벤트 상세 조회 - 인증되지 않은 사용자")
    fun `getEventDetail - 인증되지 않은 사용자는 상세를 조회할 수 없다`() {
        val event = createAndSaveEvent(
            title = "테스트 이벤트",
            startedAt = LocalDateTime.now().minusDays(1),
            expiredAt = LocalDateTime.now().plusDays(30)
        )

        RestAssured.given()
            .`when`()
            .get("/api/v1/events/{eventId}", event.id)
            .then()
            .statusCode(401)
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(EventType::class)
    @DisplayName("유형이 다른 이벤트도 목록(ALL·ACTIVE)과 상세에서 같은 조건의 일반 이벤트와 같은 필드·값으로 응답한다")
    fun `getEvents getEventDetail - 유형과 무관하게 같은 응답`(type: EventType) {
        val authHeader = getBearerToken(createAndSaveMember().id)
        val general = createAndSaveTypedEvent(EventType.GENERAL)
        val typed = createAndSaveTypedEvent(type)

        listOf("ALL", "ACTIVE").forEach { status ->
            val content = getListContent(authHeader, status)
            assertThat(content.map { it["id"] }).`as`(status)
                .containsExactlyInAnyOrder(general.id!!.toInt(), typed.id!!.toInt())
            assertThat(content.map { ResponseMaps.withoutKeys(it, setOf("id")) }.distinct()).`as`(status).hasSize(1)
        }
        assertThat(getDetailWithoutIds(authHeader, typed.id!!)).isEqualTo(getDetailWithoutIds(authHeader, general.id!!))
    }

    private fun getListContent(authHeader: String, status: String): List<Map<String, Any?>> {
        return RestAssured.given()
            .header("Authorization", authHeader)
            .param("status", status)
            .`when`()
            .get("/api/v1/events")
            .then()
            .statusCode(200)
            .extract()
            .jsonPath()
            .getList("data.content")
    }

    private fun getDetailWithoutIds(authHeader: String, eventId: Long): Any? {
        val data = RestAssured.given()
            .header("Authorization", authHeader)
            .`when`()
            .get("/api/v1/events/{eventId}", eventId)
            .then()
            .statusCode(200)
            .extract()
            .jsonPath()
            .getMap<String, Any?>("data")
        return ResponseMaps.withoutKeys(data, setOf("id", "createdAt"))
    }

    private fun createAndSaveTypedEvent(type: EventType): EventJpaEntity {
        val event = EventJpaEntity(
            title = "유형 비교 이벤트",
            subTitle = "유형 비교 부제",
            description = "유형 비교 설명",
            startedAt = LocalDateTime.of(2020, 1, 1, 0, 0),
            expiredAt = LocalDateTime.of(2099, 12, 31, 23, 59, 59),
            thumbnailUrl = "https://example.com/thumbnail.jpg",
            status = EventStatusType.ACTIVE,
            type = type,
            googleFormUrl = "https://forms.gle/abc123".takeIf { type == EventType.GOOGLE_FORM }
        )
        return eventJpaRepository.save(event)
    }

    private fun createAndSaveEvent(
        title: String,
        subTitle: String? = "테스트 부제목",
        description: String? = "테스트 이벤트 설명",
        startedAt: LocalDateTime,
        expiredAt: LocalDateTime,
        status: EventStatusType = EventStatusType.ACTIVE
    ): EventJpaEntity {
        val event = EventJpaEntity(
            title = title,
            subTitle = subTitle,
            description = description,
            startedAt = startedAt,
            expiredAt = expiredAt,
            thumbnailUrl = "https://example.com/thumbnail.jpg",
            status = status
        )
        return eventJpaRepository.save(event)
    }
}
