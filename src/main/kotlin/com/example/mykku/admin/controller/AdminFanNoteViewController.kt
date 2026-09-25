package com.example.mykku.admin.controller

import com.example.mykku.admin.service.AdminFanNoteService
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("/admin/fannote")
class AdminFanNoteViewController(
    private val adminFanNoteService: AdminFanNoteService
) {

    @GetMapping
    fun list(
        @PageableDefault(size = 20, sort = ["createdAt"], direction = Sort.Direction.DESC)
        pageable: Pageable,
        model: Model
    ): String {
        val fanNotes = adminFanNoteService.findAll(pageable)
        model.addAttribute("fanNotes", fanNotes)
        return "admin/fannote/list"
    }

    @GetMapping("/create")
    fun createForm(model: Model): String {
        return "admin/fannote/create"
    }

    @GetMapping("/{id}/edit")
    fun editForm(@PathVariable id: Long, model: Model): String {
        model.addAttribute("fanNote", adminFanNoteService.findById(id))
        return "admin/fannote/edit"
    }
}
