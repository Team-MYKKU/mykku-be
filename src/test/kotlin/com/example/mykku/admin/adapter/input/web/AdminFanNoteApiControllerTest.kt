package com.example.mykku.admin.adapter.input.web

import com.example.mykku.BaseControllerTest
import com.example.mykku.admin.exception.AdminErrorCode
import com.example.mykku.fannote.adapter.output.persistence.entity.FanNoteJpaEntity
import com.example.mykku.fannote.adapter.output.persistence.repository.FanNoteJpaRepository
import io.restassured.RestAssured
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.notNullValue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

@DisplayName("AdminFanNoteApiController 통합 테스트")
class AdminFanNoteApiControllerTest : BaseControllerTest() {

    @Autowired
    private lateinit var fanNoteJpaRepository: FanNoteJpaRepository

    private lateinit var adminSessionId: String

    @BeforeEach
    fun setUp() {
        adminSessionId = getAdminSessionId()
    }

    private fun createFanNote(
        title: String = "테스트 팬노트",
        subtitle: String? = "테스트 부제목",
        content: String? = "테스트 내용",
        productionDate: LocalDate = LocalDate.now()
    ): FanNoteJpaEntity {
        return fanNoteJpaRepository.save(
            FanNoteJpaEntity(
                title = title,
                subtitle = subtitle,
                content = content,
                productionDate = productionDate,
                coverImageUrl = null
            )
        )
    }

    @Test
    @DisplayName("팬노트 생성 - 정상 케이스")
    fun `create - 정상적으로 팬노트를 생성한다`() {
        RestAssured.given()
            .sessionId(adminSessionId)
            .contentType("multipart/form-data")
            .multiPart("title", "New FanNote")
            .multiPart("subtitle", "FanNote Subtitle")
            .multiPart("content", "FanNote Content")
            .multiPart("productionDate", "2025-01-22")
            .`when`()
            .post("/admin/api/v1/fannotes")
            .then()
            .statusCode(200)
            .body("message", equalTo("팬노트가 생성되었습니다"))
            .body("data", notNullValue())
            .body("data.title", equalTo("New FanNote"))
    }

    @Test
    @DisplayName("팬노트 생성 - 필수 필드만으로 생성")
    fun `create - 필수 필드만으로 팬노트를 생성한다`() {
        RestAssured.given()
            .sessionId(adminSessionId)
            .contentType("multipart/form-data")
            .multiPart("title", "Required Fields Only")
            .multiPart("productionDate", "2025-01-22")
            .`when`()
            .post("/admin/api/v1/fannotes")
            .then()
            .statusCode(200)
            .body("message", equalTo("팬노트가 생성되었습니다"))
            .body("data.title", equalTo("Required Fields Only"))
    }

    @Test
    @DisplayName("팬노트 생성 - 관리자 세션 없이 요청 시 실패")
    fun `create - 관리자 세션 없이 요청하면 실패한다`() {
        RestAssured.given()
            .contentType("multipart/form-data")
            .multiPart("title", "새로운 팬노트")
            .multiPart("productionDate", "2025-01-22")
            .`when`()
            .post("/admin/api/v1/fannotes")
            .then()
            .statusCode(401)
            .body("code", equalTo(AdminErrorCode.UNAUTHORIZED.code))
    }

    @Test
    @DisplayName("팬노트 삭제 - 정상 케이스")
    fun `delete - 정상적으로 팬노트를 삭제한다`() {
        val fanNote = createFanNote(
            title = "삭제할 팬노트",
            productionDate = LocalDate.of(2025, 1, 21)
        )

        RestAssured.given()
            .sessionId(adminSessionId)
            .`when`()
            .delete("/admin/api/v1/fannotes/${fanNote.id}")
            .then()
            .statusCode(200)
            .body("message", equalTo("팬노트가 삭제되었습니다"))
    }

    @Test
    @DisplayName("팬노트 삭제 - 관리자 세션 없이 요청 시 실패")
    fun `delete - 관리자 세션 없이 요청하면 실패한다`() {
        val fanNote = createFanNote()

        RestAssured.given()
            .`when`()
            .delete("/admin/api/v1/fannotes/${fanNote.id}")
            .then()
            .statusCode(401)
            .body("code", equalTo(AdminErrorCode.UNAUTHORIZED.code))
    }

    @Test
    @DisplayName("팬노트 삭제 - 존재하지 않는 팬노트")
    fun `delete - 존재하지 않는 팬노트를 삭제하면 실패한다`() {
        val nonExistentId = 99999L

        RestAssured.given()
            .sessionId(adminSessionId)
            .`when`()
            .delete("/admin/api/v1/fannotes/$nonExistentId")
            .then()
            .statusCode(404)
    }
}
