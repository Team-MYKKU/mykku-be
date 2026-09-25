package com.example.mykku.admin.controller

import com.example.mykku.admin.dto.board.BoardCreateRequest
import com.example.mykku.admin.dto.board.BoardUpdateRequest
import com.example.mykku.admin.service.AdminBoardService
import com.example.mykku.board.adapter.input.web.BoardResponse
import com.example.mykku.board.domain.vo.BoardFeedAction
import com.example.mykku.common.dto.ApiResponse
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/admin/api/v1/boards")
class AdminBoardApiController(
    private val adminBoardService: AdminBoardService
) {

    @PostMapping(consumes = ["multipart/form-data"])
    fun create(
        @Valid @ModelAttribute request: BoardCreateRequest
    ): ResponseEntity<ApiResponse<BoardResponse>> {
        val created = adminBoardService.create(request)
        return ResponseEntity.ok(
            ApiResponse(
                message = "게시판이 생성되었습니다",
                data = BoardResponse.from(created)
            )
        )
    }

    @PutMapping("/{boardId}", consumes = ["multipart/form-data"])
    fun update(
        @PathVariable boardId: Long,
        @Valid @ModelAttribute request: BoardUpdateRequest
    ): ResponseEntity<ApiResponse<BoardResponse>> {
        val updated = adminBoardService.update(boardId, request)
        return ResponseEntity.ok(
            ApiResponse(
                message = "게시판이 수정되었습니다",
                data = BoardResponse.from(updated)
            )
        )
    }

    @DeleteMapping("/{boardId}")
    fun delete(
        @PathVariable boardId: Long,
        @RequestParam(required = false) feedAction: BoardFeedAction?,
        @RequestParam(required = false) targetBoardId: Long?
    ): ResponseEntity<ApiResponse<Nothing?>> {
        adminBoardService.delete(boardId, feedAction, targetBoardId)
        return ResponseEntity.ok(ApiResponse(message = "게시판이 삭제되었습니다", data = null))
    }
}
