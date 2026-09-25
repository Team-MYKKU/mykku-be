package com.example.mykku.admin.dto.event

data class InvalidWinnerMemberIdsResponse(
    val code: String,
    val message: String,
    val invalidMemberIds: List<String>
)
