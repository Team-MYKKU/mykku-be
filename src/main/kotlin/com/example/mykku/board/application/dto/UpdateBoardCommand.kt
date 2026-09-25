package com.example.mykku.board.application.dto

data class UpdateBoardCommand(
    val boardId: Long,
    val title: String,
    val logo: String?
)
