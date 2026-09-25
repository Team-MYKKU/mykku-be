package com.example.mykku.contest.adapter.input.web

import com.example.mykku.BaseControllerTest
import com.example.mykku.contest.adapter.output.persistence.entity.ContestJpaEntity
import com.example.mykku.contest.adapter.output.persistence.entity.ContestParticipationJpaEntity
import com.example.mykku.contest.adapter.output.persistence.entity.ContestWinnerAnnouncementJpaEntity
import com.example.mykku.contest.adapter.output.persistence.entity.ContestWinnerJpaEntity
import com.example.mykku.contest.adapter.output.persistence.repository.ContestJpaRepository
import com.example.mykku.contest.adapter.output.persistence.repository.ContestParticipationJpaRepository
import com.example.mykku.contest.adapter.output.persistence.repository.ContestWinnerAnnouncementJpaRepository
import com.example.mykku.contest.adapter.output.persistence.repository.ContestWinnerJpaRepository
import com.example.mykku.contest.domain.vo.ContestStatusType
import java.time.LocalDate
import com.example.mykku.feed.adapter.output.persistence.entity.FeedJpaEntity
import com.example.mykku.feed.adapter.output.persistence.FeedJpaRepository
import com.example.mykku.member.adapter.output.persistence.entity.MemberJpaEntity
import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.notNullValue
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime

@DisplayName("ContestWinnerController 통합 테스트")
class ContestWinnerControllerTest : BaseControllerTest() {

    @Autowired
    private lateinit var contestJpaRepository: ContestJpaRepository

    @Autowired
    private lateinit var contestWinnerJpaRepository: ContestWinnerJpaRepository

    @Autowired
    private lateinit var contestParticipationJpaRepository: ContestParticipationJpaRepository

    @Autowired
    private lateinit var feedJpaRepository: FeedJpaRepository

    @Autowired
    private lateinit var contestWinnerAnnouncementJpaRepository: ContestWinnerAnnouncementJpaRepository

    @Test
    @DisplayName("수상작 목록 조회 - 정상 케이스")
    fun `getContestsWithWinners - 정상적으로 수상작 목록을 조회한다`() {
        val member = createAndSaveMember()
        val board = createAndSaveBoard()
        val contest = createAndSaveContest(status = ContestStatusType.WINNER_SELECTED)
        val feed = createAndSaveFeed(member, board)
        val participation = createAndSaveParticipation(member, contest, feed)
        createAndSaveWinner(contest, participation, winnerRank = 1)

        RestAssured.given()
            .`when`()
            .get("/api/v1/contests/winners")
            .then()
            .statusCode(200)
            .body("message", equalTo("수상작 목록을 성공적으로 조회했습니다."))
            .body("data.contests", notNullValue())
    }

    @Test
    @DisplayName("수상작 목록 조회 - 이미지가 없는 수상 피드는 콘테스트 썸네일을 feedImageUrl로 준다")
    fun `getContestsWithWinners - 피드 이미지가 없으면 콘테스트 썸네일을 준다`() {
        val member = createAndSaveMember()
        val contest = createAndSaveContest(status = ContestStatusType.WINNER_SELECTED)
        val participation = createAndSaveParticipation(member, contest, createAndSaveFeed(member, createAndSaveBoard()))
        createAndSaveWinner(contest, participation, winnerRank = 1)

        RestAssured.given()
            .`when`()
            .get("/api/v1/contests/winners")
            .then()
            .statusCode(200)
            .body("data.contests[0].winners[0].feedImageUrl", equalTo("https://example.com/thumbnail.jpg"))
    }

    @Test
    @DisplayName("수상작 상세 조회 - 피드 이미지가 없고 작성자가 탈퇴했으면 빈 문자열을 준다")
    fun `getContestWinnerDetail - 이미지 없음과 탈퇴 작성자는 빈 문자열이다`() {
        val member = createAndSaveMember()
        val contest = createAndSaveContest(status = ContestStatusType.WINNER_SELECTED)
        val feed = createAndSaveFeed(member, createAndSaveBoard())
        val participation = contestParticipationJpaRepository.save(
            ContestParticipationJpaEntity(member = null, contest = contest, feed = feed)
        )
        createAndSaveWinner(contest, participation, winnerRank = 1)

        RestAssured.given()
            .`when`()
            .get("/api/v1/contests/{contestId}/winners", contest.id)
            .then()
            .statusCode(200)
            .body("data.winners[0].feedImageUrl", equalTo(""))
            .body("data.winners[0].authorProfileImage", equalTo(""))
            .body("data.winners[0].authorNickname", equalTo(""))
    }

