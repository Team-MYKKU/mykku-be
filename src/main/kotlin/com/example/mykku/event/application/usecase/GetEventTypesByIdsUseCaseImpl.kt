package com.example.mykku.event.application.usecase

import com.example.mykku.event.application.port.input.GetEventTypesByIdsUseCase
import com.example.mykku.event.application.port.output.EventRepository
import com.example.mykku.event.domain.vo.EventId
import com.example.mykku.event.domain.vo.EventType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GetEventTypesByIdsUseCaseImpl(
    private val eventRepository: EventRepository
) : GetEventTypesByIdsUseCase {

    @Transactional(readOnly = true)
    override fun execute(eventIds: List<Long>): Map<Long, EventType> {
        return eventRepository.findTypesByIds(eventIds.map { EventId.of(it) })
    }
}
