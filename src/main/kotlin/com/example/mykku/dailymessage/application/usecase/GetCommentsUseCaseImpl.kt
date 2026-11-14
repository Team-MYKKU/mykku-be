package com.example.mykku.dailymessage.application.usecase

import com.example.mykku.block.application.port.input.BlockFilterUseCase
import com.example.mykku.comment.application.dto.CommentPageResult
import com.example.mykku.dailymessage.application.port.input.GetCommentsUseCase
import com.example.mykku.dailymessage.application.port.output.DailyMessageCommentRepository
import com.example.mykku.dailymessage.application.port.output.DailyMessageRepository
import com.example.mykku.dailymessage.domain.vo.DailyMessageId
import com.example.mykku.dailymessage.exception.DailyMessageException
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GetCommentsUseCaseImpl(
    private val dailyMessageRepository: DailyMessageRepository,
    private val dailyMessageCommentRepository: DailyMessageCommentRepository,
    private val dailyMessageCommentResultReader: DailyMessageCommentResultReader,
    private val blockFilterUseCase: BlockFilterUseCase
) : GetCommentsUseCase {

    override fun execute(dailyMessageId: Long, memberId: Long?, pageable: Pageable): CommentPageResult {
        val id = DailyMessageId.of(dailyMessageId)
        dailyMessageRepository.findById(id)
            ?: throw DailyMessageException.dailyMessageNotFound()

        val hiddenMemberIds = blockFilterUseCase.getHiddenMemberIds(memberId)
        val page = dailyMessageCommentRepository
            .findByDailyMessageIdAndParentCommentIsNull(id, hiddenMemberIds, pageable)
        val comments = dailyMessageCommentResultReader.readAll(page.content, memberId, hiddenMemberIds)
        return CommentPageResult.of(page, comments)
    }
}
