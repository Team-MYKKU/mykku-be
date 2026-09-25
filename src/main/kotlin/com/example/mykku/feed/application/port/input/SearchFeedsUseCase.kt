package com.example.mykku.feed.application.port.input

import com.example.mykku.feed.application.dto.FeedSearchResult
import com.example.mykku.feed.application.dto.SearchFeedsQuery

interface SearchFeedsUseCase {
    fun execute(query: SearchFeedsQuery): FeedSearchResult
}
