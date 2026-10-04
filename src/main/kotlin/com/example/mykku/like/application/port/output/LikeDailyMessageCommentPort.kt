package com.example.mykku.like.application.port.output

import com.example.mykku.like.domain.entity.LikeDailyMessageCommentEntity

interface LikeDailyMessageCommentPort {
    fun save(likeDailyMessageComment: LikeDailyMessageCommentEntity): LikeDailyMessageCommentEntity
    fun existsByMemberIdAndDailyMessageCommentId(memberId: Long, dailyMessageCommentId: Long): Boolean
    fun deleteByMemberIdAndDailyMessageCommentId(memberId: Long, dailyMessageCommentId: Long)
    fun findLikedCommentIds(memberId: Long, dailyMessageCommentIds: List<Long>): Set<Long>
    fun countByCommentIdIn(dailyMessageCommentIds: List<Long>): Map<Long, Int>
}
