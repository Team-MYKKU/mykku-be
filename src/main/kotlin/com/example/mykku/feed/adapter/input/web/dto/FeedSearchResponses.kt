package com.example.mykku.feed.adapter.input.web.dto

import com.example.mykku.feed.application.dto.BoardSearchGroupResult
import com.example.mykku.feed.application.dto.FeedSearchItemResult
import com.example.mykku.feed.application.dto.FeedSearchResult
import com.example.mykku.feed.application.dto.PagedFeedSearchResult
import java.time.LocalDateTime

data class FeedSearchResponse(
    val boards: List<BoardSearchGroupResponse>
) {
    companion object {
        fun from(result: FeedSearchResult): FeedSearchResponse =
            FeedSearchResponse(boards = result.boards.map { BoardSearchGroupResponse.from(it) })
    }
}

data class BoardSearchGroupResponse(
    val boardId: Long,
    val boardTitle: String,
    val boardLogo: String,
    val totalCount: Long,
    val hasMore: Boolean,
    val feeds: List<FeedSearchItemResponse>
) {
    companion object {
        fun from(result: BoardSearchGroupResult): BoardSearchGroupResponse =
            BoardSearchGroupResponse(
                boardId = result.boardId,
                boardTitle = result.boardTitle,
                boardLogo = result.boardLogo,
                totalCount = result.totalCount,
                hasMore = result.hasMore,
                feeds = result.feeds.map { FeedSearchItemResponse.from(it) }
            )
    }
}

data class FeedSearchItemResponse(
    val id: Long,
    val boardId: Long,
    val boardTitle: String,
    val title: String,
    val content: String,
    val thumbnailUrl: String?,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(result: FeedSearchItemResult): FeedSearchItemResponse =
            FeedSearchItemResponse(
                id = result.id,
                boardId = result.boardId,
                boardTitle = result.boardTitle,
                title = result.title,
                content = result.content,
                thumbnailUrl = result.thumbnailUrl,
                createdAt = result.createdAt
            )
    }
}

data class PagedFeedSearchResponse(
    val feeds: List<FeedSearchItemResponse>,
    val currentPage: Int,
    val totalPages: Int,
    val totalElements: Long,
    val size: Int,
    val hasNext: Boolean,
    val hasPrevious: Boolean
) {
    companion object {
        fun from(result: PagedFeedSearchResult): PagedFeedSearchResponse =
            PagedFeedSearchResponse(
                feeds = result.feeds.map { FeedSearchItemResponse.from(it) },
                currentPage = result.currentPage,
                totalPages = result.totalPages,
                totalElements = result.totalElements,
                size = result.size,
                hasNext = result.hasNext,
                hasPrevious = result.hasPrevious
            )
    }
}
