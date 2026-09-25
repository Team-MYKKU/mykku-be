package com.example.mykku.admin.adapter.input.web

import com.example.mykku.BaseControllerTest
import com.example.mykku.dailymessage.adapter.output.persistence.entity.DailyMessageJpaEntity
import com.example.mykku.dailymessage.adapter.output.persistence.repository.DailyMessageJpaRepository
import com.example.mykku.dailymessage.exception.DailyMessageErrorCode
import io.restassured.RestAssured
import io.restassured.http.ContentType
import io.restassured.response.ValidatableResponse
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

@DisplayName("AdminDailyMessageApiController 수정 통합 테스트")
class AdminDailyMessageUpdateApiControllerTest : BaseControllerTest() {

    @Autowired
    private lateinit var dailyMessageJpaRepository: DailyMessageJpaRepository

    @Test
    @DisplayName("제목·내용·날짜를 바꾼다(자기 날짜를 그대로 보내도 된다)")
    fun `update - 정상 수정`() {
        val message = saveMessage(LocalDate.of(2026, 1, 1))

        putMessage(message.id!!, "새 제목", "새 내용", "2026-01-01")
            .statusCode(200)
            .body("message", equalTo("데일리 메시지가 수정되었습니다"))
        putMessage(message.id!!, "새 제목", "새 내용", "2026-02-01").statusCode(200)

        val saved = dailyMessageJpaRepository.findById(message.id!!).get()
        assertThat(saved.title).isEqualTo("새 제목")
        assertThat(saved.content).isEqualTo("새 내용")
        assertThat(saved.date).isEqualTo(LocalDate.of(2026, 2, 1))
    }

    @Test
    @DisplayName("다른 메시지와 날짜가 겹치면 DM301, 없는 메시지는 DM001")
    fun `update - 날짜 중복과 없는 메시지`() {
        val message = saveMessage(LocalDate.of(2026, 1, 1))
        saveMessage(LocalDate.of(2026, 1, 2))

        putMessage(message.id!!, "제목", "내용", "2026-01-02")
            .statusCode(409)
            .body("code", equalTo(DailyMessageErrorCode.DAILY_MESSAGE_DATE_ALREADY_EXISTS.code))
        putMessage(999999L, "제목", "내용", "2026-03-01")
            .statusCode(404)
            .body("code", equalTo(DailyMessageErrorCode.DAILY_MESSAGE_NOT_FOUND.code))

        assertThat(dailyMessageJpaRepository.findById(message.id!!).get().date).isEqualTo(LocalDate.of(2026, 1, 1))
    }

    private fun putMessage(id: Long, title: String, content: String, date: String): ValidatableResponse {
        return RestAssured.given()
            .sessionId(getAdminSessionId())
            .contentType(ContentType.JSON)
            .body(mapOf("title" to title, "content" to content, "date" to date))
            .`when`()
            .put("/admin/api/v1/dailymessages/{id}", id)
            .then()
    }

    private fun saveMessage(date: LocalDate): DailyMessageJpaEntity {
        return dailyMessageJpaRepository.save(DailyMessageJpaEntity(title = "제목", content = "내용", date = date))
    }
}
