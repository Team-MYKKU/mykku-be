package com.example.mykku.admin.controller

import com.example.mykku.admin.service.AdminEventService
import com.example.mykku.event.domain.vo.EventListFilter
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
@RequestMapping("/admin/event")
class AdminEventViewController(
    private val adminEventService: AdminEventService
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
        model.addAttribute("currentStatus", status)
        return "admin/event/list"
    }

    @GetMapping("/create")
    fun createPage(): String {
        return "admin/event/create"
    }
}
