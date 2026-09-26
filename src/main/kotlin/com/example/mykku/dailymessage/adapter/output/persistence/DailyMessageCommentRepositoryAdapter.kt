package com.example.mykku.dailymessage.adapter.output.persistence

import com.example.mykku.dailymessage.adapter.output.persistence.entity.DailyMessageCommentJpaEntity
import com.example.mykku.dailymessage.adapter.output.persistence.repository.DailyMessageCommentJpaRepository
import com.example.mykku.dailymessage.adapter.output.persistence.repository.DailyMessageJpaRepository
import com.example.mykku.dailymessage.application.port.output.DailyMessageCommentRepository
import com.example.mykku.dailymessage.domain.entity.DailyMessageComment
import com.example.mykku.dailymessage.domain.vo.DailyMessageCommentId
import com.example.mykku.dailymessage.domain.vo.DailyMessageId
import com.example.mykku.dailymessage.exception.DailyMessageException
import com.example.mykku.member.adapter.output.persistence.MemberJpaRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

@Component
class DailyMessageCommentRepositoryAdapter(
    private val dailyMessageCommentJpaRepository: DailyMessageCommentJpaRepository,
    private val dailyMessageJpaRepository: DailyMessageJpaRepository,
    private val memberJpaRepository: MemberJpaRepository
) : DailyMessageCommentRepository {

    override fun save(comment: DailyMessageComment): DailyMessageComment {
        val dailyMessageJpaEntity = dailyMessageJpaRepository.findById(comment.dailyMessageId)
            .orElseThrow { DailyMessageException.dailyMessageNotFound() }

        val memberEntity = memberJpaRepository.findById(comment.memberId!!)
            .orElseThrow { throw IllegalStateException("Member not found: ${comment.memberId}") }

        val parentCommentEntity = comment.parentCommentId?.let { parentId ->
            dailyMessageCommentJpaRepository.findById(parentId)
                .orElseThrow { DailyMessageException.dailyMessageCommentNotFound() }
        }

        val jpaEntity = if (comment.id.value == 0L) {
            DailyMessageCommentJpaEntity.fromDomain(
                comment = comment,
                dailyMessageJpaEntity = dailyMessageJpaEntity,
                memberEntity = memberEntity,
                parentCommentEntity = parentCommentEntity
            )
        } else {
            dailyMessageCommentJpaRepository.findById(comment.id.value)
                .map { entity ->
                    entity.updateFromDomain(comment)
                    entity
                }
                .orElseGet {
                    DailyMessageCommentJpaEntity.fromDomain(
                        comment = comment,
                        dailyMessageJpaEntity = dailyMessageJpaEntity,
                        memberEntity = memberEntity,
                        parentCommentEntity = parentCommentEntity
                    )
                }
        }

        return dailyMessageCommentJpaRepository.save(jpaEntity).toDomain()
    }

    override fun findById(id: DailyMessageCommentId): DailyMessageComment? {
        return dailyMessageCommentJpaRepository.findById(id.value)
            .map { it.toDomain() }
            .orElse(null)
    }

    override fun findByIdAndDailyMessageId(
        commentId: DailyMessageCommentId,
        dailyMessageId: DailyMessageId
    ): DailyMessageComment? {
        return dailyMessageCommentJpaRepository
            .findByIdAndDailyMessageId(commentId.value, dailyMessageId.value)
            ?.toDomain()
    }

    override fun findByDailyMessageIdAndParentCommentIsNull(
        dailyMessageId: DailyMessageId,
        pageable: Pageable
    ): Page<DailyMessageComment> {
        return dailyMessageCommentJpaRepository
            .findByDailyMessageIdAndParentCommentIsNull(dailyMessageId.value, pageable)
            .map { it.toDomain() }
    }

    override fun findByParentCommentIds(parentCommentIds: List<Long>): List<DailyMessageComment> {
        if (parentCommentIds.isEmpty()) return emptyList()
        return dailyMessageCommentJpaRepository.findByParentCommentIdIn(parentCommentIds)
            .map { it.toDomain() }
    }

    override fun delete(comment: DailyMessageComment) {
        dailyMessageCommentJpaRepository.deleteById(comment.id.value)
    }

    override fun deleteAllByIds(ids: List<Long>) {
        if (ids.isEmpty()) return
        dailyMessageCommentJpaRepository.deleteAllByIdIn(ids)
    }
}
