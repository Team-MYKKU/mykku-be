package com.example.mykku.admin.dto.board

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.web.multipart.MultipartFile

data class BoardUpdateRequest(
    @field:NotBlank(message = "제목은 필수입니다")
    @field:Size(max = 16, message = "제목은 최대 16자입니다")
    val title: String,

    val logo: MultipartFile? = null
)
