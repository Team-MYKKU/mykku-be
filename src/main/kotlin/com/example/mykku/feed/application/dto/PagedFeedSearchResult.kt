package com.example.mykku.feed.application.dto

import org.springframework.data.domain.Page

data class PagedFeedSearchResult(
    val feeds: List<FeedSearchItemResult>,
    val currentPage: Int,
    val totalPages: Int,
    val totalElements: Long,
    val size: Int,
    val hasNext: Boolean,
    val hasPrevious: Boolean
) {
    companion object {
        fun of(page: Page<*>, feeds: List<FeedSearchItemResult>): PagedFeedSearchResult =
            PagedFeedSearchResult(
                feeds = feeds,
                currentPage = page.number,
                totalPages = page.totalPages,
                totalElements = page.totalElements,
                size = page.size,
                hasNext = page.hasNext(),
                hasPrevious = page.hasPrevious()
            )
    }
}
