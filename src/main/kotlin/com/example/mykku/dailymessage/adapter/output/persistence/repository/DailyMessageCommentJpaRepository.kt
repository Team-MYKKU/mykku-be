package com.example.mykku.dailymessage.adapter.output.persistence.repository

import com.example.mykku.dailymessage.adapter.output.persistence.entity.DailyMessageCommentJpaEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface DailyMessageCommentJpaRepository : JpaRepository<DailyMessageCommentJpaEntity, Long> {
    @Query("SELECT c FROM DailyMessageCommentJpaEntity c WHERE c.id = :commentId AND c.dailyMessage.id = :dailyMessageId")
    fun findByIdAndDailyMessageId(commentId: Long, dailyMessageId: Long): DailyMessageCommentJpaEntity?

    @Query(
        value = "SELECT c FROM DailyMessageCommentJpaEntity c LEFT JOIN FETCH c.member WHERE c.dailyMessage.id = :dailyMessageId AND c.parentComment IS NULL",
        countQuery = "SELECT COUNT(c) FROM DailyMessageCommentJpaEntity c WHERE c.dailyMessage.id = :dailyMessageId AND c.parentComment IS NULL"
    )
    fun findByDailyMessageIdAndParentCommentIsNull(
        dailyMessageId: Long,
        pageable: Pageable
    ): Page<DailyMessageCommentJpaEntity>

    @Query("SELECT c FROM DailyMessageCommentJpaEntity c LEFT JOIN FETCH c.member WHERE c.parentComment.id IN :parentCommentIds")
    fun findByParentCommentIdIn(parentCommentIds: List<Long>): List<DailyMessageCommentJpaEntity>

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM DailyMessageCommentJpaEntity c WHERE c.id IN :ids")
    fun deleteAllByIdIn(@Param("ids") ids: List<Long>)
}
