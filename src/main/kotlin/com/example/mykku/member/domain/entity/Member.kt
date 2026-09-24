package com.example.mykku.member.domain.entity

import com.example.mykku.member.domain.vo.MemberPk
import com.example.mykku.member.domain.vo.SocialProvider
import com.example.mykku.member.exception.MemberException
import java.time.LocalDateTime

class Member private constructor(
    val id: MemberPk,
    var memberId: String?,
    var nickname: String?,
    var roleId: Long?,
    var profileImage: String,
    val provider: SocialProvider?,
    val socialId: String?,
    val email: String,
    var password: String?,
    var emailVerified: Boolean,
    val createdAt: LocalDateTime,
    var updatedAt: LocalDateTime
) {
    val isProfileComplete: Boolean
        get() = memberId != null && nickname != null

    val hasPlaceholderEmail: Boolean
        get() = socialId != null && provider?.placeholderEmail(socialId) == email

    fun requireProfileCompleted() {
        if (!isProfileComplete) {
            throw MemberException.profileNotCompleted()
        }
    }

    companion object {
        const val NICKNAME_MAX_LENGTH = 10
        val VALID_NICKNAME_PATTERN = Regex("^[가-힣a-zA-Z0-9\\s]+$")

        const val MEMBER_ID_MAX_LENGTH = 16
        val VALID_MEMBER_ID_PATTERN = Regex("^[a-zA-Z0-9]+$")

        fun validateMemberId(memberId: String) {
            if (memberId.isBlank()) {
                throw MemberException.memberIdEmpty()
            }
            if (memberId.length > MEMBER_ID_MAX_LENGTH) {
                throw MemberException.memberIdTooLong()
            }
            if (!VALID_MEMBER_ID_PATTERN.matches(memberId)) {
                throw MemberException.memberIdInvalidFormat()
            }
        }

        fun createEmailMember(
            email: String,
            password: String,
            profileImage: String = ""
        ): Member {
            val now = LocalDateTime.now()
            return Member(
                id = MemberPk(0),
                memberId = null,
                nickname = null,
                roleId = null,
                profileImage = profileImage,
                provider = SocialProvider.EMAIL,
                socialId = null,
                email = email,
                password = password,
                emailVerified = false,
                createdAt = now,
                updatedAt = now
            )
        }

        fun createSocialMember(
            profileImage: String,
            provider: SocialProvider,
            socialId: String,
            email: String
        ): Member {
            val now = LocalDateTime.now()
            return Member(
                id = MemberPk(0),
                memberId = null,
                nickname = null,
                roleId = null,
                profileImage = profileImage,
                provider = provider,
                socialId = socialId,
                email = email,
                password = null,
                emailVerified = true,
                createdAt = now,
                updatedAt = now
            )
        }

        fun reconstitute(
            id: Long,
            memberId: String?,
            nickname: String?,
            roleId: Long?,
            profileImage: String,
            provider: SocialProvider?,
            socialId: String?,
            email: String,
            password: String?,
            emailVerified: Boolean,
            createdAt: LocalDateTime,
            updatedAt: LocalDateTime
        ): Member {
            return Member(
                id = MemberPk.of(id),
                memberId = memberId,
                nickname = nickname,
                roleId = roleId,
                profileImage = profileImage,
                provider = provider,
                socialId = socialId,
                email = email,
                password = password,
                emailVerified = emailVerified,
                createdAt = createdAt,
                updatedAt = updatedAt
            )
        }

        private fun validateNickname(nickname: String) {
            if (nickname.length > NICKNAME_MAX_LENGTH) {
                throw MemberException.memberNicknameTooLong()
            }
            if (!VALID_NICKNAME_PATTERN.matches(nickname)) {
                throw MemberException.memberNicknameInvalidFormat()
            }
        }
    }

    fun setupProfile(newMemberId: String, newNickname: String) {
        validateMemberId(newMemberId)
        validateNickname(newNickname)
        this.memberId = newMemberId
        this.nickname = newNickname
        this.updatedAt = LocalDateTime.now()
    }

    fun changeMemberId(newMemberId: String) {
        validateMemberId(newMemberId)
        this.memberId = newMemberId
        this.updatedAt = LocalDateTime.now()
    }

    fun updateProfile(newNickname: String?, newProfileImage: String?) {
        newNickname?.let {
            validateNickname(it)
            this.nickname = it
        }
        newProfileImage?.let {
            this.profileImage = it
        }
        this.updatedAt = LocalDateTime.now()
    }

    fun changePassword(newEncodedPassword: String) {
        this.password = newEncodedPassword
        this.updatedAt = LocalDateTime.now()
    }

    fun assignRole(roleId: Long) {
        this.roleId = roleId
        this.updatedAt = LocalDateTime.now()
    }

    fun verifyEmail() {
        this.emailVerified = true
        this.updatedAt = LocalDateTime.now()
    }
}
