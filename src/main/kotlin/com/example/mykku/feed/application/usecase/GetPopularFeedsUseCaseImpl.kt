package com.example.mykku.feed.application.usecase

import com.example.mykku.block.application.port.input.BlockFilterUseCase
import com.example.mykku.board.application.port.output.BoardRepository
import com.example.mykku.board.domain.vo.BoardId
import com.example.mykku.board.exception.BoardException
import com.example.mykku.feed.application.dto.GetPopularFeedsQuery
import com.example.mykku.feed.application.dto.PopularFeedResult
import com.example.mykku.feed.application.dto.PopularFeedsResult
import com.example.mykku.feed.application.port.input.GetPopularFeedsUseCase
import com.example.mykku.feed.application.port.output.FeedRepository
import com.example.mykku.feed.domain.entity.Feed
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GetPopularFeedsUseCaseImpl(
    private val feedRepository: FeedRepository,
    private val boardRepository: BoardRepository,
    private val blockFilterUseCase: BlockFilterUseCase
) : GetPopularFeedsUseCase {

    companion object {
        const val DEFAULT_POPULAR_FEEDS_LIMIT = 3
        const val DEFAULT_POPULAR_FEEDS_DAYS_AGO = 7
    }

    override fun execute(query: GetPopularFeedsQuery): PopularFeedsResult {
        boardRepository.findById(BoardId.of(query.boardId)) ?: throw BoardException.boardNotFound()
        val popularFeeds = feedRepository.findPopularFeedsByBoardId(
            query.boardId,
            DEFAULT_POPULAR_FEEDS_LIMIT,
            DEFAULT_POPULAR_FEEDS_DAYS_AGO
        )

        val filteredFeeds = blockFilterUseCase.filterContent<Feed>(
            items = popularFeeds,
            memberId = query.memberId,
            memberIdExtractor = { it.memberId },
            contentExtractors = listOf({ it.title }, { it.content })
        )

        return PopularFeedsResult(feeds = filteredFeeds.mapIndexed { index, feed -> toRankedResult(feed, index) })
    }

    private fun toRankedResult(feed: Feed, index: Int): PopularFeedResult {
        return PopularFeedResult(
            id = feed.id!!.value,
            rank = index + 1,
            title = feed.title,
            content = feed.content
        )
    }
}
