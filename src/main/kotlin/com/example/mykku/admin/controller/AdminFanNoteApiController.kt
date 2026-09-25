package com.example.mykku.admin.controller

import com.example.mykku.admin.dto.fannote.FanNoteCreateRequest
import com.example.mykku.admin.dto.fannote.FanNoteUpdateRequest
import com.example.mykku.admin.service.AdminFanNoteService
import com.example.mykku.common.dto.ApiResponse
import com.example.mykku.fannote.adapter.input.web.FanNoteDetailResponse
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/admin/api/v1/fannotes")
class AdminFanNoteApiController(
    private val adminFanNoteService: AdminFanNoteService
) {

    @PostMapping(consumes = ["multipart/form-data"])
    fun create(
        @Valid @ModelAttribute request: FanNoteCreateRequest
    ): ResponseEntity<ApiResponse<FanNoteDetailResponse>> {
        val created = adminFanNoteService.create(request)
        return ResponseEntity.ok(
            ApiResponse(
                message = "팬노트가 생성되었습니다",
                data = created
            )
        )
    }

    @PutMapping("/{id}", consumes = ["multipart/form-data"])
    fun update(
        @PathVariable id: Long,
        @Valid @ModelAttribute request: FanNoteUpdateRequest
    ): ResponseEntity<ApiResponse<Nothing?>> {
        adminFanNoteService.update(id, request)
        return ResponseEntity.ok(ApiResponse(message = "팬노트가 수정되었습니다", data = null))
    }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): ResponseEntity<ApiResponse<Nothing?>> {
        adminFanNoteService.deleteById(id)
        return ResponseEntity.ok(
            ApiResponse(
                message = "팬노트가 삭제되었습니다",
                data = null
            )
        )
    }
}
