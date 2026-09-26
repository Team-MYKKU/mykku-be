package com.example.mykku.dailymessage.application.usecase

import com.example.mykku.dailymessage.application.port.input.AdminDeleteDailyMessageCommentUseCase
import com.example.mykku.dailymessage.application.port.output.DailyMessageCommentRepository
import com.example.mykku.dailymessage.domain.entity.DailyMessageComment
import com.example.mykku.dailymessage.domain.vo.DailyMessageCommentId
import com.example.mykku.dailymessage.exception.DailyMessageException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminDeleteDailyMessageCommentUseCaseImpl(
    private val dailyMessageCommentRepository: DailyMessageCommentRepository
) : AdminDeleteDailyMessageCommentUseCase {

    @Transactional
    override fun execute(commentId: Long) {
        val comment = dailyMessageCommentRepository.findById(DailyMessageCommentId.of(commentId))
            ?: throw DailyMessageException.dailyMessageCommentNotFound()
        log.info(
            "admin delete daily message comment id={}, dailyMessageId={}, memberId={}",
            commentId,
            comment.dailyMessageId,
            comment.memberId
        )
        val deepestFirst = collectDescendants(comment.id.value).reversed() + comment
        deepestFirst.forEach { dailyMessageCommentRepository.delete(it) }
    }

    private fun collectDescendants(commentId: Long): List<DailyMessageComment> {
        val descendants = mutableListOf<DailyMessageComment>()
        var parentIds = listOf(commentId)
        while (parentIds.isNotEmpty()) {
            val children = dailyMessageCommentRepository.findByParentCommentIds(parentIds)
            descendants += children
            parentIds = children.map { it.id.value }
        }
        return descendants
    }

    companion object {
        private val log = LoggerFactory.getLogger(AdminDeleteDailyMessageCommentUseCaseImpl::class.java)
    }
}
