package com.example.mykku.dailymessage.application.port.output

import com.example.mykku.dailymessage.domain.entity.DailyMessageComment
import com.example.mykku.dailymessage.domain.vo.DailyMessageCommentId
import com.example.mykku.dailymessage.domain.vo.DailyMessageId
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface DailyMessageCommentRepository {
    fun save(comment: DailyMessageComment): DailyMessageComment
    fun findById(id: DailyMessageCommentId): DailyMessageComment?
    fun findByIdAndDailyMessageId(commentId: DailyMessageCommentId, dailyMessageId: DailyMessageId): DailyMessageComment?
    fun findByDailyMessageIdAndParentCommentIsNull(dailyMessageId: DailyMessageId, pageable: Pageable): Page<DailyMessageComment>
    fun findByParentCommentIds(parentCommentIds: List<Long>): List<DailyMessageComment>
    fun delete(comment: DailyMessageComment)
    fun deleteAllByIds(ids: List<Long>)
}
