package com.example.mykku.like.adapter.output.persistence

import com.example.mykku.common.adapter.persistence.IdCountRow
import com.example.mykku.like.adapter.output.persistence.entity.LikeDailyMessageCommentJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface LikeDailyMessageCommentJpaRepository : JpaRepository<LikeDailyMessageCommentJpaEntity, Long> {
    fun existsByMemberIdAndDailyMessageCommentId(memberId: Long, dailyMessageCommentId: Long): Boolean
    fun deleteByMemberIdAndDailyMessageCommentId(memberId: Long, dailyMessageCommentId: Long)

    @Query(
        "SELECT l.dailyMessageComment.id FROM LikeDailyMessageCommentJpaEntity l " +
            "WHERE l.member.id = :memberId AND l.dailyMessageComment.id IN :dailyMessageCommentIds"
    )
    fun findLikedDailyMessageCommentIds(
        @Param("memberId") memberId: Long,
        @Param("dailyMessageCommentIds") dailyMessageCommentIds: List<Long>
    ): List<Long>

    @Query(
        """
        SELECT l.dailyMessageComment.id AS entityId, COUNT(l) AS countValue
        FROM LikeDailyMessageCommentJpaEntity l
        WHERE l.dailyMessageComment.id IN :dailyMessageCommentIds
        GROUP BY l.dailyMessageComment.id
        """
    )
    fun countGroupedByDailyMessageCommentIdIn(
        @Param("dailyMessageCommentIds") dailyMessageCommentIds: List<Long>
    ): List<IdCountRow>
}
