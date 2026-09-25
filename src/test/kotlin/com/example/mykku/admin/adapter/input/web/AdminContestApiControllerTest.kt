package com.example.mykku.admin.adapter.input.web

import com.example.mykku.BaseControllerTest
import com.example.mykku.admin.exception.AdminErrorCode
import com.example.mykku.contest.adapter.output.persistence.entity.ContestJpaEntity
import com.example.mykku.contest.adapter.output.persistence.entity.ContestParticipationJpaEntity
import com.example.mykku.contest.adapter.output.persistence.repository.ContestJpaRepository
import com.example.mykku.contest.adapter.output.persistence.repository.ContestParticipationJpaRepository
import com.example.mykku.contest.adapter.output.persistence.repository.ContestWinnerJpaRepository
import com.example.mykku.contest.domain.vo.ContestStatusType
import com.example.mykku.contest.exception.ContestErrorCode
import com.example.mykku.feed.adapter.output.persistence.FeedJpaRepository
import com.example.mykku.feed.adapter.output.persistence.entity.FeedJpaEntity
import com.example.mykku.member.adapter.output.persistence.entity.MemberJpaEntity
import io.restassured.RestAssured
import io.restassured.http.ContentType
import io.restassured.response.ValidatableResponse
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.notNullValue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import java.time.LocalDateTime

@DisplayName("AdminContestController 통합 테스트")
class AdminContestApiControllerTest : BaseControllerTest() {

    @Autowired
    private lateinit var contestJpaRepository: ContestJpaRepository

    @Autowired
    private lateinit var contestParticipationJpaRepository: ContestParticipationJpaRepository

    @Autowired
    private lateinit var feedJpaRepository: FeedJpaRepository

    @Autowired
    private lateinit var contestWinnerJpaRepository: ContestWinnerJpaRepository

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Test
    @DisplayName("수상자 선정 - 정상 케이스")
    fun `setWinners - 관리자가 정상적으로 수상자를 선정한다`() {
        val adminSessionId = getAdminSessionId()
        val member = createAndSaveMember()
        val board = createAndSaveBoard()
        val contest = createAndSaveContest()
        val feed = createAndSaveFeed(member, board)
        val participation = createAndSaveParticipation(member, contest, feed)

        val request = mapOf(
            "winners" to listOf(
                mapOf(
                    "participationId" to participation.id,
                    "winnerRank" to 1,
                    "description" to "1등 수상작입니다."
                )
            )
        )

        RestAssured.given()
            .sessionId(adminSessionId)
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .post("/admin/api/v1/contests/{contestId}/winners", contest.id)
            .then()
            .statusCode(200)
            .body("message", equalTo("수상자가 성공적으로 선정되었습니다."))
            .body("data.contestId", equalTo(contest.id!!.toInt()))
            .body("data.winners", notNullValue())
    }

    @Test
    @DisplayName("수상자 선정 - 관리자 인증 없이 접근 불가")
    fun `setWinners - 관리자 인증이 없으면 수상자를 선정할 수 없다`() {
        val member = createAndSaveMember()
        val board = createAndSaveBoard()
        val contest = createAndSaveContest()
        val feed = createAndSaveFeed(member, board)
        val participation = createAndSaveParticipation(member, contest, feed)

        val request = mapOf(
            "winners" to listOf(
                mapOf(
                    "participationId" to participation.id,
                    "winnerRank" to 1,
                    "description" to "1등 수상작입니다."
                )
            )
        )

        RestAssured.given()
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .post("/admin/api/v1/contests/{contestId}/winners", contest.id)
            .then()
            .statusCode(401)
            .body("code", equalTo(AdminErrorCode.UNAUTHORIZED.code))
    }

