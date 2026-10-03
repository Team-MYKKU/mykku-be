package com.example.mykku.comment.application.dto

import com.example.mykku.member.domain.entity.Member
import com.example.mykku.role.application.dto.RoleResult
import org.springframework.data.domain.Page
import java.time.LocalDateTime

data class CommentSnapshot(
    val id: Long,
    val content: String,
    val memberId: Long?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

data class CommentLikeStats(
    val likeCountById: Map<Long, Int>,
    val likedIds: Set<Long>
) {
    fun likeCountOf(id: Long): Int = likeCountById[id] ?: 0

    fun isLiked(id: Long): Boolean = id in likedIds

    companion object {
        val NONE = CommentLikeStats(emptyMap(), emptySet())
    }
}

data class CommentAuthorResult(
    val memberId: String?,
    val nickname: String,
    val profileImage: String?,
    val role: RoleResult?
) {
    companion object {
        const val WITHDRAWN_NICKNAME = "탈퇴한 회원"

        fun withdrawn(): CommentAuthorResult = CommentAuthorResult(null, WITHDRAWN_NICKNAME, null, null)

        fun of(member: Member, role: RoleResult?): CommentAuthorResult =
            CommentAuthorResult(member.memberId, member.nickname.orEmpty(), member.profileImage, role)
    }
}

data class ReplyResult(
    val id: Long,
    val content: String,
    val author: CommentAuthorResult,
    val likeCount: Int,
    val isLiked: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

data class CommentResult(
    val id: Long,
    val content: String,
    val author: CommentAuthorResult,
    val likeCount: Int,
    val isLiked: Boolean,
    val replies: List<ReplyResult>,
    val replyCount: Int,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

data class CommentPageResult(
    val comments: List<CommentResult>,
    val totalElements: Long,
    val totalPages: Int,
    val currentPage: Int,
    val pageSize: Int,
    val hasNext: Boolean
) {
    companion object {
        fun of(page: Page<*>, comments: List<CommentResult>): CommentPageResult = CommentPageResult(
            comments = comments,
            totalElements = page.totalElements,
            totalPages = page.totalPages,
            currentPage = page.number,
            pageSize = page.size,
            hasNext = page.hasNext()
        )
    }
}