    @Test
    @DisplayName("수상작 상세 조회 - 정상 케이스")
    fun `getContestWinnerDetail - 정상적으로 수상작 상세를 조회한다`() {
        val member = createAndSaveMember()
        val board = createAndSaveBoard()
        val contest = createAndSaveContest(status = ContestStatusType.WINNER_SELECTED)
        val feed = createAndSaveFeed(member, board)
        val participation = createAndSaveParticipation(member, contest, feed)
        createAndSaveWinner(contest, participation, winnerRank = 1)

        RestAssured.given()
            .`when`()
            .get("/api/v1/contests/{contestId}/winners", contest.id)
            .then()
            .statusCode(200)
            .body("message", equalTo("수상작 상세 정보를 성공적으로 조회했습니다."))
            .body("data.contestId", equalTo(contest.id!!.toInt()))
            .body("data.winners[0].feedId", equalTo(feed.id!!.toInt()))
            .body("data.winners[0].feedTitle", equalTo("테스트 피드"))
            .body("data.winners[0].authorNickname", equalTo("테스트유저"))
    }

    @Test
    @DisplayName("수상 소감 수정 - 정상 케이스")
    fun `updateAcceptanceSpeech - 정상적으로 수상 소감을 수정한다`() {
        val member = createAndSaveMember()
        val authHeader = getBearerToken(member.id)
        val board = createAndSaveBoard()
        val contest = createAndSaveContest(status = ContestStatusType.WINNER_SELECTED)
        val feed = createAndSaveFeed(member, board)
        val participation = createAndSaveParticipation(member, contest, feed)
        val winner = createAndSaveWinner(contest, participation, winnerRank = 1)

        val request = mapOf(
            "acceptanceSpeech" to "수상 소감 테스트입니다."
        )

        RestAssured.given()
            .header("Authorization", authHeader)
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .patch("/api/v1/contests/winners/{winnerId}/acceptance-speech", winner.id)
            .then()
            .statusCode(200)
            .body("message", equalTo("수상 소감이 성공적으로 등록되었습니다."))
            .body("data.winnerId", equalTo(winner.id!!.toInt()))
    }

    @Test
    @DisplayName("수상 소감 수정 - 인증되지 않은 사용자")
    fun `updateAcceptanceSpeech - 인증되지 않은 사용자는 수상 소감을 수정할 수 없다`() {
        val member = createAndSaveMember()
        val board = createAndSaveBoard()
        val contest = createAndSaveContest(status = ContestStatusType.WINNER_SELECTED)
        val feed = createAndSaveFeed(member, board)
        val participation = createAndSaveParticipation(member, contest, feed)
        val winner = createAndSaveWinner(contest, participation, winnerRank = 1)

        val request = mapOf(
            "acceptanceSpeech" to "수상 소감 테스트입니다."
        )

        RestAssured.given()
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .patch("/api/v1/contests/winners/{winnerId}/acceptance-speech", winner.id)
            .then()
            .statusCode(401)
    }

    @Test
    @DisplayName("수상 여부 조회 - 수상자인 경우")
    fun `getMyWinnerStatus - 수상자인 경우 정상적으로 조회한다`() {
        val member = createAndSaveMember()
        val authHeader = getBearerToken(member.id)
        val board = createAndSaveBoard()
        val contest = createAndSaveContest(status = ContestStatusType.WINNER_SELECTED)
        val feed = createAndSaveFeed(member, board)
        val participation = createAndSaveParticipation(member, contest, feed)
        val winner = createAndSaveWinner(contest, participation, winnerRank = 1)

        RestAssured.given()
            .header("Authorization", authHeader)
            .`when`()
            .get("/api/v1/contests/{contestId}/my-winner-status", contest.id)
            .then()
            .statusCode(200)
            .body("message", equalTo("수상 여부를 성공적으로 조회했습니다."))
            .body("data.isWinner", equalTo(true))
            .body("data.winnerId", equalTo(winner.id!!.toInt()))
            .body("data.winnerRank", equalTo(1))
    }