    @Test
    @DisplayName("수상자 선정 - 복수 수상자 선정")
    fun `setWinners - 관리자가 복수의 수상자를 선정한다`() {
        val adminSessionId = getAdminSessionId()
        val member1 = createAndSaveMember(nickname = "회원1", memberId = "member1")
        val member2 = createAndSaveMember(nickname = "회원2", email = "test2@example.com", socialId = "12346", memberId = "member2")
        val board = createAndSaveBoard()
        val contest = createAndSaveContest()
        val feed1 = createAndSaveFeed(member1, board, title = "피드1")
        val feed2 = createAndSaveFeed(member2, board, title = "피드2")
        val participation1 = createAndSaveParticipation(member1, contest, feed1)
        val participation2 = createAndSaveParticipation(member2, contest, feed2)

        val request = mapOf(
            "winners" to listOf(
                mapOf(
                    "participationId" to participation1.id,
                    "winnerRank" to 1,
                    "description" to "1등 수상작입니다."
                ),
                mapOf(
                    "participationId" to participation2.id,
                    "winnerRank" to 2,
                    "description" to "2등 수상작입니다."
                )
            )
        )

        RestAssured.given()
            .sessionId(adminSessionId)
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .post("/admin/api/v1/contests/{contestId}/winners", contest.id)
            .then()
            .statusCode(200)
            .body("message", equalTo("수상자가 성공적으로 선정되었습니다."))
            .body("data.winners[0].feedTitle", equalTo("피드1"))
            .body("data.winners[0].authorNickname", equalTo("회원1"))
            .body("data.winners[1].feedTitle", equalTo("피드2"))
            .body("data.winners[1].authorNickname", equalTo("회원2"))
    }

    @Test
    @DisplayName("수상자 선정 - 저장 상태가 ACTIVE여도 종료된 콘테스트면 선정되고 상태가 WINNER_SELECTED가 된다")
    fun `setWinners - 종료일이 지난 콘테스트는 저장 상태와 무관하게 선정할 수 있다`() {
        val adminSessionId = getAdminSessionId()
        val member = createAndSaveMember()
        val contest = createAndSaveContest(status = ContestStatusType.ACTIVE)
        val participation = createAndSaveParticipation(member, contest, createAndSaveFeed(member, createAndSaveBoard()))

        selectWinners(adminSessionId, contest.id!!, listOf(participation.id!! to 1))

        assertThat(contestJpaRepository.findById(contest.id!!).get().status)
            .isEqualTo(ContestStatusType.WINNER_SELECTED)
    }

    @Test
    @DisplayName("수상자 선정 - 종료되지 않은 콘테스트는 CONTEST_NOT_EXPIRED")
    fun `setWinners - 종료 전이면 400을 반환한다`() {
        val adminSessionId = getAdminSessionId()
        val member = createAndSaveMember()
        val contest = createAndSaveContest(expiredAt = LocalDateTime.now().plusDays(1))
        val participation = createAndSaveParticipation(member, contest, createAndSaveFeed(member, createAndSaveBoard()))

        postWinners(adminSessionId, contest.id!!, listOf(participation.id!! to 1))
            .statusCode(400)
            .body("code", equalTo(ContestErrorCode.CONTEST_NOT_EXPIRED.code))
    }

    @Test
    @DisplayName("수상자 재선정 - 순위를 바꿔도 winnerId와 수상 소감이 유지된다")
    fun `setWinners - 순위 교환 재선정`() {
        val adminSessionId = getAdminSessionId()
        val contest = createAndSaveContest()
        val (participationA, participationB) = createTwoParticipations(contest)
        val first = selectWinners(adminSessionId, contest.id!!, listOf(participationA to 1, participationB to 2))
        val winnerIdA = first.winnerIdOf(participationA)
        saveAcceptanceSpeech(winnerIdA, "감사합니다")

        val second = selectWinners(adminSessionId, contest.id!!, listOf(participationA to 2, participationB to 1))

        assertThat(second.winnerIdOf(participationA)).isEqualTo(winnerIdA)
        assertThat(second.winnerIdOf(participationB)).isEqualTo(first.winnerIdOf(participationB))
        val savedA = contestWinnerJpaRepository.findById(winnerIdA).get()
        assertThat(savedA.winnerRank).isEqualTo(2)
        assertThat(savedA.acceptanceSpeech).isEqualTo("감사합니다")
    }

