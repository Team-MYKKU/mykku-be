package com.example.mykku.event.application.port.input

import com.example.mykku.event.application.dto.EventWinnerSelectionResult

interface GetEventWinnerSelectionUseCase {
    fun execute(eventId: Long): EventWinnerSelectionResult
}
