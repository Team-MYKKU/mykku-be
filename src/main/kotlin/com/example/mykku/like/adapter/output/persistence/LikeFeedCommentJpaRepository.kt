package com.example.mykku.like.adapter.output.persistence

import com.example.mykku.common.adapter.persistence.IdCountRow
import com.example.mykku.like.adapter.output.persistence.entity.LikeFeedCommentJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface LikeFeedCommentJpaRepository : JpaRepository<LikeFeedCommentJpaEntity, Long> {
    fun existsByMemberIdAndFeedCommentId(memberId: Long, feedCommentId: Long): Boolean
    fun deleteByMemberIdAndFeedCommentId(memberId: Long, feedCommentId: Long)
    fun deleteAllByFeedCommentIdIn(feedCommentIds: List<Long>)

    @Query(
        """
        SELECT l.feedComment.id AS entityId, COUNT(l) AS countValue
        FROM LikeFeedCommentJpaEntity l
        WHERE l.feedComment.id IN :feedCommentIds
        GROUP BY l.feedComment.id
        """
    )
    fun countGroupedByFeedCommentIdIn(@Param("feedCommentIds") feedCommentIds: List<Long>): List<IdCountRow>

    @Query(
        """
        SELECT l.feedComment.id
        FROM LikeFeedCommentJpaEntity l
        WHERE l.member.id = :memberId AND l.feedComment.id IN :feedCommentIds
        """
    )
    fun findLikedFeedCommentIds(
        @Param("memberId") memberId: Long,
        @Param("feedCommentIds") feedCommentIds: List<Long>
    ): List<Long>
}