    @Test
    @DisplayName("수상 여부 조회 - 수상자가 아닌 경우")
    fun `getMyWinnerStatus - 수상자가 아닌 경우 false를 반환한다`() {
        val winnerMember = createAndSaveMember(memberId = "winner")
        val nonWinner = createAndSaveMember(memberId = "nonwinner", nickname = "비수상자", email = "nonwinner@example.com", socialId = "99999")
        val authHeader = getBearerToken(nonWinner.id)
        val board = createAndSaveBoard()
        val contest = createAndSaveContest(status = ContestStatusType.WINNER_SELECTED)
        val feed = createAndSaveFeed(winnerMember, board)
        val participation = createAndSaveParticipation(winnerMember, contest, feed)
        createAndSaveWinner(contest, participation, winnerRank = 1)

        RestAssured.given()
            .header("Authorization", authHeader)
            .`when`()
            .get("/api/v1/contests/{contestId}/my-winner-status", contest.id)
            .then()
            .statusCode(200)
            .body("data.isWinner", equalTo(false))
            .body("data.winnerId", nullValue())
            .body("data.winnerRank", nullValue())
    }

    @Test
    @DisplayName("수상 여부 조회 - 인증되지 않은 사용자")
    fun `getMyWinnerStatus - 인증되지 않은 사용자는 조회할 수 없다`() {
        val contest = createAndSaveContest(status = ContestStatusType.WINNER_SELECTED)

        RestAssured.given()
            .`when`()
            .get("/api/v1/contests/{contestId}/my-winner-status", contest.id)
            .then()
            .statusCode(401)
    }

    @Test
    @DisplayName("내 수상 콘테스트 목록 조회 - 정상 케이스")
    fun `getMyAwardContests - 정상적으로 내 수상 콘테스트 목록을 조회한다`() {
        val member = createAndSaveMember()
        val authHeader = getBearerToken(member.id)
        val board = createAndSaveBoard()
        val contest = createAndSaveContest(status = ContestStatusType.WINNER_SELECTED)
        val feed = createAndSaveFeed(member, board)
        val participation = createAndSaveParticipation(member, contest, feed)
        createAndSaveWinner(contest, participation, winnerRank = 1)

        RestAssured.given()
            .header("Authorization", authHeader)
            .`when`()
            .get("/api/v1/contests/my-awards")
            .then()
            .statusCode(200)
            .body("message", equalTo("내 수상 콘테스트 목록을 성공적으로 조회했습니다."))
            .body("data.content[0].contestId", equalTo(contest.id!!.toInt()))
            .body("data.content[0].winnerRank", equalTo(1))
            .body("data.content[0].awardTitle", equalTo("최우수상"))
    }

    @Test
    @DisplayName("내 수상 콘테스트 목록 조회 - 수상명이 없으면 null을 반환한다")
    fun `getMyAwardContests - 수상명이 없으면 null을 반환한다`() {
        val member = createAndSaveMember()
        val authHeader = getBearerToken(member.id)
        val board = createAndSaveBoard()
        val contest = createAndSaveContest(status = ContestStatusType.WINNER_SELECTED)
        val feed = createAndSaveFeed(member, board)
        val participation = createAndSaveParticipation(member, contest, feed)
        createAndSaveWinner(contest, participation, winnerRank = 1, awardTitle = null)

        RestAssured.given()
            .header("Authorization", authHeader)
            .`when`()
            .get("/api/v1/contests/my-awards")
            .then()
            .statusCode(200)
            .body("data.content[0].winnerRank", equalTo(1))
            .body("data.content[0].awardTitle", equalTo(null))
    }

    @Test
    @DisplayName("내 수상 콘테스트 목록 조회 - 인증되지 않은 사용자")
    fun `getMyAwardContests - 인증되지 않은 사용자는 조회할 수 없다`() {
        RestAssured.given()
            .`when`()
            .get("/api/v1/contests/my-awards")
            .then()
            .statusCode(401)
    }

    @Test
    @DisplayName("내 수상 피드 목록 조회 - 정상 케이스")
    fun `getMyAwardFeeds - 정상적으로 내 수상 피드 목록을 조회한다`() {
        val member = createAndSaveMember()
        val authHeader = getBearerToken(member.id)
        val board = createAndSaveBoard()
        val contest = createAndSaveContest(status = ContestStatusType.WINNER_SELECTED)
        val feed = createAndSaveFeed(member, board)
        val participation = createAndSaveParticipation(member, contest, feed)
        createAndSaveWinner(contest, participation, winnerRank = 1)

        RestAssured.given()
            .header("Authorization", authHeader)
            .`when`()
            .get("/api/v1/contests/my-awards/feeds")
            .then()
            .statusCode(200)
            .body("message", equalTo("내 수상 피드 목록을 성공적으로 조회했습니다."))
            .body("data.feeds[0].title", equalTo("테스트 피드"))
    }

