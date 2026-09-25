package com.example.mykku.admin.service

import com.example.mykku.admin.dto.contest.ContestCreateRequest
import com.example.mykku.admin.dto.contest.ContestUpdateRequest
import com.example.mykku.contest.adapter.input.web.CreateContestResponse
import com.example.mykku.contest.application.dto.ContestImageCommand
import com.example.mykku.contest.application.dto.ContestListQuery
import com.example.mykku.contest.application.dto.CreateContestCommand
import com.example.mykku.contest.application.dto.PagedContestsResult
import com.example.mykku.contest.application.dto.UpdateContestCommand
import com.example.mykku.contest.application.port.input.CreateContestUseCase
import com.example.mykku.contest.application.port.input.ListContestsUseCase
import com.example.mykku.contest.application.port.input.UpdateContestUseCase
import com.example.mykku.contest.domain.entity.Contest
import com.example.mykku.contest.domain.entity.ContestTag
import com.example.mykku.contest.domain.vo.ContestListFilter
import com.example.mykku.contest.domain.vo.ContestSortType
import com.example.mykku.contest.exception.ContestException
import com.example.mykku.image.ImageUploadService
import com.example.mykku.image.dto.EntityImagesUpdateUploadResult
import com.example.mykku.image.dto.EntityImagesUploadResult
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AdminContestService(
    private val createContestUseCase: CreateContestUseCase,
    private val listContestsUseCase: ListContestsUseCase,
    private val updateContestUseCase: UpdateContestUseCase,
    private val imageUploadService: ImageUploadService,
    private val uploadedImageRollback: UploadedImageRollback
) {

    @Transactional
    fun create(request: ContestCreateRequest): CreateContestResponse {
        Contest.validatePeriod(request.startedAt, request.expiredAt)
        validateImageCount(request.images?.size ?: 0)
        val tags = ContestTag.normalizeAndValidate(request.tags ?: emptyList())

        val uploadResult = imageUploadService.uploadEntityImages(
            request.thumbnailImage,
            request.images,
            CONTEST_IMAGE_PATH
        )

        val result = createContestUseCase.execute(toCommand(request, uploadResult, tags))
        return CreateContestResponse.from(result)
    }

    private fun toCommand(
        request: ContestCreateRequest,
        uploadResult: EntityImagesUploadResult,
        tags: List<String>
    ): CreateContestCommand {
        return CreateContestCommand(
            title = request.title,
            description = request.description,
            startedAt = request.startedAt,
            expiredAt = request.expiredAt,
            thumbnailUrl = uploadResult.thumbnailUrl,
            images = uploadResult.imageUrls.mapIndexed { index, url ->
                ContestImageCommand(url = url, orderIndex = index)
            },
            tags = tags
        )
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun update(contestId: Long, request: ContestUpdateRequest) {
        Contest.validatePeriod(request.startedAt, request.expiredAt)
        ContestTag.normalizeAndValidate(request.tags.orEmpty())
        validateImageCount(request.keepImageUrls.orEmpty().size + request.newImageCount)
        val upload = imageUploadService.uploadEntityImagesForUpdate(
            request.thumbnailImage,
            request.newImages,
            CONTEST_IMAGE_PATH
        )
        uploadedImageRollback.runOrDelete(upload.allUrls) {
            updateContestUseCase.execute(toUpdateCommand(contestId, request, upload))
        }
    }

    private fun toUpdateCommand(
        contestId: Long,
        request: ContestUpdateRequest,
        upload: EntityImagesUpdateUploadResult
    ): UpdateContestCommand {
        return UpdateContestCommand(
            contestId = contestId,
            content = request.toContent(),
            thumbnailUrl = upload.thumbnailUrl,
            keepImageUrls = request.keepImageUrls.orEmpty(),
            newImageUrls = upload.imageUrls,
            tags = request.tags.orEmpty()
        )
    }

    private fun validateImageCount(imageCount: Int) {
        if (imageCount > Contest.IMAGE_MAX_COUNT) {
            throw ContestException.contestImageLimitExceeded()
        }
    }

    fun findAll(page: Int, size: Int, filter: ContestListFilter): PagedContestsResult {
        val query = ContestListQuery(
            filter = filter,
            sortType = ContestSortType.LATEST,
            page = page,
            size = size
        )
        return listContestsUseCase.execute(query)
    }

    companion object {
        private const val CONTEST_IMAGE_PATH = "contest-images"
    }
}
