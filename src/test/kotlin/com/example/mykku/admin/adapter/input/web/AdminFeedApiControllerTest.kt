package com.example.mykku.admin.adapter.input.web

import com.example.mykku.BaseControllerTest
import com.example.mykku.admin.exception.AdminErrorCode
import com.example.mykku.contest.adapter.output.persistence.entity.ContestJpaEntity
import com.example.mykku.contest.adapter.output.persistence.entity.ContestParticipationJpaEntity
import com.example.mykku.contest.adapter.output.persistence.repository.ContestJpaRepository
import com.example.mykku.contest.adapter.output.persistence.repository.ContestParticipationJpaRepository
import com.example.mykku.contest.domain.vo.ContestStatusType
import com.example.mykku.feed.adapter.output.persistence.FeedCommentJpaRepository
import com.example.mykku.feed.adapter.output.persistence.FeedJpaRepository
import com.example.mykku.feed.adapter.output.persistence.entity.FeedCommentJpaEntity
import com.example.mykku.feed.adapter.output.persistence.entity.FeedJpaEntity
import com.example.mykku.feed.exception.FeedErrorCode
import com.example.mykku.member.adapter.output.persistence.entity.MemberJpaEntity
import com.example.mykku.util.TestTokenGenerator
import io.restassured.RestAssured
import io.restassured.response.ValidatableResponse
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import java.time.LocalDateTime

@DisplayName("AdminFeedApiController 통합 테스트")
class AdminFeedApiControllerTest : BaseControllerTest() {

    @Autowired
    private lateinit var feedJpaRepository: FeedJpaRepository

    @Autowired
    private lateinit var feedCommentJpaRepository: FeedCommentJpaRepository

    @Autowired
    private lateinit var contestJpaRepository: ContestJpaRepository

    @Autowired
    private lateinit var contestParticipationJpaRepository: ContestParticipationJpaRepository

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Test
    @DisplayName("댓글·답글·좋아요·이미지·태그·콘테스트 참여와 수상이 있는 피드를 어드민이 지운다")
    fun `deleteFeed - 딸린 기록을 모두 지운다`() {
        val adminSessionId = getAdminSessionId()
        val author = createAndSaveMember()
        val feed = saveFeed(author, "지울 피드")
        val parent = saveComment(feed, author, null)
        val reply = saveComment(feed, author, parent)
        saveComment(feed, author, reply)
        saveFeedExtras(feed, author, listOf(parent, reply))
        saveContestWinner(feed, author)

        deleteFeed(adminSessionId, feed.id!!)
            .statusCode(200)
            .body("message", equalTo("게시글이 삭제되었습니다"))

        FEED_CHILD_TABLES.forEach { assertThat(countByFeed(it, feed.id!!)).`as`(it).isZero() }
        assertThat(count("SELECT COUNT(*) FROM contest_winner")).isZero()
        assertThat(count("SELECT COUNT(*) FROM like_feed_comment")).isZero()
        assertThat(feedJpaRepository.existsById(feed.id!!)).isFalse()
    }

    @Test
    @DisplayName("답글과 좋아요가 있는 댓글을 지우면 답글까지 지운다")
    fun `deleteFeedComment - 답글까지 지운다`() {
        val adminSessionId = getAdminSessionId()
        val author = createAndSaveMember()
        val feed = saveFeed(author, "댓글 피드")
        val parent = saveComment(feed, author, null)
        val reply = saveComment(feed, author, parent)
        val other = saveComment(feed, author, null)
        likeComment(author, parent)
        likeComment(author, reply)

        deleteComment(adminSessionId, parent.id!!)
            .statusCode(200)
            .body("message", equalTo("댓글이 삭제되었습니다"))

        assertThat(feedCommentJpaRepository.findAll().map { it.id }).containsExactly(other.id)
        assertThat(count("SELECT COUNT(*) FROM like_feed_comment")).isZero()
        assertThat(feedJpaRepository.existsById(feed.id!!)).isTrue()
    }

    @Test
    @DisplayName("없는 피드와 댓글은 FD001, FD002")
    fun `delete - 없는 대상이면 404`() {
        val adminSessionId = getAdminSessionId()

        deleteFeed(adminSessionId, 999999L)
            .statusCode(404)
            .body("code", equalTo(FeedErrorCode.FEED_NOT_FOUND.code))
        deleteComment(adminSessionId, 999999L)
            .statusCode(404)
            .body("code", equalTo(FeedErrorCode.FEED_COMMENT_NOT_FOUND.code))
    }

