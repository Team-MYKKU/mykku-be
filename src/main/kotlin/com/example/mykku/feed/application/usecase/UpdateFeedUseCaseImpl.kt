package com.example.mykku.feed.application.usecase

import com.example.mykku.block.application.port.input.BlockFilterUseCase
import com.example.mykku.board.application.port.output.BoardRepository
import com.example.mykku.board.domain.vo.BoardId
import com.example.mykku.board.exception.BoardException
import com.example.mykku.contest.application.port.output.ContestTagRepository
import com.example.mykku.feed.application.dto.AuthorResult
import com.example.mykku.feed.application.dto.FeedDetailResult
import com.example.mykku.feed.application.dto.FeedImageResult
import com.example.mykku.feed.application.dto.TagResult
import com.example.mykku.feed.application.dto.UpdateFeedCommand
import com.example.mykku.role.application.dto.RoleResult
import com.example.mykku.feed.application.port.input.UpdateFeedUseCase
import com.example.mykku.feed.application.port.output.FeedCommentRepository
import com.example.mykku.feed.application.port.output.FeedImageRepository
import com.example.mykku.feed.application.port.output.FeedRepository
import com.example.mykku.feed.application.port.output.FeedTagRepository
import com.example.mykku.feed.domain.entity.Feed
import com.example.mykku.feed.domain.entity.FeedImage
import com.example.mykku.feed.domain.entity.FeedTag
import com.example.mykku.feed.domain.vo.FeedId
import com.example.mykku.feed.exception.FeedException
import com.example.mykku.image.ImageUploadService
import com.example.mykku.image.dto.ImageUploadResult
import com.example.mykku.like.application.port.output.LikeFeedPort
import com.example.mykku.member.domain.entity.Member
import com.example.mykku.role.application.port.output.RoleRepository
import com.example.mykku.role.domain.vo.RoleId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class UpdateFeedUseCaseImpl(
    private val feedRepository: FeedRepository,
    private val feedImageRepository: FeedImageRepository,
    private val feedTagRepository: FeedTagRepository,
    private val feedCommentRepository: FeedCommentRepository,
    private val boardRepository: BoardRepository,
    private val roleRepository: RoleRepository,
    private val contestTagRepository: ContestTagRepository,
    private val imageUploadService: ImageUploadService,
    private val likeFeedPort: LikeFeedPort,
    private val blockFilterUseCase: BlockFilterUseCase
) : UpdateFeedUseCase {

    override fun execute(command: UpdateFeedCommand, member: Member): FeedDetailResult {
        val feed = feedRepository.findByIdOrThrow(FeedId.of(command.feedId))
        validateFeedOwner(feed, member)
        validateFinalImageCount(feed, command)

        val newBoardId = command.boardId
        val updatedFeed = feed.update(command.title, command.content, newBoardId)

        deleteImages(command.deleteImageIds, feed.id!!)
        val newImageResults = uploadImages(command.newImages)
        addNewImages(newImageResults, feed.id!!)
        val updatedTags = updateTags(command.tags, feed.id!!)

        val savedFeed = feedRepository.update(updatedFeed)
        val remainingImages = feedImageRepository.findByFeedId(savedFeed.id!!)

        val board = boardRepository.findById(BoardId(savedFeed.boardId))
            ?: throw BoardException.boardNotFound()
        val role = member.roleId?.let { roleRepository.findById(RoleId.of(it)) }
        val roleResult = role?.let { RoleResult(it.id.value, it.name, it.description) }

        val contestTagTitles = getContestTagTitles(updatedTags.map { it.title })
        val isLiked = likeFeedPort.existsByMemberIdAndFeedId(member.id.value, savedFeed.id!!.value)

        return FeedDetailResult(
            id = savedFeed.id!!.value,
            author = AuthorResult(
                memberId = member.memberId,
                nickname = member.nickname,
                profileImage = member.profileImage,
                role = roleResult
            ),
            boardId = savedFeed.boardId,
            boardTitle = board.title,
            createdAt = savedFeed.createdAt,
            updatedAt = savedFeed.updatedAt,
            title = savedFeed.title,
            content = savedFeed.content,
            images = remainingImages.map { FeedImageResult(it.id!!.value, it.url, it.width, it.height) },
            tags = updatedTags.map { TagResult(it.title, contestTagTitles.contains(it.title)) },
            likeCount = likeFeedPort.countByFeedId(savedFeed.id!!.value),
            isLiked = isLiked,
            commentCount = countVisibleComments(savedFeed.id!!, member.id.value)
        )
    }

    private fun validateFeedOwner(feed: Feed, member: Member) {
        if (!feed.isOwnedBy(member.id.value)) {
            throw FeedException.feedForbiddenAccess()
        }
    }

    private fun validateFinalImageCount(feed: Feed, command: UpdateFeedCommand) {
        val existingImages = feedImageRepository.findByFeedId(feed.id!!)
        val existingImageCount = existingImages.size
        val deleteImageCount = command.deleteImageIds.size
        val newImageCount = command.newImages.size
        val finalImageCount = existingImageCount - deleteImageCount + newImageCount

        if (finalImageCount > Feed.IMAGE_MAX_COUNT) {
            throw FeedException.feedImageLimitExceeded()
        }
    }

    private fun deleteImages(deleteImageIds: List<Long>, feedId: FeedId) {
        if (deleteImageIds.isEmpty()) return

        val imagesToDelete = feedImageRepository.findAllByIdInAndFeedId(deleteImageIds, feedId)
        if (imagesToDelete.size != deleteImageIds.size) {
            throw FeedException.feedImageNotFound()
        }
        feedImageRepository.deleteAllByIds(deleteImageIds)
    }

    private fun uploadImages(images: List<org.springframework.web.multipart.MultipartFile>): List<ImageUploadResult> {
        return if (images.isEmpty()) emptyList()
        else imageUploadService.uploadImages(images, "feed-images")
    }

    private fun addNewImages(imageResults: List<ImageUploadResult>, feedId: FeedId) {
        if (imageResults.isEmpty()) return

        val newFeedImages = imageResults.map { imageResult ->
            FeedImage.create(
                url = imageResult.url,
                width = imageResult.width,
                height = imageResult.height,
                feedId = feedId
            )
        }
        feedImageRepository.saveAll(newFeedImages, feedId)
    }

    private fun updateTags(tags: List<String>?, feedId: FeedId): List<FeedTag> {
        if (tags == null) {
            return feedTagRepository.findByFeedId(feedId)
        }

        feedTagRepository.deleteAllByFeedId(feedId)

        val normalizedDistinctTags = tags.asSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .toList()

        if (normalizedDistinctTags.size > Feed.TAG_MAX_COUNT) {
            throw FeedException.feedTagLimitExceeded()
        }

        val feedTags = normalizedDistinctTags.map { tagTitle ->
            FeedTag.create(feedId = feedId, title = tagTitle)
        }
        return feedTagRepository.saveAll(feedTags, feedId)
    }

    private fun getContestTagTitles(tagTitles: List<String>): Set<String> {
        val contestTags = contestTagRepository.findAllByTitleIn(tagTitles)
        return contestTags.map { it.title }.toSet()
    }

    private fun countVisibleComments(feedId: FeedId, viewerId: Long?): Int =
        feedCommentRepository.countByFeedId(feedId, blockFilterUseCase.getHiddenMemberIds(viewerId)).toInt()
}
