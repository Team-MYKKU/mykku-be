package com.example.mykku.feed.application.port.input

interface DeleteFeedsByBoardUseCase {
    fun execute(boardId: Long): Int
}
