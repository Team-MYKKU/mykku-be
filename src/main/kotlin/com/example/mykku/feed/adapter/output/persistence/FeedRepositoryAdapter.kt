package com.example.mykku.feed.adapter.output.persistence

import com.example.mykku.board.adapter.output.persistence.BoardJpaRepository
import com.example.mykku.common.adapter.persistence.toCountMap
import com.example.mykku.feed.adapter.output.persistence.entity.FeedJpaEntity
import com.example.mykku.feed.application.port.output.FeedRepository
import com.example.mykku.feed.domain.entity.Feed
import com.example.mykku.feed.domain.vo.FeedId
import com.example.mykku.feed.exception.FeedException
import com.example.mykku.member.adapter.output.persistence.MemberJpaRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class FeedRepositoryAdapter(
    private val feedJpaRepository: FeedJpaRepository,
    private val boardJpaRepository: BoardJpaRepository,
    private val memberJpaRepository: MemberJpaRepository
) : FeedRepository {

    companion object {
        const val DEFAULT_POPULAR_FEEDS_LIMIT = 3
        const val DEFAULT_POPULAR_FEEDS_DAYS_AGO = 7
    }

    override fun save(feed: Feed, boardId: Long, memberId: Long): Feed {
        val board = boardJpaRepository.findById(boardId)
            .orElseThrow { IllegalArgumentException("Board not found: $boardId") }
        val member = memberJpaRepository.findById(memberId)
            .orElseThrow { IllegalArgumentException("Member not found: $memberId") }
        val jpaEntity = FeedJpaEntity.fromDomain(feed, board, member)
        return feedJpaRepository.save(jpaEntity).toDomain()
    }

    override fun update(feed: Feed): Feed {
        val existing = feedJpaRepository.findById(feed.id!!.value)
            .orElseThrow { FeedException.feedNotFound() }
        existing.title = feed.title
        existing.content = feed.content
        return feedJpaRepository.save(existing).toDomain()
    }

    override fun findById(id: FeedId): Feed? {
        return feedJpaRepository.findById(id.value)
            .map { it.toDomain() }
            .orElse(null)
    }

    override fun findByIdOrThrow(id: FeedId): Feed {
        return feedJpaRepository.findById(id.value)
            .map { it.toDomain() }
            .orElseThrow { FeedException.feedNotFound() }
    }

    override fun findByBoardId(boardId: Long, pageable: Pageable): Page<Feed> {
        val board = boardJpaRepository.findById(boardId)
            .orElseThrow { IllegalArgumentException("Board not found") }
        return feedJpaRepository.findAllByBoardOrderByCreatedAtDesc(board, pageable)
            .map { it.toDomain() }
    }

    override fun findAllByIds(ids: List<FeedId>): List<Feed> {
        if (ids.isEmpty()) return emptyList()
        return feedJpaRepository.findAllByIdIn(ids.map { it.value })
            .map { it.toDomain() }
    }

    override fun findByMemberId(memberId: Long, pageable: Pageable): Page<Feed> {
        return feedJpaRepository.findAllByMemberIdOrderByCreatedAtDesc(memberId, pageable)
            .map { it.toDomain() }
    }

    override fun findPopularFeedsByBoardId(boardId: Long, limit: Int, daysAgo: Int): List<Feed> {
        val board = boardJpaRepository.findById(boardId)
            .orElseThrow { IllegalArgumentException("Board not found") }
        val since = LocalDateTime.now().minusDays(daysAgo.toLong())
        val pageable = PageRequest.of(0, limit)
        return feedJpaRepository.findPopularFeedsByBoardSince(board, since, pageable)
            .map { it.toDomain() }
    }

    override fun findAllByBoardId(boardId: Long): List<Feed> {
        return feedJpaRepository.findAllByBoardIdOrderByIdAsc(boardId).map { it.toDomain() }
    }

    override fun countByBoardIdIn(boardIds: List<Long>): Map<Long, Int> {
        if (boardIds.isEmpty()) return emptyMap()
        return feedJpaRepository.countGroupedByBoardIdIn(boardIds).toCountMap()
    }

    override fun moveAllToBoard(fromBoardId: Long, toBoardId: Long): Int {
        return feedJpaRepository.moveAllToBoard(fromBoardId, toBoardId)
    }

    override fun delete(feed: Feed) {
        val jpaEntity = feedJpaRepository.findById(feed.id!!.value)
            .orElseThrow { FeedException.feedNotFound() }
        feedJpaRepository.delete(jpaEntity)
    }
}
