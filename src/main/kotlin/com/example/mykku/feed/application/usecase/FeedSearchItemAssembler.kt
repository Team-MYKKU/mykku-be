package com.example.mykku.feed.application.usecase

import com.example.mykku.board.domain.entity.Board
import com.example.mykku.feed.application.dto.FeedSearchItemResult
import com.example.mykku.feed.application.port.output.FeedImageRepository
import com.example.mykku.feed.domain.entity.Feed
import org.springframework.stereotype.Component

@Component
class FeedSearchItemAssembler(
    private val feedImageRepository: FeedImageRepository
) {

    fun assemble(feeds: List<Feed>, boards: Map<Long, Board>): List<FeedSearchItemResult> {
        if (feeds.isEmpty()) return emptyList()

        val thumbnails = feedImageRepository.findThumbnailUrlsByFeedIds(feeds.map { it.id!! })
        return feeds.mapNotNull { feed ->
            boards[feed.boardId]?.let { FeedSearchItemResult.of(feed, it, thumbnails[feed.id!!]) }
        }
    }
}
