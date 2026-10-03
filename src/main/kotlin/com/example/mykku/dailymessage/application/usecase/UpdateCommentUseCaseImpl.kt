package com.example.mykku.dailymessage.application.usecase

import com.example.mykku.comment.application.dto.CommentResult
import com.example.mykku.dailymessage.application.dto.UpdateCommentCommand
import com.example.mykku.dailymessage.application.port.input.UpdateCommentUseCase
import com.example.mykku.dailymessage.application.port.output.DailyMessageCommentRepository
import com.example.mykku.dailymessage.domain.vo.DailyMessageCommentId
import com.example.mykku.dailymessage.exception.DailyMessageException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class UpdateCommentUseCaseImpl(
    private val dailyMessageCommentRepository: DailyMessageCommentRepository,
    private val dailyMessageCommentResultReader: DailyMessageCommentResultReader
) : UpdateCommentUseCase {

    override fun execute(command: UpdateCommentCommand): CommentResult {
        val comment = dailyMessageCommentRepository.findById(DailyMessageCommentId.of(command.commentId))
            ?: throw DailyMessageException.dailyMessageCommentNotFound()

        if (!comment.isOwnedBy(command.memberId)) {
            throw DailyMessageException.commentForbiddenAccess()
        }

        val updated = comment.updateContent(command.content)
        dailyMessageCommentRepository.save(updated)
        return dailyMessageCommentResultReader.readOne(updated, command.memberId)
    }
}
