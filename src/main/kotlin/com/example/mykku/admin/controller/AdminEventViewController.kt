package com.example.mykku.admin.controller

import com.example.mykku.admin.service.AdminEventService
import com.example.mykku.event.application.dto.EventWinnerAnnouncementResult
import com.example.mykku.event.application.port.input.GetEventDeletionSummariesUseCase
import com.example.mykku.event.application.port.input.GetEventForEditUseCase
import com.example.mykku.event.application.port.input.GetEventWinnerAnnouncementUseCase
import com.example.mykku.event.application.port.input.GetEventWinnerSelectionUseCase
import com.example.mykku.event.domain.vo.EventListFilter
import com.example.mykku.event.exception.EventErrorCode
import com.example.mykku.event.exception.EventException
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
@RequestMapping("/admin/event")
class AdminEventViewController(
    private val adminEventService: AdminEventService,
    private val getEventWinnerSelectionUseCase: GetEventWinnerSelectionUseCase,
    private val getEventWinnerAnnouncementUseCase: GetEventWinnerAnnouncementUseCase,
    private val getEventDeletionSummariesUseCase: GetEventDeletionSummariesUseCase,
    private val getEventForEditUseCase: GetEventForEditUseCase
) {

    @GetMapping
    fun listPage(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(defaultValue = "ALL") status: EventListFilter,
        model: Model
    ): String {
        val events = adminEventService.findAll(page, size, status)
        model.addAttribute("events", events)
        val deletionSummaries = getEventDeletionSummariesUseCase.execute(events.content.map { it.id })
        model.addAttribute("deletionSummaries", deletionSummaries)
        model.addAttribute("currentStatus", status)
        return "admin/event/list"
    }

    @GetMapping("/create")
    fun createPage(): String {
        return "admin/event/create"
    }

    @GetMapping("/{eventId}/edit")
    fun editPage(@PathVariable eventId: Long, model: Model): String {
        model.addAttribute("event", getEventForEditUseCase.execute(eventId))
        return "admin/event/edit"
    }

    @GetMapping("/{eventId}/winners")
    fun winnersPage(@PathVariable eventId: Long, model: Model): String {
        val selection = getEventWinnerSelectionUseCase.execute(eventId)
        model.addAttribute("event", selection)
        model.addAttribute("winnerMemberIdsText", selection.winnerMemberIds.joinToString("\n"))
        model.addAttribute("announcement", findAnnouncement(eventId))
        return "admin/event/winners"
    }

    private fun findAnnouncement(eventId: Long): EventWinnerAnnouncementResult? {
        return try {
            getEventWinnerAnnouncementUseCase.execute(eventId)
        } catch (e: EventException) {
            if (e.errorCode != EventErrorCode.WINNER_ANNOUNCEMENT_NOT_FOUND) throw e
            null
        }
    }
}
