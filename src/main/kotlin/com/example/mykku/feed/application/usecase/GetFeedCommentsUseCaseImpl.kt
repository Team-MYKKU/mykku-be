package com.example.mykku.feed.application.usecase

import com.example.mykku.block.application.port.input.BlockFilterUseCase
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
    private val feedCommentResultReader: FeedCommentResultReader,
    private val blockFilterUseCase: BlockFilterUseCase
) : GetFeedCommentsUseCase {

    override fun execute(query: GetFeedCommentsQuery): CommentPageResult {
        val feed = feedRepository.findByIdOrThrow(FeedId.of(query.feedId))
        val hiddenMemberIds = blockFilterUseCase.getHiddenMemberIds(query.memberId)
        val page = feedCommentRepository.findByFeedIdAndParentCommentIsNull(feed.id!!, hiddenMemberIds, query.pageable)
        val comments = feedCommentResultReader.readAll(page.content, query.memberId, hiddenMemberIds)
        return CommentPageResult.of(page, comments)
    }
}
