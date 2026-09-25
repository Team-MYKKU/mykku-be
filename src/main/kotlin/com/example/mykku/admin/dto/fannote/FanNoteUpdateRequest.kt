package com.example.mykku.admin.dto.fannote

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDate

data class FanNoteUpdateRequest(
    @field:NotBlank(message = "제목은 필수입니다")
    @field:Size(max = 100, message = "제목은 100자 이하여야 합니다")
    val title: String,

    @field:Size(max = 200, message = "부제목은 200자 이하여야 합니다")
    val subtitle: String?,

    val content: String?,

    @field:NotNull(message = "제작일은 필수입니다")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    val productionDate: LocalDate,

    val coverImage: MultipartFile? = null,

    val removeCoverImage: Boolean = false,

    @field:NotNull(message = "유지할 페이지 이미지 목록(_keepPageImageUrls)은 필수입니다")
    val keepPageImageUrls: List<String>?,

    val newPageImages: List<MultipartFile>? = null
) {
    val hasNewCoverImage: Boolean
        get() = coverImage?.isEmpty == false
}
