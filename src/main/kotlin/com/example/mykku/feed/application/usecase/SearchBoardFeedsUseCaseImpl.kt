package com.example.mykku.feed.application.usecase

import com.example.mykku.board.application.port.output.BoardRepository
import com.example.mykku.board.domain.entity.Board
import com.example.mykku.board.domain.vo.BoardId
import com.example.mykku.board.exception.BoardException
import com.example.mykku.feed.application.dto.FeedSearchCondition
import com.example.mykku.feed.application.dto.PagedFeedSearchResult
import com.example.mykku.feed.application.dto.SearchBoardFeedsQuery
import com.example.mykku.feed.application.port.input.SearchBoardFeedsUseCase
import com.example.mykku.feed.application.port.output.FeedSearchRepository
import com.example.mykku.feed.domain.vo.SearchKeyword
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class SearchBoardFeedsUseCaseImpl(
    private val feedSearchRepository: FeedSearchRepository,
    private val boardRepository: BoardRepository,
    private val feedSearchItemAssembler: FeedSearchItemAssembler
) : SearchBoardFeedsUseCase {

    override fun execute(query: SearchBoardFeedsQuery): PagedFeedSearchResult {
        val condition = FeedSearchCondition(SearchKeyword.of(query.keyword), query.memberId)
        val board = findBoard(query.boardId)
        val page = feedSearchRepository.findByBoard(condition, query.boardId, query.pageable)
        val feeds = feedSearchItemAssembler.assemble(page.content, mapOf(board.id.value to board))
        return PagedFeedSearchResult.of(page, feeds)
    }

    private fun findBoard(boardId: Long): Board =
        boardRepository.findById(BoardId(boardId)) ?: throw BoardException.boardNotFound()
}
