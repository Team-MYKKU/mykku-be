package com.example.mykku.event.application.port.input

import com.example.mykku.event.domain.vo.EventType

interface GetEventTypesByIdsUseCase {
    fun execute(eventIds: List<Long>): Map<Long, EventType>
}
