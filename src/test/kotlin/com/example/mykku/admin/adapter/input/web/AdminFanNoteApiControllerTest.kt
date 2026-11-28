package com.example.mykku.admin.adapter.input.web

import com.example.mykku.BaseControllerTest
import com.example.mykku.admin.exception.AdminErrorCode
import com.example.mykku.fannote.adapter.output.persistence.entity.FanNoteJpaEntity
import com.example.mykku.fannote.adapter.output.persistence.entity.FanNotePageJpaEntity
import com.example.mykku.fannote.adapter.output.persistence.repository.FanNoteJpaRepository
import com.example.mykku.fannote.adapter.output.persistence.repository.FanNotePageJpaRepository
import com.example.mykku.util.MultipartParts.emptyImage
import com.example.mykku.util.MultipartParts.image
import com.example.mykku.util.MultipartParts.text
import io.restassured.RestAssured
import io.restassured.response.ValidatableResponse
import io.restassured.specification.MultiPartSpecification
import org.assertj.core.api.Assertions.assertThat
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

    @Autowired
    private lateinit var fanNotePageJpaRepository: FanNotePageJpaRepository

    private lateinit var adminSessionId: String

    @BeforeEach
    fun setUp() {
        adminSessionId = getAdminSessionId()
    }

    private fun createFanNote(
        title: String = "테스트 팬노트",
        subtitle: String? = "테스트 부제목",
        content: String? = "테스트 내용",
        productionDate: LocalDate = LocalDate.now(),
        coverImageUrl: String? = null
    ): FanNoteJpaEntity {
        return fanNoteJpaRepository.save(
            FanNoteJpaEntity(
                title = title,
                subtitle = subtitle,
                content = content,
                productionDate = productionDate,
                coverImageUrl = coverImageUrl
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
    @DisplayName("팬노트 생성 - 커버와 페이지 이미지를 올리고, 실제 업로드처럼 빈 페이지 파트는 건너뛴다")
    fun `create - 커버와 페이지 이미지로 팬노트를 생성한다`() {
        val texts = listOf(text("title", "Image FanNote"), text("productionDate", "2025-01-22"))
        val images = listOf(image("coverImage"), image("pageImages"), emptyImage("pageImages"), image("pageImages"))
        val response = postFanNote(texts + images).statusCode(200).extract()

        val uploaded = testImageUploadService.uploadedUrls
        assertThat(uploaded).hasSize(3)
        assertThat(response.path<String>("data.coverImageUrl")).isEqualTo(uploaded[0])
        assertThat(response.path<List<String>>("data.pages.imageUrl")).containsExactly(uploaded[1], uploaded[2])
        assertThat(pageUrlsOf(response.path<Int>("data.id").toLong())).containsExactly(uploaded[1], uploaded[2])
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
    @DisplayName("팬노트 삭제 - 그 팬노트의 커버와 페이지 이미지만 지우고, 다른 팬노트의 이미지는 남긴다")
    fun `delete - 커버와 페이지 이미지를 함께 지운다`() {
        val fanNote = createFanNote(title = "삭제할 팬노트", coverImageUrl = COVER_URL)
        val pageIds = savePages(fanNote, PAGE_URL_1, PAGE_URL_2)
        savePages(createFanNote(title = "남는 팬노트", coverImageUrl = KEPT_COVER_URL), KEPT_PAGE_URL)

        RestAssured.given()
            .sessionId(adminSessionId)
            .`when`()
            .delete("/admin/api/v1/fannotes/${fanNote.id}")
            .then()
            .statusCode(200)

        assertThat(testImageUploadService.deletedUrls).containsExactly(COVER_URL, PAGE_URL_1, PAGE_URL_2)
        assertThat(pageIds.filter { fanNotePageJpaRepository.existsById(it) }).isEmpty()
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

    private fun postFanNote(parts: List<MultiPartSpecification>): ValidatableResponse {
        var spec = RestAssured.given()
            .sessionId(adminSessionId)
            .contentType("multipart/form-data")
        parts.forEach { spec = spec.multiPart(it) }
        return spec.`when`().post("/admin/api/v1/fannotes").then()
    }

    private fun savePages(fanNote: FanNoteJpaEntity, vararg urls: String): List<Long> {
        return urls.mapIndexed { index, url ->
            val page = FanNotePageJpaEntity(pageNumber = index + 1, imageUrl = url, fanNote = fanNote)
            fanNotePageJpaRepository.save(page).id!!
        }
    }

    private fun pageUrlsOf(fanNoteId: Long): List<String> {
        return fanNotePageJpaRepository.findByFanNoteIdOrderByPageNumber(fanNoteId).map { it.imageUrl }
    }

    companion object {
        private const val COVER_URL = "https://test-bucket.s3.amazonaws.com/fan-note/cover.jpg"
        private const val PAGE_URL_1 = "https://test-bucket.s3.amazonaws.com/fan-note/1.jpg"
        private const val PAGE_URL_2 = "https://test-bucket.s3.amazonaws.com/fan-note/2.jpg"
        private const val KEPT_COVER_URL = "https://test-bucket.s3.amazonaws.com/fan-note/kept-cover.jpg"
        private const val KEPT_PAGE_URL = "https://test-bucket.s3.amazonaws.com/fan-note/kept-1.jpg"
    }
}
