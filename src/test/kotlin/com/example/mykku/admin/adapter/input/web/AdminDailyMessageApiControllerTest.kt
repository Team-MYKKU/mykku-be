package com.example.mykku.admin.adapter.input.web

import com.example.mykku.BaseControllerTest
import com.example.mykku.admin.dto.dailymessage.DailyMessageCreateRequest
import com.example.mykku.admin.exception.AdminErrorCode
import com.example.mykku.admin.service.AdminDailyMessageService
import com.example.mykku.common.exception.CommonErrorCode
import com.example.mykku.dailymessage.adapter.output.persistence.entity.DailyMessageJpaEntity
import com.example.mykku.dailymessage.adapter.output.persistence.repository.DailyMessageJpaRepository
import com.example.mykku.dailymessage.exception.DailyMessageErrorCode
import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.notNullValue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import java.time.LocalDate

@DisplayName("AdminDailyMessageApiController 통합 테스트")
class AdminDailyMessageApiControllerTest : BaseControllerTest() {

    @Autowired
    private lateinit var dailyMessageJpaRepository: DailyMessageJpaRepository

    @Autowired
    private lateinit var adminDailyMessageService: AdminDailyMessageService

    private lateinit var adminSessionId: String

    @BeforeEach
    fun setUp() {
        adminSessionId = getAdminSessionId()
    }

    private fun createDailyMessage(
        title: String = "테스트 제목",
        content: String = "테스트 내용",
        date: LocalDate = LocalDate.now()
    ): DailyMessageJpaEntity {
        return dailyMessageJpaRepository.save(
            DailyMessageJpaEntity(
                title = title,
                content = content,
                date = date
            )
        )
    }

    @Test
    @DisplayName("데일리 메시지 생성 - 정상 케이스")
    fun `create - 정상적으로 데일리 메시지를 생성한다`() {
        val request = DailyMessageCreateRequest(
            title = "오늘의 메시지",
            content = "오늘도 덕질 파이팅!",
            date = LocalDate.of(2025, 1, 22)
        )

        RestAssured.given()
            .sessionId(adminSessionId)
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .post("/admin/api/v1/dailymessages")
            .then()
            .statusCode(200)
            .body("message", equalTo("데일리 메시지가 생성되었습니다"))
            .body("data", notNullValue())
            .body("data.title", equalTo("오늘의 메시지"))
            .body("data.content", equalTo("오늘도 덕질 파이팅!"))
    }

    @Test
    @DisplayName("데일리 메시지 생성 - 관리자 세션 없이 요청 시 실패")
    fun `create - 관리자 세션 없이 요청하면 실패한다`() {
        val request = DailyMessageCreateRequest(
            title = "오늘의 메시지",
            content = "오늘도 덕질 파이팅!",
            date = LocalDate.of(2025, 1, 22)
        )

        RestAssured.given()
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .post("/admin/api/v1/dailymessages")
            .then()
            .statusCode(401)
            .body("code", equalTo(AdminErrorCode.UNAUTHORIZED.code))
    }

    @Test
    @DisplayName("데일리 메시지 삭제 - 정상 케이스")
    fun `delete - 정상적으로 데일리 메시지를 삭제한다`() {
        val dailyMessage = createDailyMessage(
            title = "삭제할 메시지",
            content = "삭제될 내용입니다",
            date = LocalDate.of(2025, 1, 21)
        )

        RestAssured.given()
            .sessionId(adminSessionId)
            .`when`()
            .delete("/admin/api/v1/dailymessages/${dailyMessage.id}")
            .then()
            .statusCode(200)
            .body("message", equalTo("데일리 메시지가 삭제되었습니다"))
    }

    @Test
    @DisplayName("데일리 메시지 삭제 - 관리자 세션 없이 요청 시 실패")
    fun `delete - 관리자 세션 없이 요청하면 실패한다`() {
        val dailyMessage = createDailyMessage()

        RestAssured.given()
            .`when`()
            .delete("/admin/api/v1/dailymessages/${dailyMessage.id}")
            .then()
            .statusCode(401)
            .body("code", equalTo(AdminErrorCode.UNAUTHORIZED.code))
    }

    @Test
    @DisplayName("데일리 메시지 삭제 - 존재하지 않는 메시지")
    fun `delete - 존재하지 않는 메시지를 삭제하면 실패한다`() {
        val nonExistentId = 99999L

        RestAssured.given()
            .sessionId(adminSessionId)
            .`when`()
            .delete("/admin/api/v1/dailymessages/$nonExistentId")
            .then()
            .statusCode(404)
    }

    @Test
    @DisplayName("데일리 메시지 생성 - 같은 날짜로 두 번 생성할 수 없다")
    fun `create - 같은 날짜의 메시지가 이미 있으면 실패한다`() {
        val date = LocalDate.of(2026, 8, 20)
        createDailyMessage(date = date)
        val request = DailyMessageCreateRequest(title = "중복 날짜", content = "내용", date = date)

        RestAssured.given()
            .sessionId(adminSessionId)
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .post("/admin/api/v1/dailymessages")
            .then()
            .statusCode(409)
            .body("code", equalTo(DailyMessageErrorCode.DAILY_MESSAGE_DATE_ALREADY_EXISTS.code))
    }

    @Test
    @DisplayName("데일리 메시지 생성 - 제목이 255자를 넘으면 실패한다")
    fun `create - 제목이 컬럼 길이를 넘으면 400을 반환한다`() {
        val request = DailyMessageCreateRequest(
            title = "가".repeat(256),
            content = "내용",
            date = LocalDate.of(2026, 8, 21)
        )

        RestAssured.given()
            .sessionId(adminSessionId)
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .post("/admin/api/v1/dailymessages")
            .then()
            .statusCode(400)
            .body("code", equalTo(CommonErrorCode.INVALID_INPUT.code))
    }

    @Test
    @DisplayName("데일리 메시지 목록 - createdAt이 조회 시각이 아니라 저장된 생성 시각이다")
    fun `findAll - createdAt이 저장된 생성 시각을 반환한다`() {
        val saved = createDailyMessage(date = LocalDate.of(2026, 8, 22))
        val persistedCreatedAt = dailyMessageJpaRepository.findById(saved.id!!).orElseThrow().createdAt

        val found = adminDailyMessageService.findAll(PageRequest.of(0, 10)).content.single()

        assertThat(found.createdAt).isEqualTo(persistedCreatedAt)
    }

    @Test
    @DisplayName("데일리 메시지 목록 - 항목마다 각자의 생성 시각을 가진다")
    fun `findAll - 여러 항목의 createdAt이 서로 다르다`() {
        createDailyMessage(title = "먼저", date = LocalDate.of(2026, 8, 23))
        createDailyMessage(title = "나중", date = LocalDate.of(2026, 8, 24))

        val createdAts = adminDailyMessageService.findAll(PageRequest.of(0, 10))
            .content
            .map { it.createdAt }

        assertThat(createdAts).hasSize(2)
        assertThat(createdAts[0]).isNotEqualTo(createdAts[1])
    }
}
