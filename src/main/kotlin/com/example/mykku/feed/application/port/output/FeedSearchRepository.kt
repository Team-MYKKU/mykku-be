package com.example.mykku.feed.application.port.output

import com.example.mykku.feed.application.dto.BoardMatchSummary
import com.example.mykku.feed.application.dto.FeedSearchCondition
import com.example.mykku.feed.domain.entity.Feed
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface FeedSearchRepository {
    fun summarizeByBoard(condition: FeedSearchCondition): List<BoardMatchSummary>
    fun findTopByBoard(condition: FeedSearchCondition, boardId: Long, limit: Int): List<Feed>
    fun findByBoard(condition: FeedSearchCondition, boardId: Long, pageable: Pageable): Page<Feed>
}
