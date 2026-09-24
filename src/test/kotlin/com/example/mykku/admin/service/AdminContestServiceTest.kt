package com.example.mykku.admin.service

import com.example.mykku.admin.dto.contest.ContestCreateRequest
import com.example.mykku.contest.application.port.input.CreateContestUseCase
import com.example.mykku.contest.application.port.input.ListContestsUseCase
import com.example.mykku.contest.exception.ContestErrorCode
import com.example.mykku.contest.exception.ContestException
import com.example.mykku.image.ImageUploadService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.springframework.mock.web.MockMultipartFile
import java.time.LocalDateTime

@DisplayName("AdminContestService 테스트")
class AdminContestServiceTest {

    private val createContestUseCase: CreateContestUseCase = mock()
    private val listContestsUseCase: ListContestsUseCase = mock()
    private val imageUploadService: ImageUploadService = mock()
    private val adminContestService = AdminContestService(createContestUseCase, listContestsUseCase, imageUploadService)

    @Test
    @DisplayName("태그 없이 콘테스트를 만들면 이미지를 올리기 전에 CONTEST_TAG_REQUIRED 예외가 발생한다")
    fun `콘테스트 생성 실패 - 태그 없음`() {
        val exception = assertThrows<ContestException> {
            adminContestService.create(createRequest(tags = null))
        }

        assertThat(exception.errorCode).isEqualTo(ContestErrorCode.CONTEST_TAG_REQUIRED)
        verify(imageUploadService, never()).uploadEntityImages(any(), any(), any())
    }

    @Test
    @DisplayName("형식이 잘못된 태그가 있으면 이미지를 올리기 전에 TAG_INVALID_FORMAT 예외가 발생한다")
    fun `콘테스트 생성 실패 - 태그 형식 오류`() {
        val exception = assertThrows<ContestException> {
            adminContestService.create(createRequest(tags = listOf("팬아트", "#일러스트")))
        }

        assertThat(exception.errorCode).isEqualTo(ContestErrorCode.TAG_INVALID_FORMAT)
        verify(imageUploadService, never()).uploadEntityImages(any(), any(), any())
    }

    private fun createRequest(tags: List<String>?): ContestCreateRequest {
        return ContestCreateRequest(
            title = "테스트 콘테스트",
            description = "설명",
            startedAt = LocalDateTime.now(),
            expiredAt = LocalDateTime.now().plusDays(7),
            thumbnailImage = MockMultipartFile("thumbnailImage", "thumb.png", "image/png", byteArrayOf(1)),
            images = null,
            tags = tags
        )
    }
}
