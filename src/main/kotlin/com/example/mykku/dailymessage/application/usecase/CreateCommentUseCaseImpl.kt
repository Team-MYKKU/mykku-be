package com.example.mykku.dailymessage.application.usecase

import com.example.mykku.achievement.application.event.ActivityEvent
import com.example.mykku.achievement.application.port.output.ActivityEventPublisher
import com.example.mykku.achievement.domain.vo.ActivityType
import com.example.mykku.comment.application.dto.CommentResult
import com.example.mykku.dailymessage.application.dto.CreateCommentCommand
import com.example.mykku.dailymessage.application.port.input.CreateCommentUseCase
import com.example.mykku.dailymessage.application.port.output.DailyMessageCommentRepository
import com.example.mykku.dailymessage.application.port.output.DailyMessageRepository
import com.example.mykku.dailymessage.domain.entity.DailyMessageComment
import com.example.mykku.dailymessage.domain.vo.DailyMessageCommentId
import com.example.mykku.dailymessage.domain.vo.DailyMessageId
import com.example.mykku.dailymessage.exception.DailyMessageException
import com.example.mykku.member.application.port.output.MemberRepository
import com.example.mykku.member.domain.vo.MemberPk
import com.example.mykku.member.exception.MemberException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class CreateCommentUseCaseImpl(
    private val dailyMessageRepository: DailyMessageRepository,
    private val dailyMessageCommentRepository: DailyMessageCommentRepository,
    private val activityEventPublisher: ActivityEventPublisher,
    private val dailyMessageCommentResultReader: DailyMessageCommentResultReader,
    private val memberRepository: MemberRepository
) : CreateCommentUseCase {

    override fun execute(command: CreateCommentCommand): CommentResult {
        validateProfileCompleted(command.memberId)
        validateDailyMessageExists(command.dailyMessageId)
        validateParentCommentExists(command)

        val savedComment = dailyMessageCommentRepository.save(newComment(command))

        activityEventPublisher.publish(ActivityEvent(command.memberId, ActivityType.COMMENT_CREATE))

        return dailyMessageCommentResultReader.readOne(savedComment, command.memberId)
    }

    private fun validateProfileCompleted(memberId: Long) {
        val member = memberRepository.findById(MemberPk.of(memberId))
            ?: throw MemberException.memberNotFound()
        member.requireProfileCompleted()
    }

    private fun validateDailyMessageExists(dailyMessageId: Long) {
        dailyMessageRepository.findById(DailyMessageId.of(dailyMessageId))
            ?: throw DailyMessageException.dailyMessageNotFound()
    }

    private fun validateParentCommentExists(command: CreateCommentCommand) {
        val parentCommentId = command.parentCommentId ?: return

        val parentComment = dailyMessageCommentRepository.findByIdAndDailyMessageId(
            DailyMessageCommentId.of(parentCommentId),
            DailyMessageId.of(command.dailyMessageId)
        ) ?: throw DailyMessageException.dailyMessageCommentNotFound()

        if (parentComment.parentCommentId != null) {
            throw DailyMessageException.replyDepthExceeded()
        }
    }

    private fun newComment(command: CreateCommentCommand): DailyMessageComment {
        return DailyMessageComment.create(
            dailyMessageId = command.dailyMessageId,
            memberId = command.memberId,
            content = command.content,
            parentCommentId = command.parentCommentId
        )
    }
}