    @Test
    @DisplayName("수상자 재선정 - 빠진 수상자는 삭제되고 새 수상자는 추가되며 남은 수상자는 그대로다")
    fun `setWinners - 일부 교체 재선정`() {
        val adminSessionId = getAdminSessionId()
        val contest = createAndSaveContest()
        val (participationA, participationB) = createTwoParticipations(contest)
        val memberC = createAndSaveMember(memberId = "memberC", email = "c@example.com", socialId = "c1")
        val feedC = createAndSaveFeed(memberC, createAndSaveBoard(), title = "피드C")
        val participationC = createAndSaveParticipation(memberC, contest, feedC).id!!
        val first = selectWinners(adminSessionId, contest.id!!, listOf(participationA to 1, participationB to 2))
        saveAcceptanceSpeech(first.winnerIdOf(participationA), "감사합니다")

        val second = selectWinners(adminSessionId, contest.id!!, listOf(participationA to 1, participationC to 2))

        assertThat(second.winnerIdOf(participationA)).isEqualTo(first.winnerIdOf(participationA))
        assertThat(contestWinnerJpaRepository.findById(first.winnerIdOf(participationB))).isEmpty
        val winnerC = contestWinnerJpaRepository.findById(second.winnerIdOf(participationC)).get()
        assertThat(winnerC.acceptanceSpeech).isEmpty()
        val winnerA = contestWinnerJpaRepository.findById(first.winnerIdOf(participationA)).get()
        assertThat(winnerA.acceptanceSpeech).isEqualTo("감사합니다")
    }

    @Test
    @DisplayName("수상자 선정 - 같은 참여작을 두 번 넣으면 DUPLICATE_WINNER_PARTICIPATION")
    fun `setWinners - 참여작 중복이면 400을 반환한다`() {
        val adminSessionId = getAdminSessionId()
        val member = createAndSaveMember()
        val contest = createAndSaveContest()
        val participation = createAndSaveParticipation(member, contest, createAndSaveFeed(member, createAndSaveBoard()))

        postWinners(adminSessionId, contest.id!!, listOf(participation.id!! to 1, participation.id!! to 2))
            .statusCode(400)
            .body("code", equalTo(ContestErrorCode.DUPLICATE_WINNER_PARTICIPATION.code))
    }

    @Test
    @DisplayName("수상자 선정 - 한 회원의 참여작 두 개를 선정하면 DUPLICATE_WINNER_MEMBER")
    fun `setWinners - 같은 회원 복수 수상이면 400을 반환한다`() {
        val adminSessionId = getAdminSessionId()
        val member = createAndSaveMember()
        val board = createAndSaveBoard()
        val contest = createAndSaveContest()
        val first = createAndSaveParticipation(member, contest, createAndSaveFeed(member, board, title = "피드1"))
        val second = createAndSaveParticipation(member, contest, createAndSaveFeed(member, board, title = "피드2"))

        postWinners(adminSessionId, contest.id!!, listOf(first.id!! to 1, second.id!! to 2))
            .statusCode(400)
            .body("code", equalTo(ContestErrorCode.DUPLICATE_WINNER_MEMBER.code))
    }

    @Test
    @DisplayName("수상자 선정 - 탈퇴한 작성자의 참여작끼리는 회원 중복으로 보지 않는다")
    fun `setWinners - 탈퇴 작성자 참여작 두 개는 선정할 수 있다`() {
        val adminSessionId = getAdminSessionId()
        val member = createAndSaveMember()
        val board = createAndSaveBoard()
        val contest = createAndSaveContest()
        val first = createAndSaveParticipation(null, contest, createAndSaveFeed(member, board, title = "피드1"))
        val second = createAndSaveParticipation(null, contest, createAndSaveFeed(member, board, title = "피드2"))

        postWinners(adminSessionId, contest.id!!, listOf(first.id!! to 1, second.id!! to 2))
            .statusCode(200)
            .body("data.winners[0].authorNickname", equalTo(""))
    }

    @Test
    @DisplayName("수상자 발표 공지 저장 - 정상 케이스(신규 생성)")
    fun `upsertWinnerAnnouncement - 관리자가 공지를 신규 저장한다`() {
        val adminSessionId = getAdminSessionId()
        val contest = createAndSaveContest(status = ContestStatusType.WINNER_SELECTED)

        val request = mapOf(
            "title" to "[봄맞이 콘테스트] 수상자 발표",
            "content" to "참여해 주신 모든 분들께 감사드립니다.\n대상: OOO",
            "announcedAt" to "2025-10-10"
        )

        RestAssured.given()
            .sessionId(adminSessionId)
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .put("/admin/api/v1/contests/{contestId}/winner-announcement", contest.id)
            .then()
            .statusCode(200)
            .body("message", equalTo("수상자 발표 공지가 성공적으로 저장되었습니다."))
            .body("data.contestId", equalTo(contest.id!!.toInt()))
            .body("data.title", equalTo("[봄맞이 콘테스트] 수상자 발표"))
            .body("data.announcedAt", equalTo("2025-10-10"))
    }

