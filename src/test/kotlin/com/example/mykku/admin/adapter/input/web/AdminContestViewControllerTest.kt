package com.example.mykku.admin.adapter.input.web

import com.example.mykku.BaseControllerTest
import com.example.mykku.contest.adapter.output.persistence.entity.ContestJpaEntity
import com.example.mykku.contest.adapter.output.persistence.entity.ContestParticipationJpaEntity
import com.example.mykku.contest.adapter.output.persistence.entity.ContestTagJpaEntity
import com.example.mykku.contest.adapter.output.persistence.repository.ContestJpaRepository
import com.example.mykku.contest.adapter.output.persistence.repository.ContestParticipationJpaRepository
import com.example.mykku.contest.adapter.output.persistence.repository.ContestTagJpaRepository
import com.example.mykku.contest.domain.vo.ContestStatusType
import com.example.mykku.feed.adapter.output.persistence.FeedJpaRepository
import com.example.mykku.feed.adapter.output.persistence.entity.FeedJpaEntity
import com.example.mykku.member.adapter.output.persistence.entity.MemberJpaEntity
import io.restassured.RestAssured
import io.restassured.http.ContentType
import io.restassured.response.ValidatableResponse
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.endsWith
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import java.time.LocalDateTime

@DisplayName("AdminContestViewController 통합 테스트")
class AdminContestViewControllerTest : BaseControllerTest() {

    @Autowired
    private lateinit var contestJpaRepository: ContestJpaRepository

    @Autowired
    private lateinit var contestParticipationJpaRepository: ContestParticipationJpaRepository

    @Autowired
    private lateinit var contestTagJpaRepository: ContestTagJpaRepository

    @Autowired
    private lateinit var feedJpaRepository: FeedJpaRepository

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Test
    @DisplayName("status를 생략하면 전체 탭이 활성화되고 모든 콘테스트가 보인다")
    fun `listPage - 기본 ALL`() {
        val adminSessionId = getAdminSessionId()
        createAndSaveContest(title = "종료된 콘테스트", expiredAt = LocalDateTime.now().minusDays(1))
        createAndSaveContest(title = "진행중 콘테스트", expiredAt = LocalDateTime.now().plusDays(7))

        RestAssured.given()
            .sessionId(adminSessionId)
            .`when`()
            .get("/admin/contest")
            .then()
            .statusCode(200)
            .body(containsString("종료된 콘테스트"))
            .body(containsString("진행중 콘테스트"))
            .body(containsString(activeTab("ALL")))
            .body(not(containsString(activeTab("ACTIVE"))))
            .body(containsString("class=\"badge badge-active\""))
            .body(containsString("class=\"badge badge-expired\""))
    }

    @Test
    @DisplayName("PENDING_SELECTION 필터는 종료됐고 선정하지 않은 콘테스트만 보여 준다")
    fun `listPage - 선정 대기 필터`() {
        val adminSessionId = getAdminSessionId()
        val past = LocalDateTime.now().minusDays(1)
        createAndSaveContest(title = "선정 대기 콘테스트", expiredAt = past)
        createAndSaveContest(title = "선정 완료 콘테스트", expiredAt = past, status = ContestStatusType.WINNER_SELECTED)
        createAndSaveContest(title = "진행중 콘테스트", expiredAt = LocalDateTime.now().plusDays(7))

        RestAssured.given()
            .sessionId(adminSessionId)
            .queryParam("status", "PENDING_SELECTION")
            .`when`()
            .get("/admin/contest")
            .then()
            .statusCode(200)
            .body(containsString("선정 대기 콘테스트"))
            .body(not(containsString("선정 완료 콘테스트")))
            .body(not(containsString("진행중 콘테스트")))
            .body(containsString(activeTab("PENDING_SELECTION")))
            .body(containsString("class=\"badge badge-expired\""))
    }

    @Test
    @DisplayName("WINNER_SELECTED 필터는 선정 완료 콘테스트만 보여 준다")
    fun `listPage - 선정 완료 필터`() {
        val adminSessionId = getAdminSessionId()
        val past = LocalDateTime.now().minusDays(1)
        createAndSaveContest(title = "선정 완료 콘테스트", expiredAt = past, status = ContestStatusType.WINNER_SELECTED)
        createAndSaveContest(title = "선정 대기 콘테스트", expiredAt = past)

        RestAssured.given()
            .sessionId(adminSessionId)
            .queryParam("status", "WINNER_SELECTED")
            .`when`()
            .get("/admin/contest")
            .then()
            .statusCode(200)
            .body(containsString("선정 완료 콘테스트"))
            .body(not(containsString("선정 대기 콘테스트")))
            .body(containsString("class=\"badge badge-winner-selected\""))
            .body(not(containsString("class=\"badge badge-expired\"")))
    }

