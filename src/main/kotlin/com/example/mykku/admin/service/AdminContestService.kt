package com.example.mykku.admin.service

import com.example.mykku.admin.dto.contest.ContestCreateRequest
import com.example.mykku.contest.adapter.input.web.CreateContestResponse
import com.example.mykku.contest.application.dto.ContestImageCommand
import com.example.mykku.contest.application.dto.ContestListQuery
import com.example.mykku.contest.application.dto.CreateContestCommand
import com.example.mykku.contest.application.dto.PagedContestsResult
import com.example.mykku.contest.application.port.input.CreateContestUseCase
import com.example.mykku.contest.application.port.input.ListContestsUseCase
import com.example.mykku.contest.domain.entity.Contest
import com.example.mykku.contest.domain.entity.ContestTag
import com.example.mykku.contest.domain.vo.ContestListFilter
import com.example.mykku.contest.domain.vo.ContestSortType
import com.example.mykku.contest.exception.ContestException
import com.example.mykku.image.ImageUploadService
import com.example.mykku.image.dto.EntityImagesUploadResult
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile

@Service
@Transactional(readOnly = true)
class AdminContestService(
    private val createContestUseCase: CreateContestUseCase,
    private val listContestsUseCase: ListContestsUseCase,
    private val imageUploadService: ImageUploadService
) {

    @Transactional
    fun create(request: ContestCreateRequest): CreateContestResponse {
        validateImageCount(request.images)
        val tags = ContestTag.normalizeAndValidate(request.tags ?: emptyList())

        val uploadResult = imageUploadService.uploadEntityImages(
            request.thumbnailImage,
            request.images,
            "contest-images"
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

    private fun validateImageCount(images: List<MultipartFile>?) {
        if ((images?.size ?: 0) > Contest.IMAGE_MAX_COUNT) {
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
}
