package com.example.mykku.feed.adapter.output.persistence

import com.example.mykku.feed.adapter.output.persistence.entity.FeedImageJpaEntity
import com.example.mykku.feed.adapter.output.persistence.entity.FeedJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface FeedImageJpaRepository : JpaRepository<FeedImageJpaEntity, Long> {
    fun findByFeed(feed: FeedJpaEntity): List<FeedImageJpaEntity>
    fun findByFeedIn(feeds: List<FeedJpaEntity>): List<FeedImageJpaEntity>

    @Query("""
        SELECT fi.feed.id AS feedId, fi.url AS url
        FROM FeedImageJpaEntity fi
        WHERE fi.id IN (
            SELECT MIN(candidate.id) FROM FeedImageJpaEntity candidate
            WHERE candidate.feed.id IN :feedIds
            GROUP BY candidate.feed.id
        )
    """)
    fun findThumbnailsByFeedIdIn(@Param("feedIds") feedIds: List<Long>): List<FeedThumbnailRow>

    fun deleteAllByFeed(feed: FeedJpaEntity)
    fun deleteAllByIdIn(ids: List<Long>)
    fun findAllByIdInAndFeed(ids: List<Long>, feed: FeedJpaEntity): List<FeedImageJpaEntity>
}