    @Test
    @DisplayName("ACTIVE 필터에서 시작 전 콘테스트는 예정, 시작된 콘테스트는 진행중 배지로 보인다")
    fun `listPage - 시작 전이면 예정 배지`() {
        val adminSessionId = getAdminSessionId()
        createAndSaveContest(
            title = "예정 콘테스트",
            startedAt = LocalDateTime.now().plusDays(1),
            expiredAt = LocalDateTime.now().plusDays(7)
        )
        createAndSaveContest(title = "시작된 콘테스트", expiredAt = LocalDateTime.now().plusDays(7))

        RestAssured.given()
            .sessionId(adminSessionId)
            .queryParam("status", "ACTIVE")
            .`when`()
            .get("/admin/contest")
            .then()
            .statusCode(200)
            .body(containsString("class=\"badge badge-upcoming\">예정</span>"))
            .body(containsString("class=\"badge badge-active\">진행중</span>"))
    }

    @Test
    @DisplayName("페이지 링크는 현재 status를 유지한다")
    fun `listPage - 페이지 링크 status 유지`() {
        val adminSessionId = getAdminSessionId()
        repeat(21) { index ->
            createAndSaveContest(title = "종료 콘테스트 $index", expiredAt = LocalDateTime.now().minusDays(1))
        }

        RestAssured.given()
            .sessionId(adminSessionId)
            .queryParam("status", "PENDING_SELECTION")
            .`when`()
            .get("/admin/contest")
            .then()
            .statusCode(200)
            .body(containsString("page=1&amp;status=PENDING_SELECTION"))
    }

    @Test
    @DisplayName("세션 없이 접근하면 로그인으로 리다이렉트된다")
    fun `listPage - 미인증이면 302`() {
        RestAssured.given()
            .redirects().follow(false)
            .`when`()
            .get("/admin/contest")
            .then()
            .statusCode(302)
            .header("Location", endsWith("/admin/login"))
    }

    @Test
    @DisplayName("목록의 각 행에 참여자·수상자 화면 링크가 있다")
    fun `listPage - 참여자 링크`() {
        val adminSessionId = getAdminSessionId()
        val contest = createAndSaveContest(title = "링크 콘테스트", expiredAt = LocalDateTime.now().minusDays(1))

        getPage(adminSessionId, "/admin/contest")
            .statusCode(200)
            .body(containsString("href=\"/admin/contest/${contest.id}/participants\""))
    }

    @Test
    @DisplayName("목록의 삭제 버튼에 참여·수상 건수를 싣는다")
    fun `listPage - 삭제 확인용 건수`() {
        val adminSessionId = getAdminSessionId()
        val contest = createAndSaveContest(title = "건수 콘테스트", expiredAt = LocalDateTime.now().minusDays(1))
        val member = createAndSaveMember()
        val participation = saveParticipation(contest, member, "피드")
        saveParticipation(contest, member, "다른 피드")
        selectWinner(adminSessionId, contest.id!!, participation.id!!)

        getPage(adminSessionId, "/admin/contest")
            .statusCode(200)
            .body(containsString("data-id=\"${contest.id}\""))
            .body(containsString("data-participation-count=\"2\""))
            .body(containsString("data-winner-count=\"1\""))
    }

    @Test
    @DisplayName("참여자 화면은 참여작·작성자·표시를 보여 주고 공지가 없으면 빈 폼을 띄운다")
    fun `participantsPage - 참여작 목록과 빈 공지 폼`() {
        val adminSessionId = getAdminSessionId()
        val contest = createAndSaveContest(title = "참여 콘테스트", expiredAt = LocalDateTime.now().minusDays(1))
        saveTag(contest, "덕질")
        val memberA = createAndSaveMember(
            memberId = "memberA",
            nickname = "작성자A",
            email = "a@example.com",
            socialId = "a1"
        )
        val participation = saveParticipation(contest, memberA, "피드A")
        saveParticipation(contest, null, "탈퇴자 피드")

        val body = getPage(adminSessionId, "/admin/contest/${contest.id}/participants")
            .statusCode(200)
            .body(containsString("class=\"nav-link active\" href=\"/admin/contest\""))
            .body(containsString("data-participation-id=\"${participation.id}\""))
            .body(containsString("id=\"announcement-empty\""))
            .body(not(containsString("id=\"no-tag-warning\"")))
            .body(not(containsString("id=\"not-selectable-notice\"")))
            .extract().asString()

        val rowA = rowOf(body, "피드A")
        assertThat(rowA).contains("memberA", "작성자A", "이미지 없음").doesNotContain("탈퇴한 회원")
        assertThat(rowOf(body, "탈퇴자 피드")).contains("탈퇴한 회원")
    }