    @Test
    @DisplayName("수상자 발표 공지 저장 - 재호출 시 수정(upsert)")
    fun `upsertWinnerAnnouncement - 재호출하면 기존 공지를 수정한다`() {
        val adminSessionId = getAdminSessionId()
        val contest = createAndSaveContest(status = ContestStatusType.WINNER_SELECTED)

        val createRequest = mapOf(
            "title" to "초안 제목",
            "content" to "초안 본문",
            "announcedAt" to "2025-10-10"
        )
        RestAssured.given()
            .sessionId(adminSessionId)
            .contentType(ContentType.JSON)
            .body(createRequest)
            .`when`()
            .put("/admin/api/v1/contests/{contestId}/winner-announcement", contest.id)
            .then()
            .statusCode(200)

        val updateRequest = mapOf(
            "title" to "수정된 제목",
            "content" to "수정된 본문",
            "announcedAt" to "2025-10-11"
        )
        RestAssured.given()
            .sessionId(adminSessionId)
            .contentType(ContentType.JSON)
            .body(updateRequest)
            .`when`()
            .put("/admin/api/v1/contests/{contestId}/winner-announcement", contest.id)
            .then()
            .statusCode(200)
            .body("data.title", equalTo("수정된 제목"))
            .body("data.content", equalTo("수정된 본문"))
            .body("data.announcedAt", equalTo("2025-10-11"))
    }

    @Test
    @DisplayName("수상자 발표 공지 저장 - 관리자 인증 없이 접근 불가")
    fun `upsertWinnerAnnouncement - 관리자 인증이 없으면 저장할 수 없다`() {
        val contest = createAndSaveContest(status = ContestStatusType.WINNER_SELECTED)

        val request = mapOf(
            "title" to "제목",
            "content" to "본문",
            "announcedAt" to "2025-10-10"
        )

        RestAssured.given()
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .put("/admin/api/v1/contests/{contestId}/winner-announcement", contest.id)
            .then()
            .statusCode(401)
            .body("code", equalTo(AdminErrorCode.UNAUTHORIZED.code))
    }

    @Test
    @DisplayName("콘테스트 삭제 - 참여·수상·태그·이미지·공지를 함께 지우고 피드 글은 남긴다")
    fun `delete - 딸린 기록을 함께 지운다`() {
        val adminSessionId = getAdminSessionId()
        val contest = createAndSaveContest()
        val (participationA, _) = createTwoParticipations(contest)
        postWinners(adminSessionId, contest.id!!, listOf(participationA to 1)).statusCode(200)
        jdbcTemplate.update(
            "INSERT INTO contest_tag (title, contest_id, created_at, updated_at) VALUES ('덕질', ?, NOW(), NOW())",
            contest.id
        )
        jdbcTemplate.update(
            "INSERT INTO contest_image (url, order_index, contest_id, created_at, updated_at) " +
                "VALUES ('https://test-bucket.s3.amazonaws.com/c.jpg', 0, ?, NOW(), NOW())",
            contest.id
        )
        saveContestAnnouncement(adminSessionId, contest.id!!)

        RestAssured.given()
            .sessionId(adminSessionId)
            .`when`()
            .delete("/admin/api/v1/contests/{contestId}", contest.id)
            .then()
            .statusCode(200)
            .body("message", equalTo("콘테스트가 삭제되었습니다"))

        CONTEST_CHILD_TABLES.forEach { assertThat(countByContest(it, contest.id!!)).`as`(it).isZero() }
        assertThat(contestJpaRepository.existsById(contest.id!!)).isFalse()
        assertThat(feedJpaRepository.count()).isEqualTo(2)
    }

    @Test
    @DisplayName("콘테스트 삭제 - 없는 콘테스트는 CN001, 미인증은 401")
    fun `delete - 없는 콘테스트와 미인증`() {
        RestAssured.given()
            .sessionId(getAdminSessionId())
            .`when`()
            .delete("/admin/api/v1/contests/{contestId}", 999999L)
            .then()
            .statusCode(404)
            .body("code", equalTo(ContestErrorCode.CONTEST_NOT_FOUND.code))
        RestAssured.given()
            .`when`()
            .delete("/admin/api/v1/contests/{contestId}", 1L)
            .then()
            .statusCode(401)
            .body("code", equalTo(AdminErrorCode.UNAUTHORIZED.code))
    }

