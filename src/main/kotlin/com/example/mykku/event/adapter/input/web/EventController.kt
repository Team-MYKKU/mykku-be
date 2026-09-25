package com.example.mykku.event.adapter.input.web

import com.example.mykku.auth.config.CurrentMember
import com.example.mykku.common.dto.ApiResponse
import com.example.mykku.event.application.dto.EventListQuery
import com.example.mykku.event.application.port.input.GetEventUseCase
import com.example.mykku.event.application.port.input.ListEventsUseCase
import com.example.mykku.event.domain.vo.EventSortType
import com.example.mykku.event.domain.vo.EventStatusFilter
import com.example.mykku.member.domain.entity.Member
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/events")
class EventController(
    private val getEventUseCase: GetEventUseCase,
    private val listEventsUseCase: ListEventsUseCase
) {

    @GetMapping
    fun getEvents(
        @RequestParam(defaultValue = "ACTIVE") status: EventStatusFilter,
        @RequestParam(defaultValue = "LATEST") sortType: EventSortType,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @CurrentMember member: Member
    ): ResponseEntity<ApiResponse<PagedEventsResponse>> {
        val query = EventListQuery(
            filter = status.listFilter,
            sortType = sortType,
            page = page,
            size = size
        )
        val result = listEventsUseCase.execute(query)
        return ResponseEntity.ok(
            ApiResponse(
                message = "이벤트 목록을 성공적으로 조회했습니다.",
                data = PagedEventsResponse.from(result)
            )
        )
    }

    @GetMapping("/{eventId}")
    fun getEventDetail(
        @PathVariable eventId: Long,
        @CurrentMember member: Member
    ): ResponseEntity<ApiResponse<EventDetailResponse>> {
        val result = getEventUseCase.execute(eventId, member.id.value)
        return ResponseEntity.ok(
            ApiResponse(
                message = "이벤트 상세 정보를 성공적으로 조회했습니다.",
                data = EventDetailResponse.from(result)
            )
        )
    }
}
