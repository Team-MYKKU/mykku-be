package com.example.mykku.member.adapter.input.web

import com.example.mykku.BaseControllerTest
import com.example.mykku.contest.adapter.output.persistence.entity.ContestJpaEntity
import com.example.mykku.contest.adapter.output.persistence.entity.ContestParticipationJpaEntity
import com.example.mykku.contest.adapter.output.persistence.entity.ContestWinnerJpaEntity
import com.example.mykku.contest.adapter.output.persistence.repository.ContestJpaRepository
import com.example.mykku.contest.adapter.output.persistence.repository.ContestParticipationJpaRepository
import com.example.mykku.contest.adapter.output.persistence.repository.ContestWinnerJpaRepository
import com.example.mykku.contest.domain.vo.ContestStatusType
import com.example.mykku.feed.adapter.output.persistence.FeedJpaRepository
import com.example.mykku.feed.adapter.output.persistence.entity.FeedJpaEntity
import io.restassured.RestAssured
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.notNullValue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime

@DisplayName("MemberContestController 통합 테스트")
class MemberContestControllerTest : BaseControllerTest() {

    @Autowired
    private lateinit var contestJpaRepository: ContestJpaRepository

    @Autowired
    private lateinit var contestParticipationJpaRepository: ContestParticipationJpaRepository

    @Autowired
    private lateinit var contestWinnerJpaRepository: ContestWinnerJpaRepository

    @Autowired
    private lateinit var feedJpaRepository: FeedJpaRepository

    @Test
    @DisplayName("내가 참여한 콘테스트 목록 조회 - 정상 케이스")
    fun `getMyParticipatedContests - 참여한 콘테스트 목록을 조회한다`() {
        // given
        val member = createAndSaveMember()
        val board = createAndSaveBoard()
        val authHeader = getBearerToken(member.id)

        val contest1 = contestJpaRepository.save(
            ContestJpaEntity(
                title = "콘테스트1",
                startedAt = LocalDateTime.now(),
                expiredAt = LocalDateTime.now().plusDays(7),
                thumbnailUrl = "https://example.com/thumbnail.jpg"
            )
        )
        val contest2 = contestJpaRepository.save(
            ContestJpaEntity(
                title = "콘테스트2",
                startedAt = LocalDateTime.now(),
                expiredAt = LocalDateTime.now().plusDays(7),
                thumbnailUrl = "https://example.com/thumbnail.jpg"
            )
        )

        val feed1 = feedJpaRepository.save(
            FeedJpaEntity(title = "피드1", content = "내용1", board = board, member = member)
        )
        val feed2 = feedJpaRepository.save(
            FeedJpaEntity(title = "피드2", content = "내용2", board = board, member = member)
        )

        contestParticipationJpaRepository.save(ContestParticipationJpaEntity(member = member, contest = contest1, feed = feed1))
        contestParticipationJpaRepository.save(ContestParticipationJpaEntity(member = member, contest = contest2, feed = feed2))

        // when & then
        RestAssured.given()
            .header("Authorization", authHeader)
            .queryParam("page", 0)
            .queryParam("size", 20)
        .`when`()
            .get("/api/v1/members/me/contests")
        .then()
            .statusCode(200)
            .body("message", equalTo("참여한 콘테스트 목록을 성공적으로 조회했습니다."))
            .body("data", notNullValue())
            .body("data.totalElements", equalTo(2))
    }

    @Test
    @DisplayName("내가 참여한 콘테스트 목록 조회 - 참여한 콘테스트가 없는 경우")
    fun `getMyParticipatedContests - 참여한 콘테스트가 없으면 빈 목록을 반환한다`() {
        // given
        val member = createAndSaveMember()
        val authHeader = getBearerToken(member.id)

        // when & then
        RestAssured.given()
            .header("Authorization", authHeader)
            .queryParam("page", 0)
            .queryParam("size", 20)
        .`when`()
            .get("/api/v1/members/me/contests")
        .then()
            .statusCode(200)
            .body("message", equalTo("참여한 콘테스트 목록을 성공적으로 조회했습니다."))
            .body("data", notNullValue())
            .body("data.totalElements", equalTo(0))
    }

