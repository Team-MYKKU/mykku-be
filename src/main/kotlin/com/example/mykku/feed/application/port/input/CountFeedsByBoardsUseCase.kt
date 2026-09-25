package com.example.mykku.feed.application.port.input

interface CountFeedsByBoardsUseCase {
    fun execute(boardIds: List<Long>): Map<Long, Int>
}
