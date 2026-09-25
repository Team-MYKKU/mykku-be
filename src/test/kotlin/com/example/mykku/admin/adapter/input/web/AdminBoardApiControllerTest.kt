package com.example.mykku.admin.adapter.input.web

import com.example.mykku.BaseControllerTest
import com.example.mykku.admin.exception.AdminErrorCode
import com.example.mykku.board.adapter.output.persistence.entity.BoardJpaEntity
import com.example.mykku.board.exception.BoardErrorCode
import com.example.mykku.common.exception.CommonErrorCode
import com.example.mykku.feed.adapter.output.persistence.FeedCommentJpaRepository
import com.example.mykku.feed.adapter.output.persistence.FeedJpaRepository
import com.example.mykku.feed.adapter.output.persistence.entity.FeedCommentJpaEntity
import com.example.mykku.feed.adapter.output.persistence.entity.FeedJpaEntity
import io.restassured.RestAssured
import io.restassured.builder.MultiPartSpecBuilder
import io.restassured.response.ValidatableResponse
import io.restassured.specification.MultiPartSpecification
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.notNullValue
import org.hamcrest.Matchers.startsWith
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate

@DisplayName("AdminBoardApiController 통합 테스트")
class AdminBoardApiControllerTest : BaseControllerTest() {

    @Autowired
    private lateinit var feedJpaRepository: FeedJpaRepository

    @Autowired
    private lateinit var feedCommentJpaRepository: FeedCommentJpaRepository

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    private lateinit var adminSessionId: String

    @BeforeEach
    fun setUp() {
        adminSessionId = getAdminSessionId()
    }

    @Test
    @DisplayName("게시판 생성 - 정상 케이스")
    fun `create - 정상적으로 게시판을 생성한다`() {
        RestAssured.given()
            .sessionId(adminSessionId)
            .contentType("multipart/form-data")
            .multiPart("title", "FreeBoard")
            .multiPart("logo", "logo.png", "fake-image".toByteArray(), "image/png")
            .`when`()
            .post("/admin/api/v1/boards")
            .then()
            .statusCode(200)
            .body("message", equalTo("게시판이 생성되었습니다"))
            .body("data", notNullValue())
            .body("data.title", equalTo("FreeBoard"))
            .body("data.logo", notNullValue())

        val boards = boardJpaRepository.findAll()
        assertThat(boards).hasSize(1)
        assertThat(boards.first().title).isEqualTo("FreeBoard")
    }

    @Test
    @DisplayName("게시판 생성 - 관리자 세션 없이 요청 시 실패")
    fun `create - 관리자 세션 없이 요청하면 실패한다`() {
        RestAssured.given()
            .contentType("multipart/form-data")
            .multiPart("title", "자유게시판")
            .multiPart("logo", "logo.png", "fake-image".toByteArray(), "image/png")
            .`when`()
            .post("/admin/api/v1/boards")
            .then()
            .statusCode(401)
            .body("code", equalTo(AdminErrorCode.UNAUTHORIZED.code))
    }

    @Test
    @DisplayName("게시판 수정 - 제목과 로고를 바꾼다")
    fun `update - 제목과 로고를 바꾼다`() {
        val board = createAndSaveBoard(title = "옛 제목", logo = "https://test-bucket.s3.amazonaws.com/old.png")

        RestAssured.given()
            .sessionId(adminSessionId)
            .contentType("multipart/form-data")
            .multiPart(titlePart("새 제목"))
            .multiPart("logo", "logo.png", "fake-image".toByteArray(), "image/png")
            .`when`()
            .put("/admin/api/v1/boards/{boardId}", board.id)
            .then()
            .statusCode(200)
            .body("message", equalTo("게시판이 수정되었습니다"))
            .body("data.title", equalTo("새 제목"))
            .body("data.logo", startsWith("https://test-bucket.s3.amazonaws.com/board-images/"))

        assertThat(boardJpaRepository.findById(board.id!!).get().title).isEqualTo("새 제목")
    }

