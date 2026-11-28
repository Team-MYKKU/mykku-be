package com.example.mykku.event.adapter.input.web

import com.example.mykku.auth.config.CurrentMember
import com.example.mykku.common.dto.ApiResponse
import com.example.mykku.event.application.dto.AttendEventCommand
import com.example.mykku.event.application.port.input.AttendEventUseCase
import com.example.mykku.member.domain.entity.Member
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/events")
class EventAttendanceController(
    private val attendEventUseCase: AttendEventUseCase
) {

    @PostMapping("/{eventId}/attendance")
    fun attend(
        @PathVariable eventId: Long,
        @CurrentMember member: Member
    ): ResponseEntity<ApiResponse<MyEventStatusResponse>> {
        val result = attendEventUseCase.execute(AttendEventCommand(eventId = eventId), member)
        return ResponseEntity.ok(
            ApiResponse(
                message = "출석을 완료했습니다.",
                data = MyEventStatusResponse.from(result)
            )
        )
    }
}
