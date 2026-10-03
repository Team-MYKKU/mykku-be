package com.example.mykku.dailymessage.application.dto

import java.time.LocalDate

data class CreateDailyMessageCommand(
    val title: String,
    val content: String,
    val date: LocalDate
)

data class UpdateDailyMessageCommand(
    val id: Long,
    val title: String,
    val content: String,
    val date: LocalDate
)

data class CreateCommentCommand(
    val dailyMessageId: Long,
    val memberId: Long,
    val content: String,
    val parentCommentId: Long?
)

data class UpdateCommentCommand(
    val commentId: Long,
    val memberId: Long,
    val content: String
)