    @Test
    @DisplayName("게시판 수정 - 로고를 보내지 않으면 기존 로고를 유지한다")
    fun `update - 로고 없이 제목만 바꾼다`() {
        val board = createAndSaveBoard(title = "옛 제목", logo = "https://test-bucket.s3.amazonaws.com/old.png")

        putBoard(board.id!!, "새 제목")
            .statusCode(200)
            .body("data.logo", equalTo("https://test-bucket.s3.amazonaws.com/old.png"))

        val saved = boardJpaRepository.findById(board.id!!).get()
        assertThat(saved.title).isEqualTo("새 제목")
        assertThat(saved.logo).isEqualTo("https://test-bucket.s3.amazonaws.com/old.png")
    }

    @Test
    @DisplayName("게시판 수정 - 없는 게시판은 BO001, 17자 제목은 C101")
    fun `update - 없는 게시판과 제목 길이 초과`() {
        val board = createAndSaveBoard()

        putBoard(999999L, "새 제목")
            .statusCode(404)
            .body("code", equalTo(BoardErrorCode.BOARD_NOT_FOUND.code))
        putBoard(board.id!!, "가".repeat(17))
            .statusCode(400)
            .body("code", equalTo(CommonErrorCode.INVALID_INPUT.code))
    }

    @Test
    @DisplayName("게시판 삭제 - 글이 없으면 처리 방식 없이 지우고 게시판 즐겨찾기도 함께 지운다")
    fun `delete - 글이 없는 게시판`() {
        val board = createAndSaveBoard()
        val member = createAndSaveMember()
        jdbcTemplate.update(
            "INSERT INTO like_board (member_id, board_id, created_at, updated_at) VALUES (?, ?, NOW(), NOW())",
            member.id,
            board.id
        )

        deleteBoard(board.id!!, "")
            .statusCode(200)
            .body("message", equalTo("게시판이 삭제되었습니다"))

        assertThat(boardJpaRepository.existsById(board.id!!)).isFalse()
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM like_board", Int::class.java)).isZero()
    }

    @Test
    @DisplayName("게시판 삭제 - 글이 있는데 처리 방식이 없으면 409 BO301")
    fun `delete - 처리 방식 없음`() {
        val board = createAndSaveBoard()
        saveFeed(board)

        deleteBoard(board.id!!, "")
            .statusCode(409)
            .body("code", equalTo(BoardErrorCode.BOARD_HAS_FEEDS.code))

        assertThat(boardJpaRepository.existsById(board.id!!)).isTrue()
    }

    @Test
    @DisplayName("게시판 삭제 - MOVE면 글을 대상 게시판으로 옮기고 지운다")
    fun `delete - MOVE`() {
        val board = createAndSaveBoard(title = "지울 게시판")
        val target = createAndSaveBoard(title = "대상 게시판")
        val feeds = listOf(saveFeed(board), saveFeed(board))

        deleteBoard(board.id!!, "?feedAction=MOVE&targetBoardId=${target.id}")
            .statusCode(200)

        assertThat(boardJpaRepository.existsById(board.id!!)).isFalse()
        val boardIds = feeds.map { boardIdOf(it.id!!) }
        assertThat(boardIds).containsOnly(target.id)
    }

    @Test
    @DisplayName("게시판 삭제 - MOVE 대상이 없거나 자기 자신이면 BO102, BO101")
    fun `delete - MOVE 대상 오류`() {
        val board = createAndSaveBoard()
        saveFeed(board)

        deleteBoard(board.id!!, "?feedAction=MOVE")
            .statusCode(400)
            .body("code", equalTo(BoardErrorCode.MOVE_TARGET_BOARD_NOT_FOUND.code))
        deleteBoard(board.id!!, "?feedAction=MOVE&targetBoardId=999999")
            .statusCode(400)
            .body("code", equalTo(BoardErrorCode.MOVE_TARGET_BOARD_NOT_FOUND.code))
        deleteBoard(board.id!!, "?feedAction=MOVE&targetBoardId=${board.id}")
            .statusCode(400)
            .body("code", equalTo(BoardErrorCode.MOVE_TARGET_SAME_BOARD.code))

        assertThat(boardJpaRepository.existsById(board.id!!)).isTrue()
    }

