package com.example.mykku.feed.application.usecase

import com.example.mykku.block.application.port.input.BlockFilterUseCase
import com.example.mykku.board.application.port.output.BoardRepository
import com.example.mykku.board.domain.vo.BoardId
import com.example.mykku.contest.application.port.output.ContestTagRepository
import com.example.mykku.feed.application.dto.AuthorResult
import com.example.mykku.feed.application.dto.CommentPreviewResult
import com.example.mykku.feed.application.dto.FeedImageResult
import com.example.mykku.feed.application.dto.FeedResult
import com.example.mykku.feed.application.dto.TagResult
import com.example.mykku.feed.application.port.output.FeedCommentRepository
import com.example.mykku.feed.application.port.output.FeedImageRepository
import com.example.mykku.feed.application.port.output.FeedTagRepository
import com.example.mykku.feed.domain.entity.Feed
import com.example.mykku.feed.domain.entity.FeedComment
import com.example.mykku.feed.domain.entity.FeedImage
import com.example.mykku.feed.domain.entity.FeedTag
import com.example.mykku.like.application.port.output.LikeFeedPort
import com.example.mykku.member.application.port.output.MemberRepository
import com.example.mykku.member.domain.entity.Member
import com.example.mykku.member.domain.vo.MemberPk
import com.example.mykku.role.application.dto.RoleResult
import com.example.mykku.role.application.port.output.RoleRepository
import com.example.mykku.role.domain.entity.Role
import com.example.mykku.role.domain.vo.RoleId
import org.springframework.stereotype.Component

@Component
class FeedResultAssembler(
    private val feedImageRepository: FeedImageRepository,
    private val feedTagRepository: FeedTagRepository,
    private val feedCommentRepository: FeedCommentRepository,
    private val boardRepository: BoardRepository,
    private val memberRepository: MemberRepository,
    private val roleRepository: RoleRepository,
    private val contestTagRepository: ContestTagRepository,
    private val likeFeedPort: LikeFeedPort,
    private val blockFilterUseCase: BlockFilterUseCase
) {

    fun assemble(feeds: List<Feed>, viewerId: Long?): List<FeedResult> {
        if (feeds.isEmpty()) return emptyList()

        val context = loadContext(feeds, viewerId)
        return feeds.map { buildFeedResult(it, context) }
    }

    private fun loadContext(feeds: List<Feed>, viewerId: Long?): FeedAssemblyContext {
        val feedIds = feeds.map { it.id!! }
        val feedIdValues = feedIds.map { it.value }
        val tagsByFeedId = feedTagRepository.findByFeedIds(feedIds).groupBy { it.feedId.value }
        val hiddenMemberIds = blockFilterUseCase.getHiddenMemberIds(viewerId)
        val firstComments = feedCommentRepository.findFirstCommentsByFeedIds(feedIds, hiddenMemberIds)
        val authors = loadMembers(feeds.mapNotNull { it.memberId })

        return FeedAssemblyContext(
            imagesByFeedId = feedImageRepository.findByFeedIds(feedIds).groupBy { it.feedId.value },
            tagsByFeedId = tagsByFeedId,
            contestTagTitles = contestTagTitles(tagsByFeedId.values.flatten()),
            authorsById = authors,
            rolesById = loadRoles(authors.values),
            boardTitlesById = loadBoardTitles(feeds.map { it.boardId }),
            firstCommentByFeedId = firstComments,
            commentAuthorsById = loadMembers(firstComments.values.mapNotNull { it.memberId }),
            likeCountByFeedId = likeFeedPort.countByFeedIdIn(feedIdValues),
            commentCountByFeedId = feedCommentRepository.countByFeedIdIn(feedIds, hiddenMemberIds),
            likedFeedIds = loadLikedFeedIds(viewerId, feedIdValues)
        )
    }

    private fun buildFeedResult(feed: Feed, context: FeedAssemblyContext): FeedResult {
        val feedId = feed.id!!.value
        val images = context.imagesByFeedId[feedId] ?: emptyList()
        val tags = context.tagsByFeedId[feedId] ?: emptyList()

        return FeedResult(
            id = feedId,
            author = buildAuthorResult(feed, context),
            board = context.boardTitlesById[feed.boardId] ?: "",
            createdAt = feed.createdAt,
            title = feed.title,
            content = feed.content,
            images = images.map { FeedImageResult(it.id!!.value, it.url, it.width, it.height) },
            tags = tags.map { TagResult(it.title, it.title in context.contestTagTitles) },
            likeCount = context.likeCountByFeedId[feedId] ?: 0,
            isLiked = feedId in context.likedFeedIds,
            commentCount = context.commentCountByFeedId[feedId] ?: 0,
            comment = buildCommentPreview(feedId, context)
        )
    }

    private fun buildCommentPreview(feedId: Long, context: FeedAssemblyContext): CommentPreviewResult {
        val firstComment = context.firstCommentByFeedId[feedId]
        val author = firstComment?.memberId?.let { context.commentAuthorsById[it] }
        return CommentPreviewResult(
            profileImage = author?.profileImage,
            content = firstComment?.content ?: ""
        )
    }

    private fun buildAuthorResult(feed: Feed, context: FeedAssemblyContext): AuthorResult? {
        val member = feed.memberId?.let { context.authorsById[it] } ?: return null
        val role = member.roleId?.let { context.rolesById[it] }
        return AuthorResult(
            memberId = member.memberId,
            nickname = member.nickname,
            profileImage = member.profileImage,
            role = role?.let { RoleResult(it.id.value, it.name, it.description) }
        )
    }

    private fun loadMembers(memberIds: List<Long>): Map<Long, Member> {
        val memberPks = memberIds.distinct().map { MemberPk.of(it) }
        if (memberPks.isEmpty()) return emptyMap()
        return memberRepository.findByIds(memberPks).associateBy { it.id.value }
    }

    private fun loadRoles(members: Collection<Member>): Map<Long, Role> {
        val roleIds = members.mapNotNull { it.roleId }.distinct().map { RoleId.of(it) }
        if (roleIds.isEmpty()) return emptyMap()
        return roleRepository.findByIds(roleIds).associateBy { it.id.value }
    }

    private fun loadBoardTitles(boardIds: List<Long>): Map<Long, String> {
        val ids = boardIds.distinct().map { BoardId(it) }
        if (ids.isEmpty()) return emptyMap()
        return boardRepository.findByIds(ids).associate { it.id.value to it.title }
    }

    private fun contestTagTitles(feedTags: List<FeedTag>): Set<String> {
        val titles = feedTags.map { it.title }.distinct()
        if (titles.isEmpty()) return emptySet()
        return contestTagRepository.findAllByTitleIn(titles).map { it.title }.toSet()
    }

    private fun loadLikedFeedIds(viewerId: Long?, feedIds: List<Long>): Set<Long> {
        if (viewerId == null) return emptySet()
        return likeFeedPort.findByMemberIdAndFeedIdIn(viewerId, feedIds).map { it.feedId }.toSet()
    }

    private data class FeedAssemblyContext(
        val imagesByFeedId: Map<Long, List<FeedImage>>,
        val tagsByFeedId: Map<Long, List<FeedTag>>,
        val contestTagTitles: Set<String>,
        val authorsById: Map<Long, Member>,
        val rolesById: Map<Long, Role>,
        val boardTitlesById: Map<Long, String>,
        val firstCommentByFeedId: Map<Long, FeedComment>,
        val commentAuthorsById: Map<Long, Member>,
        val likeCountByFeedId: Map<Long, Int>,
        val commentCountByFeedId: Map<Long, Int>,
        val likedFeedIds: Set<Long>
    )
}
