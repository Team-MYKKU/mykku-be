package com.example.mykku.feed.application.usecase

import com.example.mykku.feed.application.port.input.AdminDeleteFeedCommentUseCase
import com.example.mykku.feed.application.port.output.FeedCommentRepository
import com.example.mykku.feed.domain.vo.FeedCommentId
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminDeleteFeedCommentUseCaseImpl(
    private val feedCommentRepository: FeedCommentRepository,
    private val feedCascadeDeleter: FeedCascadeDeleter
) : AdminDeleteFeedCommentUseCase {

    @Transactional
    override fun execute(commentId: Long) {
        val comment = feedCommentRepository.findByIdOrThrow(FeedCommentId.of(commentId))
        log.info(
            "admin delete feed comment id={}, feedId={}, memberId={}",
            commentId,
            comment.feedId.value,
            comment.memberId
        )
        feedCascadeDeleter.deleteComment(comment)
    }

    companion object {
        private val log = LoggerFactory.getLogger(AdminDeleteFeedCommentUseCaseImpl::class.java)
    }
}