    @Test
    @DisplayName("게시판 삭제 - DELETE면 답글 달린 글까지 모두 지운다")
    fun `delete - DELETE`() {
        val board = createAndSaveBoard()
        val other = createAndSaveBoard(title = "남는 게시판")
        val feed = saveFeed(board)
        val keptFeed = saveFeed(other)
        val parent = feedCommentJpaRepository.save(FeedCommentJpaEntity(content = "부모", feed = feed))
        feedCommentJpaRepository.save(FeedCommentJpaEntity(content = "답글", feed = feed, parentComment = parent))

        deleteBoard(board.id!!, "?feedAction=DELETE&targetBoardId=${other.id}")
            .statusCode(200)

        assertThat(boardJpaRepository.existsById(board.id!!)).isFalse()
        assertThat(feedJpaRepository.findAll().map { it.id }).containsExactly(keptFeed.id)
        assertThat(feedCommentJpaRepository.count()).isZero()
    }

    @Test
    @DisplayName("게시판 삭제 - 잘못된 처리 방식은 C106, 없는 게시판은 BO001")
    fun `delete - 잘못된 enum과 없는 게시판`() {
        val board = createAndSaveBoard()

        deleteBoard(board.id!!, "?feedAction=ARCHIVE")
            .statusCode(400)
            .body("code", equalTo(CommonErrorCode.INVALID_PARAMETER_TYPE.code))
        deleteBoard(999999L, "")
            .statusCode(404)
            .body("code", equalTo(BoardErrorCode.BOARD_NOT_FOUND.code))
    }

    @Test
    @DisplayName("게시판 수정·삭제 - 관리자 세션이 없으면 401")
    fun `update delete - 미인증이면 401`() {
        RestAssured.given()
            .contentType("multipart/form-data")
            .multiPart(titlePart("새 제목"))
            .`when`()
            .put("/admin/api/v1/boards/{boardId}", 1L)
            .then()
            .statusCode(401)
            .body("code", equalTo(AdminErrorCode.UNAUTHORIZED.code))
        RestAssured.given()
            .`when`()
            .delete("/admin/api/v1/boards/{boardId}", 1L)
            .then()
            .statusCode(401)
            .body("code", equalTo(AdminErrorCode.UNAUTHORIZED.code))
    }

    private fun putBoard(boardId: Long, title: String): ValidatableResponse {
        return RestAssured.given()
            .sessionId(adminSessionId)
            .contentType("multipart/form-data")
            .multiPart(titlePart(title))
            .`when`()
            .put("/admin/api/v1/boards/{boardId}", boardId)
            .then()
    }

    private fun deleteBoard(boardId: Long, query: String): ValidatableResponse {
        return RestAssured.given()
            .sessionId(adminSessionId)
            .urlEncodingEnabled(false)
            .`when`()
            .delete("/admin/api/v1/boards/$boardId$query")
            .then()
    }

    private fun saveFeed(board: BoardJpaEntity): FeedJpaEntity {
        return feedJpaRepository.save(FeedJpaEntity(title = "글", content = "내용", board = board))
    }

    private fun boardIdOf(feedId: Long): Long {
        return jdbcTemplate.queryForObject("SELECT board_id FROM feed WHERE id = ?", Long::class.java, feedId)!!
    }

    private fun titlePart(title: String): MultiPartSpecification {
        return MultiPartSpecBuilder(title).controlName("title").charset(Charsets.UTF_8).build()
    }
}
