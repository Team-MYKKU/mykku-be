package com.example.mykku.admin.dto.dailymessage

import com.example.mykku.dailymessage.application.dto.UpdateDailyMessageCommand
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDate

data class DailyMessageUpdateRequest(
    @field:NotBlank(message = "제목은 필수입니다")
    @field:Size(max = 255, message = "제목은 255자 이하여야 합니다")
    val title: String,

    @field:NotBlank(message = "내용은 필수입니다")
    @field:Size(max = 42, message = "내용은 최대 42자까지 입력 가능합니다")
    val content: String,

    @field:NotNull(message = "날짜는 필수입니다")
    val date: LocalDate
) {
    fun toCommand(id: Long): UpdateDailyMessageCommand {
        return UpdateDailyMessageCommand(id = id, title = title, content = content, date = date)
    }
}
