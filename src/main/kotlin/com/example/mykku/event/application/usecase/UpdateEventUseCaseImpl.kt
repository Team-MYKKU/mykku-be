package com.example.mykku.event.application.usecase

import com.example.mykku.event.application.dto.UpdateEventCommand
import com.example.mykku.event.application.port.input.UpdateEventUseCase
import com.example.mykku.event.application.port.output.EventImageRepository
import com.example.mykku.event.application.port.output.EventRepository
import com.example.mykku.event.domain.entity.Event
import com.example.mykku.event.domain.entity.EventImage
import com.example.mykku.event.domain.vo.EventId
import com.example.mykku.event.exception.EventException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UpdateEventUseCaseImpl(
    private val eventRepository: EventRepository,
    private val eventImageRepository: EventImageRepository
) : UpdateEventUseCase {

    @Transactional
    override fun execute(command: UpdateEventCommand) {
        val event = eventRepository.findById(EventId.of(command.eventId))
            ?: throw EventException.eventNotFound()
        val existingUrls = eventImageRepository.findByEventIds(listOf(event.id)).map { it.url }
        validateImages(existingUrls, command)
        val previousThumbnailUrl = event.thumbnailUrl
        event.update(command.content, command.thumbnailUrl)
        eventRepository.save(event)
        replaceImages(event, command.keepImageUrls + command.newImageUrls)
        logReplacedImages(event, previousThumbnailUrl, existingUrls - command.keepImageUrls.toSet())
    }

    private fun validateImages(existingUrls: List<String>, command: UpdateEventCommand) {
        val keep = command.keepImageUrls
        if (keep.size != keep.toSet().size || !existingUrls.containsAll(keep)) {
            throw EventException.invalidKeepImageUrls()
        }
        if (keep.size + command.newImageUrls.size > Event.IMAGE_MAX_COUNT) {
            throw EventException.eventImageLimitExceeded()
        }
    }

    private fun replaceImages(event: Event, urls: List<String>) {
        eventImageRepository.deleteAllByEventId(event.id)
        val images = urls.mapIndexed { index, url ->
            EventImage.create(url = url, orderIndex = index, eventId = event.id)
        }
        eventImageRepository.saveAll(images)
    }

    private fun logReplacedImages(event: Event, previousThumbnailUrl: String, removedUrls: List<String>) {
        val replacedThumbnail = previousThumbnailUrl.takeIf { it != event.thumbnailUrl }
        if (replacedThumbnail == null && removedUrls.isEmpty()) return
        log.info(
            "event images replaced id={}, previousThumbnail={}, removedImages={}",
            event.id.value,
            replacedThumbnail,
            removedUrls
        )
    }

    companion object {
        private val log = LoggerFactory.getLogger(UpdateEventUseCaseImpl::class.java)
    }
}
