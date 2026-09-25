package com.example.mykku.event.application.usecase

import com.example.mykku.event.application.port.input.DeleteEventUseCase
import com.example.mykku.event.application.port.output.EventImageRepository
import com.example.mykku.event.application.port.output.EventParticipationRepository
import com.example.mykku.event.application.port.output.EventRepository
import com.example.mykku.event.application.port.output.EventWinnerRepository
import com.example.mykku.event.domain.entity.Event
import com.example.mykku.event.domain.vo.EventId
import com.example.mykku.event.exception.EventException
import com.example.mykku.image.ImageCleanupPort
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DeleteEventUseCaseImpl(
    private val eventRepository: EventRepository,
    private val eventImageRepository: EventImageRepository,
    private val eventParticipationRepository: EventParticipationRepository,
    private val eventWinnerRepository: EventWinnerRepository,
    private val imageCleanupPort: ImageCleanupPort
) : DeleteEventUseCase {

    @Transactional
    override fun execute(eventId: Long) {
        val event = eventRepository.findById(EventId.of(eventId))
            ?: throw EventException.eventNotFound()
        val imageUrls = listOf(event.thumbnailUrl) +
            eventImageRepository.findByEventIds(listOf(event.id)).map { it.url }
        logDeletion(event, imageUrls)
        eventRepository.deleteById(event.id)
        imageCleanupPort.deleteAfterCommit(imageUrls)
    }

    private fun logDeletion(event: Event, imageUrls: List<String>) {
        val ids = listOf(event.id)
        log.info(
            "admin delete event id={}, title={}, participations={}, winners={}, imageUrls={}",
            event.id.value,
            event.title,
            eventParticipationRepository.countByEventIds(ids)[event.id.value] ?: 0,
            eventWinnerRepository.countByEventIds(ids)[event.id.value] ?: 0,
            imageUrls
        )
    }

    companion object {
        private val log = LoggerFactory.getLogger(DeleteEventUseCaseImpl::class.java)
    }
}
