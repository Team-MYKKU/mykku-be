package com.example.mykku.admin.adapter.input.web

import com.example.mykku.BaseControllerTest
import com.example.mykku.common.exception.CommonErrorCode
import com.example.mykku.contest.adapter.output.persistence.entity.ContestJpaEntity
import com.example.mykku.contest.adapter.output.persistence.repository.ContestJpaRepository
import com.example.mykku.contest.domain.vo.ContestStatusType
import com.example.mykku.contest.exception.ContestErrorCode
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

@DisplayName("AdminContestApiController 수정 통합 테스트")
class AdminContestUpdateApiControllerTest : BaseControllerTest() {

    @Autowired
    private lateinit var contestJpaRepository: ContestJpaRepository

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Test
    @DisplayName("텍스트·기간·태그를 바꾸고, 표시값과 기존 URL을 모두 보내면 이미지는 그대로다")
    fun `update - 텍스트·기간·태그 수정`() {
        val contest = saveContest()
        val urls = saveImages(contest, 2)

        putContest(
            contest.id!!,
            basicParts(title = "새 콘테스트", startedAt = "2026-01-02T09:30") + tagParts("새태그", "둘째") + keepParts(urls)
        ).statusCode(200).body("message", equalTo("콘테스트가 수정되었습니다"))

        val saved = contestJpaRepository.findById(contest.id!!).get()
        assertThat(saved.title).isEqualTo("새 콘테스트")
        assertThat(saved.startedAt).isEqualTo(LocalDateTime.of(2026, 1, 2, 9, 30))
        assertThat(tagsOf(contest.id!!)).containsExactlyInAnyOrder("새태그", "둘째")
        assertThat(imageUrlsOf(contest.id!!)).containsExactlyElementsOf(urls)
    }

    @Test
    @DisplayName("뺀 이미지는 지우고 새 이미지를 뒤에 붙인다")
    fun `update - 이미지 교체`() {
        val contest = saveContest()
        val urls = saveImages(contest, 2)

        putContest(contest.id!!, basicParts() + tagParts("덕질") + keepParts(listOf(urls[0])) + image("newImages"))
            .statusCode(200)

        val images = imageUrlsOf(contest.id!!)
        assertThat(images).hasSize(2)
        assertThat(images[0]).isEqualTo(urls[0])
        assertThat(images[1]).startsWith("https://test-bucket.s3.amazonaws.com/contest-images/image-")
    }

    @Test
    @DisplayName("tags를 보내지 않으면 CN113이고 DB 태그는 그대로다")
    fun `update - 태그 미전송`() {
        val contest = saveContest()

        putContest(contest.id!!, basicParts() + keepParts(emptyList()))
            .statusCode(400)
            .body("code", equalTo(ContestErrorCode.CONTEST_TAG_REQUIRED.code))

        assertThat(tagsOf(contest.id!!)).containsExactly("덕질")
    }

    @Test
    @DisplayName("수상자 선정 뒤에는 같은 태그(순서·공백 달라도)와 기간으로 제목만 바꿀 수 있다")
    fun `update - 선정 후 제목만 수정`() {
        val contest = saveContest(status = ContestStatusType.WINNER_SELECTED, tags = listOf("덕질", "굿즈"))

        putContest(contest.id!!, basicParts(title = "제목만 변경") + tagParts(" 굿즈 ", "덕질") + keepParts(emptyList()))
            .statusCode(200)

        assertThat(contestJpaRepository.findById(contest.id!!).get().title).isEqualTo("제목만 변경")
    }

    @Test
    @DisplayName("수상자 선정 뒤 태그나 기간을 바꾸면 CN116")
    fun `update - 선정 후 태그·기간 잠금`() {
        val contest = saveContest(status = ContestStatusType.WINNER_SELECTED)

        putContest(contest.id!!, basicParts() + tagParts("다른태그") + keepParts(emptyList()))
            .statusCode(400)
            .body("code", equalTo(ContestErrorCode.PERIOD_OR_TAGS_LOCKED_AFTER_WINNER_SELECTED.code))
        val periodChanged = basicParts(expiredAt = "2026-02-28T00:00:00") + tagParts("덕질") + keepParts(emptyList())
        putContest(contest.id!!, periodChanged)
            .statusCode(400)
            .body("code", equalTo(ContestErrorCode.PERIOD_OR_TAGS_LOCKED_AFTER_WINNER_SELECTED.code))

        assertThat(tagsOf(contest.id!!)).containsExactly("덕질")
    }

