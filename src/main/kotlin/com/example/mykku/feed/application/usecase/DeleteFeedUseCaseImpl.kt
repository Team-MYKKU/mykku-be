package com.example.mykku.feed.application.usecase

import com.example.mykku.feed.application.dto.DeleteFeedCommand
import com.example.mykku.feed.application.port.input.DeleteFeedUseCase
import com.example.mykku.feed.application.port.output.FeedRepository
import com.example.mykku.feed.domain.entity.Feed
import com.example.mykku.feed.domain.vo.FeedId
import com.example.mykku.feed.exception.FeedException
import com.example.mykku.member.domain.entity.Member
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class DeleteFeedUseCaseImpl(
    private val feedRepository: FeedRepository,
    private val feedCascadeDeleter: FeedCascadeDeleter
) : DeleteFeedUseCase {

    override fun execute(command: DeleteFeedCommand, member: Member) {
        val feed = feedRepository.findByIdOrThrow(FeedId.of(command.feedId))
        validateFeedOwner(feed, member)
        feedCascadeDeleter.deleteFeed(feed)
    }

    private fun validateFeedOwner(feed: Feed, member: Member) {
        if (!feed.isOwnedBy(member.id.value)) {
            throw FeedException.feedForbiddenAccess()
        }
    }
}
