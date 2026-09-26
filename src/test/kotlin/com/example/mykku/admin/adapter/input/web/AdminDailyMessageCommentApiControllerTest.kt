package com.example.mykku.admin.adapter.input.web

import com.example.mykku.BaseControllerTest
import com.example.mykku.admin.exception.AdminErrorCode
import com.example.mykku.dailymessage.adapter.output.persistence.entity.DailyMessageCommentJpaEntity
import com.example.mykku.dailymessage.adapter.output.persistence.entity.DailyMessageJpaEntity
import com.example.mykku.dailymessage.adapter.output.persistence.repository.DailyMessageCommentJpaRepository
import com.example.mykku.dailymessage.adapter.output.persistence.repository.DailyMessageJpaRepository
import com.example.mykku.dailymessage.exception.DailyMessageErrorCode
import com.example.mykku.member.adapter.output.persistence.entity.MemberJpaEntity
import io.restassured.RestAssured
import io.restassured.response.ValidatableResponse
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import java.time.LocalDate

@DisplayName("AdminDailyMessageCommentApiController 통합 테스트")
class AdminDailyMessageCommentApiControllerTest : BaseControllerTest() {

    @Autowired
    private lateinit var dailyMessageJpaRepository: DailyMessageJpaRepository

    @Autowired
    private lateinit var dailyMessageCommentJpaRepository: DailyMessageCommentJpaRepository

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Test
    @DisplayName("답글과 좋아요가 있는 하루덕담 댓글을 지우면 답글까지 지운다")
    fun `deleteDailyMessageComment - 답글까지 지운다`() {
        val author = createAndSaveMember()
        val dailyMessage = saveDailyMessage()
        val parent = saveLikedCommentTree(dailyMessage, author)
        val other = saveComment(dailyMessage, author, null)

        deleteComment(getAdminSessionId(), parent.id!!)
            .statusCode(200)
            .body("message", equalTo("하루 덕담 댓글이 삭제되었습니다"))

        assertThat(dailyMessageCommentJpaRepository.findAll().map { it.id }).containsExactly(other.id)
        assertThat(count("SELECT COUNT(*) FROM like_daily_message_comment")).isZero()
        assertThat(dailyMessageJpaRepository.existsById(dailyMessage.id!!)).isTrue()
    }

    @Test
    @DisplayName("없는 하루덕담 댓글은 DM002")
    fun `deleteDailyMessageComment - 없는 대상이면 404`() {
        deleteComment(getAdminSessionId(), 999999L)
            .statusCode(404)
            .body("code", equalTo(DailyMessageErrorCode.DAILY_MESSAGE_COMMENT_NOT_FOUND.code))
    }

    @Test
    @DisplayName("관리자 세션이 없으면 401 AD202")
    fun `deleteDailyMessageComment - 미인증이면 401`() {
        RestAssured.given()
            .`when`()
            .delete("/admin/api/v1/daily-message-comments/{commentId}", 1L)
            .then()
            .statusCode(401)
            .body("code", equalTo(AdminErrorCode.UNAUTHORIZED.code))
    }

    private fun deleteComment(adminSessionId: String, commentId: Long): ValidatableResponse {
        return RestAssured.given()
            .sessionId(adminSessionId)
            .`when`()
            .delete("/admin/api/v1/daily-message-comments/{commentId}", commentId)
            .then()
    }

    private fun saveDailyMessage(): DailyMessageJpaEntity {
        return dailyMessageJpaRepository.save(
            DailyMessageJpaEntity(title = "오늘의 덕담", content = "오늘도 좋은 하루!", date = LocalDate.now())
        )
    }

    private fun saveLikedCommentTree(
        dailyMessage: DailyMessageJpaEntity,
        author: MemberJpaEntity
    ): DailyMessageCommentJpaEntity {
        val parent = saveComment(dailyMessage, author, null)
        val reply = saveComment(dailyMessage, author, parent)
        saveComment(dailyMessage, author, reply)
        likeComment(author, parent)
        likeComment(author, reply)
        return parent
    }

    private fun saveComment(
        dailyMessage: DailyMessageJpaEntity,
        author: MemberJpaEntity,
        parent: DailyMessageCommentJpaEntity?
    ): DailyMessageCommentJpaEntity {
        return dailyMessageCommentJpaRepository.save(
            DailyMessageCommentJpaEntity(
                content = "댓글",
                dailyMessage = dailyMessage,
                member = author,
                parentComment = parent
            )
        )
    }

    private fun likeComment(member: MemberJpaEntity, comment: DailyMessageCommentJpaEntity) {
        jdbcTemplate.update(
            "INSERT INTO like_daily_message_comment (member_id, daily_message_comment_id, created_at, updated_at) " +
                "VALUES (?, ?, NOW(), NOW())",
            member.id,
            comment.id
        )
    }

    private fun count(sql: String): Int {
        return jdbcTemplate.queryForObject(sql, Int::class.java)!!
    }
}
