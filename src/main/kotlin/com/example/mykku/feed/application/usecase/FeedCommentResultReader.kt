package com.example.mykku.feed.application.usecase

import com.example.mykku.block.application.port.input.BlockFilterUseCase
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
    private val commentResultAssembler: CommentResultAssembler,
    private val blockFilterUseCase: BlockFilterUseCase
) {
    fun readAll(comments: List<FeedComment>, viewerId: Long?, hiddenMemberIds: Set<Long>): List<CommentResult> =
        read(comments.filterNot { it.isWrittenByAny(hiddenMemberIds) }, viewerId, hiddenMemberIds)

    fun readOne(comment: FeedComment, viewerId: Long?): CommentResult =
        read(listOf(comment), viewerId, blockFilterUseCase.getHiddenMemberIds(viewerId)).single()

    private fun read(comments: List<FeedComment>, viewerId: Long?, hiddenMemberIds: Set<Long>): List<CommentResult> {
        val replies = loadReplies(comments).filterNot { it.isWrittenByAny(hiddenMemberIds) }
        val likes = loadLikes(viewerId, (comments + replies).map { it.id!!.value })
        val repliesByParentId = replies.groupBy { it.parentCommentId!!.value }
            .mapValues { (_, values) -> values.map { it.toSnapshot() } }
        return commentResultAssembler.assemble(comments.map { it.toSnapshot() }, repliesByParentId, likes)
    }

    private fun loadReplies(comments: List<FeedComment>): List<FeedComment> {
        val parentIds = comments.filter { it.parentCommentId == null }.map { it.id!! }
        return feedCommentRepository.findByParentCommentIds(parentIds)
    }

    private fun loadLikes(viewerId: Long?, ids: List<Long>): CommentLikeStats {
        if (ids.isEmpty()) return CommentLikeStats.NONE
        val liked = viewerId?.let { likeFeedCommentPort.findLikedFeedCommentIds(it, ids) } ?: emptySet()
        return CommentLikeStats(likeFeedCommentPort.countByFeedCommentIdIn(ids), liked)
    }

    private fun FeedComment.isWrittenByAny(memberIds: Set<Long>): Boolean =
        memberId != null && memberId in memberIds

    private fun FeedComment.toSnapshot(): CommentSnapshot =
        CommentSnapshot(id!!.value, content, memberId, createdAt, updatedAt)
}
