package com.example.mykku.admin.adapter.input.web

import com.example.mykku.BaseControllerTest
import com.example.mykku.contest.adapter.output.persistence.entity.ContestJpaEntity
import com.example.mykku.contest.adapter.output.persistence.repository.ContestJpaRepository
import com.example.mykku.contest.domain.vo.ContestStatusType
import com.example.mykku.dailymessage.adapter.output.persistence.entity.DailyMessageJpaEntity
import com.example.mykku.dailymessage.adapter.output.persistence.repository.DailyMessageJpaRepository
import com.example.mykku.event.adapter.output.persistence.entity.EventJpaEntity
import com.example.mykku.event.adapter.output.persistence.repository.EventJpaRepository
import com.example.mykku.event.domain.vo.EventStatusType
import com.example.mykku.event.domain.vo.EventType
import com.example.mykku.fannote.adapter.output.persistence.entity.FanNoteJpaEntity
import com.example.mykku.fannote.adapter.output.persistence.entity.FanNotePageJpaEntity
import com.example.mykku.fannote.adapter.output.persistence.repository.FanNoteJpaRepository
import com.example.mykku.fannote.adapter.output.persistence.repository.FanNotePageJpaRepository
import io.restassured.RestAssured
import io.restassured.response.ValidatableResponse
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.endsWith
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("어드민 수정 화면 통합 테스트")
class AdminEditViewControllerTest : BaseControllerTest() {

    @Autowired
    private lateinit var eventJpaRepository: EventJpaRepository

    @Autowired
    private lateinit var contestJpaRepository: ContestJpaRepository

    @Autowired
    private lateinit var fanNoteJpaRepository: FanNoteJpaRepository

    @Autowired
    private lateinit var fanNotePageJpaRepository: FanNotePageJpaRepository

    @Autowired
    private lateinit var dailyMessageJpaRepository: DailyMessageJpaRepository

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    private lateinit var adminSessionId: String

    @BeforeEach
    fun setUp() {
        adminSessionId = getAdminSessionId()
    }

    @Test
    @DisplayName("이벤트 수정 화면은 현재 값과 이미지를 채우고, 목록에 수정 링크가 있다")
    fun `event editPage - 현재 값 채움`() {
        val event = eventJpaRepository.save(
            EventJpaEntity(
                title = "수정할 이벤트",
                subTitle = "부제",
                startedAt = LocalDateTime.of(2026, 1, 1, 9, 30, 15),
                expiredAt = LocalDateTime.of(2026, 1, 31, 0, 0),
                status = EventStatusType.ACTIVE,
                thumbnailUrl = "https://test-bucket.s3.amazonaws.com/e/thumb.jpg"
            )
        )
        jdbcTemplate.update(
            "INSERT INTO event_image (url, order_index, event_id, created_at, updated_at) " +
                "VALUES ('https://test-bucket.s3.amazonaws.com/e/1.jpg', 0, ?, NOW(), NOW())",
            event.id
        )

        getPage("/admin/event/${event.id}/edit")
            .statusCode(200)
            .body(containsString("value=\"수정할 이벤트\""))
            .body(containsString("value=\"2026-01-01T09:30:15\""))
            .body(containsString("data-url=\"https://test-bucket.s3.amazonaws.com/e/1.jpg\""))
            .body(not(containsString("id=\"period-locked-notice\"")))
        getPage("/admin/event")
            .statusCode(200)
            .body(containsString("href=\"/admin/event/${event.id}/edit\""))
    }

    @Test
    @DisplayName("GOOGLE_FORM 이벤트 수정 화면은 유형을 읽기 전용으로 보이고 구글폼 링크 입력에 현재 값을 채운다")
    fun `event editPage - 구글폼 유형`() {
        val event = saveTypedEvent(EventType.GOOGLE_FORM, FORM_URL)

        getPage("/admin/event/${event.id}/edit")
            .statusCode(200)
            .body(containsString("id=\"eventType\" value=\"구글폼 제출\" data-event-type=\"GOOGLE_FORM\" readonly"))
            .body(containsString("id=\"googleFormUrl\" maxlength=\"500\" value=\"$FORM_URL\""))
            .body(not(containsString("id=\"attendance-period-notice\"")))
    }

    @Test
    @DisplayName("GENERAL 이벤트 수정 화면에는 구글폼 링크 입력이 없고, 출석체크 이벤트는 기간 변경 경고를 띄운다")
    fun `event editPage - 일반과 출석체크 유형`() {
        val general = saveTypedEvent(EventType.GENERAL, null)
        val attendance = saveTypedEvent(EventType.ATTENDANCE, null)

        getPage("/admin/event/${general.id}/edit")
            .statusCode(200)
            .body(containsString("value=\"일반 이벤트(앱 안 참여 없음)\" data-event-type=\"GENERAL\" readonly"))
            .body(not(containsString("id=\"googleFormUrl\"")))
            .body(not(containsString("id=\"attendance-period-notice\"")))
        getPage("/admin/event/${attendance.id}/edit")
            .statusCode(200)
            .body(containsString("data-event-type=\"ATTENDANCE\" readonly"))
            .body(containsString("id=\"attendance-period-notice\""))
            .body(not(containsString("id=\"googleFormUrl\"")))
    }

