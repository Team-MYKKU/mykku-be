package com.example.mykku.admin.controller

import com.example.mykku.admin.service.AdminContestService
import com.example.mykku.contest.application.dto.ContestWinnerAnnouncementResult
import com.example.mykku.contest.application.dto.GetContestParticipantsQuery
import com.example.mykku.contest.application.port.input.GetContestParticipantsUseCase
import com.example.mykku.contest.application.port.input.GetContestWinnerAnnouncementUseCase
import com.example.mykku.contest.domain.vo.ContestListFilter
import com.example.mykku.contest.exception.ContestErrorCode
import com.example.mykku.contest.exception.ContestException
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
@RequestMapping("/admin/contest")
class AdminContestViewController(
    private val adminContestService: AdminContestService,
    private val getContestParticipantsUseCase: GetContestParticipantsUseCase,
    private val getContestWinnerAnnouncementUseCase: GetContestWinnerAnnouncementUseCase
) {

    @GetMapping
    fun listPage(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(defaultValue = "ALL") status: ContestListFilter,
        model: Model
    ): String {
        val contests = adminContestService.findAll(page, size, status)
        model.addAttribute("contests", contests)
        model.addAttribute("currentStatus", status)
        return "admin/contest/list"
    }

    @GetMapping("/create")
    fun createPage(): String {
        return "admin/contest/create"
    }

    @GetMapping("/{contestId}/participants")
    fun participantsPage(
        @PathVariable contestId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        model: Model
    ): String {
        val result = getContestParticipantsUseCase.execute(GetContestParticipantsQuery(contestId, page, size))
        model.addAttribute("contest", result.contest)
        model.addAttribute("currentWinners", result.currentWinners)
        model.addAttribute("participants", result.participants)
        model.addAttribute("announcement", findAnnouncement(contestId))
        return "admin/contest/participants"
    }

    private fun findAnnouncement(contestId: Long): ContestWinnerAnnouncementResult? {
        return try {
            getContestWinnerAnnouncementUseCase.execute(contestId)
        } catch (e: ContestException) {
            if (e.errorCode != ContestErrorCode.WINNER_ANNOUNCEMENT_NOT_FOUND) throw e
            null
        }
    }
}
