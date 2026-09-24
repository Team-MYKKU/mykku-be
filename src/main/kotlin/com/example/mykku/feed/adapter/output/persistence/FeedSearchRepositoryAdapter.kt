package com.example.mykku.feed.adapter.output.persistence

import com.example.mykku.feed.application.dto.BoardMatchSummary
import com.example.mykku.feed.application.dto.FeedSearchCondition
import com.example.mykku.feed.application.port.output.FeedSearchRepository
import com.example.mykku.feed.domain.entity.Feed
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class FeedSearchRepositoryAdapter(
    private val feedSearchJpaRepository: FeedSearchJpaRepository
) : FeedSearchRepository {

    override fun summarizeByBoard(condition: FeedSearchCondition): List<BoardMatchSummary> {
        return feedSearchJpaRepository.summarizeByBoard(condition.keyword.value, condition.viewerId)
            .map { BoardMatchSummary(it.getBoardId(), it.getMatchCount(), it.getLatestCreatedAt()) }
    }

    override fun findTopByBoard(condition: FeedSearchCondition, boardId: Long, limit: Int): List<Feed> {
        val pageable = PageRequest.of(0, limit)
        return feedSearchJpaRepository.findTopByBoard(condition.keyword.value, condition.viewerId, boardId, pageable)
            .map { it.toDomain() }
    }

    override fun findByBoard(condition: FeedSearchCondition, boardId: Long, pageable: Pageable): Page<Feed> {
        return feedSearchJpaRepository.findByBoard(condition.keyword.value, condition.viewerId, boardId, pageable)
            .map { it.toDomain() }
    }
}
