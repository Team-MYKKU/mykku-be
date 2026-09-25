package com.example.mykku.admin.adapter.input.web

import com.example.mykku.BaseControllerTest
import com.example.mykku.common.exception.CommonErrorCode
import com.example.mykku.fannote.adapter.output.persistence.entity.FanNoteJpaEntity
import com.example.mykku.fannote.adapter.output.persistence.entity.FanNotePageJpaEntity
import com.example.mykku.fannote.adapter.output.persistence.repository.FanNoteJpaRepository
import com.example.mykku.fannote.adapter.output.persistence.repository.FanNotePageJpaRepository
import com.example.mykku.fannote.exception.FanNoteErrorCode
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
import java.time.LocalDate

@DisplayName("AdminFanNoteApiController 수정 통합 테스트")
class AdminFanNoteUpdateApiControllerTest : BaseControllerTest() {

    @Autowired
    private lateinit var fanNoteJpaRepository: FanNoteJpaRepository

    @Autowired
    private lateinit var fanNotePageJpaRepository: FanNotePageJpaRepository

    @Test
    @DisplayName("텍스트를 바꾸고 표시값과 기존 페이지를 모두 보내면 페이지와 커버는 그대로다")
    fun `update - 텍스트 수정, 이미지 유지`() {
        val fanNote = saveFanNote()
        val pages = savePages(fanNote, 2)

        putFanNote(fanNote.id!!, basicParts(title = "새 노트") + text("subtitle", "새 부제") + keepParts(pages))
            .statusCode(200)
            .body("message", equalTo("팬노트가 수정되었습니다"))

        val saved = fanNoteJpaRepository.findById(fanNote.id!!).get()
        assertThat(saved.title).isEqualTo("새 노트")
        assertThat(saved.subtitle).isEqualTo("새 부제")
        assertThat(saved.coverImageUrl).isEqualTo(COVER_URL)
        assertThat(pageUrlsOf(fanNote.id!!)).containsExactlyElementsOf(pages)
    }

    @Test
    @DisplayName("뺀 페이지는 지우고 새 페이지를 뒤에 붙이며, 새 커버로 바꾼다")
    fun `update - 페이지와 커버 교체`() {
        val fanNote = saveFanNote()
        val pages = savePages(fanNote, 2)

        val parts = basicParts() + keepParts(listOf(pages[1])) + image("newPageImages") + image("coverImage")
        putFanNote(fanNote.id!!, parts).statusCode(200)

        val urls = pageUrlsOf(fanNote.id!!)
        assertThat(urls).hasSize(2)
        assertThat(urls[0]).isEqualTo(pages[1])
        assertThat(urls[1]).startsWith("https://test-bucket.s3.amazonaws.com/page-")
        assertThat(fanNoteJpaRepository.findById(fanNote.id!!).get().coverImageUrl)
            .startsWith("https://test-bucket.s3.amazonaws.com/cover-")
    }

    @Test
    @DisplayName("removeCoverImage=true면 커버를 지우고, 표시값만 보내면 페이지를 모두 지운다")
    fun `update - 커버 삭제와 페이지 전부 제거`() {
        val fanNote = saveFanNote()
        savePages(fanNote, 2)

        putFanNote(fanNote.id!!, basicParts() + text("removeCoverImage", "true") + keepParts(emptyList()))
            .statusCode(200)

        assertThat(fanNoteJpaRepository.findById(fanNote.id!!).get().coverImageUrl).isNull()
        assertThat(pageUrlsOf(fanNote.id!!)).isEmpty()
    }

    @Test
    @DisplayName("새 커버와 커버 삭제를 함께 보내면 FN102, 남의 페이지 URL은 FN101, 표시값 없음은 C101")
    fun `update - 검증 오류`() {
        val fanNote = saveFanNote()
        val pages = savePages(fanNote, 1)

        val conflict = basicParts() + image("coverImage") + text("removeCoverImage", "true") + keepParts(pages)
        putFanNote(fanNote.id!!, conflict)
            .statusCode(400)
            .body("code", equalTo(FanNoteErrorCode.COVER_IMAGE_CONFLICT.code))
        putFanNote(fanNote.id!!, basicParts() + keepParts(listOf("https://other.example.com/p.jpg")))
            .statusCode(400)
            .body("code", equalTo(FanNoteErrorCode.INVALID_KEEP_IMAGE_URLS.code))
        putFanNote(fanNote.id!!, basicParts())
            .statusCode(400)
            .body("code", equalTo(CommonErrorCode.INVALID_INPUT.code))

        assertThat(pageUrlsOf(fanNote.id!!)).containsExactlyElementsOf(pages)
    }

    @Test
    @DisplayName("없는 덕질노트는 FN001")
    fun `update - 없는 덕질노트`() {
        putFanNote(999999L, basicParts() + keepParts(emptyList()))
            .statusCode(404)
            .body("code", equalTo(FanNoteErrorCode.FAN_NOTE_NOT_FOUND.code))
    }

    private fun putFanNote(fanNoteId: Long, parts: List<MultiPartSpecification>): ValidatableResponse {
        var spec = RestAssured.given()
            .sessionId(getAdminSessionId())
            .contentType("multipart/form-data")
        parts.forEach { spec = spec.multiPart(it) }
        return spec.`when`().put("/admin/api/v1/fannotes/{id}", fanNoteId).then()
    }

    private fun basicParts(title: String = "노트"): List<MultiPartSpecification> {
        return listOf(text("title", title), text("productionDate", "2026-01-01"))
    }

    private fun keepParts(urls: List<String>): List<MultiPartSpecification> {
        return listOf(text("_keepPageImageUrls", "on")) + urls.map { text("keepPageImageUrls", it) }
    }

    private fun saveFanNote(): FanNoteJpaEntity {
        return fanNoteJpaRepository.save(
            FanNoteJpaEntity(
                title = "노트",
                productionDate = LocalDate.of(2025, 12, 1),
                coverImageUrl = COVER_URL
            )
        )
    }

    private fun savePages(fanNote: FanNoteJpaEntity, count: Int): List<String> {
        return (1..count).map { pageNumber ->
            val url = "https://test-bucket.s3.amazonaws.com/fan-note/old-$pageNumber.jpg"
            fanNotePageJpaRepository.save(
                FanNotePageJpaEntity(pageNumber = pageNumber, imageUrl = url, fanNote = fanNote)
            )
            url
        }
    }

    private fun pageUrlsOf(fanNoteId: Long): List<String> {
        return fanNotePageJpaRepository.findByFanNoteIdOrderByPageNumber(fanNoteId).map { it.imageUrl }
    }

    companion object {
        private const val COVER_URL = "https://test-bucket.s3.amazonaws.com/fan-note/old-cover.jpg"
    }
}
