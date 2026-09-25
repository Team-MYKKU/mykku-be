package com.example.mykku.feed.application.usecase

import com.example.mykku.feed.application.port.input.CountFeedsByBoardsUseCase
import com.example.mykku.feed.application.port.output.FeedRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CountFeedsByBoardsUseCaseImpl(
    private val feedRepository: FeedRepository
) : CountFeedsByBoardsUseCase {

    @Transactional(readOnly = true)
    override fun execute(boardIds: List<Long>): Map<Long, Int> {
        return feedRepository.countByBoardIdIn(boardIds)
    }
}
