package com.example.mykku.dailymessage.domain

import com.example.mykku.dailymessage.domain.entity.DailyMessageComment
import com.example.mykku.dailymessage.domain.vo.DailyMessageCommentId
import com.example.mykku.dailymessage.exception.DailyMessageErrorCode
import com.example.mykku.dailymessage.exception.DailyMessageException
import java.time.LocalDateTime
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName("DailyMessageComment 도메인 엔티티 테스트")
class DailyMessageCommentTest {

    @Nested
    @DisplayName("create 메서드")
    inner class Create {

        @Test
        @DisplayName("정상적으로 댓글을 생성한다")
        fun `댓글 생성 - 정상 케이스`() {
            val comment = DailyMessageComment.create(
                dailyMessageId = 1L,
                memberId = 1L,
                content = "테스트 댓글"
            )

            assertThat(comment.id.value).isEqualTo(0L)
            assertThat(comment.dailyMessageId).isEqualTo(1L)
            assertThat(comment.memberId).isEqualTo(1L)
            assertThat(comment.content).isEqualTo("테스트 댓글")
            assertThat(comment.parentCommentId).isNull()
        }

        @Test
        @DisplayName("댓글 생성시 likeCount가 0이다")
        fun `댓글 생성 - 기본값 설정 검증`() {
            val comment = DailyMessageComment.create(
                dailyMessageId = 1L,
                memberId = 1L,
                content = "테스트 댓글"
            )

            assertThat(comment.likeCount).isEqualTo(0)
        }

        @Test
        @DisplayName("댓글 생성시 createdAt과 updatedAt이 설정된다")
        fun `댓글 생성 - 시간 설정 검증`() {
            val comment = DailyMessageComment.create(
                dailyMessageId = 1L,
                memberId = 1L,
                content = "테스트 댓글"
            )

            assertThat(comment.createdAt).isNotNull()
            assertThat(comment.updatedAt).isNotNull()
            assertThat(comment.createdAt).isEqualTo(comment.updatedAt)
        }

        @Test
        @DisplayName("대댓글을 생성할 수 있다")
        fun `댓글 생성 - 대댓글`() {
            val comment = DailyMessageComment.create(
                dailyMessageId = 1L,
                memberId = 1L,
                content = "대댓글 내용",
                parentCommentId = 10L
            )

            assertThat(comment.parentCommentId).isNotNull()
            assertThat(comment.parentCommentId).isEqualTo(10L)
        }

        @Test
        @DisplayName("댓글 내용이 1000자를 초과하면 예외가 발생한다")
        fun `댓글 생성 - 컨텐츠 길이 초과시 예외`() {
            val longContent = "a".repeat(DailyMessageComment.CONTENT_MAX_LENGTH + 1)

            val exception = assertThrows<DailyMessageException> {
                DailyMessageComment.create(
                    dailyMessageId = 1L,
                    memberId = 1L,
                    content = longContent
                )
            }

            assertThat(exception.errorCode).isEqualTo(DailyMessageErrorCode.DAILY_MESSAGE_COMMENT_CONTENT_TOO_LONG)
        }

        @Test
        @DisplayName("댓글 내용이 정확히 1000자일 때는 생성된다")
        fun `댓글 생성 - 컨텐츠 길이 경계값`() {
            val exactContent = "a".repeat(DailyMessageComment.CONTENT_MAX_LENGTH)

            val comment = DailyMessageComment.create(
                dailyMessageId = 1L,
                memberId = 1L,
                content = exactContent
            )

            assertThat(comment.content.length).isEqualTo(DailyMessageComment.CONTENT_MAX_LENGTH)
        }

        @Test
        @DisplayName("빈 컨텐츠로도 생성할 수 있다")
        fun `댓글 생성 - 빈 컨텐츠`() {
            val comment = DailyMessageComment.create(
                dailyMessageId = 1L,
                memberId = 1L,
                content = ""
            )

            assertThat(comment.content).isEmpty()
        }
    }