    @Test
    @DisplayName("내가 참여한 콘테스트 목록 조회 - 페이지네이션 동작 확인")
    fun `getMyParticipatedContests - 페이지네이션이 정상 동작한다`() {
        // given
        val member = createAndSaveMember()
        val board = createAndSaveBoard()
        val authHeader = getBearerToken(member.id)

        repeat(5) { index ->
            val contest = contestJpaRepository.save(
                ContestJpaEntity(
                    title = "콘테스트${index + 1}",
                    startedAt = LocalDateTime.now(),
                    expiredAt = LocalDateTime.now().plusDays(7),
                    thumbnailUrl = "https://example.com/thumbnail.jpg"
                )
            )
            val feed = feedJpaRepository.save(
                FeedJpaEntity(title = "피드${index + 1}", content = "내용", board = board, member = member)
            )
            contestParticipationJpaRepository.save(ContestParticipationJpaEntity(member = member, contest = contest, feed = feed))
        }

        // when & then
        RestAssured.given()
            .header("Authorization", authHeader)
            .queryParam("page", 0)
            .queryParam("size", 2)
        .`when`()
            .get("/api/v1/members/me/contests")
        .then()
            .statusCode(200)
            .body("data.totalElements", equalTo(5))
            .body("data.totalPages", equalTo(3))
            .body("data.size", equalTo(2))
    }

    @Test
    @DisplayName("내가 참여한 콘테스트 목록 조회 - 수상 시 winnerStatus가 WON, 미수상 발표완료 시 LOST, 발표 전 PENDING")
    fun `getMyParticipatedContests - winnerStatus 3상태를 구분한다`() {
        val member = createAndSaveMember()
        val board = createAndSaveBoard()
        val authHeader = getBearerToken(member.id)

        val wonContest = contestJpaRepository.save(
            ContestJpaEntity(
                title = "수상 콘테스트",
                startedAt = LocalDateTime.now().minusDays(7),
                expiredAt = LocalDateTime.now().minusDays(1),
                status = ContestStatusType.WINNER_SELECTED,
                thumbnailUrl = "https://example.com/thumbnail.jpg"
            )
        )
        val feed = feedJpaRepository.save(
            FeedJpaEntity(title = "수상작", content = "내용", board = board, member = member)
        )
        val participation = contestParticipationJpaRepository.save(
            ContestParticipationJpaEntity(member = member, contest = wonContest, feed = feed)
        )
        contestWinnerJpaRepository.save(
            ContestWinnerJpaEntity(
                winnerRank = 1,
                description = "수상 설명",
                acceptanceSpeech = "",
                contest = wonContest,
                participation = participation
            )
        )

        RestAssured.given()
            .header("Authorization", authHeader)
            .queryParam("page", 0)
            .queryParam("size", 20)
        .`when`()
            .get("/api/v1/members/me/contests")
        .then()
            .statusCode(200)
            .body("data.content[0].status", equalTo("EXPIRED"))
            .body("data.content[0].winnerStatus", equalTo("WON"))
            .body("data.content[0].winnerRank", equalTo(1))
    }