    @Test
    @DisplayName("참여작은 참여일 최신순으로 보여 준다")
    fun `participantsPage - 참여일 최신순`() {
        val adminSessionId = getAdminSessionId()
        val contest = createAndSaveContest(title = "정렬 콘테스트", expiredAt = LocalDateTime.now().minusDays(1))
        val member = createAndSaveMember()
        val older = saveParticipation(contest, member, "오래된 피드")
        saveParticipation(contest, member, "최근 피드")
        setParticipatedAt(older.id!!, LocalDateTime.now().minusDays(3))

        val body = getPage(adminSessionId, "/admin/contest/${contest.id}/participants")
            .statusCode(200)
            .extract().asString()

        assertThat(body.indexOf("최근 피드")).isLessThan(body.indexOf("오래된 피드"))
    }

    @Test
    @DisplayName("태그가 없는 콘테스트에는 참여가 기록되지 않는다는 경고를 띄운다")
    fun `participantsPage - 태그 0개 경고`() {
        val adminSessionId = getAdminSessionId()
        val contest = createAndSaveContest(title = "태그 없는 콘테스트", expiredAt = LocalDateTime.now().minusDays(1))

        getPage(adminSessionId, "/admin/contest/${contest.id}/participants")
            .statusCode(200)
            .body(containsString("id=\"no-tag-warning\""))
    }

    @Test
    @DisplayName("시작 전에 작성된 참여작에만 '시작 전 참여' 표시를 단다")
    fun `participantsPage - 시작 전 참여 표시`() {
        val adminSessionId = getAdminSessionId()
        val contest = createAndSaveContest(
            title = "시작 전 콘테스트",
            startedAt = LocalDateTime.now().minusDays(1),
            expiredAt = LocalDateTime.now().minusHours(1)
        )
        val member = createAndSaveMember()
        val early = saveParticipation(contest, member, "일찍 쓴 피드")
        saveParticipation(contest, member, "제때 쓴 피드")
        setParticipatedAt(early.id!!, LocalDateTime.now().minusDays(2))

        val body = getPage(adminSessionId, "/admin/contest/${contest.id}/participants")
            .statusCode(200)
            .extract().asString()

        assertThat(rowOf(body, "일찍 쓴 피드")).contains(">시작 전 참여<")
        assertThat(rowOf(body, "제때 쓴 피드")).doesNotContain(">시작 전 참여<")
    }

    @Test
    @DisplayName("종료 전인 콘테스트는 선정 버튼을 끄고 안내를 띄운다")
    fun `participantsPage - 종료 전이면 선정 불가 안내`() {
        val adminSessionId = getAdminSessionId()
        val contest = createAndSaveContest(title = "진행중 콘테스트", expiredAt = LocalDateTime.now().plusDays(3))

        getPage(adminSessionId, "/admin/contest/${contest.id}/participants")
            .statusCode(200)
            .body(containsString("id=\"not-selectable-notice\""))
            .body(containsString("id=\"submit-winners\" class=\"btn btn-primary\" disabled"))
    }

