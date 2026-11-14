package com.example.mykku.feed.adapter.output.persistence

import com.example.mykku.common.adapter.persistence.toCountMap
import com.example.mykku.feed.adapter.output.persistence.entity.FeedCommentJpaEntity
import com.example.mykku.feed.application.port.output.FeedCommentRepository
import com.example.mykku.feed.domain.entity.FeedComment
import com.example.mykku.feed.domain.vo.FeedCommentId
import com.example.mykku.feed.domain.vo.FeedId
import com.example.mykku.feed.exception.FeedException
import com.example.mykku.member.adapter.output.persistence.MemberJpaRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class FeedCommentRepositoryAdapter(
    private val feedCommentJpaRepository: FeedCommentJpaRepository,
    private val feedJpaRepository: FeedJpaRepository,
    private val memberJpaRepository: MemberJpaRepository
) : FeedCommentRepository {

    override fun save(feedComment: FeedComment, feedId: FeedId, memberId: Long): FeedComment {
        val feed = feedJpaRepository.findById(feedId.value)
            .orElseThrow { IllegalArgumentException("Feed not found: ${feedId.value}") }
        val member = memberJpaRepository.findById(memberId)
            .orElseThrow { IllegalArgumentException("Member not found: $memberId") }
        val parentComment = feedComment.parentCommentId?.let {
            feedCommentJpaRepository.findById(it.value).orElse(null)
        }
        val jpaEntity = FeedCommentJpaEntity.fromDomain(feedComment, feed, member, parentComment)
        return feedCommentJpaRepository.save(jpaEntity).toDomain()
    }

    override fun findById(id: FeedCommentId): FeedComment? {
        return feedCommentJpaRepository.findById(id.value)
            .map { it.toDomain() }
            .orElse(null)
    }

    override fun findByIdOrThrow(id: FeedCommentId): FeedComment {
        return feedCommentJpaRepository.findById(id.value)
            .map { it.toDomain() }
            .orElseThrow { FeedException.feedCommentNotFound() }
    }

    override fun findByFeedIdAndParentCommentIsNull(
        feedId: FeedId,
        excludedMemberIds: Set<Long>,
        pageable: Pageable
    ): Page<FeedComment> {
        val feed = feedJpaRepository.findById(feedId.value).orElse(null)
            ?: return Page.empty(pageable)
        val page = if (excludedMemberIds.isEmpty()) {
            feedCommentJpaRepository.findByFeedAndParentCommentIsNull(feed, pageable)
        } else {
            feedCommentJpaRepository.findTopLevelByFeedExcludingMembers(feed, excludedMemberIds, pageable)
        }
        return page.map { it.toDomain() }
    }

    override fun findFirstCommentsByFeedIds(
        feedIds: List<FeedId>,
        excludedMemberIds: Set<Long>
    ): Map<Long, FeedComment> {
        if (feedIds.isEmpty()) return emptyMap()
        val ids = feedIds.map { it.value }
        val comments = if (excludedMemberIds.isEmpty()) {
            feedCommentJpaRepository.findFirstCommentsByFeedIds(ids)
        } else {
            feedCommentJpaRepository.findFirstCommentsByFeedIdsExcludingMembers(ids, excludedMemberIds)
        }
        return comments.associate { it.feed.id!! to it.toDomain() }
    }

    override fun findByParentCommentIds(parentCommentIds: List<FeedCommentId>): List<FeedComment> {
        if (parentCommentIds.isEmpty()) return emptyList()
        val parentComments = feedCommentJpaRepository.findAllById(parentCommentIds.map { it.value })
        if (parentComments.isEmpty()) return emptyList()
        return feedCommentJpaRepository.findByParentCommentIn(parentComments.toList()).map { it.toDomain() }
    }

    override fun countByFeedId(feedId: FeedId, excludedMemberIds: Set<Long>): Long {
        if (excludedMemberIds.isEmpty()) return feedCommentJpaRepository.countByFeedId(feedId.value)
        return feedCommentJpaRepository.countByFeedIdExcludingMembers(feedId.value, excludedMemberIds)
    }

    override fun countByFeedIdIn(feedIds: List<FeedId>, excludedMemberIds: Set<Long>): Map<Long, Int> {
        if (feedIds.isEmpty()) return emptyMap()
        val ids = feedIds.map { it.value }
        val rows = if (excludedMemberIds.isEmpty()) {
            feedCommentJpaRepository.countGroupedByFeedIdIn(ids)
        } else {
            feedCommentJpaRepository.countGroupedByFeedIdInExcludingMembers(ids, excludedMemberIds)
        }
        return rows.toCountMap()
    }

    override fun findIdsByFeedId(feedId: FeedId): List<Long> {
        val feed = feedJpaRepository.findById(feedId.value).orElse(null) ?: return emptyList()
        return feedCommentJpaRepository.findIdsByFeed(feed)
    }

    override fun delete(feedComment: FeedComment) {
        val id = feedComment.id?.value ?: return
        feedCommentJpaRepository.deleteById(id)
    }

    override fun deleteAllByFeedId(feedId: FeedId) {
        val feed = feedJpaRepository.findById(feedId.value).orElse(null) ?: return
        val comments = feedCommentJpaRepository.findAllByFeed(feed)
        feedCommentJpaRepository.deleteAll(sortRepliesFirst(comments))
        feedCommentJpaRepository.flush()
    }

    private fun sortRepliesFirst(comments: List<FeedCommentJpaEntity>): List<FeedCommentJpaEntity> {
        val parentIds = comments.associate { it.id to it.parentComment?.id }
        return comments.sortedByDescending { depthOf(it.id, parentIds) }
    }

    private fun depthOf(commentId: Long?, parentIds: Map<Long?, Long?>): Int {
        var depth = 0
        var parentId = parentIds[commentId]
        while (parentId != null && depth < parentIds.size) {
            depth++
            parentId = parentIds[parentId]
        }
        return depth
    }
}