    @Test
    @DisplayName("관리자 세션이 없으면 401 AD202")
    fun `delete - 미인증이면 401`() {
        RestAssured.given()
            .`when`()
            .delete("/admin/api/v1/feeds/{feedId}", 1L)
            .then()
            .statusCode(401)
            .body("code", equalTo(AdminErrorCode.UNAUTHORIZED.code))
        RestAssured.given()
            .`when`()
            .delete("/admin/api/v1/feed-comments/{commentId}", 1L)
            .then()
            .statusCode(401)
            .body("code", equalTo(AdminErrorCode.UNAUTHORIZED.code))
    }

    @Test
    @DisplayName("답글이 달린 피드도 작성자가 지울 수 있다")
    fun `회원 deleteFeed - 답글이 있어도 200`() {
        val author = createAndSaveMember()
        val feed = saveFeed(author, "답글 피드")
        val parent = saveComment(feed, author, null)
        saveComment(feed, author, parent)

        RestAssured.given()
            .header("Authorization", TestTokenGenerator.getBearerToken(author.id))
            .`when`()
            .delete("/api/v1/feeds/{feedId}", feed.id)
            .then()
            .statusCode(200)

        assertThat(feedCommentJpaRepository.count()).isZero()
        assertThat(feedJpaRepository.existsById(feed.id!!)).isFalse()
    }

    private fun deleteFeed(adminSessionId: String, feedId: Long): ValidatableResponse {
        return RestAssured.given()
            .sessionId(adminSessionId)
            .`when`()
            .delete("/admin/api/v1/feeds/{feedId}", feedId)
            .then()
    }

    private fun deleteComment(adminSessionId: String, commentId: Long): ValidatableResponse {
        return RestAssured.given()
            .sessionId(adminSessionId)
            .`when`()
            .delete("/admin/api/v1/feed-comments/{commentId}", commentId)
            .then()
    }

    private fun saveFeed(author: MemberJpaEntity, title: String): FeedJpaEntity {
        return feedJpaRepository.save(
            FeedJpaEntity(title = title, content = "내용", member = author, board = createAndSaveBoard())
        )
    }

    private fun saveComment(
        feed: FeedJpaEntity,
        author: MemberJpaEntity,
        parent: FeedCommentJpaEntity?
    ): FeedCommentJpaEntity {
        return feedCommentJpaRepository.save(
            FeedCommentJpaEntity(content = "댓글", feed = feed, member = author, parentComment = parent)
        )
    }

    private fun saveFeedExtras(feed: FeedJpaEntity, member: MemberJpaEntity, comments: List<FeedCommentJpaEntity>) {
        jdbcTemplate.update(
            "INSERT INTO like_feed (member_id, feed_id, created_at, updated_at) VALUES (?, ?, NOW(), NOW())",
            member.id,
            feed.id
        )
        jdbcTemplate.update(
            "INSERT INTO feed_image (url, width, height, feed_id, created_at, updated_at) " +
                "VALUES ('https://test-bucket.s3.amazonaws.com/feed-images/a.jpg', 10, 10, ?, NOW(), NOW())",
            feed.id
        )
        jdbcTemplate.update(
            "INSERT INTO feed_tag (feed_id, title, created_at, updated_at) VALUES (?, '태그', NOW(), NOW())",
            feed.id
        )
        comments.forEach { likeComment(member, it) }
    }

    private fun likeComment(member: MemberJpaEntity, comment: FeedCommentJpaEntity) {
        jdbcTemplate.update(
            "INSERT INTO like_feed_comment (member_id, feed_comment_id, created_at, updated_at) " +
                "VALUES (?, ?, NOW(), NOW())",
            member.id,
            comment.id
        )
    }

    private fun saveContestWinner(feed: FeedJpaEntity, member: MemberJpaEntity) {
        val contest = contestJpaRepository.save(
            ContestJpaEntity(
                title = "콘테스트",
                startedAt = LocalDateTime.now().minusDays(10),
                expiredAt = LocalDateTime.now().minusDays(1),
                status = ContestStatusType.WINNER_SELECTED,
                thumbnailUrl = "https://example.com/thumbnail.jpg"
            )
        )
        val participation = contestParticipationJpaRepository.save(
            ContestParticipationJpaEntity(member = member, contest = contest, feed = feed)
        )
        jdbcTemplate.update(
            "INSERT INTO contest_winner (winner_rank, description, acceptance_speech, contest_id, participation_id, " +
                "created_at, updated_at) VALUES (1, '대상', '소감', ?, ?, NOW(), NOW())",
            contest.id,
            participation.id
        )
    }

    private fun countByFeed(table: String, feedId: Long): Int {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM $table WHERE feed_id = ?", Int::class.java, feedId)!!
    }

    private fun count(sql: String): Int {
        return jdbcTemplate.queryForObject(sql, Int::class.java)!!
    }

    companion object {
        private val FEED_CHILD_TABLES = listOf(
            "feed_comment",
            "like_feed",
            "feed_image",
            "feed_tag",
            "contest_participation"
        )
    }
}
