package com.example.mykku.feed.application.port.input

interface MoveFeedsToBoardUseCase {
    fun execute(fromBoardId: Long, toBoardId: Long): Int
}
