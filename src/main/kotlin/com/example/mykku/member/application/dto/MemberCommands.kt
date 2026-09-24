package com.example.mykku.member.application.dto

import org.springframework.web.multipart.MultipartFile

data class UpdateProfileCommand(
    val nickname: String?,
    val profileImage: String?,
    val profileImageFile: MultipartFile? = null
)

data class ChangePasswordCommand(
    val currentPassword: String,
    val newPassword: String
)

data class SetupProfileCommand(
    val memberId: String,
    val nickname: String
)

data class ChangeMemberIdCommand(
    val memberId: String
)

data class SearchMembersQuery(
    val keyword: String?,
    val page: Int,
    val size: Int
)