    @Test
    @DisplayName("피드 제목은 이스케이프하고, 현재 수상자와 공지를 미리 채운다")
    fun `participantsPage - XSS 이스케이프와 현재 수상자·공지 미리 채움`() {
        val adminSessionId = getAdminSessionId()
        val contest = createAndSaveContest(title = "수상 콘테스트", expiredAt = LocalDateTime.now().minusDays(1))
        val member = createAndSaveMember()
        val participation = saveParticipation(contest, member, "<script>alert(1)</script>")
        selectWinner(adminSessionId, contest.id!!, participation.id!!)
        saveAnnouncement(adminSessionId, contest.id!!)

        val body = getPage(adminSessionId, "/admin/contest/${contest.id}/participants")
            .statusCode(200)
            .body(containsString(">&lt;script&gt;alert(1)&lt;/script&gt;<"))
            .body(containsString("data-feed-title=\"&lt;script&gt;alert(1)&lt;/script&gt;\""))
            .body(not(containsString("<script>alert(1)")))
            .body(containsString("value=\"수상자 발표\""))
            .body(containsString("value=\"2025-10-10\""))
            .body(not(containsString("id=\"announcement-empty\"")))
            .extract().asString()

        val winnersStart = body.indexOf("id=\"current-winners\"")
        val currentWinners = body.substring(winnersStart, body.indexOf("</ul>", winnersStart))
        assertThat(currentWinners).contains("data-rank=\"1\"", "data-participation-id=\"${participation.id}\"")
    }

    @Test
    @DisplayName("없는 콘테스트의 참여자 화면은 404")
    fun `participantsPage - 없는 콘테스트면 404`() {
        val adminSessionId = getAdminSessionId()

        getPage(adminSessionId, "/admin/contest/999999/participants")
            .statusCode(404)
    }

    @Test
    @DisplayName("세션 없이 참여자 화면에 접근하면 로그인으로 리다이렉트된다")
    fun `participantsPage - 미인증이면 302`() {
        RestAssured.given()
            .redirects().follow(false)
            .`when`()
            .get("/admin/contest/1/participants")
            .then()
            .statusCode(302)
            .header("Location", endsWith("/admin/login"))
    }

    private fun getPage(adminSessionId: String, path: String): ValidatableResponse {
        return RestAssured.given()
            .sessionId(adminSessionId)
            .`when`()
            .get(path)
            .then()
    }

    private fun rowOf(body: String, feedTitle: String): String {
        val titleIndex = body.indexOf(">$feedTitle<")
        val rowStart = body.lastIndexOf("<tr", titleIndex)
        return body.substring(rowStart, body.indexOf("</tr>", titleIndex))
    }

    private fun saveTag(contest: ContestJpaEntity, title: String) {
        contestTagJpaRepository.save(ContestTagJpaEntity(title = title, contest = contest))
    }

    private fun saveParticipation(
        contest: ContestJpaEntity,
        member: MemberJpaEntity?,
        feedTitle: String
    ): ContestParticipationJpaEntity {
        val feed = feedJpaRepository.save(
            FeedJpaEntity(title = feedTitle, content = "내용", member = member, board = createAndSaveBoard())
        )
        return contestParticipationJpaRepository.save(
            ContestParticipationJpaEntity(member = member, contest = contest, feed = feed)
        )
    }

    private fun setParticipatedAt(participationId: Long, participatedAt: LocalDateTime) {
        jdbcTemplate.update(
            "UPDATE contest_participation SET created_at = ? WHERE id = ?",
            participatedAt,
            participationId
        )
    }

    private fun selectWinner(adminSessionId: String, contestId: Long, participationId: Long) {
        RestAssured.given()
            .sessionId(adminSessionId)
            .contentType(ContentType.JSON)
            .body(mapOf("winners" to listOf(mapOf("participationId" to participationId, "winnerRank" to 1))))
            .`when`()
            .post("/admin/api/v1/contests/{contestId}/winners", contestId)
            .then()
            .statusCode(200)
    }

    private fun saveAnnouncement(adminSessionId: String, contestId: Long) {
        RestAssured.given()
            .sessionId(adminSessionId)
            .contentType(ContentType.JSON)
            .body(mapOf("title" to "수상자 발표", "content" to "축하합니다", "announcedAt" to "2025-10-10"))
            .`when`()
            .put("/admin/api/v1/contests/{contestId}/winner-announcement", contestId)
            .then()
            .statusCode(200)
    }

    private fun activeTab(status: String): String {
        return "class=\"nav-link active\" href=\"/admin/contest?status=$status\""
    }

    private fun createAndSaveContest(
        title: String,
        startedAt: LocalDateTime = LocalDateTime.now().minusDays(10),
        expiredAt: LocalDateTime,
        status: ContestStatusType = ContestStatusType.ACTIVE
    ): ContestJpaEntity {
        val contest = ContestJpaEntity(
            title = title,
            startedAt = startedAt,
            expiredAt = expiredAt,
            status = status,
            thumbnailUrl = "https://example.com/thumbnail.jpg"
        )
        return contestJpaRepository.save(contest)
    }
}
