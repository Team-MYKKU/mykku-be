package com.example.mykku.admin.adapter.input.web

import com.example.mykku.BaseControllerTest
import com.example.mykku.admin.exception.AdminErrorCode
import com.example.mykku.contest.adapter.output.persistence.entity.ContestJpaEntity
import com.example.mykku.contest.adapter.output.persistence.entity.ContestParticipationJpaEntity
import com.example.mykku.contest.adapter.output.persistence.repository.ContestJpaRepository
import com.example.mykku.contest.adapter.output.persistence.repository.ContestParticipationJpaRepository
import com.example.mykku.contest.domain.vo.ContestStatusType
import com.example.mykku.feed.adapter.output.persistence.entity.FeedJpaEntity
import com.example.mykku.feed.adapter.output.persistence.FeedJpaRepository
import com.example.mykku.member.adapter.output.persistence.entity.MemberJpaEntity
import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.notNullValue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime

@DisplayName("AdminContestController 통합 테스트")
class AdminContestApiControllerTest : BaseControllerTest() {

    @Autowired
    private lateinit var contestJpaRepository: ContestJpaRepository

    @Autowired
    private lateinit var contestParticipationJpaRepository: ContestParticipationJpaRepository

    @Autowired
    private lateinit var feedJpaRepository: FeedJpaRepository

    @Test
    @DisplayName("수상자 선정 - 정상 케이스")
    fun `setWinners - 관리자가 정상적으로 수상자를 선정한다`() {
        val adminSessionId = getAdminSessionId()
        val member = createAndSaveMember()
        val board = createAndSaveBoard()
        val contest = createAndSaveContest(status = ContestStatusType.EXPIRED)
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
        val contest = createAndSaveContest(status = ContestStatusType.EXPIRED)
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
        val contest = createAndSaveContest(status = ContestStatusType.EXPIRED)
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

    private fun createAndSaveContest(
        title: String = "테스트 공모전",
        status: ContestStatusType = ContestStatusType.ACTIVE
    ): ContestJpaEntity {
        val contest = ContestJpaEntity(
            title = title,
            description = "테스트 공모전 설명",
            startedAt = LocalDateTime.now().minusDays(30),
            expiredAt = LocalDateTime.now().minusDays(1),
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

    private fun createAndSaveParticipation(
        member: MemberJpaEntity,
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
}
