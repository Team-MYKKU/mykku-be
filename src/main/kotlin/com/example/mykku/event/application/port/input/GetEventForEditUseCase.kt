package com.example.mykku.event.application.port.input

import com.example.mykku.event.application.dto.EventEditResult

interface GetEventForEditUseCase {
    fun execute(eventId: Long): EventEditResult
}
