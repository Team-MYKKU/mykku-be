package com.example.mykku.feed.application.dto

import com.example.mykku.board.domain.entity.Board
import com.example.mykku.feed.domain.entity.Feed
import java.time.LocalDateTime

data class FeedSearchResult(
    val boards: List<BoardSearchGroupResult>
)

data class BoardSearchGroupResult(
    val boardId: Long,
    val boardTitle: String,
    val boardLogo: String,
    val totalCount: Long,
    val hasMore: Boolean,
    val feeds: List<FeedSearchItemResult>
) {
    companion object {
        fun of(summary: BoardMatchSummary, board: Board, feeds: List<FeedSearchItemResult>): BoardSearchGroupResult =
            BoardSearchGroupResult(
                boardId = summary.boardId,
                boardTitle = board.title,
                boardLogo = board.logo,
                totalCount = summary.count,
                hasMore = summary.count > feeds.size,
                feeds = feeds
            )
    }
}

data class FeedSearchItemResult(
    val id: Long,
    val boardId: Long,
    val boardTitle: String,
    val title: String,
    val content: String,
    val thumbnailUrl: String?,
    val createdAt: LocalDateTime
) {
    companion object {
        fun of(feed: Feed, board: Board, thumbnailUrl: String?): FeedSearchItemResult =
            FeedSearchItemResult(
                id = feed.id!!.value,
                boardId = feed.boardId,
                boardTitle = board.title,
                title = feed.title,
                content = feed.content,
                thumbnailUrl = thumbnailUrl,
                createdAt = feed.createdAt
            )
    }
}