    private fun saveContestAnnouncement(adminSessionId: String, contestId: Long) {
        RestAssured.given()
            .sessionId(adminSessionId)
            .contentType(ContentType.JSON)
            .body(mapOf("title" to "발표", "content" to "축하합니다", "announcedAt" to "2025-10-10"))
            .`when`()
            .put("/admin/api/v1/contests/{contestId}/winner-announcement", contestId)
            .then()
            .statusCode(200)
    }

    private fun countByContest(table: String, contestId: Long): Int {
        return jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM $table WHERE contest_id = ?",
            Int::class.java,
            contestId
        )!!
    }

    private fun createAndSaveContest(
        title: String = "테스트 공모전",
        status: ContestStatusType = ContestStatusType.ACTIVE,
        expiredAt: LocalDateTime = LocalDateTime.now().minusDays(1)
    ): ContestJpaEntity {
        val contest = ContestJpaEntity(
            title = title,
            description = "테스트 공모전 설명",
            startedAt = LocalDateTime.now().minusDays(30),
            expiredAt = expiredAt,
            status = status,
            thumbnailUrl = "https://example.com/thumbnail.jpg"
        )
        return contestJpaRepository.save(contest)
    }

    private fun createAndSaveFeed(
        member: MemberJpaEntity,
        board: com.example.mykku.board.adapter.output.persistence.entity.BoardJpaEntity,
        title: String = "테스트 피드"
    ): FeedJpaEntity {
        val feed = FeedJpaEntity(
            title = title,
            content = "테스트 피드 내용",
            member = member,
            board = board
        )
        return feedJpaRepository.save(feed)
    }

    private fun postWinners(
        adminSessionId: String,
        contestId: Long,
        selections: List<Pair<Long, Int>>
    ): ValidatableResponse {
        val request = mapOf(
            "winners" to selections.map { (participationId, rank) ->
                mapOf("participationId" to participationId, "winnerRank" to rank, "description" to "${rank}등")
            }
        )
        return RestAssured.given()
            .sessionId(adminSessionId)
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .post("/admin/api/v1/contests/{contestId}/winners", contestId)
            .then()
    }

    private fun selectWinners(
        adminSessionId: String,
        contestId: Long,
        selections: List<Pair<Long, Int>>
    ): List<Map<String, Any>> {
        return postWinners(adminSessionId, contestId, selections)
            .statusCode(200)
            .extract()
            .jsonPath()
            .getList("data.winners")
    }

    private fun List<Map<String, Any>>.winnerIdOf(participationId: Long): Long {
        val feedId = contestParticipationJpaRepository.findById(participationId).get().feed.id
        return (first { (it["feedId"] as Number).toLong() == feedId }["winnerId"] as Number).toLong()
    }

    private fun saveAcceptanceSpeech(winnerId: Long, speech: String) {
        val winner = contestWinnerJpaRepository.findById(winnerId).get()
        winner.updateAcceptanceSpeech(speech)
        contestWinnerJpaRepository.save(winner)
    }

    private fun createTwoParticipations(contest: ContestJpaEntity): Pair<Long, Long> {
        val memberA = createAndSaveMember(memberId = "memberA", email = "a@example.com", socialId = "a1")
        val memberB = createAndSaveMember(memberId = "memberB", email = "b@example.com", socialId = "b1")
        val board = createAndSaveBoard()
        val participationA = createAndSaveParticipation(memberA, contest, createAndSaveFeed(memberA, board, "피드A"))
        val participationB = createAndSaveParticipation(memberB, contest, createAndSaveFeed(memberB, board, "피드B"))
        return participationA.id!! to participationB.id!!
    }

    private fun createAndSaveParticipation(
        member: MemberJpaEntity?,
        contest: ContestJpaEntity,
        feed: FeedJpaEntity
    ): ContestParticipationJpaEntity {
        val participation = ContestParticipationJpaEntity(
            member = member,
            contest = contest,
            feed = feed
        )
        return contestParticipationJpaRepository.save(participation)
    }

    companion object {
        private val CONTEST_CHILD_TABLES = listOf(
            "contest_participation",
            "contest_winner",
            "contest_tag",
            "contest_image",
            "contest_winner_announcement"
        )
    }
}
