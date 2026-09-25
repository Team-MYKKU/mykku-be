package com.example.mykku.member.application.dto

import com.example.mykku.member.domain.entity.Member
import com.example.mykku.member.domain.vo.SocialProvider
import com.example.mykku.role.application.dto.RoleResult
import java.time.LocalDateTime

data class MemberProfileResult(
    val memberId: String?,
    val email: String,
    val nickname: String?,
    val profileImage: String,
    val role: RoleResult?,
    val provider: String?,
    val emailVerified: Boolean,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(member: Member, role: RoleResult?): MemberProfileResult {
            return MemberProfileResult(
                memberId = member.memberId,
                email = member.email,
                nickname = member.nickname,
                profileImage = member.profileImage,
                role = role,
                provider = member.provider?.name,
                emailVerified = member.emailVerified,
                createdAt = member.createdAt
            )
        }
    }
}

data class MemberSearchItemResult(
    val memberId: String?,
    val nickname: String?,
    val email: String?,
    val provider: SocialProvider?,
    val profileComplete: Boolean,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(member: Member): MemberSearchItemResult {
            return MemberSearchItemResult(
                memberId = member.memberId,
                nickname = member.nickname,
                email = if (member.hasPlaceholderEmail) null else member.email,
                provider = member.provider,
                profileComplete = member.isProfileComplete,
                createdAt = member.createdAt
            )
        }
    }
}

data class PagedMemberSearchResult(
    val content: List<MemberSearchItemResult>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val isLast: Boolean
)
