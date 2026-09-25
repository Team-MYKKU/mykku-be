package com.example.mykku.feed.application.dto

import org.springframework.data.domain.Pageable

data class SearchBoardFeedsQuery(
    val keyword: String,
    val boardId: Long,
    val memberId: Long?,
    val pageable: Pageable
)
