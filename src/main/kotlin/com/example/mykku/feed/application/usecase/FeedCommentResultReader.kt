package com.example.mykku.feed.application.usecase

import com.example.mykku.comment.application.dto.CommentLikeStats
import com.example.mykku.comment.application.dto.CommentResult
import com.example.mykku.comment.application.dto.CommentSnapshot
import com.example.mykku.comment.application.usecase.CommentResultAssembler
import com.example.mykku.feed.application.port.output.FeedCommentRepository
import com.example.mykku.feed.domain.entity.FeedComment
import com.example.mykku.like.application.port.output.LikeFeedCommentPort
import org.springframework.stereotype.Component

@Component
class FeedCommentResultReader(
    private val feedCommentRepository: FeedCommentRepository,
    private val likeFeedCommentPort: LikeFeedCommentPort,
    private val commentResultAssembler: CommentResultAssembler
) {
    fun readAll(comments: List<FeedComment>, viewerId: Long?): List<CommentResult> {
        val replies = loadReplies(comments)
        val likes = loadLikes(viewerId, (comments + replies).map { it.id!!.value })
        val repliesByParentId = replies.groupBy { it.parentCommentId!!.value }
            .mapValues { (_, values) -> values.map { it.toSnapshot() } }
        return commentResultAssembler.assemble(comments.map { it.toSnapshot() }, repliesByParentId, likes)
    }

    fun readOne(comment: FeedComment, viewerId: Long?): CommentResult =
        readAll(listOf(comment), viewerId).single()

    private fun loadReplies(comments: List<FeedComment>): List<FeedComment> {
        val parentIds = comments.filter { it.parentCommentId == null }.map { it.id!! }
        return feedCommentRepository.findByParentCommentIds(parentIds)
    }

    private fun loadLikes(viewerId: Long?, ids: List<Long>): CommentLikeStats {
        if (ids.isEmpty()) return CommentLikeStats.NONE
        val liked = viewerId?.let { likeFeedCommentPort.findLikedFeedCommentIds(it, ids) } ?: emptySet()
        return CommentLikeStats(likeFeedCommentPort.countByFeedCommentIdIn(ids), liked)
    }

    private fun FeedComment.toSnapshot(): CommentSnapshot =
        CommentSnapshot(id!!.value, content, memberId, createdAt, updatedAt)
}
