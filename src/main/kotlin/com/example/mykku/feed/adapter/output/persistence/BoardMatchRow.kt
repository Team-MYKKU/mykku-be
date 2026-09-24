package com.example.mykku.feed.adapter.output.persistence

import java.time.LocalDateTime

interface BoardMatchRow {
    fun getBoardId(): Long
    fun getMatchCount(): Long
    fun getLatestCreatedAt(): LocalDateTime
}
