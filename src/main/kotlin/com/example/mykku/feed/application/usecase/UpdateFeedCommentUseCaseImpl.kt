package com.example.mykku.feed.application.usecase

import com.example.mykku.comment.application.dto.CommentResult
import com.example.mykku.feed.application.dto.UpdateFeedCommentCommand
import com.example.mykku.feed.application.port.input.UpdateFeedCommentUseCase
import com.example.mykku.feed.application.port.output.FeedCommentRepository
import com.example.mykku.feed.domain.vo.FeedCommentId
import com.example.mykku.feed.exception.FeedException
import com.example.mykku.member.domain.entity.Member
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class UpdateFeedCommentUseCaseImpl(
    private val feedCommentRepository: FeedCommentRepository,
    private val feedCommentResultReader: FeedCommentResultReader
) : UpdateFeedCommentUseCase {

    override fun execute(command: UpdateFeedCommentCommand, member: Member): CommentResult {
        val comment = feedCommentRepository.findByIdOrThrow(FeedCommentId.of(command.commentId))

        if (!comment.isOwnedBy(member.id.value)) {
            throw FeedException.feedCommentForbiddenAccess()
        }

        val updated = comment.updateContent(command.content)
        feedCommentRepository.save(updated, comment.feedId, member.id.value)
        return feedCommentResultReader.readOne(updated, member.id.value)
    }
}
