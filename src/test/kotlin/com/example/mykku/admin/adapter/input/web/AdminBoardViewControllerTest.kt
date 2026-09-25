package com.example.mykku.admin.adapter.input.web

import com.example.mykku.BaseControllerTest
import com.example.mykku.feed.adapter.output.persistence.FeedJpaRepository
import com.example.mykku.feed.adapter.output.persistence.entity.FeedJpaEntity
import io.restassured.RestAssured
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.endsWith
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@DisplayName("AdminBoardViewController 통합 테스트")
class AdminBoardViewControllerTest : BaseControllerTest() {

    @Autowired
    private lateinit var feedJpaRepository: FeedJpaRepository

    @Test
    @DisplayName("게시판 목록은 글 수와 수정 링크, 삭제 버튼 data 속성을 보여 준다")
    fun `list - 글 수와 관리 버튼`() {
        val adminSessionId = getAdminSessionId()
        val board = createAndSaveBoard(title = "글 있는 게시판")
        val empty = createAndSaveBoard(title = "빈 게시판")
        feedJpaRepository.save(FeedJpaEntity(title = "글", content = "내용", board = board))

        val body = RestAssured.given()
            .sessionId(adminSessionId)
            .`when`()
            .get("/admin/board")
            .then()
            .statusCode(200)
            .body(containsString("href=\"/admin/board/${board.id}/edit\""))
            .extract().asString()

        assertThat(deleteButtonOf(body, board.id!!)).contains("data-feed-count=\"1\"", "data-board-title=\"글 있는 게시판\"")
        assertThat(deleteButtonOf(body, empty.id!!)).contains("data-feed-count=\"0\"")
    }

    @Test
    @DisplayName("게시판 수정 화면은 현재 제목과 로고를 채운다")
    fun `editForm - 현재 값 채움`() {
        val adminSessionId = getAdminSessionId()
        val board = createAndSaveBoard(title = "옛 제목", logo = "https://test-bucket.s3.amazonaws.com/old.png")

        RestAssured.given()
            .sessionId(adminSessionId)
            .`when`()
            .get("/admin/board/{boardId}/edit", board.id)
            .then()
            .statusCode(200)
            .body(containsString("value=\"옛 제목\""))
            .body(containsString("src=\"https://test-bucket.s3.amazonaws.com/old.png\""))
    }

    @Test
    @DisplayName("세션 없이 수정 화면에 접근하면 로그인으로 리다이렉트된다")
    fun `editForm - 미인증이면 302`() {
        RestAssured.given()
            .redirects().follow(false)
            .`when`()
            .get("/admin/board/1/edit")
            .then()
            .statusCode(302)
            .header("Location", endsWith("/admin/login"))
    }

    private fun deleteButtonOf(body: String, boardId: Long): String {
        val idIndex = body.indexOf("data-board-id=\"$boardId\"")
        val start = body.lastIndexOf("<button", idIndex)
        return body.substring(start, body.indexOf("</button>", idIndex))
    }
}
