package com.example.mykku.feed.application.dto

import com.example.mykku.feed.domain.vo.SearchKeyword

data class FeedSearchCondition(
    val keyword: SearchKeyword,
    val viewerId: Long?
)
