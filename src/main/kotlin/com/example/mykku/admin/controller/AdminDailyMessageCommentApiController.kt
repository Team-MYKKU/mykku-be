package com.example.mykku.admin.controller

import com.example.mykku.common.dto.ApiResponse
import com.example.mykku.dailymessage.application.port.input.AdminDeleteDailyMessageCommentUseCase
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/admin/api/v1/daily-message-comments")
class AdminDailyMessageCommentApiController(
    private val adminDeleteDailyMessageCommentUseCase: AdminDeleteDailyMessageCommentUseCase
) {

    @DeleteMapping("/{commentId}")
    fun deleteDailyMessageComment(@PathVariable commentId: Long): ResponseEntity<ApiResponse<Nothing?>> {
        adminDeleteDailyMessageCommentUseCase.execute(commentId)
        return ResponseEntity.ok(ApiResponse(message = "하루 덕담 댓글이 삭제되었습니다", data = null))
    }
}
