package com.example.mykku.admin.controller

import com.example.mykku.admin.dto.event.EventCreateRequest
import com.example.mykku.admin.dto.event.EventWinnerAnnouncementUpsertRequest
import com.example.mykku.admin.dto.event.InvalidWinnerMemberIdsResponse
import com.example.mykku.admin.service.AdminEventService
import com.example.mykku.common.dto.ApiResponse
import com.example.mykku.common.exception.ExceptionLoggingSupport
import com.example.mykku.event.adapter.input.web.CreateEventResponse
import com.example.mykku.event.adapter.input.web.EventWinnerAnnouncementResponse
import com.example.mykku.event.adapter.input.web.SetEventWinnersRequest
import com.example.mykku.event.adapter.input.web.SetEventWinnersResponse
import com.example.mykku.event.application.port.input.SetEventWinnersUseCase
import com.example.mykku.event.application.port.input.UpsertEventWinnerAnnouncementUseCase
import com.example.mykku.event.exception.InvalidWinnerMemberIdsException
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/admin/api/v1/events")
class AdminEventApiController(
    private val setEventWinnersUseCase: SetEventWinnersUseCase,
    private val upsertEventWinnerAnnouncementUseCase: UpsertEventWinnerAnnouncementUseCase,
    private val adminEventService: AdminEventService
) {

    private val logger = LoggerFactory.getLogger(AdminEventApiController::class.java)

    @PostMapping(consumes = ["multipart/form-data"])
    fun create(
        @Valid @ModelAttribute request: EventCreateRequest
    ): ResponseEntity<ApiResponse<CreateEventResponse>> {
        val created = adminEventService.create(request)
        return ResponseEntity.ok(
            ApiResponse(
                message = "이벤트가 성공적으로 생성되었습니다.",
                data = created
            )
        )
    }

    @PutMapping("/{eventId}/winners")
    fun setWinners(
        @PathVariable eventId: Long,
        @RequestBody @Valid request: SetEventWinnersRequest
    ): ResponseEntity<ApiResponse<SetEventWinnersResponse>> {
        val result = setEventWinnersUseCase.execute(request.toCommand(eventId))
        return ResponseEntity.ok(
            ApiResponse(
                message = if (result.dryRun) "당첨자 입력을 확인했습니다." else "당첨자가 성공적으로 선정되었습니다.",
                data = SetEventWinnersResponse.from(result)
            )
        )
    }

    @PutMapping("/{eventId}/winner-announcement")
    fun upsertWinnerAnnouncement(
        @PathVariable eventId: Long,
        @RequestBody @Valid request: EventWinnerAnnouncementUpsertRequest
    ): ResponseEntity<ApiResponse<EventWinnerAnnouncementResponse>> {
        val result = upsertEventWinnerAnnouncementUseCase.execute(request.toCommand(eventId))
        return ResponseEntity.ok(
            ApiResponse(
                message = "당첨자 발표 공지가 성공적으로 저장되었습니다.",
                data = EventWinnerAnnouncementResponse.from(result)
            )
        )
    }

    @ExceptionHandler(InvalidWinnerMemberIdsException::class)
    fun handleInvalidWinnerMemberIds(
        exception: InvalidWinnerMemberIdsException
    ): ResponseEntity<InvalidWinnerMemberIdsResponse> {
        ExceptionLoggingSupport.logException(logger, exception)
        val errorCode = exception.errorCode
        return ResponseEntity
            .status(errorCode.status)
            .contentType(MediaType.APPLICATION_JSON)
            .body(InvalidWinnerMemberIdsResponse(errorCode.code, errorCode.message, exception.invalidMemberIds))
    }
}
