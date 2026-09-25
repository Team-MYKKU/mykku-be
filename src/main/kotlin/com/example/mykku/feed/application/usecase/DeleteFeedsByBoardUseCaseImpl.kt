package com.example.mykku.feed.application.usecase

import com.example.mykku.feed.application.port.input.DeleteFeedsByBoardUseCase
import com.example.mykku.feed.application.port.output.FeedRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DeleteFeedsByBoardUseCaseImpl(
    private val feedRepository: FeedRepository,
    private val feedCascadeDeleter: FeedCascadeDeleter
) : DeleteFeedsByBoardUseCase {

    @Transactional
    override fun execute(boardId: Long): Int {
        val feeds = feedRepository.findAllByBoardId(boardId)
        log.info("admin delete feeds boardId={}, feeds={}", boardId, feeds.size)
        feeds.forEach { feedCascadeDeleter.deleteFeed(it) }
        return feeds.size
    }

    companion object {
        private val log = LoggerFactory.getLogger(DeleteFeedsByBoardUseCaseImpl::class.java)
    }
}
