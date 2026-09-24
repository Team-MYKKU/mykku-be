package com.example.mykku.contest.adapter.input.web

import com.example.mykku.BaseControllerTest
import com.example.mykku.common.exception.CommonErrorCode
import com.example.mykku.contest.adapter.output.persistence.entity.ContestJpaEntity
import com.example.mykku.contest.adapter.output.persistence.repository.ContestJpaRepository
import com.example.mykku.contest.domain.vo.ContestStatusType
import io.restassured.RestAssured
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.notNullValue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime

@DisplayName("ContestController 통합 테스트")
class ContestControllerTest : BaseControllerTest() {

    @Autowired
    private lateinit var contestJpaRepository: ContestJpaRepository

    @Test
    @DisplayName("공모전 목록 조회 - 정상 케이스")
    fun `getContests - 정상적으로 공모전 목록을 조회한다`() {
        val member = createAndSaveMember()
        val authHeader = getBearerToken(member.id)

        createAndSaveContest(
            title = "진행중인 공모전",
            startedAt = LocalDateTime.now().minusDays(1),
            expiredAt = LocalDateTime.now().plusDays(30)
        )

        RestAssured.given()
            .header("Authorization", authHeader)
            .param("status", "ACTIVE")
            .param("sortType", "LATEST")
            .param("page", 0)
            .param("size", 20)
            .`when`()
            .get("/api/v1/contests")
            .then()
            .statusCode(200)
            .body("message", equalTo("공모전 목록을 성공적으로 조회했습니다."))
            .body("data.content", notNullValue())
    }

    @Test
    @DisplayName("공모전 목록 조회 - 종료된 공모전은 EXPIRED로 응답한다")
    fun `getContests - 종료된 공모전의 상태는 EXPIRED다`() {
        val member = createAndSaveMember()
        val authHeader = getBearerToken(member.id)

        createAndSaveContest(
            title = "종료된 공모전",
            startedAt = LocalDateTime.now().minusDays(10),
            expiredAt = LocalDateTime.now().minusDays(1)
        )

        RestAssured.given()
            .header("Authorization", authHeader)
            .param("status", "EXPIRED")
            .`when`()
            .get("/api/v1/contests")
            .then()
            .statusCode(200)
            .body("data.content[0].status", equalTo("EXPIRED"))
    }

    @Test
    @DisplayName("공모전 상세 조회 - 종료된 공모전은 EXPIRED로 응답한다")
    fun `getContestDetail - 종료된 공모전의 상태는 EXPIRED다`() {
        val member = createAndSaveMember()
        val authHeader = getBearerToken(member.id)

        val contest = createAndSaveContest(
            title = "종료된 공모전",
            startedAt = LocalDateTime.now().minusDays(10),
            expiredAt = LocalDateTime.now().minusDays(1)
        )

        RestAssured.given()
            .header("Authorization", authHeader)
            .`when`()
            .get("/api/v1/contests/{contestId}", contest.id)
            .then()
            .statusCode(200)
            .body("data.status", equalTo("EXPIRED"))
    }

    @Test
    @DisplayName("공모전 목록 조회 - 수상자를 선정한 공모전은 목록에서 EXPIRED, 상세에서 WINNER_SELECTED로 응답한다")
    fun `getContests - 수상자 선정 공모전은 목록에서 EXPIRED로 응답한다`() {
        val member = createAndSaveMember()
        val authHeader = getBearerToken(member.id)
        val contest = createAndSaveContest(
            title = "수상자 선정 공모전",
            startedAt = LocalDateTime.now().minusDays(10),
            expiredAt = LocalDateTime.now().minusDays(1),
            status = ContestStatusType.WINNER_SELECTED
        )

        RestAssured.given()
            .header("Authorization", authHeader)
            .param("status", "ALL")
            .`when`()
            .get("/api/v1/contests")
            .then()
            .statusCode(200)
            .body("data.content[0].status", equalTo("EXPIRED"))

        RestAssured.given()
            .header("Authorization", authHeader)
            .`when`()
            .get("/api/v1/contests/{contestId}", contest.id)
            .then()
            .statusCode(200)
            .body("data.status", equalTo("WINNER_SELECTED"))
    }

    @Test
    @DisplayName("공모전 목록 조회 - ACTIVE, EXPIRED, ALL 이외의 status는 400을 반환한다")
    fun `getContests - WINNER_SELECTED 필터는 400을 반환한다`() {
        val member = createAndSaveMember()
        val authHeader = getBearerToken(member.id)

        RestAssured.given()
            .header("Authorization", authHeader)
            .param("status", "WINNER_SELECTED")
            .`when`()
            .get("/api/v1/contests")
            .then()
            .statusCode(400)
            .body("code", equalTo(CommonErrorCode.INVALID_PARAMETER_TYPE.code))
    }

    @Test
    @DisplayName("공모전 상세 조회 - 정상 케이스")
    fun `getContestDetail - 정상적으로 공모전 상세를 조회한다`() {
        val member = createAndSaveMember()
        val authHeader = getBearerToken(member.id)

        val contest = createAndSaveContest(
            title = "상세 조회 테스트 공모전",
            startedAt = LocalDateTime.now().minusDays(1),
            expiredAt = LocalDateTime.now().plusDays(30)
        )

        RestAssured.given()
            .header("Authorization", authHeader)
            .`when`()
            .get("/api/v1/contests/{contestId}", contest.id)
            .then()
            .statusCode(200)
            .body("message", equalTo("공모전 상세 정보를 성공적으로 조회했습니다."))
            .body("data.id", equalTo(contest.id!!.toInt()))
            .body("data.title", equalTo("상세 조회 테스트 공모전"))
    }

    @Test
    @DisplayName("공모전 목록 조회 - 인증되지 않은 사용자")
    fun `getContests - 인증되지 않은 사용자는 목록을 조회할 수 없다`() {
        RestAssured.given()
            .param("status", "ACTIVE")
            .`when`()
            .get("/api/v1/contests")
            .then()
            .statusCode(401)
    }

    @Test
    @DisplayName("공모전 상세 조회 - 인증되지 않은 사용자")
    fun `getContestDetail - 인증되지 않은 사용자는 상세를 조회할 수 없다`() {
        val contest = createAndSaveContest(
            title = "테스트 공모전",
            startedAt = LocalDateTime.now().minusDays(1),
            expiredAt = LocalDateTime.now().plusDays(30)
        )

        RestAssured.given()
            .`when`()
            .get("/api/v1/contests/{contestId}", contest.id)
            .then()
            .statusCode(401)
    }

    private fun createAndSaveContest(
        title: String,
        description: String? = "테스트 공모전 설명",
        startedAt: LocalDateTime,
        expiredAt: LocalDateTime,
        status: ContestStatusType = ContestStatusType.ACTIVE
    ): ContestJpaEntity {
        val contest = ContestJpaEntity(
            title = title,
            description = description,
            startedAt = startedAt,
            expiredAt = expiredAt,
            status = status,
            thumbnailUrl = "https://example.com/thumbnail.jpg"
        )
        return contestJpaRepository.save(contest)
    }
}
