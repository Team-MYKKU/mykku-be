package com.example.mykku.admin.controller

import com.example.mykku.common.dto.ApiResponse
import com.example.mykku.feed.application.port.input.AdminDeleteFeedCommentUseCase
import com.example.mykku.feed.application.port.input.AdminDeleteFeedUseCase
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/admin/api/v1")
class AdminFeedApiController(
    private val adminDeleteFeedUseCase: AdminDeleteFeedUseCase,
    private val adminDeleteFeedCommentUseCase: AdminDeleteFeedCommentUseCase
) {

    @DeleteMapping("/feeds/{feedId}")
    fun deleteFeed(@PathVariable feedId: Long): ResponseEntity<ApiResponse<Nothing?>> {
        adminDeleteFeedUseCase.execute(feedId)
        return ResponseEntity.ok(ApiResponse(message = "게시글이 삭제되었습니다", data = null))
    }

    @DeleteMapping("/feed-comments/{commentId}")
    fun deleteFeedComment(@PathVariable commentId: Long): ResponseEntity<ApiResponse<Nothing?>> {
        adminDeleteFeedCommentUseCase.execute(commentId)
        return ResponseEntity.ok(ApiResponse(message = "댓글이 삭제되었습니다", data = null))
    }
}
