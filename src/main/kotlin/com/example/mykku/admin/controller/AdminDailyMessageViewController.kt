package com.example.mykku.admin.controller

import com.example.mykku.admin.service.AdminDailyMessageService
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
@RequestMapping("/admin/dailymessage")
class AdminDailyMessageViewController(
    private val adminDailyMessageService: AdminDailyMessageService
) {

    @GetMapping
    fun listPage(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        model: Model
    ): String {
        val pageable = PageRequest.of(
            page,
            size,
            Sort.by(Sort.Direction.DESC, "date")
        )
        val messages = adminDailyMessageService.findAll(pageable)

        model.addAttribute("title", "데일리 메시지 관리")
        model.addAttribute("messages", messages)
        return "admin/dailymessage/list"
    }

    @GetMapping("/create")
    fun createPage(model: Model): String {
        model.addAttribute("title", "데일리 메시지 생성")
        return "admin/dailymessage/create"
    }

    @GetMapping("/{id}/edit")
    fun editPage(@PathVariable id: Long, model: Model): String {
        model.addAttribute("title", "데일리 메시지 수정")
        model.addAttribute("message", adminDailyMessageService.findById(id))
        return "admin/dailymessage/edit"
    }
}
