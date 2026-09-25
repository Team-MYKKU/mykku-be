package com.example.mykku.feed.application.usecase

import com.example.mykku.feed.application.port.input.MoveFeedsToBoardUseCase
import com.example.mykku.feed.application.port.output.FeedRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MoveFeedsToBoardUseCaseImpl(
    private val feedRepository: FeedRepository
) : MoveFeedsToBoardUseCase {

    @Transactional
    override fun execute(fromBoardId: Long, toBoardId: Long): Int {
        val moved = feedRepository.moveAllToBoard(fromBoardId, toBoardId)
        log.info("admin move feeds fromBoardId={}, toBoardId={}, feeds={}", fromBoardId, toBoardId, moved)
        return moved
    }

    companion object {
        private val log = LoggerFactory.getLogger(MoveFeedsToBoardUseCaseImpl::class.java)
    }
}
