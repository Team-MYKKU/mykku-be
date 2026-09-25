package com.example.mykku.admin.controller

import com.example.mykku.admin.dto.contest.ContestCreateRequest
import com.example.mykku.admin.dto.contest.ContestUpdateRequest
import com.example.mykku.admin.dto.contest.ContestWinnerAnnouncementUpsertRequest
import com.example.mykku.admin.service.AdminContestService
import com.example.mykku.common.dto.ApiResponse
import com.example.mykku.contest.adapter.input.web.ContestWinnerAnnouncementResponse
import com.example.mykku.contest.adapter.input.web.CreateContestResponse
import com.example.mykku.contest.adapter.input.web.SetContestWinnersRequest
import com.example.mykku.contest.adapter.input.web.SetContestWinnersResponse
import com.example.mykku.contest.application.port.input.DeleteContestUseCase
import com.example.mykku.contest.application.port.input.SetContestWinnersUseCase
import com.example.mykku.contest.application.port.input.UpsertContestWinnerAnnouncementUseCase
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/admin/api/v1/contests")
class AdminContestApiController(
    private val setContestWinnersUseCase: SetContestWinnersUseCase,
    private val upsertContestWinnerAnnouncementUseCase: UpsertContestWinnerAnnouncementUseCase,
    private val deleteContestUseCase: DeleteContestUseCase,
    private val adminContestService: AdminContestService
) {

    @PostMapping(consumes = ["multipart/form-data"])
    fun create(
        @Valid @ModelAttribute request: ContestCreateRequest
    ): ResponseEntity<ApiResponse<CreateContestResponse>> {
        val created = adminContestService.create(request)
        return ResponseEntity.ok(
            ApiResponse(
                message = "공모전이 성공적으로 생성되었습니다.",
                data = created
            )
        )
    }

    @PostMapping("/{contestId}/winners")
    fun setWinners(
        @PathVariable contestId: Long,
        @RequestBody @Valid request: SetContestWinnersRequest
    ): ResponseEntity<ApiResponse<SetContestWinnersResponse>> {
        val result = setContestWinnersUseCase.execute(request.toCommand(contestId))
        return ResponseEntity.ok(
            ApiResponse(
                message = "수상자가 성공적으로 선정되었습니다.",
                data = SetContestWinnersResponse.from(result)
            )
        )
    }

    @PutMapping("/{contestId}/winner-announcement")
    fun upsertWinnerAnnouncement(
        @PathVariable contestId: Long,
        @RequestBody @Valid request: ContestWinnerAnnouncementUpsertRequest
    ): ResponseEntity<ApiResponse<ContestWinnerAnnouncementResponse>> {
        val result = upsertContestWinnerAnnouncementUseCase.execute(request.toCommand(contestId))
        return ResponseEntity.ok(
            ApiResponse(
                message = "수상자 발표 공지가 성공적으로 저장되었습니다.",
                data = ContestWinnerAnnouncementResponse.from(result)
            )
        )
    }

    @PutMapping("/{contestId}", consumes = ["multipart/form-data"])
    fun update(
        @PathVariable contestId: Long,
        @Valid @ModelAttribute request: ContestUpdateRequest
    ): ResponseEntity<ApiResponse<Nothing?>> {
        adminContestService.update(contestId, request)
        return ResponseEntity.ok(ApiResponse(message = "콘테스트가 수정되었습니다", data = null))
    }

    @DeleteMapping("/{contestId}")
    fun delete(@PathVariable contestId: Long): ResponseEntity<ApiResponse<Nothing?>> {
        deleteContestUseCase.execute(contestId)
        return ResponseEntity.ok(ApiResponse(message = "콘테스트가 삭제되었습니다", data = null))
    }
}
