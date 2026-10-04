package com.example.mykku.dailymessage.adapter.input.web

import com.example.mykku.auth.config.CurrentMember
import com.example.mykku.comment.adapter.input.web.dto.CommentPageResponse
import com.example.mykku.comment.adapter.input.web.dto.CommentResponse
import com.example.mykku.common.dto.ApiResponse
import com.example.mykku.common.util.PageableValidator
import com.example.mykku.dailymessage.application.port.input.CreateCommentUseCase
import com.example.mykku.dailymessage.application.port.input.DeleteCommentUseCase
import com.example.mykku.dailymessage.application.port.input.GetCommentsUseCase
import com.example.mykku.dailymessage.application.port.input.UpdateCommentUseCase
import com.example.mykku.member.domain.entity.Member
import org.springframework.data.domain.Sort
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/daily-messages")
class DailyMessageCommentController(
    private val getCommentsUseCase: GetCommentsUseCase,
    private val createCommentUseCase: CreateCommentUseCase,
    private val updateCommentUseCase: UpdateCommentUseCase,
    private val deleteCommentUseCase: DeleteCommentUseCase
) {
    @GetMapping("/{dailyMessageId}/comments")
    fun getComments(
        @PathVariable dailyMessageId: Long,
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
        val result = getCommentsUseCase.execute(dailyMessageId, member?.id?.value, pageable)

        return ResponseEntity.ok(
            ApiResponse(
                message = "댓글 목록을 성공적으로 조회했습니다.",
                data = CommentPageResponse.from(result)
            )
        )
    }

    @PostMapping("/{dailyMessageId}/comments")
    fun createComment(
        @PathVariable dailyMessageId: Long,
        @RequestBody request: CreateCommentRequest,
        @CurrentMember member: Member
    ): ResponseEntity<ApiResponse<CommentResponse>> {
        val command = request.toCommand(
            dailyMessageId = dailyMessageId,
            memberId = member.id.value
        )
        val result = createCommentUseCase.execute(command)

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
        @RequestBody request: UpdateCommentRequest,
        @CurrentMember member: Member
    ): ResponseEntity<ApiResponse<CommentResponse>> {
        val command = request.toCommand(commentId, member.id.value)
        val result = updateCommentUseCase.execute(command)

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
        deleteCommentUseCase.execute(commentId, member.id.value)

        return ResponseEntity.ok(
            ApiResponse(
                message = "댓글이 성공적으로 삭제되었습니다.",
                data = Unit
            )
        )
    }
}
