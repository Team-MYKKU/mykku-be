package com.example.mykku.admin.controller

import com.example.mykku.admin.service.AdminContestService
import com.example.mykku.contest.domain.vo.ContestListFilter
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
@RequestMapping("/admin/contest")
class AdminContestViewController(
    private val adminContestService: AdminContestService
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
}
