package com.example.mykku.feed.application.port.output

import com.example.mykku.feed.domain.entity.FeedComment
import com.example.mykku.feed.domain.vo.FeedCommentId
import com.example.mykku.feed.domain.vo.FeedId
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface FeedCommentRepository {
    fun save(feedComment: FeedComment, feedId: FeedId, memberId: Long): FeedComment
    fun findById(id: FeedCommentId): FeedComment?
    fun findByIdOrThrow(id: FeedCommentId): FeedComment
    fun findByFeedIdAndParentCommentIsNull(feedId: FeedId, pageable: Pageable): Page<FeedComment>
    fun findFirstCommentsByFeedIds(feedIds: List<FeedId>): Map<Long, FeedComment>
    fun findByParentCommentIds(parentCommentIds: List<FeedCommentId>): List<FeedComment>
    fun countByFeedId(feedId: FeedId): Long
    fun countByFeedIdIn(feedIds: List<FeedId>): Map<Long, Int>
    fun findIdsByFeedId(feedId: FeedId): List<Long>
    fun delete(feedComment: FeedComment)
    fun deleteAllByFeedId(feedId: FeedId)
}
