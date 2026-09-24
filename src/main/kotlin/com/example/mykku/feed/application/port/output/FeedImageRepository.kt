package com.example.mykku.feed.application.port.output

import com.example.mykku.feed.domain.entity.FeedImage
import com.example.mykku.feed.domain.vo.FeedId

interface FeedImageRepository {
    fun saveAll(feedImages: List<FeedImage>, feedId: FeedId): List<FeedImage>
    fun findByFeedId(feedId: FeedId): List<FeedImage>
    fun findByFeedIds(feedIds: List<FeedId>): List<FeedImage>
    fun findThumbnailUrlsByFeedIds(feedIds: List<FeedId>): Map<FeedId, String>
    fun findAllByIdInAndFeedId(ids: List<Long>, feedId: FeedId): List<FeedImage>
    fun deleteAll(feedImages: List<FeedImage>)
    fun deleteAllByIds(ids: List<Long>)
    fun deleteAllByFeedId(feedId: FeedId)
}
