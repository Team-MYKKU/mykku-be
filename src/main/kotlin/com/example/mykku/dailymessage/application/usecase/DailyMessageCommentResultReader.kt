package com.example.mykku.dailymessage.application.usecase

import com.example.mykku.comment.application.dto.CommentLikeStats
import com.example.mykku.comment.application.dto.CommentResult
import com.example.mykku.comment.application.dto.CommentSnapshot
import com.example.mykku.comment.application.usecase.CommentResultAssembler
import com.example.mykku.dailymessage.application.port.output.DailyMessageCommentRepository
import com.example.mykku.dailymessage.domain.entity.DailyMessageComment
import com.example.mykku.like.application.port.output.LikeDailyMessageCommentPort
import org.springframework.stereotype.Component

@Component
class DailyMessageCommentResultReader(
    private val dailyMessageCommentRepository: DailyMessageCommentRepository,
    private val likeDailyMessageCommentPort: LikeDailyMessageCommentPort,
    private val commentResultAssembler: CommentResultAssembler
) {
    fun readAll(comments: List<DailyMessageComment>, viewerId: Long?): List<CommentResult> {
        val replies = loadReplies(comments)
        val likes = loadLikes(viewerId, (comments + replies).map { it.id.value })
        val repliesByParentId = replies.groupBy { it.parentCommentId!! }
            .mapValues { (_, values) -> values.map { it.toSnapshot() } }
        return commentResultAssembler.assemble(comments.map { it.toSnapshot() }, repliesByParentId, likes)
    }

    fun readOne(comment: DailyMessageComment, viewerId: Long?): CommentResult =
        readAll(listOf(comment), viewerId).single()

    private fun loadReplies(comments: List<DailyMessageComment>): List<DailyMessageComment> {
        val parentIds = comments.filter { it.parentCommentId == null }.map { it.id.value }
        return dailyMessageCommentRepository.findByParentCommentIds(parentIds)
    }

    private fun loadLikes(viewerId: Long?, ids: List<Long>): CommentLikeStats {
        if (ids.isEmpty()) return CommentLikeStats.NONE
        val liked = viewerId?.let { likeDailyMessageCommentPort.findLikedCommentIds(it, ids) } ?: emptySet()
        return CommentLikeStats(likeDailyMessageCommentPort.countByCommentIdIn(ids), liked)
    }

    private fun DailyMessageComment.toSnapshot(): CommentSnapshot =
        CommentSnapshot(id.value, content, memberId, createdAt, updatedAt)
}
