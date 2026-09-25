package com.example.mykku.admin.service

import com.example.mykku.admin.dto.board.BoardCreateRequest
import com.example.mykku.admin.dto.board.BoardUpdateRequest
import com.example.mykku.board.application.dto.BoardResult
import com.example.mykku.board.application.dto.CreateBoardCommand
import com.example.mykku.board.application.dto.DeleteBoardCommand
import com.example.mykku.board.application.dto.UpdateBoardCommand
import com.example.mykku.board.application.port.input.CreateBoardUseCase
import com.example.mykku.board.application.port.input.DeleteBoardUseCase
import com.example.mykku.board.application.port.input.GetBoardUseCase
import com.example.mykku.board.application.port.input.ListBoardsUseCase
import com.example.mykku.board.application.port.input.UpdateBoardUseCase
import com.example.mykku.board.domain.vo.BoardFeedAction
import com.example.mykku.board.domain.vo.BoardId
import com.example.mykku.feed.application.port.input.CountFeedsByBoardsUseCase
import com.example.mykku.feed.application.port.input.DeleteFeedsByBoardUseCase
import com.example.mykku.feed.application.port.input.MoveFeedsToBoardUseCase
import com.example.mykku.image.ImageUploadService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AdminBoardService(
    private val createBoardUseCase: CreateBoardUseCase,
    private val listBoardsUseCase: ListBoardsUseCase,
    private val getBoardUseCase: GetBoardUseCase,
    private val updateBoardUseCase: UpdateBoardUseCase,
    private val deleteBoardUseCase: DeleteBoardUseCase,
    private val countFeedsByBoardsUseCase: CountFeedsByBoardsUseCase,
    private val moveFeedsToBoardUseCase: MoveFeedsToBoardUseCase,
    private val deleteFeedsByBoardUseCase: DeleteFeedsByBoardUseCase,
    private val imageUploadService: ImageUploadService,
    private val uploadedImageRollback: UploadedImageRollback
) {

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun create(request: BoardCreateRequest): BoardResult {
        val logoUrl = imageUploadService.uploadImage(request.logo, BOARD_IMAGE_PATH).url
        val command = CreateBoardCommand(
            title = request.title,
            logo = logoUrl
        )
        return uploadedImageRollback.runOrDelete(listOf(logoUrl)) { createBoardUseCase.create(command) }
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun update(boardId: Long, request: BoardUpdateRequest): BoardResult {
        val logoUrl = request.logo?.takeIf { !it.isEmpty }
            ?.let { imageUploadService.uploadImage(it, BOARD_IMAGE_PATH).url }
        val command = UpdateBoardCommand(boardId = boardId, title = request.title, logo = logoUrl)
        return uploadedImageRollback.runOrDelete(listOfNotNull(logoUrl)) { updateBoardUseCase.execute(command) }
    }

    @Transactional
    fun delete(boardId: Long, feedAction: BoardFeedAction?, targetBoardId: Long?) {
        val feedCount = countFeedsByBoardsUseCase.execute(listOf(boardId))[boardId] ?: 0
        deleteBoardUseCase.validate(DeleteBoardCommand(boardId, feedCount, feedAction, targetBoardId))
        if (feedCount > 0) {
            handleFeeds(boardId, feedAction!!, targetBoardId)
        }
        deleteBoardUseCase.execute(boardId)
    }

    fun findAll(): List<BoardResult> {
        return listBoardsUseCase.listBoards()
    }

    fun findById(boardId: Long): BoardResult {
        return getBoardUseCase.getBoard(BoardId.of(boardId))
    }

    fun countFeeds(boardIds: List<Long>): Map<Long, Int> {
        return countFeedsByBoardsUseCase.execute(boardIds)
    }

    private fun handleFeeds(boardId: Long, feedAction: BoardFeedAction, targetBoardId: Long?) {
        when (feedAction) {
            BoardFeedAction.MOVE -> moveFeedsToBoardUseCase.execute(boardId, targetBoardId!!)
            BoardFeedAction.DELETE -> deleteFeedsByBoardUseCase.execute(boardId)
        }
    }

    companion object {
        private const val BOARD_IMAGE_PATH = "board-images"
    }
}
