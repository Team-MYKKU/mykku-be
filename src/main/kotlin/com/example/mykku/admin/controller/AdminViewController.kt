package com.example.mykku.admin.controller

import com.example.mykku.admin.config.AdminInterceptor
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
@RequestMapping("/admin")
class AdminViewController(
    private val adminInterceptor: AdminInterceptor
) {

    private val logger = LoggerFactory.getLogger(AdminViewController::class.java)

    @GetMapping("/login")
    fun loginPage(model: Model): String {
        model.addAttribute("title", "로그인")
        return "admin/login"
    }

    @PostMapping("/api/login")
    fun login(
        @RequestParam token: String,
        request: HttpServletRequest
    ): String {
        if (!adminInterceptor.authenticate(token, request)) {
            logger.warn("Admin login failed: remoteAddr={}", request.remoteAddr)
            return "redirect:/admin/login?error"
        }
        return "redirect:/admin"
    }

    @PostMapping("/logout")
    fun logout(request: HttpServletRequest): String {
        adminInterceptor.logout(request)
        return "redirect:/admin/login"
    }

    @GetMapping
    fun index(model: Model): String {
        model.addAttribute("title", "대시보드")
        return "admin/index"
    }
}
