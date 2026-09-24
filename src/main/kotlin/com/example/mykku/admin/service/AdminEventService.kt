package com.example.mykku.admin.service

import com.example.mykku.admin.dto.event.EventCreateRequest
import com.example.mykku.event.adapter.input.web.CreateEventResponse
import com.example.mykku.event.application.dto.CreateEventCommand
import com.example.mykku.event.application.dto.EventImageCommand
import com.example.mykku.event.application.dto.EventListQuery
import com.example.mykku.event.application.dto.PagedEventsResult
import com.example.mykku.event.application.port.input.CreateEventUseCase
import com.example.mykku.event.application.port.input.ListEventsUseCase
import com.example.mykku.event.domain.entity.Event
import com.example.mykku.event.exception.EventException
import com.example.mykku.event.domain.vo.EventListFilter
import com.example.mykku.event.domain.vo.EventSortType
import com.example.mykku.image.ImageUploadService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile

@Service
@Transactional(readOnly = true)
class AdminEventService(
    private val createEventUseCase: CreateEventUseCase,
    private val listEventsUseCase: ListEventsUseCase,
    private val imageUploadService: ImageUploadService
) {

    @Transactional
    fun create(request: EventCreateRequest): CreateEventResponse {
        validateImageCount(request.images)

        val uploadResult = imageUploadService.uploadEntityImages(
            request.thumbnailImage,
            request.images,
            "event-images"
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

    private fun validateImageCount(images: List<MultipartFile>?) {
        if ((images?.size ?: 0) > Event.IMAGE_MAX_COUNT) {
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
}
