package com.example.mykku.comment

import com.example.mykku.comment.application.dto.CommentAuthorResult
import com.example.mykku.comment.application.dto.CommentPageResult
import com.example.mykku.comment.application.dto.CommentResult
import com.example.mykku.comment.application.dto.ReplyResult
import com.example.mykku.role.application.dto.RoleResult
import java.time.LocalDateTime

object CommentFixtures {

    private val CREATED_AT: LocalDateTime = LocalDateTime.of(2026, 10, 1, 12, 0, 0)

    fun author(
        memberId: String? = "testmember",
        nickname: String = "테스터",
        profileImage: String? = "https://example.com/profile.jpg",
        role: RoleResult? = RoleResult(1L, "덕담왕", "덕담을 많이 남긴 사람")
    ): CommentAuthorResult = CommentAuthorResult(memberId, nickname, profileImage, role)

    fun reply(
        id: Long,
        content: String = "답글입니다",
        author: CommentAuthorResult = author(),
        createdAt: LocalDateTime = CREATED_AT
    ): ReplyResult = ReplyResult(id, content, author, 0, false, createdAt, createdAt)

    fun comment(
        id: Long,
        content: String = "댓글입니다",
        author: CommentAuthorResult = author(),
        replies: List<ReplyResult> = emptyList(),
        likeCount: Int = 0,
        isLiked: Boolean = false,
        createdAt: LocalDateTime = CREATED_AT,
        updatedAt: LocalDateTime = createdAt
    ): CommentResult =
        CommentResult(id, content, author, likeCount, isLiked, replies, replies.size, createdAt, updatedAt)

    fun page(comments: List<CommentResult>): CommentPageResult =
        CommentPageResult(comments, comments.size.toLong(), 1, 0, 20, false)
}
