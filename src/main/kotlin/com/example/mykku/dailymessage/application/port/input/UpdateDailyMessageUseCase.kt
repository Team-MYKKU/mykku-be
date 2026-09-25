package com.example.mykku.dailymessage.application.port.input

import com.example.mykku.dailymessage.application.dto.DailyMessageSummaryResult
import com.example.mykku.dailymessage.application.dto.UpdateDailyMessageCommand

interface UpdateDailyMessageUseCase {
    fun execute(command: UpdateDailyMessageCommand): DailyMessageSummaryResult
}
