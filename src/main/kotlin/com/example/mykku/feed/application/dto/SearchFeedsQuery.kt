package com.example.mykku.feed.application.dto

data class SearchFeedsQuery(
    val keyword: String,
    val memberId: Long?
)
