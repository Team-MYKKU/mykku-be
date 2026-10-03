package com.example.mykku.like.adapter.output.persistence

import com.example.mykku.common.adapter.persistence.toCountMap
import com.example.mykku.feed.adapter.output.persistence.FeedCommentJpaRepository
import com.example.mykku.feed.exception.FeedException
import com.example.mykku.like.adapter.output.persistence.entity.LikeFeedCommentJpaEntity
import com.example.mykku.like.application.port.output.LikeFeedCommentPort
import com.example.mykku.like.domain.entity.LikeFeedCommentEntity
import com.example.mykku.member.adapter.output.persistence.MemberJpaRepository
import com.example.mykku.member.exception.MemberException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

@Repository
class LikeFeedCommentPersistenceAdapter(
    private val likeFeedCommentJpaRepository: LikeFeedCommentJpaRepository,
    private val memberJpaRepository: MemberJpaRepository,
    private val feedCommentJpaRepository: FeedCommentJpaRepository
) : LikeFeedCommentPort {

    override fun save(likeFeedComment: LikeFeedCommentEntity): LikeFeedCommentEntity {
        val member = memberJpaRepository.findByIdOrNull(likeFeedComment.memberId)
            ?: throw MemberException.memberNotFound()
        val feedComment = feedCommentJpaRepository.findByIdOrNull(likeFeedComment.feedCommentId)
            ?: throw FeedException.feedCommentNotFound()

        val jpaEntity = LikeFeedCommentJpaEntity.fromDomain(likeFeedComment, member, feedComment)
        return likeFeedCommentJpaRepository.save(jpaEntity).toDomain()
    }

    override fun existsByMemberIdAndFeedCommentId(memberId: Long, feedCommentId: Long): Boolean {
        return likeFeedCommentJpaRepository.existsByMemberIdAndFeedCommentId(memberId, feedCommentId)
    }

    override fun deleteByMemberIdAndFeedCommentId(memberId: Long, feedCommentId: Long) {
        likeFeedCommentJpaRepository.deleteByMemberIdAndFeedCommentId(memberId, feedCommentId)
    }

    override fun deleteAllByFeedCommentIdIn(feedCommentIds: List<Long>) {
        likeFeedCommentJpaRepository.deleteAllByFeedCommentIdIn(feedCommentIds)
    }

    override fun countByFeedCommentIdIn(feedCommentIds: List<Long>): Map<Long, Int> {
        if (feedCommentIds.isEmpty()) return emptyMap()
        return likeFeedCommentJpaRepository.countGroupedByFeedCommentIdIn(feedCommentIds).toCountMap()
    }

    override fun findLikedFeedCommentIds(memberId: Long, feedCommentIds: List<Long>): Set<Long> {
        if (feedCommentIds.isEmpty()) return emptySet()
        return likeFeedCommentJpaRepository.findLikedFeedCommentIds(memberId, feedCommentIds).toSet()
    }
}
