package com.example.mykku.dailymessage.application.usecase

import com.example.mykku.dailymessage.application.port.input.AdminDeleteDailyMessageCommentUseCase
import com.example.mykku.dailymessage.application.port.output.DailyMessageCommentRepository
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
        collectDescendantLevels(comment.id.value).reversed()
            .forEach { dailyMessageCommentRepository.deleteAllByIds(it) }
        dailyMessageCommentRepository.delete(comment)
    }

    private fun collectDescendantLevels(commentId: Long): List<List<Long>> {
        val levels = mutableListOf<List<Long>>()
        var parentIds = listOf(commentId)
        while (parentIds.isNotEmpty()) {
            parentIds = dailyMessageCommentRepository.findByParentCommentIds(parentIds).map { it.id.value }
            if (parentIds.isNotEmpty()) levels += parentIds
        }
        return levels
    }

    companion object {
        private val log = LoggerFactory.getLogger(AdminDeleteDailyMessageCommentUseCaseImpl::class.java)
    }
}
