package com.example.mykku.event.application.usecase

import com.example.mykku.event.application.dto.CreateEventCommand
import com.example.mykku.event.application.dto.CreateEventResult
import com.example.mykku.event.application.dto.EventImageResult
import com.example.mykku.event.application.port.input.CreateEventUseCase
import com.example.mykku.event.application.port.output.EventImageRepository
import com.example.mykku.event.application.port.output.EventRepository
import com.example.mykku.event.domain.entity.Event
import com.example.mykku.event.domain.entity.EventImage
import com.example.mykku.event.exception.EventException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CreateEventUseCaseImpl(
    private val eventRepository: EventRepository,
    private val eventImageRepository: EventImageRepository
) : CreateEventUseCase {

    @Transactional
    override fun execute(command: CreateEventCommand): CreateEventResult {
        Event.validatePeriod(command.startedAt, command.expiredAt)
        validateImages(command)

        val event = createAndSaveEvent(command)
        val images = createAndSaveImages(command, event)

        return buildResult(event, images)
    }

    private fun validateImages(command: CreateEventCommand) {
        if (command.images.size > Event.IMAGE_MAX_COUNT) {
            throw EventException.eventImageLimitExceeded()
        }
    }

    private fun createAndSaveEvent(command: CreateEventCommand): Event {
        val event = Event.create(
            title = command.title,
            subTitle = command.subTitle,
            description = command.description,
            startedAt = command.startedAt,
            expiredAt = command.expiredAt,
            thumbnailUrl = command.thumbnailUrl
        )
        return eventRepository.save(event)
    }

    private fun createAndSaveImages(command: CreateEventCommand, event: Event): List<EventImage> {
        val images = command.images.map { imageCommand ->
            EventImage.create(
                url = imageCommand.url,
                orderIndex = imageCommand.orderIndex,
                eventId = event.id
            )
        }
        return eventImageRepository.saveAll(images)
    }

    private fun buildResult(
        event: Event,
        images: List<EventImage>
    ): CreateEventResult {
        return CreateEventResult(
            id = event.id.value,
            title = event.title,
            subTitle = event.subTitle,
            description = event.description,
            startedAt = event.startedAt,
            expiredAt = event.expiredAt,
            thumbnailUrl = event.thumbnailUrl,
            images = images.sortedBy { it.orderIndex }.map {
                EventImageResult(url = it.url, orderIndex = it.orderIndex)
            },
            createdAt = event.createdAt
        )
    }
}
