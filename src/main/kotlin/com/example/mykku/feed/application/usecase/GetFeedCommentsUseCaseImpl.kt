package com.example.mykku.feed.application.usecase

import com.example.mykku.comment.application.dto.CommentPageResult
import com.example.mykku.feed.application.dto.GetFeedCommentsQuery
import com.example.mykku.feed.application.port.input.GetFeedCommentsUseCase
import com.example.mykku.feed.application.port.output.FeedCommentRepository
import com.example.mykku.feed.application.port.output.FeedRepository
import com.example.mykku.feed.domain.vo.FeedId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GetFeedCommentsUseCaseImpl(
    private val feedRepository: FeedRepository,
    private val feedCommentRepository: FeedCommentRepository,
    private val feedCommentResultReader: FeedCommentResultReader
) : GetFeedCommentsUseCase {

    override fun execute(query: GetFeedCommentsQuery): CommentPageResult {
        val feed = feedRepository.findByIdOrThrow(FeedId.of(query.feedId))
        val page = feedCommentRepository.findByFeedIdAndParentCommentIsNull(feed.id!!, query.pageable)
        return CommentPageResult.of(page, feedCommentResultReader.readAll(page.content, query.memberId))
    }
}
