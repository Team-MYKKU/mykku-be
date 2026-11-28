package com.example.mykku.like.adapter.input.web

import com.example.mykku.BaseControllerTest
import com.example.mykku.board.adapter.output.persistence.entity.BoardJpaEntity
import com.example.mykku.member.adapter.output.persistence.entity.MemberJpaEntity
import com.example.mykku.member.domain.vo.SocialProvider
import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.notNullValue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("LikeController 통합 테스트")
class LikeControllerTest : BaseControllerTest() {

    @Test
    @DisplayName("게시판 좋아요 목록 조회 - 정상 케이스")
    fun `getLikedBoards - 정상적으로 좋아요한 게시판 목록을 조회한다`() {
        // given
        val member = memberJpaRepository.save(
            MemberJpaEntity(
                memberId = "testmemberid1",
                socialId = "member1",
                provider = SocialProvider.GOOGLE,
                email = "member1@example.com",
                nickname = "Member1",
                role = null,
                profileImage = ""
            )
        )
        val authHeader = getBearerToken(member.id)

        // when & then
        RestAssured.given()
            .header("Authorization", authHeader)
            .`when`()
            .get("/api/v1/likes/boards")
            .then()
            .statusCode(200)
            .body("message", equalTo("즐겨찾기한 게시판 목록을 성공적으로 조회하였습니다."))
            .body("data", notNullValue())
    }

    @Test
    @DisplayName("게시판 좋아요 목록 조회 - 인증되지 않은 사용자")
    fun `getLikedBoards - 인증되지 않은 사용자는 조회할 수 없다`() {
        // when & then
        RestAssured.given()
            .`when`()
            .get("/api/v1/likes/boards")
            .then()
            .statusCode(401)
    }

    @Test
    @DisplayName("게시판 좋아요 - 정상 케이스")
    fun `likeBoard - 정상적으로 게시판을 좋아요한다`() {
        // given
        val member = memberJpaRepository.save(
            MemberJpaEntity(
                memberId = "member1",
                socialId = "member1",
                provider = SocialProvider.GOOGLE,
                email = "member1@example.com",
                nickname = "Member1",
                role = null,
                profileImage = ""
            )
        )
        val board = boardJpaRepository.save(
            BoardJpaEntity(
                title = "테스트 게시판",
                logo = "test_logo.png"
            )
        )
        val authHeader = getBearerToken(member.id)

        // when & then
        RestAssured.given()
            .header("Authorization", authHeader)
            .contentType(ContentType.JSON)
            .`when`()
            .post("/api/v1/likes/boards/{boardId}", board.id!!)
            .then()
            .statusCode(200)
            .body("message", equalTo("게시판 즐겨찾기가 성공적으로 처리되었습니다."))
            .body("data.memberId", equalTo(member.memberId))
            .body("data.boardId", equalTo(board.id!!.toInt()))
    }

    @Test
    @DisplayName("게시판 좋아요 - 인증되지 않은 사용자")
    fun `likeBoard - 인증되지 않은 사용자는 좋아요할 수 없다`() {
        // when & then
        RestAssured.given()
            .contentType(ContentType.JSON)
            .`when`()
            .post("/api/v1/likes/boards/{boardId}", 1L)
            .then()
            .statusCode(401)
    }
}
