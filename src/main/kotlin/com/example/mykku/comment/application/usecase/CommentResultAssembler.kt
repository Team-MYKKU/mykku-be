package com.example.mykku.comment.application.usecase

import com.example.mykku.comment.application.dto.CommentAuthorResult
import com.example.mykku.comment.application.dto.CommentLikeStats
import com.example.mykku.comment.application.dto.CommentResult
import com.example.mykku.comment.application.dto.CommentSnapshot
import com.example.mykku.comment.application.dto.ReplyResult
import org.springframework.stereotype.Component

@Component
class CommentResultAssembler(
    private val commentAuthorResolver: CommentAuthorResolver
) {
    fun assemble(
        comments: List<CommentSnapshot>,
        repliesByParentId: Map<Long, List<CommentSnapshot>>,
        likes: CommentLikeStats
    ): List<CommentResult> {
        val allSnapshots = comments + repliesByParentId.values.flatten()
        val authors = commentAuthorResolver.resolve(allSnapshots.mapNotNull { it.memberId })
        return comments.map { toCommentResult(it, repliesByParentId[it.id].orEmpty(), likes, authors) }
    }

    private fun toCommentResult(
        comment: CommentSnapshot,
        replies: List<CommentSnapshot>,
        likes: CommentLikeStats,
        authors: Map<Long, CommentAuthorResult>
    ): CommentResult {
        val replyResults = replies.map { toReplyResult(it, likes, authors) }
        return CommentResult(
            id = comment.id,
            content = comment.content,
            author = authorOf(comment.memberId, authors),
            likeCount = likes.likeCountOf(comment.id),
            isLiked = likes.isLiked(comment.id),
            replies = replyResults,
            replyCount = replyResults.size,
            createdAt = comment.createdAt,
            updatedAt = comment.updatedAt
        )
    }

    private fun toReplyResult(
        reply: CommentSnapshot,
        likes: CommentLikeStats,
        authors: Map<Long, CommentAuthorResult>
    ): ReplyResult = ReplyResult(
        id = reply.id,
        content = reply.content,
        author = authorOf(reply.memberId, authors),
        likeCount = likes.likeCountOf(reply.id),
        isLiked = likes.isLiked(reply.id),
        createdAt = reply.createdAt,
        updatedAt = reply.updatedAt
    )

    private fun authorOf(memberId: Long?, authors: Map<Long, CommentAuthorResult>): CommentAuthorResult =
        memberId?.let { authors[it] } ?: CommentAuthorResult.withdrawn()
}
