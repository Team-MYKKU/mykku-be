package com.example.mykku.admin.controller

import com.example.mykku.member.application.dto.SearchMembersQuery
import com.example.mykku.member.application.port.input.SearchMembersUseCase
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
@RequestMapping("/admin/member")
class AdminMemberViewController(
    private val searchMembersUseCase: SearchMembersUseCase
) {

    @GetMapping
    fun listPage(
        @RequestParam(required = false) keyword: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        model: Model
    ): String {
        val members = searchMembersUseCase.search(SearchMembersQuery(keyword, page, size))
        model.addAttribute("members", members)
        model.addAttribute("keyword", keyword?.trim().orEmpty())
        return "admin/member/list"
    }
}
