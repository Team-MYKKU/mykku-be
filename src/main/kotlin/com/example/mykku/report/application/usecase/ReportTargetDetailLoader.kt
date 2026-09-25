package com.example.mykku.report.application.usecase

import com.example.mykku.board.application.port.output.BoardRepository
import com.example.mykku.board.domain.vo.BoardId
import com.example.mykku.contest.application.port.output.ContestParticipationRepository
import com.example.mykku.contest.application.port.output.ContestRepository
import com.example.mykku.contest.application.port.output.ContestWinnerRepository
import com.example.mykku.feed.application.port.output.FeedCommentRepository
import com.example.mykku.feed.application.port.output.FeedImageRepository
import com.example.mykku.feed.application.port.output.FeedRepository
import com.example.mykku.feed.domain.vo.FeedCommentId
import com.example.mykku.feed.domain.vo.FeedId
import com.example.mykku.report.application.dto.ReportedCommentResult
import com.example.mykku.report.application.dto.ReportedContestResult
import com.example.mykku.report.application.dto.ReportedFeedResult
import org.springframework.stereotype.Component

@Component
class ReportTargetDetailLoader(
    private val feedRepository: FeedRepository,
    private val feedImageRepository: FeedImageRepository,
    private val feedCommentRepository: FeedCommentRepository,
    private val boardRepository: BoardRepository,
    private val contestParticipationRepository: ContestParticipationRepository,
    private val contestRepository: ContestRepository,
    private val contestWinnerRepository: ContestWinnerRepository
) {

    fun loadFeed(feedId: Long): ReportedFeedResult? {
        val feed = feedRepository.findById(FeedId.of(feedId)) ?: return null
        return ReportedFeedResult(
            feedId = feedId,
            title = feed.title,
            content = feed.content,
            imageUrls = feedImageRepository.findByFeedId(FeedId.of(feedId)).sortedBy { it.id?.value }.map { it.url },
            boardTitle = boardRepository.findById(BoardId.of(feed.boardId))?.title,
            contests = loadContests(feedId)
        )
    }

    fun loadComment(commentId: Long): ReportedCommentResult? {
        val comment = feedCommentRepository.findById(FeedCommentId.of(commentId)) ?: return null
        return ReportedCommentResult(
            commentId = commentId,
            content = comment.content,
            feedId = comment.feedId.value,
            feedTitle = feedRepository.findById(comment.feedId)?.title
        )
    }

    private fun loadContests(feedId: Long): List<ReportedContestResult> {
        val participations = contestParticipationRepository.findByFeedId(feedId)
        if (participations.isEmpty()) return emptyList()
        val contestIds = participations.map { it.contestId }.distinct()
        val titles = contestRepository.findAllByIds(contestIds).associate { it.id.value to it.title }
        val ranks = contestWinnerRepository.findByContestIds(contestIds)
            .associate { it.participationId.value to it.winnerRank }
        return participations.map {
            ReportedContestResult(it.contestId.value, titles[it.contestId.value] ?: "", ranks[it.id.value])
        }
    }
}
