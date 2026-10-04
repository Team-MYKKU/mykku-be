package com.example.mykku.dailymessage.domain.entity

import com.example.mykku.dailymessage.domain.vo.DailyMessageCommentId
import com.example.mykku.dailymessage.exception.DailyMessageException
import java.time.LocalDateTime

class DailyMessageComment private constructor(
    val id: DailyMessageCommentId,
    val dailyMessageId: Long,
    val memberId: Long?,
    val content: String,
    val likeCount: Int,
    val parentCommentId: Long?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        const val CONTENT_MAX_LENGTH = 1000

        fun create(
            dailyMessageId: Long,
            memberId: Long,
            content: String,
            parentCommentId: Long? = null
        ): DailyMessageComment {
            validateContent(content)
            val now = LocalDateTime.now()
            return DailyMessageComment(
                id = DailyMessageCommentId(0L),
                dailyMessageId = dailyMessageId,
                memberId = memberId,
                content = content,
                likeCount = 0,
                parentCommentId = parentCommentId,
                createdAt = now,
                updatedAt = now
            )
        }

        fun reconstitute(
            id: DailyMessageCommentId,
            dailyMessageId: Long,
            memberId: Long?,
            content: String,
            likeCount: Int,
            parentCommentId: Long?,
            createdAt: LocalDateTime,
            updatedAt: LocalDateTime
        ): DailyMessageComment {
            return DailyMessageComment(
                id = id,
                dailyMessageId = dailyMessageId,
                memberId = memberId,
                content = content,
                likeCount = likeCount,
                parentCommentId = parentCommentId,
                createdAt = createdAt,
                updatedAt = updatedAt
            )
        }

        private fun validateContent(content: String) {
            if (content.length > CONTENT_MAX_LENGTH) {
                throw DailyMessageException.dailyMessageCommentContentTooLong()
            }
        }
    }

    fun updateContent(newContent: String): DailyMessageComment {
        validateContent(newContent)
        return DailyMessageComment(
            id = this.id,
            dailyMessageId = this.dailyMessageId,
            memberId = this.memberId,
            content = newContent,
            likeCount = this.likeCount,
            parentCommentId = this.parentCommentId,
            createdAt = this.createdAt,
            updatedAt = LocalDateTime.now()
        )
    }

    fun isOwnedBy(memberId: Long): Boolean {
        return this.memberId == memberId
    }
}
