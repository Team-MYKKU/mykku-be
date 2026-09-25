package com.example.mykku.admin.dto.contest

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDateTime

data class ContestCreateRequest(
    @field:NotBlank(message = "제목은 필수입니다")
    @field:Size(max = 255, message = "제목은 255자 이하여야 합니다")
    val title: String,

    val description: String?,

    @field:NotNull(message = "시작일은 필수입니다")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", fallbackPatterns = ["yyyy-MM-dd'T'HH:mm"])
    val startedAt: LocalDateTime,

    @field:NotNull(message = "종료일은 필수입니다")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", fallbackPatterns = ["yyyy-MM-dd'T'HH:mm"])
    val expiredAt: LocalDateTime,

    val thumbnailImage: MultipartFile,

    val images: List<MultipartFile>?,

    val tags: List<String>?
)
