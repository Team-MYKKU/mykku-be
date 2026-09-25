package com.example.mykku.feed.application.usecase

import com.example.mykku.feed.application.port.input.AdminDeleteFeedUseCase
import com.example.mykku.feed.application.port.output.FeedRepository
import com.example.mykku.feed.domain.vo.FeedId
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminDeleteFeedUseCaseImpl(
    private val feedRepository: FeedRepository,
    private val feedCascadeDeleter: FeedCascadeDeleter
) : AdminDeleteFeedUseCase {

    @Transactional
    override fun execute(feedId: Long) {
        val feed = feedRepository.findByIdOrThrow(FeedId.of(feedId))
        log.info("admin delete feed id={}, boardId={}, memberId={}", feedId, feed.boardId, feed.memberId)
        feedCascadeDeleter.deleteFeed(feed)
    }

    companion object {
        private val log = LoggerFactory.getLogger(AdminDeleteFeedUseCaseImpl::class.java)
    }
}
