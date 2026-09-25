package com.example.mykku.feed.application.usecase

import com.example.mykku.board.application.port.output.BoardRepository
import com.example.mykku.board.domain.entity.Board
import com.example.mykku.board.domain.vo.BoardId
import com.example.mykku.feed.application.dto.BoardMatchSummary
import com.example.mykku.feed.application.dto.BoardSearchGroupResult
import com.example.mykku.feed.application.dto.FeedSearchCondition
import com.example.mykku.feed.application.dto.FeedSearchItemResult
import com.example.mykku.feed.application.dto.FeedSearchResult
import com.example.mykku.feed.application.dto.SearchFeedsQuery
import com.example.mykku.feed.application.port.input.SearchFeedsUseCase
import com.example.mykku.feed.application.port.output.FeedSearchRepository
import com.example.mykku.feed.domain.vo.SearchKeyword
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class SearchFeedsUseCaseImpl(
    private val feedSearchRepository: FeedSearchRepository,
    private val boardRepository: BoardRepository,
    private val feedSearchItemAssembler: FeedSearchItemAssembler
) : SearchFeedsUseCase {

    override fun execute(query: SearchFeedsQuery): FeedSearchResult {
        val condition = FeedSearchCondition(SearchKeyword.of(query.keyword), query.memberId)
        val summaries = feedSearchRepository.summarizeByBoard(condition)
        if (summaries.isEmpty()) return FeedSearchResult(emptyList())

        val boards = loadBoards(summaries)
        val ordered = summaries.filter { it.boardId in boards }.sortedWith(GROUP_ORDER)
        val previews = ordered.flatMap { feedSearchRepository.findTopByBoard(condition, it.boardId, PREVIEW_SIZE) }
        val itemsByBoard = feedSearchItemAssembler.assemble(previews, boards).groupBy { it.boardId }
        return FeedSearchResult(ordered.map { toGroup(it, boards.getValue(it.boardId), itemsByBoard) })
    }

    private fun loadBoards(summaries: List<BoardMatchSummary>): Map<Long, Board> =
        boardRepository.findByIds(summaries.map { BoardId(it.boardId) }).associateBy { it.id.value }

    private fun toGroup(
        summary: BoardMatchSummary,
        board: Board,
        itemsByBoard: Map<Long, List<FeedSearchItemResult>>
    ): BoardSearchGroupResult = BoardSearchGroupResult.of(summary, board, itemsByBoard[summary.boardId].orEmpty())

    companion object {
        const val PREVIEW_SIZE = 3

        private val GROUP_ORDER = compareByDescending<BoardMatchSummary> { it.count }
            .thenByDescending { it.latestCreatedAt }
            .thenBy { it.boardId }
    }
}
