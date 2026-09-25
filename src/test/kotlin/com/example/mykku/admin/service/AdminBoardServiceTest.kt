package com.example.mykku.admin.service

import com.example.mykku.admin.dto.board.BoardCreateRequest
import com.example.mykku.admin.dto.board.BoardUpdateRequest
import com.example.mykku.board.application.dto.BoardResult
import com.example.mykku.board.application.dto.CreateBoardCommand
import com.example.mykku.board.application.dto.UpdateBoardCommand
import com.example.mykku.board.application.port.input.CreateBoardUseCase
import com.example.mykku.board.application.port.input.DeleteBoardUseCase
import com.example.mykku.board.application.port.input.GetBoardUseCase
import com.example.mykku.board.application.port.input.ListBoardsUseCase
import com.example.mykku.board.application.port.input.UpdateBoardUseCase
import com.example.mykku.board.domain.vo.BoardFeedAction
import com.example.mykku.feed.application.port.input.CountFeedsByBoardsUseCase
import com.example.mykku.feed.application.port.input.DeleteFeedsByBoardUseCase
import com.example.mykku.feed.application.port.input.MoveFeedsToBoardUseCase
import com.example.mykku.image.ImageUploadService
import com.example.mykku.image.dto.ImageUploadResult
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.mock.web.MockMultipartFile

@DisplayName("AdminBoardService 테스트")
class AdminBoardServiceTest {

    private val createBoardUseCase: CreateBoardUseCase = mock()
    private val listBoardsUseCase: ListBoardsUseCase = mock()
    private val getBoardUseCase: GetBoardUseCase = mock()
    private val updateBoardUseCase: UpdateBoardUseCase = mock()
    private val deleteBoardUseCase: DeleteBoardUseCase = mock()
    private val countFeedsByBoardsUseCase: CountFeedsByBoardsUseCase = mock()
    private val moveFeedsToBoardUseCase: MoveFeedsToBoardUseCase = mock()
    private val deleteFeedsByBoardUseCase: DeleteFeedsByBoardUseCase = mock()
    private val imageUploadService: ImageUploadService = mock()
    private val adminBoardService = AdminBoardService(
        createBoardUseCase,
        listBoardsUseCase,
        getBoardUseCase,
        updateBoardUseCase,
        deleteBoardUseCase,
        countFeedsByBoardsUseCase,
        moveFeedsToBoardUseCase,
        deleteFeedsByBoardUseCase,
        imageUploadService,
        UploadedImageRollback(imageUploadService)
    )

    @Test
    @DisplayName("게시판 생성 시 로고를 board-images 경로로 업로드한다")
    fun `게시판 생성 - 로고 업로드 경로`() {
        val logoUrl = "https://cdn.test.com/board-images/20260725123000_abc.png"
        val request = BoardCreateRequest(title = "테스트 게시판", logo = logoFile())
        whenever(imageUploadService.uploadImage(any(), any()))
            .thenReturn(ImageUploadResult(url = logoUrl, width = 100, height = 100))
        whenever(createBoardUseCase.create(any()))
            .thenReturn(BoardResult(id = 1L, title = request.title, logo = logoUrl))

        adminBoardService.create(request)

        verify(imageUploadService).uploadImage(request.logo, "board-images")
    }

    @Test
    @DisplayName("업로드된 로고 URL이 게시판 생성 커맨드에 전달된다")
    fun `게시판 생성 - 커맨드 로고 URL`() {
        val logoUrl = "https://cdn.test.com/board-images/20260725123000_abc.png"
        val request = BoardCreateRequest(title = "테스트 게시판", logo = logoFile())
        whenever(imageUploadService.uploadImage(any(), any()))
            .thenReturn(ImageUploadResult(url = logoUrl, width = 100, height = 100))
        whenever(createBoardUseCase.create(any()))
            .thenReturn(BoardResult(id = 1L, title = request.title, logo = logoUrl))

        adminBoardService.create(request)

        val captor = argumentCaptor<CreateBoardCommand>()
        verify(createBoardUseCase).create(captor.capture())
        assertThat(captor.firstValue.logo).isEqualTo(logoUrl)
        assertThat(captor.firstValue.title).isEqualTo("테스트 게시판")
    }

