package com.example.mykku.event.application.usecase

import com.example.mykku.event.application.dto.EventEditResult
import com.example.mykku.event.application.port.input.GetEventForEditUseCase
import com.example.mykku.event.application.port.output.EventImageRepository
import com.example.mykku.event.application.port.output.EventRepository
import com.example.mykku.event.domain.vo.EventId
import com.example.mykku.event.exception.EventException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class GetEventForEditUseCaseImpl(
    private val eventRepository: EventRepository,
    private val eventImageRepository: EventImageRepository
) : GetEventForEditUseCase {

    @Transactional(readOnly = true)
    override fun execute(eventId: Long): EventEditResult {
        val event = eventRepository.findById(EventId.of(eventId))
            ?: throw EventException.eventNotFound()
        val images = eventImageRepository.findByEventIds(listOf(event.id)).sortedBy { it.orderIndex }
        return EventEditResult(
            id = event.id.value,
            title = event.title,
            subTitle = event.subTitle,
            description = event.description,
            startedAt = event.startedAt,
            expiredAt = event.expiredAt,
            status = event.resolveStatus(LocalDateTime.now()),
            thumbnailUrl = event.thumbnailUrl,
            imageUrls = images.map { it.url }
        )
    }
}
