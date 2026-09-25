package com.example.mykku.event.application.port.input

import com.example.mykku.event.application.dto.EventDeletionSummaryResult

interface GetEventDeletionSummariesUseCase {
    fun execute(eventIds: List<Long>): Map<Long, EventDeletionSummaryResult>
}