    @Test
    @DisplayName("게시판 생성이 실패하면 업로드된 로고를 삭제한다")
    fun `게시판 생성 실패 - 로고 삭제`() {
        val logoUrl = "https://cdn.test.com/board-images/20260725123000_abc.png"
        val request = BoardCreateRequest(title = "테스트 게시판", logo = logoFile())
        whenever(imageUploadService.uploadImage(any(), any()))
            .thenReturn(ImageUploadResult(url = logoUrl, width = 100, height = 100))
        whenever(createBoardUseCase.create(any())).thenThrow(IllegalStateException("저장 실패"))

        runCatching { adminBoardService.create(request) }

        verify(imageUploadService).delete(logoUrl)
    }

    @Test
    @DisplayName("게시판 수정이 실패하면 새로 올린 로고만 지우고 원래 예외를 던진다")
    fun `게시판 수정 실패 - 새 로고 삭제`() {
        val logoUrl = "https://cdn.test.com/board-images/new.png"
        whenever(imageUploadService.uploadImage(any(), any()))
            .thenReturn(ImageUploadResult(url = logoUrl, width = 100, height = 100))
        whenever(updateBoardUseCase.execute(any())).thenThrow(IllegalStateException("저장 실패"))

        val result = runCatching { adminBoardService.update(1L, BoardUpdateRequest("새 제목", logoFile())) }

        assertThat(result.exceptionOrNull()).isInstanceOf(IllegalStateException::class.java)
        verify(imageUploadService).delete(logoUrl)
    }

    @Test
    @DisplayName("로고 없이 수정하면 업로드하지 않고 logo=null 커맨드를 보낸다")
    fun `게시판 수정 - 로고 없음`() {
        whenever(updateBoardUseCase.execute(any())).thenReturn(BoardResult(1L, "새 제목", "old.png"))

        adminBoardService.update(1L, BoardUpdateRequest("새 제목", null))

        verify(imageUploadService, never()).uploadImage(any(), any())
        verify(updateBoardUseCase).execute(UpdateBoardCommand(boardId = 1L, title = "새 제목", logo = null))
    }

    @Test
    @DisplayName("글이 없는 게시판은 글 처리 UseCase를 부르지 않고 지운다")
    fun `게시판 삭제 - 글 없음`() {
        whenever(countFeedsByBoardsUseCase.execute(listOf(1L))).thenReturn(emptyMap())

        adminBoardService.delete(1L, BoardFeedAction.DELETE, null)

        verify(deleteFeedsByBoardUseCase, never()).execute(any())
        verify(moveFeedsToBoardUseCase, never()).execute(any(), any())
        verify(deleteBoardUseCase).execute(1L)
    }

    @Test
    @DisplayName("MOVE는 글을 옮긴 뒤, DELETE는 글을 지운 뒤 게시판을 지운다")
    fun `게시판 삭제 - MOVE와 DELETE`() {
        whenever(countFeedsByBoardsUseCase.execute(any())).thenReturn(mapOf(1L to 2, 3L to 1))

        adminBoardService.delete(1L, BoardFeedAction.MOVE, 2L)
        adminBoardService.delete(3L, BoardFeedAction.DELETE, null)

        verify(moveFeedsToBoardUseCase).execute(1L, 2L)
        verify(deleteFeedsByBoardUseCase).execute(3L)
        verify(deleteBoardUseCase).execute(1L)
        verify(deleteBoardUseCase).execute(3L)
    }

    private fun logoFile() = MockMultipartFile("logo", "logo.png", "image/png", byteArrayOf(1, 2, 3))
}
