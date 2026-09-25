package com.example.mykku.contest.domain.vo

import java.time.LocalDateTime

data class ContestContent(
    val title: String,
    val description: String?,
    val startedAt: LocalDateTime,
    val expiredAt: LocalDateTime
)
