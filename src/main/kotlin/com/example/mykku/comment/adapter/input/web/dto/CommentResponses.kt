package com.example.mykku.comment.adapter.input.web.dto

import com.example.mykku.comment.application.dto.CommentAuthorResult
import com.example.mykku.comment.application.dto.CommentPageResult
import com.example.mykku.comment.application.dto.CommentResult
import com.example.mykku.comment.application.dto.ReplyResult
import com.example.mykku.role.adapter.input.web.RoleResponse
import java.time.LocalDateTime

data class CommentAuthorResponse(
    val memberId: String?,
    val nickname: String,
    val profileImage: String?,
    val role: RoleResponse?
) {
    companion object {
        fun from(result: CommentAuthorResult): CommentAuthorResponse = CommentAuthorResponse(
            memberId = result.memberId,
            nickname = result.nickname,
            profileImage = result.profileImage,
            role = result.role?.let(RoleResponse::from)
        )
    }
}

data class ReplyResponse(
    val id: Long,
    val content: String,
    val author: CommentAuthorResponse,
    val likeCount: Int,
    val isLiked: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(result: ReplyResult): ReplyResponse = ReplyResponse(
            id = result.id,
            content = result.content,
            author = CommentAuthorResponse.from(result.author),
            likeCount = result.likeCount,
            isLiked = result.isLiked,
            createdAt = result.createdAt,
            updatedAt = result.updatedAt
        )
    }
}

data class CommentResponse(
    val id: Long,
    val content: String,
    val author: CommentAuthorResponse,
    val likeCount: Int,
    val isLiked: Boolean,
    val replies: List<ReplyResponse>,
    val replyCount: Int,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(result: CommentResult): CommentResponse = CommentResponse(
            id = result.id,
            content = result.content,
            author = CommentAuthorResponse.from(result.author),
            likeCount = result.likeCount,
            isLiked = result.isLiked,
            replies = result.replies.map(ReplyResponse::from),
            replyCount = result.replyCount,
            createdAt = result.createdAt,
            updatedAt = result.updatedAt
        )
    }
}

data class CommentPageResponse(
    val comments: List<CommentResponse>,
    val totalElements: Long,
    val totalPages: Int,
    val currentPage: Int,
    val pageSize: Int,
    val hasNext: Boolean
) {
    companion object {
        fun from(result: CommentPageResult): CommentPageResponse = CommentPageResponse(
            comments = result.comments.map(CommentResponse::from),
            totalElements = result.totalElements,
            totalPages = result.totalPages,
            currentPage = result.currentPage,
            pageSize = result.pageSize,
            hasNext = result.hasNext
        )
    }
}