    @Nested
    @DisplayName("updateContent 메서드")
    inner class UpdateContent {

        @Test
        @DisplayName("댓글 내용을 수정할 수 있다")
        fun `댓글 수정 - 정상 케이스`() {
            val comment = createComment()

            val updatedComment = comment.updateContent("수정된 내용")

            assertThat(updatedComment.content).isEqualTo("수정된 내용")
        }

        @Test
        @DisplayName("수정시 updatedAt이 갱신된다")
        fun `댓글 수정 - 시간 갱신 검증`() {
            val comment = createComment()
            val originalUpdatedAt = comment.updatedAt

            Thread.sleep(10)
            val updatedComment = comment.updateContent("수정된 내용")

            assertThat(updatedComment.updatedAt).isAfter(originalUpdatedAt)
        }

        @Test
        @DisplayName("수정시 다른 필드는 유지된다")
        fun `댓글 수정 - 다른 필드 유지`() {
            val comment = createComment()

            val updatedComment = comment.updateContent("수정된 내용")

            assertThat(updatedComment.id).isEqualTo(comment.id)
            assertThat(updatedComment.dailyMessageId).isEqualTo(comment.dailyMessageId)
            assertThat(updatedComment.memberId).isEqualTo(comment.memberId)
            assertThat(updatedComment.likeCount).isEqualTo(comment.likeCount)
            assertThat(updatedComment.parentCommentId).isEqualTo(comment.parentCommentId)
            assertThat(updatedComment.createdAt).isEqualTo(comment.createdAt)
        }

        @Test
        @DisplayName("수정할 내용이 1000자를 초과하면 예외가 발생한다")
        fun `댓글 수정 - 컨텐츠 길이 초과시 예외`() {
            val comment = createComment()
            val longContent = "a".repeat(DailyMessageComment.CONTENT_MAX_LENGTH + 1)

            val exception = assertThrows<DailyMessageException> {
                comment.updateContent(longContent)
            }

            assertThat(exception.errorCode).isEqualTo(DailyMessageErrorCode.DAILY_MESSAGE_COMMENT_CONTENT_TOO_LONG)
        }

        @Test
        @DisplayName("수정할 내용이 정확히 1000자일 때는 수정된다")
        fun `댓글 수정 - 컨텐츠 길이 경계값`() {
            val comment = createComment()
            val exactContent = "a".repeat(DailyMessageComment.CONTENT_MAX_LENGTH)

            val updatedComment = comment.updateContent(exactContent)

            assertThat(updatedComment.content.length).isEqualTo(DailyMessageComment.CONTENT_MAX_LENGTH)
        }
    }

    @Nested
    @DisplayName("isOwnedBy 메서드")
    inner class IsOwnedBy {

        @Test
        @DisplayName("작성자와 memberId가 일치하면 true를 반환한다")
        fun `소유 여부 - 일치`() {
            val comment = createComment()

            assertThat(comment.isOwnedBy(1L)).isTrue()
        }

        @Test
        @DisplayName("작성자와 memberId가 다르면 false를 반환한다")
        fun `소유 여부 - 불일치`() {
            val comment = createComment()

            assertThat(comment.isOwnedBy(2L)).isFalse()
        }

        @Test
        @DisplayName("다른 memberId와 비교시 false를 반환한다")
        fun `소유 여부 - 다른 memberId`() {
            val comment = createComment()

            assertThat(comment.isOwnedBy(0L)).isFalse()
        }
    }

    @Nested
    @DisplayName("reconstitute 메서드")
    inner class Reconstitute {

        @Test
        @DisplayName("저장된 데이터로 DailyMessageComment를 복원한다")
        fun `복원 - 정상 케이스`() {
            val now = LocalDateTime.now()
            val comment = DailyMessageComment.reconstitute(
                id = DailyMessageCommentId(1L),
                dailyMessageId = 10L,
                memberId = 1L,
                content = "댓글 내용",
                likeCount = 5,
                parentCommentId = null,
                createdAt = now,
                updatedAt = now
            )

            assertThat(comment.id.value).isEqualTo(1L)
            assertThat(comment.dailyMessageId).isEqualTo(10L)
            assertThat(comment.memberId).isEqualTo(1L)
            assertThat(comment.content).isEqualTo("댓글 내용")
            assertThat(comment.likeCount).isEqualTo(5)
            assertThat(comment.parentCommentId).isNull()
            assertThat(comment.createdAt).isEqualTo(now)
            assertThat(comment.updatedAt).isEqualTo(now)
        }

        @Test
        @DisplayName("대댓글 정보와 함께 복원한다")
        fun `복원 - 대댓글`() {
            val now = LocalDateTime.now()
            val comment = DailyMessageComment.reconstitute(
                id = DailyMessageCommentId(2L),
                dailyMessageId = 10L,
                memberId = 1L,
                content = "대댓글 내용",
                likeCount = 0,
                parentCommentId = 1L,
                createdAt = now,
                updatedAt = now
            )

            assertThat(comment.parentCommentId).isEqualTo(1L)
        }

        @Test
        @DisplayName("복원시 검증을 수행하지 않는다")
        fun `복원 - 검증 미수행`() {
            val now = LocalDateTime.now()
            val longContent = "a".repeat(DailyMessageComment.CONTENT_MAX_LENGTH + 100)

            val comment = DailyMessageComment.reconstitute(
                id = DailyMessageCommentId(1L),
                dailyMessageId = 10L,
                memberId = 1L,
                content = longContent,
                likeCount = 0,
                parentCommentId = null,
                createdAt = now,
                updatedAt = now
            )

            assertThat(comment.content.length).isGreaterThan(DailyMessageComment.CONTENT_MAX_LENGTH)
        }
    }

    @Nested
    @DisplayName("CONTENT_MAX_LENGTH 상수")
    inner class ContentMaxLength {

        @Test
        @DisplayName("CONTENT_MAX_LENGTH는 1000이다")
        fun `상수값 검증`() {
            assertThat(DailyMessageComment.CONTENT_MAX_LENGTH).isEqualTo(1000)
        }
    }

    private fun createComment(): DailyMessageComment {
        return DailyMessageComment.create(
            dailyMessageId = 1L,
            memberId = 1L,
            content = "테스트 댓글"
        )
    }
}
