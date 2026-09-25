package com.example.mykku.board.application.port.input

import com.example.mykku.board.application.dto.DeleteBoardCommand

interface DeleteBoardUseCase {
    fun validate(command: DeleteBoardCommand)
    fun execute(boardId: Long)
}
