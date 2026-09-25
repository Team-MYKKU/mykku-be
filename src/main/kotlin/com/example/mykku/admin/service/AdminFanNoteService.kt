package com.example.mykku.admin.service

import com.example.mykku.admin.dto.fannote.FanNoteCreateRequest
import com.example.mykku.admin.dto.fannote.FanNoteUpdateRequest
import com.example.mykku.fannote.adapter.input.web.FanNoteDetailResponse
import com.example.mykku.fannote.adapter.input.web.FanNoteListResponse
import com.example.mykku.fannote.application.dto.CreateFanNoteCommand
import com.example.mykku.fannote.application.dto.UpdateFanNoteCommand
import com.example.mykku.fannote.application.port.input.CreateFanNoteUseCase
import com.example.mykku.fannote.application.port.input.DeleteFanNoteUseCase
import com.example.mykku.fannote.application.port.input.GetFanNoteDetailUseCase
import com.example.mykku.fannote.application.port.input.GetFanNoteListUseCase
import com.example.mykku.fannote.application.port.input.UpdateFanNoteUseCase
import com.example.mykku.fannote.exception.FanNoteException
import com.example.mykku.image.ImageCleanupPort
import com.example.mykku.image.ImageUploadService
import com.example.mykku.image.dto.FanNoteImagesUploadResult
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AdminFanNoteService(
    private val createFanNoteUseCase: CreateFanNoteUseCase,
    private val getFanNoteListUseCase: GetFanNoteListUseCase,
    private val getFanNoteDetailUseCase: GetFanNoteDetailUseCase,
    private val deleteFanNoteUseCase: DeleteFanNoteUseCase,
    private val updateFanNoteUseCase: UpdateFanNoteUseCase,
    private val s3ImageUploadService: ImageUploadService,
    private val imageCleanupPort: ImageCleanupPort,
    private val uploadedImageRollback: UploadedImageRollback
) {

    @Transactional
    fun create(request: FanNoteCreateRequest): FanNoteDetailResponse {
        val uploadResult = s3ImageUploadService.uploadFanNoteImages(
            request.coverImage,
            request.pageImages
        )

        val command = CreateFanNoteCommand(
            title = request.title,
            subtitle = request.subtitle,
            content = request.content,
            productionDate = request.productionDate,
            coverImageUrl = uploadResult.coverImageUrl,
            pageImageUrls = uploadResult.pageImageUrls
        )

        val result = createFanNoteUseCase.execute(command)
        return FanNoteDetailResponse.from(result)
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun update(id: Long, request: FanNoteUpdateRequest) {
        if (request.hasNewCoverImage && request.removeCoverImage) {
            throw FanNoteException.coverImageConflict()
        }
        val upload = s3ImageUploadService.uploadFanNoteImagesForUpdate(request.coverImage, request.newPageImages)
        val uploadedUrls = listOfNotNull(upload.coverImageUrl) + upload.pageImageUrls
        uploadedImageRollback.runOrDelete(uploadedUrls) {
            updateFanNoteUseCase.execute(toUpdateCommand(id, request, upload))
        }
    }

    private fun toUpdateCommand(
        id: Long,
        request: FanNoteUpdateRequest,
        upload: FanNoteImagesUploadResult
    ): UpdateFanNoteCommand {
        return UpdateFanNoteCommand(
            fanNoteId = id,
            title = request.title,
            subtitle = request.subtitle?.takeIf { it.isNotBlank() },
            content = request.content?.takeIf { it.isNotBlank() },
            productionDate = request.productionDate,
            newCoverImageUrl = upload.coverImageUrl,
            removeCoverImage = request.removeCoverImage,
            keepPageImageUrls = request.keepPageImageUrls.orEmpty(),
            newPageImageUrls = upload.pageImageUrls
        )
    }

    fun findAll(pageable: Pageable): Page<FanNoteListResponse> {
        return getFanNoteListUseCase.execute(pageable)
            .map { FanNoteListResponse.from(it) }
    }

    fun findById(id: Long): FanNoteDetailResponse {
        val result = getFanNoteDetailUseCase.execute(id, null)
        return FanNoteDetailResponse.from(result)
    }

    @Transactional
    fun deleteById(id: Long) {
        val imageUrls = collectImageUrls(id)
        deleteFanNoteUseCase.execute(id)
        imageCleanupPort.deleteAfterCommit(imageUrls)
    }

    private fun collectImageUrls(id: Long): List<String> {
        val detail = getFanNoteDetailUseCase.execute(id, null)
        return listOfNotNull(detail.coverImageUrl) + detail.pages.map { it.imageUrl }
    }
}