    @Test
    @DisplayName("기간 역전은 CN117, 남의 이미지 URL은 CN112, 11장은 CN101, 표시값 없음은 C101")
    fun `update - 검증 오류`() {
        val contest = saveContest()
        val urls = saveImages(contest, 2)

        putContest(contest.id!!, basicParts(startedAt = "2026-03-01T00:00:00") + tagParts("덕질") + keepParts(urls))
            .statusCode(400)
            .body("code", equalTo(ContestErrorCode.INVALID_CONTEST_PERIOD.code))
        putContest(contest.id!!, basicParts() + tagParts("덕질") + keepParts(listOf("https://other.example.com/x.jpg")))
            .statusCode(400)
            .body("code", equalTo(ContestErrorCode.INVALID_KEEP_IMAGE_URLS.code))
        putContest(contest.id!!, basicParts() + tagParts("덕질") + keepParts(urls) + (1..9).map { image("newImages") })
            .statusCode(400)
            .body("code", equalTo(ContestErrorCode.CONTEST_IMAGE_LIMIT_EXCEEDED.code))
        putContest(contest.id!!, basicParts() + tagParts("덕질"))
            .statusCode(400)
            .body("code", equalTo(CommonErrorCode.INVALID_INPUT.code))

        assertThat(imageUrlsOf(contest.id!!)).containsExactlyElementsOf(urls)
    }

    private fun putContest(contestId: Long, parts: List<MultiPartSpecification>): ValidatableResponse {
        var spec = RestAssured.given()
            .sessionId(getAdminSessionId())
            .contentType("multipart/form-data")
        parts.forEach { spec = spec.multiPart(it) }
        return spec.`when`().put("/admin/api/v1/contests/{contestId}", contestId).then()
    }

    private fun basicParts(
        title: String = "콘테스트",
        startedAt: String = "2026-01-01T00:00:00",
        expiredAt: String = "2026-01-31T00:00:00"
    ): List<MultiPartSpecification> {
        return listOf(text("title", title), text("startedAt", startedAt), text("expiredAt", expiredAt))
    }

    private fun tagParts(vararg tags: String): List<MultiPartSpecification> {
        return tags.map { text("tags", it) }
    }

    private fun keepParts(urls: List<String>): List<MultiPartSpecification> {
        return listOf(text("_keepImageUrls", "on")) + urls.map { text("keepImageUrls", it) }
    }

    private fun saveContest(
        status: ContestStatusType = ContestStatusType.ACTIVE,
        tags: List<String> = listOf("덕질")
    ): ContestJpaEntity {
        val contest = contestJpaRepository.save(
            ContestJpaEntity(
                title = "콘테스트",
                startedAt = LocalDateTime.of(2026, 1, 1, 0, 0),
                expiredAt = LocalDateTime.of(2026, 1, 31, 0, 0),
                status = status,
                thumbnailUrl = "https://test-bucket.s3.amazonaws.com/contest-images/old-thumbnail.jpg"
            )
        )
        tags.forEach {
            jdbcTemplate.update(
                "INSERT INTO contest_tag (title, contest_id, created_at, updated_at) VALUES (?, ?, NOW(), NOW())",
                it,
                contest.id
            )
        }
        return contest
    }

    private fun saveImages(contest: ContestJpaEntity, count: Int): List<String> {
        return (0 until count).map { index ->
            val url = "https://test-bucket.s3.amazonaws.com/contest-images/old-$index.jpg"
            jdbcTemplate.update(
                "INSERT INTO contest_image (url, order_index, contest_id, created_at, updated_at) " +
                    "VALUES (?, ?, ?, NOW(), NOW())",
                url,
                index,
                contest.id
            )
            url
        }
    }

    private fun tagsOf(contestId: Long): List<String> {
        return jdbcTemplate.queryForList(
            "SELECT title FROM contest_tag WHERE contest_id = ?",
            String::class.java,
            contestId
        )
    }

    private fun imageUrlsOf(contestId: Long): List<String> {
        return jdbcTemplate.queryForList(
            "SELECT url FROM contest_image WHERE contest_id = ? ORDER BY order_index",
            String::class.java,
            contestId
        )
    }
}
