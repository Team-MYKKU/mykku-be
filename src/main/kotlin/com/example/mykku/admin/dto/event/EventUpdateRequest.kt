package com.example.mykku.admin.dto.event

import com.example.mykku.event.domain.vo.EventContent
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDateTime

data class EventUpdateRequest(
    @field:NotBlank(message = "제목은 필수입니다")
    @field:Size(max = 255, message = "제목은 255자 이하여야 합니다")
    val title: String,

    @field:Size(max = 255, message = "부제목은 255자 이하여야 합니다")
    val subTitle: String?,

    val description: String?,

    @field:NotNull(message = "시작일은 필수입니다")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", fallbackPatterns = ["yyyy-MM-dd'T'HH:mm"])
    val startedAt: LocalDateTime,

    @field:NotNull(message = "종료일은 필수입니다")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", fallbackPatterns = ["yyyy-MM-dd'T'HH:mm"])
    val expiredAt: LocalDateTime,

    val thumbnailImage: MultipartFile? = null,

    @field:NotNull(message = "유지할 이미지 목록(_keepImageUrls)은 필수입니다")
    val keepImageUrls: List<String>?,

    val newImages: List<MultipartFile>? = null
) {
    val newImageCount: Int
        get() = newImages.orEmpty().count { !it.isEmpty }

    fun toContent(): EventContent {
        return EventContent(
            title = title,
            subTitle = subTitle?.takeIf { it.isNotBlank() },
            description = description?.takeIf { it.isNotBlank() },
            startedAt = startedAt,
            expiredAt = expiredAt
        )
    }
}
