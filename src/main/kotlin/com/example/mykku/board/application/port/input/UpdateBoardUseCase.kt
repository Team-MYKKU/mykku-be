package com.example.mykku.board.application.port.input

import com.example.mykku.board.application.dto.BoardResult
import com.example.mykku.board.application.dto.UpdateBoardCommand

interface UpdateBoardUseCase {
    fun execute(command: UpdateBoardCommand): BoardResult
}
