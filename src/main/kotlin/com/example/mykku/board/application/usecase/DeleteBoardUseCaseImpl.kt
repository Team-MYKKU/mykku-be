package com.example.mykku.board.application.usecase

import com.example.mykku.board.application.dto.DeleteBoardCommand
import com.example.mykku.board.application.port.input.DeleteBoardUseCase
import com.example.mykku.board.application.port.output.BoardRepository
import com.example.mykku.board.domain.entity.Board
import com.example.mykku.board.domain.vo.BoardFeedAction
import com.example.mykku.board.domain.vo.BoardId
import com.example.mykku.board.exception.BoardException
import com.example.mykku.image.ImageCleanupPort
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DeleteBoardUseCaseImpl(
    private val boardRepository: BoardRepository,
    private val imageCleanupPort: ImageCleanupPort
) : DeleteBoardUseCase {

    @Transactional(readOnly = true)
    override fun validate(command: DeleteBoardCommand) {
        findBoard(command.boardId)
        if (command.feedCount == 0) return
        when (command.feedAction) {
            null -> throw BoardException.boardHasFeeds()
            BoardFeedAction.MOVE -> validateMoveTarget(command.boardId, command.targetBoardId)
            BoardFeedAction.DELETE -> Unit
        }
    }

    @Transactional
    override fun execute(boardId: Long) {
        val board = findBoard(boardId)
        log.info("admin delete board id={}, title={}, logo={}", boardId, board.title, board.logo)
        boardRepository.delete(board)
        imageCleanupPort.deleteAfterCommit(listOf(board.logo))
    }

    private fun findBoard(boardId: Long): Board {
        return boardRepository.findById(BoardId.of(boardId))
            ?: throw BoardException.boardNotFound()
    }

    private fun validateMoveTarget(boardId: Long, targetBoardId: Long?) {
        if (targetBoardId == null) throw BoardException.moveTargetBoardNotFound()
        if (targetBoardId == boardId) throw BoardException.moveTargetSameBoard()
        if (!boardRepository.existsById(BoardId.of(targetBoardId))) throw BoardException.moveTargetBoardNotFound()
    }

    companion object {
        private val log = LoggerFactory.getLogger(DeleteBoardUseCaseImpl::class.java)
    }
}
