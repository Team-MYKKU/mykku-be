package com.example.mykku.feed.adapter.input.web

import com.example.mykku.auth.config.CurrentMember
import com.example.mykku.comment.adapter.input.web.dto.CommentPageResponse
import com.example.mykku.common.dto.ApiResponse
import com.example.mykku.common.util.PageableValidator
import com.example.mykku.feed.adapter.input.web.dto.CreateFeedRequest
import com.example.mykku.feed.adapter.input.web.dto.CreateFeedRequestDto
import com.example.mykku.feed.adapter.input.web.dto.CreateFeedResponse
import com.example.mykku.feed.adapter.input.web.dto.FeedDetailResponse
import com.example.mykku.feed.adapter.input.web.dto.UpdateFeedRequest
import com.example.mykku.feed.adapter.input.web.dto.UpdateFeedRequestDto
import com.example.mykku.feed.application.dto.CreateFeedCommand
import com.example.mykku.feed.application.dto.DeleteFeedCommand
import com.example.mykku.feed.application.dto.GetFeedCommentsQuery
import com.example.mykku.feed.application.dto.GetFeedDetailQuery
import com.example.mykku.feed.application.dto.UpdateFeedCommand
import com.example.mykku.feed.application.port.input.CreateFeedUseCase
import com.example.mykku.feed.application.port.input.DeleteFeedUseCase
import com.example.mykku.feed.application.port.input.GetFeedCommentsUseCase
import com.example.mykku.feed.application.port.input.GetFeedDetailUseCase
import com.example.mykku.feed.application.port.input.UpdateFeedUseCase
import com.example.mykku.feed.exception.FeedException
import com.example.mykku.member.domain.entity.Member
import jakarta.validation.Valid
import org.springframework.data.domain.Sort
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/v1/feeds")
class FeedController(
    private val createFeedUseCase: CreateFeedUseCase,
    private val getFeedDetailUseCase: GetFeedDetailUseCase,
    private val updateFeedUseCase: UpdateFeedUseCase,
    private val deleteFeedUseCase: DeleteFeedUseCase,
    private val getFeedCommentsUseCase: GetFeedCommentsUseCase
) {
    @PostMapping(consumes = ["multipart/form-data"])
    fun createFeed(
        @RequestPart("request") @Valid requestDto: CreateFeedRequestDto,
        @RequestPart("images", required = false) images: List<MultipartFile>?,
        @CurrentMember member: Member
    ): ResponseEntity<ApiResponse<CreateFeedResponse>> {
        val request = buildCreateFeedRequest(requestDto, images)
        validateImageCount(request.images.size)

        val command = CreateFeedCommand(
            title = request.title,
            content = request.content,
            boardId = request.boardId,
            memberId = member.id.value,
            images = request.images,
            tags = request.tags
        )

        val result = createFeedUseCase.execute(command, member)

        return ResponseEntity.ok(
            ApiResponse(
                message = "피드가 성공적으로 작성되었습니다.",
                data = CreateFeedResponse.from(result)
            )
        )
    }

    private fun buildCreateFeedRequest(
        requestDto: CreateFeedRequestDto,
        images: List<MultipartFile>?
    ): CreateFeedRequest {
        val imageList = images ?: emptyList()
        return CreateFeedRequest(
            title = requestDto.title,
            content = requestDto.content,
            boardId = requestDto.boardId,
            images = imageList,
            tags = requestDto.tags
        )
    }

    private fun validateImageCount(imageCount: Int) {
        if (imageCount > CreateFeedRequest.MAX_IMAGE_COUNT) {
            throw FeedException.feedImageLimitExceeded()
        }
    }

    @GetMapping("/{feedId}")
    fun getFeedDetail(
        @PathVariable feedId: Long,
        @CurrentMember(required = false) member: Member?
    ): ResponseEntity<ApiResponse<FeedDetailResponse>> {
        val query = GetFeedDetailQuery(feedId = feedId, memberId = member?.id?.value)
        val result = getFeedDetailUseCase.execute(query)

        return ResponseEntity.ok(
            ApiResponse(
                message = "피드 상세 정보를 성공적으로 조회했습니다.",
                data = FeedDetailResponse.from(result)
            )
        )
    }

    @GetMapping("/{feedId}/comments")
    fun getComments(
        @PathVariable feedId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @CurrentMember(required = false) member: Member?
    ): ResponseEntity<ApiResponse<CommentPageResponse>> {
        val pageable = PageableValidator.validateAndCreate(
            page,
            size,
            "createdAt",
            Sort.Direction.DESC
        )

        val query = GetFeedCommentsQuery(
            feedId = feedId,
            memberId = member?.id?.value,
            pageable = pageable
        )

        val result = getFeedCommentsUseCase.execute(query)

        return ResponseEntity.ok(
            ApiResponse(
                message = "댓글 목록을 성공적으로 조회했습니다.",
                data = CommentPageResponse.from(result)
            )
        )
    }

    @DeleteMapping("/{feedId}")
    fun deleteFeed(
        @PathVariable feedId: Long,
        @CurrentMember member: Member
    ): ResponseEntity<ApiResponse<Unit>> {
        val command = DeleteFeedCommand(feedId = feedId, memberId = member.id.value)
        deleteFeedUseCase.execute(command, member)

        return ResponseEntity.ok(
            ApiResponse(
                message = "피드가 성공적으로 삭제되었습니다.",
                data = Unit
            )
        )
    }

    @PatchMapping("/{feedId}", consumes = ["multipart/form-data"])
    fun updateFeed(
        @PathVariable feedId: Long,
        @RequestPart("request") @Valid requestDto: UpdateFeedRequestDto,
        @RequestPart("images", required = false) images: List<MultipartFile>?,
        @CurrentMember member: Member
    ): ResponseEntity<ApiResponse<FeedDetailResponse>> {
        val request = UpdateFeedRequest.from(requestDto, images)

        val command = UpdateFeedCommand(
            feedId = feedId,
            memberId = member.id.value,
            title = request.title,
            content = request.content,
            boardId = request.boardId,
            tags = request.tags,
            deleteImageIds = request.deleteImageIds,
            newImages = request.newImages
        )

        val result = updateFeedUseCase.execute(command, member)

        return ResponseEntity.ok(
            ApiResponse(
                message = "피드가 성공적으로 수정되었습니다.",
                data = FeedDetailResponse.from(result)
            )
        )
    }
}
