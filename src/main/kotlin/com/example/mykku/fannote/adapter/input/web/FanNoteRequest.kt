package com.example.mykku.fannote.adapter.input.web

import com.example.mykku.fannote.application.dto.CreateFanNoteCommand
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import org.springframework.format.annotation.DateTimeFormat
import java.time.LocalDate

data class FanNoteCreateRequest(
    @field:NotBlank(message = "제목은 필수입니다")
    val title: String,

    val subtitle: String?,

    val content: String?,

    @field:NotNull(message = "제작일은 필수입니다")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    val productionDate: LocalDate,

    val coverImageUrl: String?,

    val pageImageUrls: List<String> = emptyList()
) {
    fun toCommand(): CreateFanNoteCommand {
        return CreateFanNoteCommand(
            title = title,
            subtitle = subtitle,
            content = content,
            productionDate = productionDate,
            coverImageUrl = coverImageUrl,
            pageImageUrls = pageImageUrls
        )
    }
}
