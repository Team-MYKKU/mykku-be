package com.example.mykku.feed.application.dto

import java.time.LocalDateTime

data class BoardMatchSummary(
    val boardId: Long,
    val count: Long,
    val latestCreatedAt: LocalDateTime
)
