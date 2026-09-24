package com.example.mykku.feed.application.port.input

import com.example.mykku.feed.application.dto.PagedFeedSearchResult
import com.example.mykku.feed.application.dto.SearchBoardFeedsQuery

interface SearchBoardFeedsUseCase {
    fun execute(query: SearchBoardFeedsQuery): PagedFeedSearchResult
}
