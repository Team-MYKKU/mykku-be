package com.example.mykku.feed.application.usecase

import com.example.mykku.block.application.port.input.BlockFilterUseCase
import com.example.mykku.board.application.port.output.BoardRepository
import com.example.mykku.board.domain.vo.BoardId
import com.example.mykku.board.exception.BoardException
import com.example.mykku.feed.application.dto.FeedResult
import com.example.mykku.feed.application.dto.ListFeedsQuery
import com.example.mykku.feed.application.dto.PagedFeedsResult
import com.example.mykku.feed.application.port.input.ListFeedsUseCase
import com.example.mykku.feed.application.port.output.FeedRepository
import com.example.mykku.feed.domain.entity.Feed
import org.springframework.data.domain.Page
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class ListFeedsUseCaseImpl(
    private val feedRepository: FeedRepository,
    private val boardRepository: BoardRepository,
    private val feedResultAssembler: FeedResultAssembler,
    private val blockFilterUseCase: BlockFilterUseCase
) : ListFeedsUseCase {

    override fun execute(query: ListFeedsQuery): PagedFeedsResult {
        boardRepository.findById(BoardId.of(query.boardId)) ?: throw BoardException.boardNotFound()
        val feedPage = feedRepository.findByBoardId(query.boardId, query.pageable)

        val filteredFeeds = blockFilterUseCase.filterContent(
            items = feedPage.content,
            memberId = query.memberId,
            memberIdExtractor = { it.memberId },
            contentExtractors = listOf({ it.title }, { it.content })
        )

        return toPagedResult(feedPage, feedResultAssembler.assemble(filteredFeeds, query.memberId))
    }

    private fun toPagedResult(feedPage: Page<Feed>, feeds: List<FeedResult>): PagedFeedsResult {
        return PagedFeedsResult(
            feeds = feeds,
            currentPage = feedPage.number,
            totalPages = feedPage.totalPages,
            totalElements = feedPage.totalElements,
            size = feedPage.size,
            hasNext = feedPage.hasNext(),
            hasPrevious = feedPage.hasPrevious()
        )
    }
}