    @Test
    @DisplayName("내 수상 미리보기 조회 - 정상 케이스")
    fun `getMyAwardsPreview - 정상적으로 미리보기를 조회한다`() {
        val member = createAndSaveMember()
        val authHeader = getBearerToken(member.id)
        val board = createAndSaveBoard()
        val contest = createAndSaveContest(status = ContestStatusType.WINNER_SELECTED)
        val feed = createAndSaveFeed(member, board)
        val participation = createAndSaveParticipation(member, contest, feed)
        createAndSaveWinner(contest, participation, winnerRank = 1)

        RestAssured.given()
            .header("Authorization", authHeader)
            .`when`()
            .get("/api/v1/contests/my-awards/preview")
            .then()
            .statusCode(200)
            .body("message", equalTo("내 수상 미리보기를 성공적으로 조회했습니다."))
            .body("data[0].contestId", equalTo(contest.id!!.toInt()))
            .body("data[0].thumbnailUrl", equalTo("https://example.com/thumbnail.jpg"))
    }

    @Test
    @DisplayName("내 수상 미리보기 조회 - 수상 내역이 없는 경우")
    fun `getMyAwardsPreview - 수상 내역이 없으면 빈 목록을 반환한다`() {
        val member = createAndSaveMember()
        val authHeader = getBearerToken(member.id)

        RestAssured.given()
            .header("Authorization", authHeader)
            .`when`()
            .get("/api/v1/contests/my-awards/preview")
            .then()
            .statusCode(200)
            .body("data.size()", equalTo(0))
    }

    @Test
    @DisplayName("수상자 발표 공지 조회 - 정상 케이스")
    fun `getContestWinnerAnnouncement - 저장된 공지를 조회한다`() {
        val contest = createAndSaveContest(status = ContestStatusType.WINNER_SELECTED)
        contestWinnerAnnouncementJpaRepository.save(
            ContestWinnerAnnouncementJpaEntity(
                contest = contest,
                title = "[봄맞이 콘테스트] 수상자 발표",
                content = "감사합니다.\n대상: OOO",
                announcedAt = LocalDate.of(2025, 10, 10)
            )
        )

        RestAssured.given()
            .`when`()
            .get("/api/v1/contests/{contestId}/winner-announcement", contest.id)
            .then()
            .statusCode(200)
            .body("message", equalTo("수상자 발표 공지를 성공적으로 조회했습니다."))
            .body("data.contestId", equalTo(contest.id!!.toInt()))
            .body("data.title", equalTo("[봄맞이 콘테스트] 수상자 발표"))
            .body("data.content", equalTo("감사합니다.\n대상: OOO"))
            .body("data.announcedAt", equalTo("2025-10-10"))
    }

    @Test
    @DisplayName("수상자 발표 공지 조회 - 공지 미작성 시 404")
    fun `getContestWinnerAnnouncement - 공지가 없으면 404를 반환한다`() {
        val contest = createAndSaveContest(status = ContestStatusType.WINNER_SELECTED)

        RestAssured.given()
            .`when`()
            .get("/api/v1/contests/{contestId}/winner-announcement", contest.id)
            .then()
            .statusCode(404)
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
        board: com.example.mykku.board.adapter.output.persistence.entity.BoardJpaEntity
    ): FeedJpaEntity {
        val feed = FeedJpaEntity(
            title = "테스트 피드",
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

    private fun createAndSaveWinner(
        contest: ContestJpaEntity,
        participation: ContestParticipationJpaEntity,
        winnerRank: Int,
        awardTitle: String? = "최우수상"
    ): ContestWinnerJpaEntity {
        val winner = ContestWinnerJpaEntity(
            winnerRank = winnerRank,
            awardTitle = awardTitle,
            description = "수상 설명",
            acceptanceSpeech = "",
            contest = contest,
            participation = participation
        )
        return contestWinnerJpaRepository.save(winner)
    }
}
