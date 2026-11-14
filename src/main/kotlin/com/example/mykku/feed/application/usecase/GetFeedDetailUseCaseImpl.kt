package com.example.mykku.feed.application.usecase

import com.example.mykku.block.application.port.input.BlockFilterUseCase
import com.example.mykku.board.application.port.output.BoardRepository
import com.example.mykku.board.domain.vo.BoardId
import com.example.mykku.board.exception.BoardException
import com.example.mykku.contest.application.port.output.ContestTagRepository
import com.example.mykku.feed.application.dto.AuthorResult
import com.example.mykku.feed.application.dto.FeedDetailResult
import com.example.mykku.feed.application.dto.FeedImageResult
import com.example.mykku.feed.application.dto.GetFeedDetailQuery
import com.example.mykku.feed.application.dto.TagResult
import com.example.mykku.role.application.dto.RoleResult
import com.example.mykku.feed.application.port.input.GetFeedDetailUseCase
import com.example.mykku.feed.application.port.output.FeedCommentRepository
import com.example.mykku.feed.application.port.output.FeedImageRepository
import com.example.mykku.feed.application.port.output.FeedRepository
import com.example.mykku.feed.application.port.output.FeedTagRepository
import com.example.mykku.feed.domain.vo.FeedId
import com.example.mykku.like.application.port.output.LikeFeedPort
import com.example.mykku.member.application.port.output.MemberRepository
import com.example.mykku.member.domain.vo.MemberPk
import com.example.mykku.role.application.port.output.RoleRepository
import com.example.mykku.role.domain.vo.RoleId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GetFeedDetailUseCaseImpl(
    private val feedRepository: FeedRepository,
    private val feedImageRepository: FeedImageRepository,
    private val feedTagRepository: FeedTagRepository,
    private val feedCommentRepository: FeedCommentRepository,
    private val boardRepository: BoardRepository,
    private val memberRepository: MemberRepository,
    private val roleRepository: RoleRepository,
    private val contestTagRepository: ContestTagRepository,
    private val likeFeedPort: LikeFeedPort,
    private val blockFilterUseCase: BlockFilterUseCase
) : GetFeedDetailUseCase {

    override fun execute(query: GetFeedDetailQuery): FeedDetailResult {
        val feed = feedRepository.findByIdOrThrow(FeedId.of(query.feedId))

        val feedImages = feedImageRepository.findByFeedId(feed.id!!)
        val feedTags = feedTagRepository.findByFeedId(feed.id!!)

        val board = boardRepository.findById(BoardId(feed.boardId))
            ?: throw BoardException.boardNotFound()
        val member = feed.memberId?.let { memberRepository.findById(MemberPk.of(it)) }
        val authorResult = member?.let {
            val role = it.roleId?.let { roleId -> roleRepository.findById(RoleId.of(roleId)) }
            val roleResult = role?.let { r -> RoleResult(r.id.value, r.name, r.description) }
            AuthorResult(
                memberId = it.memberId,
                nickname = it.nickname,
                profileImage = it.profileImage,
                role = roleResult
            )
        }

        val contestTagTitles = getContestTagTitles(feedTags.map { it.title })

        val isLiked = query.memberId?.let { likeFeedPort.existsByMemberIdAndFeedId(it, feed.id!!.value) } ?: false

        return FeedDetailResult(
            id = feed.id!!.value,
            author = authorResult,
            boardId = feed.boardId,
            boardTitle = board.title,
            createdAt = feed.createdAt,
            updatedAt = feed.updatedAt,
            title = feed.title,
            content = feed.content,
            images = feedImages.map { FeedImageResult(it.id!!.value, it.url, it.width, it.height) },
            tags = feedTags.map { TagResult(it.title, contestTagTitles.contains(it.title)) },
            likeCount = likeFeedPort.countByFeedId(feed.id!!.value),
            isLiked = isLiked,
            commentCount = countVisibleComments(feed.id!!, query.memberId)
        )
    }

    private fun getContestTagTitles(tagTitles: List<String>): Set<String> {
        val contestTags = contestTagRepository.findAllByTitleIn(tagTitles)
        return contestTags.map { it.title }.toSet()
    }

    private fun countVisibleComments(feedId: FeedId, viewerId: Long?): Int =
        feedCommentRepository.countByFeedId(feedId, blockFilterUseCase.getHiddenMemberIds(viewerId)).toInt()
}
