package com.example.mykku.feed.application.usecase

import com.example.mykku.contest.application.port.output.ContestParticipationRepository
import com.example.mykku.contest.application.port.output.ContestWinnerRepository
import com.example.mykku.feed.application.port.output.FeedCommentRepository
import com.example.mykku.feed.application.port.output.FeedImageRepository
import com.example.mykku.feed.application.port.output.FeedRepository
import com.example.mykku.feed.application.port.output.FeedTagRepository
import com.example.mykku.feed.domain.entity.Feed
import com.example.mykku.feed.domain.entity.FeedComment
import com.example.mykku.feed.domain.vo.FeedCommentId
import com.example.mykku.feed.domain.vo.FeedId
import com.example.mykku.image.ImageCleanupPort
import com.example.mykku.like.application.port.output.LikeFeedCommentPort
import com.example.mykku.like.application.port.output.LikeFeedPort
import org.springframework.stereotype.Component

@Component
class FeedCascadeDeleter(
    private val feedRepository: FeedRepository,
    private val feedImageRepository: FeedImageRepository,
    private val feedTagRepository: FeedTagRepository,
    private val feedCommentRepository: FeedCommentRepository,
    private val likeFeedPort: LikeFeedPort,
    private val likeFeedCommentPort: LikeFeedCommentPort,
    private val contestParticipationRepository: ContestParticipationRepository,
    private val contestWinnerRepository: ContestWinnerRepository,
    private val imageCleanupPort: ImageCleanupPort
) {

    fun deleteFeed(feed: Feed) {
        val feedId = feed.id!!
        val imageUrls = feedImageRepository.findByFeedId(feedId).map { it.url }
        deleteComments(feedId)
        likeFeedPort.deleteAllByFeedId(feedId.value)
        deleteContestRecords(feedId)
        feedTagRepository.deleteAllByFeedId(feedId)
        feedImageRepository.deleteAllByFeedId(feedId)
        feedRepository.delete(feed)
        imageCleanupPort.deleteAfterCommit(imageUrls)
    }

    fun deleteComment(comment: FeedComment) {
        val deepestFirst = collectDescendants(comment.id!!).reversed() + comment
        likeFeedCommentPort.deleteAllByFeedCommentIdIn(deepestFirst.map { it.id!!.value })
        deepestFirst.forEach { feedCommentRepository.delete(it) }
    }

    private fun deleteComments(feedId: FeedId) {
        likeFeedCommentPort.deleteAllByFeedCommentIdIn(feedCommentRepository.findIdsByFeedId(feedId))
        feedCommentRepository.deleteAllByFeedId(feedId)
    }

    private fun deleteContestRecords(feedId: FeedId) {
        val participations = contestParticipationRepository.findByFeedId(feedId.value)
        contestWinnerRepository.deleteAllByParticipationIds(participations.map { it.id })
        contestParticipationRepository.deleteAll(participations)
    }

    private fun collectDescendants(commentId: FeedCommentId): List<FeedComment> {
        val descendants = mutableListOf<FeedComment>()
        var parentIds = listOf(commentId)
        while (parentIds.isNotEmpty()) {
            val children = feedCommentRepository.findByParentCommentIds(parentIds)
            descendants += children
            parentIds = children.map { it.id!! }
        }
        return descendants
    }
}
