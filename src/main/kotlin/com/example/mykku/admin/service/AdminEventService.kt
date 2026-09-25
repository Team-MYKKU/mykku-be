package com.example.mykku.admin.service

import com.example.mykku.admin.dto.event.EventCreateRequest
import com.example.mykku.admin.dto.event.EventUpdateRequest
import com.example.mykku.event.adapter.input.web.CreateEventResponse
import com.example.mykku.event.application.dto.CreateEventCommand
import com.example.mykku.event.application.dto.EventImageCommand
import com.example.mykku.event.application.dto.EventListQuery
import com.example.mykku.event.application.dto.PagedEventsResult
import com.example.mykku.event.application.dto.UpdateEventCommand
import com.example.mykku.event.application.port.input.CreateEventUseCase
import com.example.mykku.event.application.port.input.ListEventsUseCase
import com.example.mykku.event.application.port.input.UpdateEventUseCase
import com.example.mykku.event.domain.entity.Event
import com.example.mykku.event.domain.vo.EventListFilter
import com.example.mykku.event.domain.vo.EventSortType
import com.example.mykku.event.exception.EventException
import com.example.mykku.image.ImageUploadService
import com.example.mykku.image.dto.EntityImagesUpdateUploadResult
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AdminEventService(
    private val createEventUseCase: CreateEventUseCase,
    private val listEventsUseCase: ListEventsUseCase,
    private val updateEventUseCase: UpdateEventUseCase,
    private val imageUploadService: ImageUploadService,
    private val uploadedImageRollback: UploadedImageRollback
) {

    @Transactional
    fun create(request: EventCreateRequest): CreateEventResponse {
        Event.validatePeriod(request.startedAt, request.expiredAt)
        validateImageCount(request.images?.size ?: 0)

        val uploadResult = imageUploadService.uploadEntityImages(
            request.thumbnailImage,
            request.images,
            EVENT_IMAGE_PATH
        )

        val command = CreateEventCommand(
            title = request.title,
            subTitle = request.subTitle,
            description = request.description,
            startedAt = request.startedAt,
            expiredAt = request.expiredAt,
            thumbnailUrl = uploadResult.thumbnailUrl,
            images = uploadResult.imageUrls.mapIndexed { index, url ->
                EventImageCommand(url = url, orderIndex = index)
            }
        )

        val result = createEventUseCase.execute(command)
        return CreateEventResponse.from(result)
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun update(eventId: Long, request: EventUpdateRequest) {
        Event.validatePeriod(request.startedAt, request.expiredAt)
        validateImageCount(request.keepImageUrls.orEmpty().size + request.newImageCount)
        val upload = imageUploadService.uploadEntityImagesForUpdate(
            request.thumbnailImage,
            request.newImages,
            EVENT_IMAGE_PATH
        )
        uploadedImageRollback.runOrDelete(upload.allUrls) {
            updateEventUseCase.execute(toUpdateCommand(eventId, request, upload))
        }
    }

    private fun toUpdateCommand(
        eventId: Long,
        request: EventUpdateRequest,
        upload: EntityImagesUpdateUploadResult
    ): UpdateEventCommand {
        return UpdateEventCommand(
            eventId = eventId,
            content = request.toContent(),
            thumbnailUrl = upload.thumbnailUrl,
            keepImageUrls = request.keepImageUrls.orEmpty(),
            newImageUrls = upload.imageUrls
        )
    }

    private fun validateImageCount(imageCount: Int) {
        if (imageCount > Event.IMAGE_MAX_COUNT) {
            throw EventException.eventImageLimitExceeded()
        }
    }

    fun findAll(page: Int, size: Int, filter: EventListFilter): PagedEventsResult {
        val query = EventListQuery(
            filter = filter,
            sortType = EventSortType.LATEST,
            page = page,
            size = size
        )
        return listEventsUseCase.execute(query)
    }

    companion object {
        private const val EVENT_IMAGE_PATH = "event-images"
    }
}
