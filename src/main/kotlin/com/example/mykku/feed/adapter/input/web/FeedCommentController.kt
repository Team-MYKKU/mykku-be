package com.example.mykku.feed.adapter.input.web

import com.example.mykku.auth.config.CurrentMember
import com.example.mykku.comment.adapter.input.web.dto.CommentResponse
import com.example.mykku.common.dto.ApiResponse
import com.example.mykku.feed.adapter.input.web.dto.CreateFeedCommentRequest
import com.example.mykku.feed.adapter.input.web.dto.UpdateFeedCommentRequest
import com.example.mykku.feed.application.dto.CreateFeedCommentCommand
import com.example.mykku.feed.application.dto.DeleteFeedCommentCommand
import com.example.mykku.feed.application.dto.UpdateFeedCommentCommand
import com.example.mykku.feed.application.port.input.CreateFeedCommentUseCase
import com.example.mykku.feed.application.port.input.DeleteFeedCommentUseCase
import com.example.mykku.feed.application.port.input.UpdateFeedCommentUseCase
import com.example.mykku.member.domain.entity.Member
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/feeds")
class FeedCommentController(
    private val createFeedCommentUseCase: CreateFeedCommentUseCase,
    private val updateFeedCommentUseCase: UpdateFeedCommentUseCase,
    private val deleteFeedCommentUseCase: DeleteFeedCommentUseCase
) {
    @PostMapping("/{feedId}/comments")
    fun createComment(
        @PathVariable feedId: Long,
        @RequestBody request: CreateFeedCommentRequest,
        @CurrentMember member: Member
    ): ResponseEntity<ApiResponse<CommentResponse>> {
        val command = CreateFeedCommentCommand(
            feedId = feedId,
            memberId = member.id.value,
            content = request.content,
            parentCommentId = request.parentCommentId
        )

        val result = createFeedCommentUseCase.execute(command, member)

        return ResponseEntity.ok(
            ApiResponse(
                message = "댓글이 성공적으로 등록되었습니다.",
                data = CommentResponse.from(result)
            )
        )
    }

    @PutMapping("/comments/{commentId}")
    fun updateComment(
        @PathVariable commentId: Long,
        @RequestBody request: UpdateFeedCommentRequest,
        @CurrentMember member: Member
    ): ResponseEntity<ApiResponse<CommentResponse>> {
        val command = UpdateFeedCommentCommand(
            commentId = commentId,
            memberId = member.id.value,
            content = request.content
        )

        val result = updateFeedCommentUseCase.execute(command, member)

        return ResponseEntity.ok(
            ApiResponse(
                message = "댓글이 성공적으로 수정되었습니다.",
                data = CommentResponse.from(result)
            )
        )
    }

    @DeleteMapping("/comments/{commentId}")
    fun deleteComment(
        @PathVariable commentId: Long,
        @CurrentMember member: Member
    ): ResponseEntity<ApiResponse<Unit>> {
        val command = DeleteFeedCommentCommand(
            commentId = commentId,
            memberId = member.id.value
        )

        deleteFeedCommentUseCase.execute(command, member)

        return ResponseEntity.ok(
            ApiResponse(
                message = "댓글이 성공적으로 삭제되었습니다.",
                data = Unit
            )
        )
    }
}
