package com.example.mykku.admin.controller

import com.example.mykku.admin.service.AdminBoardService
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("/admin/board")
class AdminBoardViewController(
    private val adminBoardService: AdminBoardService
) {

    @GetMapping
    fun list(model: Model): String {
        val boards = adminBoardService.findAll()
        model.addAttribute("boards", boards)
        model.addAttribute("feedCounts", adminBoardService.countFeeds(boards.map { it.id }))
        return "admin/board/list"
    }

    @GetMapping("/create")
    fun createForm(): String {
        return "admin/board/create"
    }

    @GetMapping("/{boardId}/edit")
    fun editForm(@PathVariable boardId: Long, model: Model): String {
        model.addAttribute("board", adminBoardService.findById(boardId))
        return "admin/board/edit"
    }
}
