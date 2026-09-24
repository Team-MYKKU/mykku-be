package com.example.mykku.feed.adapter.output.persistence

import com.example.mykku.feed.adapter.output.persistence.entity.FeedImageJpaEntity
import com.example.mykku.feed.application.port.output.FeedImageRepository
import com.example.mykku.feed.domain.entity.FeedImage
import com.example.mykku.feed.domain.vo.FeedId
import org.springframework.stereotype.Repository

@Repository
class FeedImageRepositoryAdapter(
    private val feedImageJpaRepository: FeedImageJpaRepository,
    private val feedJpaRepository: FeedJpaRepository
) : FeedImageRepository {

    override fun saveAll(feedImages: List<FeedImage>, feedId: FeedId): List<FeedImage> {
        if (feedImages.isEmpty()) return emptyList()
        val feed = feedJpaRepository.findById(feedId.value)
            .orElseThrow { IllegalArgumentException("Feed not found: ${feedId.value}") }
        val jpaEntities = feedImages.map { FeedImageJpaEntity.fromDomain(it, feed) }
        return feedImageJpaRepository.saveAll(jpaEntities).map { it.toDomain() }
    }

    override fun findByFeedId(feedId: FeedId): List<FeedImage> {
        val feed = feedJpaRepository.findById(feedId.value).orElse(null) ?: return emptyList()
        return feedImageJpaRepository.findByFeed(feed).map { it.toDomain() }
    }

    override fun findByFeedIds(feedIds: List<FeedId>): List<FeedImage> {
        if (feedIds.isEmpty()) return emptyList()
        val feeds = feedJpaRepository.findAllById(feedIds.map { it.value })
        if (feeds.isEmpty()) return emptyList()
        return feedImageJpaRepository.findByFeedIn(feeds).map { it.toDomain() }
    }

    override fun findThumbnailUrlsByFeedIds(feedIds: List<FeedId>): Map<FeedId, String> {
        if (feedIds.isEmpty()) return emptyMap()
        return feedImageJpaRepository.findThumbnailsByFeedIdIn(feedIds.map { it.value })
            .associate { FeedId.of(it.getFeedId()) to it.getUrl() }
    }

    override fun findAllByIdInAndFeedId(ids: List<Long>, feedId: FeedId): List<FeedImage> {
        if (ids.isEmpty()) return emptyList()
        val feed = feedJpaRepository.findById(feedId.value).orElse(null) ?: return emptyList()
        return feedImageJpaRepository.findAllByIdInAndFeed(ids, feed).map { it.toDomain() }
    }

    override fun deleteAll(feedImages: List<FeedImage>) {
        if (feedImages.isEmpty()) return
        val ids = feedImages.mapNotNull { it.id?.value }
        if (ids.isNotEmpty()) {
            feedImageJpaRepository.deleteAllById(ids)
        }
    }

    override fun deleteAllByIds(ids: List<Long>) {
        if (ids.isEmpty()) return
        feedImageJpaRepository.deleteAllById(ids)
    }

    override fun deleteAllByFeedId(feedId: FeedId) {
        val feed = feedJpaRepository.findById(feedId.value).orElse(null) ?: return
        feedImageJpaRepository.deleteAllByFeed(feed)
    }
}
