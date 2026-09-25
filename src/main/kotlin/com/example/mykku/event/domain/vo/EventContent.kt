package com.example.mykku.event.domain.vo

import java.time.LocalDateTime

data class EventContent(
    val title: String,
    val subTitle: String?,
    val description: String?,
    val startedAt: LocalDateTime,
    val expiredAt: LocalDateTime
)
