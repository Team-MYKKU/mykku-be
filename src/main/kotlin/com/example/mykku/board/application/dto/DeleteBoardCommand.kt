package com.example.mykku.board.application.dto

import com.example.mykku.board.domain.vo.BoardFeedAction

data class DeleteBoardCommand(
    val boardId: Long,
    val feedCount: Int,
    val feedAction: BoardFeedAction?,
    val targetBoardId: Long?
)
