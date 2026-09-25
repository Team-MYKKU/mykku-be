package com.example.mykku.board.application.usecase

import com.example.mykku.board.application.dto.BoardResult
import com.example.mykku.board.application.dto.UpdateBoardCommand
import com.example.mykku.board.application.port.input.UpdateBoardUseCase
import com.example.mykku.board.application.port.output.BoardRepository
import com.example.mykku.board.domain.vo.BoardId
import com.example.mykku.board.exception.BoardException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UpdateBoardUseCaseImpl(
    private val boardRepository: BoardRepository
) : UpdateBoardUseCase {

    @Transactional
    override fun execute(command: UpdateBoardCommand): BoardResult {
        val board = boardRepository.findById(BoardId.of(command.boardId))
            ?: throw BoardException.boardNotFound()
        val previousLogo = board.logo
        board.update(command.title, command.logo)
        val saved = boardRepository.save(board)
        if (command.logo != null) {
            log.info("board logo replaced id={}, previousLogo={}", command.boardId, previousLogo)
        }
        return BoardResult.from(saved)
    }

    companion object {
        private val log = LoggerFactory.getLogger(UpdateBoardUseCaseImpl::class.java)
    }
}