    @Test
    @DisplayName("내가 참여한 콘테스트 목록 조회 - 발표 완료 미수상이면 winnerStatus가 LOST이다")
    fun `getMyParticipatedContests - 발표 완료 미수상이면 winnerStatus가 LOST이다`() {
        val member = createAndSaveMember()
        val board = createAndSaveBoard()
        val authHeader = getBearerToken(member.id)

        val contest = contestJpaRepository.save(
            ContestJpaEntity(
                title = "낙선 콘테스트",
                startedAt = LocalDateTime.now().minusDays(7),
                expiredAt = LocalDateTime.now().minusDays(1),
                status = ContestStatusType.WINNER_SELECTED,
                thumbnailUrl = "https://example.com/thumbnail.jpg"
            )
        )
        val feed = feedJpaRepository.save(
            FeedJpaEntity(title = "참여작", content = "내용", board = board, member = member)
        )
        contestParticipationJpaRepository.save(
            ContestParticipationJpaEntity(member = member, contest = contest, feed = feed)
        )

        RestAssured.given()
            .header("Authorization", authHeader)
            .queryParam("page", 0)
            .queryParam("size", 20)
        .`when`()
            .get("/api/v1/members/me/contests")
        .then()
            .statusCode(200)
            .body("data.content[0].status", equalTo("EXPIRED"))
            .body("data.content[0].winnerStatus", equalTo("LOST"))
    }

    @Test
    @DisplayName("내가 참여한 콘테스트 목록 조회 - 마감됐지만 발표 전이면 status는 EXPIRED, winnerStatus는 PENDING이다")
    fun `getMyParticipatedContests - 마감 후 발표 전이면 EXPIRED와 PENDING이다`() {
        val member = createAndSaveMember()
        val board = createAndSaveBoard()
        val authHeader = getBearerToken(member.id)

        val contest = contestJpaRepository.save(
            ContestJpaEntity(
                title = "마감된 콘테스트",
                startedAt = LocalDateTime.now().minusDays(7),
                expiredAt = LocalDateTime.now().minusDays(1),
                status = ContestStatusType.ACTIVE,
                thumbnailUrl = "https://example.com/thumbnail.jpg"
            )
        )
        val feed = feedJpaRepository.save(
            FeedJpaEntity(title = "참여작", content = "내용", board = board, member = member)
        )
        contestParticipationJpaRepository.save(
            ContestParticipationJpaEntity(member = member, contest = contest, feed = feed)
        )

        RestAssured.given()
            .header("Authorization", authHeader)
            .queryParam("page", 0)
            .queryParam("size", 20)
        .`when`()
            .get("/api/v1/members/me/contests")
        .then()
            .statusCode(200)
            .body("data.content[0].status", equalTo("EXPIRED"))
            .body("data.content[0].winnerStatus", equalTo("PENDING"))
    }

    @Test
    @DisplayName("내가 참여한 콘테스트 목록 조회 - 발표 전이면 winnerStatus가 PENDING이다")
    fun `getMyParticipatedContests - 발표 전이면 winnerStatus가 PENDING이다`() {
        val member = createAndSaveMember()
        val board = createAndSaveBoard()
        val authHeader = getBearerToken(member.id)

        val contest = contestJpaRepository.save(
            ContestJpaEntity(
                title = "진행중 콘테스트",
                startedAt = LocalDateTime.now().minusDays(1),
                expiredAt = LocalDateTime.now().plusDays(7),
                status = ContestStatusType.ACTIVE,
                thumbnailUrl = "https://example.com/thumbnail.jpg"
            )
        )
        val feed = feedJpaRepository.save(
            FeedJpaEntity(title = "참여작", content = "내용", board = board, member = member)
        )
        contestParticipationJpaRepository.save(
            ContestParticipationJpaEntity(member = member, contest = contest, feed = feed)
        )

        RestAssured.given()
            .header("Authorization", authHeader)
            .queryParam("page", 0)
            .queryParam("size", 20)
        .`when`()
            .get("/api/v1/members/me/contests")
        .then()
            .statusCode(200)
            .body("data.content[0].winnerStatus", equalTo("PENDING"))
    }

    @Test
    @DisplayName("내가 참여한 콘테스트 목록 조회 - 인증 없이 접근 시 401 에러")
    fun `getMyParticipatedContests - 인증 없이 접근하면 401 에러가 발생한다`() {
        // when & then
        RestAssured.given()
            .queryParam("page", 0)
            .queryParam("size", 20)
        .`when`()
            .get("/api/v1/members/me/contests")
        .then()
            .statusCode(401)
    }
}