    @Test
    @DisplayName("수상자를 선정한 콘테스트 수정 화면은 기간·태그를 잠그고 현재 태그를 채운다")
    fun `contest editPage - 선정 후 잠금`() {
        val contest = contestJpaRepository.save(
            ContestJpaEntity(
                title = "수정할 콘테스트",
                subTitle = "콘테스트 부제",
                startedAt = LocalDateTime.of(2026, 1, 1, 0, 0),
                expiredAt = LocalDateTime.of(2026, 1, 31, 0, 0),
                status = ContestStatusType.WINNER_SELECTED,
                thumbnailUrl = "https://test-bucket.s3.amazonaws.com/c/thumb.jpg"
            )
        )
        listOf("덕질", "굿즈").forEach {
            jdbcTemplate.update(
                "INSERT INTO contest_tag (title, contest_id, created_at, updated_at) VALUES (?, ?, NOW(), NOW())",
                it,
                contest.id
            )
        }

        getPage("/admin/contest/${contest.id}/edit")
            .statusCode(200)
            .body(containsString("value=\"수정할 콘테스트\""))
            .body(containsString("value=\"콘테스트 부제\""))
            .body(containsString("value=\"덕질, 굿즈\""))
            .body(containsString("id=\"period-locked-notice\""))
            .body(containsString("readonly=\"readonly\""))
        getPage("/admin/contest")
            .statusCode(200)
            .body(containsString("href=\"/admin/contest/${contest.id}/edit\""))
    }

    @Test
    @DisplayName("덕질노트 수정 화면은 현재 값·커버·페이지를 채운다")
    fun `fannote editForm - 현재 값 채움`() {
        val fanNote = fanNoteJpaRepository.save(
            FanNoteJpaEntity(
                title = "수정할 노트",
                productionDate = LocalDate.of(2025, 12, 1),
                coverImageUrl = "https://test-bucket.s3.amazonaws.com/f/cover.jpg"
            )
        )
        fanNotePageJpaRepository.save(
            FanNotePageJpaEntity(
                pageNumber = 1,
                imageUrl = "https://test-bucket.s3.amazonaws.com/f/1.jpg",
                fanNote = fanNote
            )
        )

        getPage("/admin/fannote/${fanNote.id}/edit")
            .statusCode(200)
            .body(containsString("value=\"수정할 노트\""))
            .body(containsString("value=\"2025-12-01\""))
            .body(containsString("src=\"https://test-bucket.s3.amazonaws.com/f/cover.jpg\""))
            .body(containsString("data-url=\"https://test-bucket.s3.amazonaws.com/f/1.jpg\""))
        getPage("/admin/fannote")
            .statusCode(200)
            .body(containsString("href=\"/admin/fannote/${fanNote.id}/edit\""))
    }

    @Test
    @DisplayName("데일리 메시지 수정 화면은 현재 값과 날짜 변경 안내를 보여 준다")
    fun `dailymessage editPage - 현재 값 채움`() {
        val message = dailyMessageJpaRepository.save(
            DailyMessageJpaEntity(title = "수정할 메시지", content = "내용", date = LocalDate.of(2026, 1, 5))
        )

        getPage("/admin/dailymessage/${message.id}/edit")
            .statusCode(200)
            .body(containsString("value=\"수정할 메시지\""))
            .body(containsString("value=\"2026-01-05\""))
            .body(containsString("날짜를 오늘 이후로 바꾸면"))
        getPage("/admin/dailymessage")
            .statusCode(200)
            .body(containsString("href=\"/admin/dailymessage/${message.id}/edit\""))
    }

    @Test
    @DisplayName("세션 없이 수정 화면에 접근하면 로그인으로 리다이렉트된다")
    fun `editPage - 미인증이면 302`() {
        RestAssured.given()
            .redirects().follow(false)
            .`when`()
            .get("/admin/event/1/edit")
            .then()
            .statusCode(302)
            .header("Location", endsWith("/admin/login"))
    }

    private fun saveTypedEvent(type: EventType, googleFormUrl: String?): EventJpaEntity {
        return eventJpaRepository.save(
            EventJpaEntity(
                title = "${type.name} 이벤트",
                startedAt = LocalDateTime.of(2026, 12, 1, 0, 0),
                expiredAt = LocalDateTime.of(2026, 12, 7, 23, 59, 59),
                status = EventStatusType.ACTIVE,
                thumbnailUrl = "https://test-bucket.s3.amazonaws.com/e/thumb.jpg",
                type = type,
                googleFormUrl = googleFormUrl
            )
        )
    }

    private fun getPage(path: String): ValidatableResponse {
        return RestAssured.given()
            .sessionId(adminSessionId)
            .`when`()
            .get(path)
            .then()
    }

    companion object {
        private const val FORM_URL = "https://docs.google.com/forms/d/e/1FAIpQLSc_example/viewform"
    }
}
