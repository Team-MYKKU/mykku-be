package com.example.mykku.like.adapter.output.persistence

import com.example.mykku.common.adapter.persistence.toCountMap
import com.example.mykku.dailymessage.adapter.output.persistence.repository.DailyMessageCommentJpaRepository
import com.example.mykku.dailymessage.exception.DailyMessageException
import com.example.mykku.like.adapter.output.persistence.entity.LikeDailyMessageCommentJpaEntity
import com.example.mykku.like.application.port.output.LikeDailyMessageCommentPort
import com.example.mykku.like.domain.entity.LikeDailyMessageCommentEntity
import com.example.mykku.member.adapter.output.persistence.MemberJpaRepository
import com.example.mykku.member.exception.MemberException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

@Repository
class LikeDailyMessageCommentPersistenceAdapter(
    private val likeDailyMessageCommentJpaRepository: LikeDailyMessageCommentJpaRepository,
    private val memberJpaRepository: MemberJpaRepository,
    private val dailyMessageCommentJpaRepository: DailyMessageCommentJpaRepository
) : LikeDailyMessageCommentPort {

    override fun save(likeDailyMessageComment: LikeDailyMessageCommentEntity): LikeDailyMessageCommentEntity {
        val member = memberJpaRepository.findByIdOrNull(likeDailyMessageComment.memberId)
            ?: throw MemberException.memberNotFound()
        val dailyMessageComment = dailyMessageCommentJpaRepository.findByIdOrNull(likeDailyMessageComment.dailyMessageCommentId)
            ?: throw DailyMessageException.dailyMessageCommentNotFound()

        val jpaEntity = LikeDailyMessageCommentJpaEntity.fromDomain(likeDailyMessageComment, member, dailyMessageComment)
        return likeDailyMessageCommentJpaRepository.save(jpaEntity).toDomain()
    }

    override fun existsByMemberIdAndDailyMessageCommentId(memberId: Long, dailyMessageCommentId: Long): Boolean {
        return likeDailyMessageCommentJpaRepository.existsByMemberIdAndDailyMessageCommentId(memberId, dailyMessageCommentId)
    }

    override fun deleteByMemberIdAndDailyMessageCommentId(memberId: Long, dailyMessageCommentId: Long) {
        likeDailyMessageCommentJpaRepository.deleteByMemberIdAndDailyMessageCommentId(memberId, dailyMessageCommentId)
    }

    override fun findLikedCommentIds(memberId: Long, dailyMessageCommentIds: List<Long>): Set<Long> {
        if (dailyMessageCommentIds.isEmpty()) {
            return emptySet()
        }
        return likeDailyMessageCommentJpaRepository
            .findLikedDailyMessageCommentIds(memberId, dailyMessageCommentIds)
            .toSet()
    }

    override fun countByCommentIdIn(dailyMessageCommentIds: List<Long>): Map<Long, Int> {
        if (dailyMessageCommentIds.isEmpty()) return emptyMap()
        return likeDailyMessageCommentJpaRepository
            .countGroupedByDailyMessageCommentIdIn(dailyMessageCommentIds)
            .toCountMap()
    }
}
