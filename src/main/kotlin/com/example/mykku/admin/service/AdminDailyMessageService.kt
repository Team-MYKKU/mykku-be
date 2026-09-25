package com.example.mykku.admin.service

import com.example.mykku.admin.dto.dailymessage.DailyMessageCreateRequest
import com.example.mykku.admin.dto.dailymessage.DailyMessageListResponse
import com.example.mykku.admin.dto.dailymessage.DailyMessageUpdateRequest
import com.example.mykku.dailymessage.application.dto.CreateDailyMessageCommand
import com.example.mykku.dailymessage.application.dto.DailyMessageResult
import com.example.mykku.dailymessage.application.port.input.CreateDailyMessageUseCase
import com.example.mykku.dailymessage.application.port.input.DeleteDailyMessageUseCase
import com.example.mykku.dailymessage.application.port.input.GetAllDailyMessagesUseCase
import com.example.mykku.dailymessage.application.port.input.GetDailyMessageUseCase
import com.example.mykku.dailymessage.application.port.input.UpdateDailyMessageUseCase
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AdminDailyMessageService(
    private val createDailyMessageUseCase: CreateDailyMessageUseCase,
    private val getAllDailyMessagesUseCase: GetAllDailyMessagesUseCase,
    private val deleteDailyMessageUseCase: DeleteDailyMessageUseCase,
    private val updateDailyMessageUseCase: UpdateDailyMessageUseCase,
    private val getDailyMessageUseCase: GetDailyMessageUseCase
) {

    @Transactional
    fun create(request: DailyMessageCreateRequest): DailyMessageListResponse {
        val command = CreateDailyMessageCommand(
            title = request.title,
            content = request.content,
            date = request.date
        )

        val result = createDailyMessageUseCase.execute(command)
        return DailyMessageListResponse(
            id = result.id,
            title = result.title,
            content = result.content,
            date = result.date,
            createdAt = result.createdAt
        )
    }

    fun findAll(pageable: Pageable): Page<DailyMessageListResponse> {
        return getAllDailyMessagesUseCase.execute(pageable)
            .map { result ->
                DailyMessageListResponse(
                    id = result.id,
                    title = result.title,
                    content = result.content,
                    date = result.date,
                    createdAt = result.createdAt
                )
            }
    }

    @Transactional
    fun deleteById(id: Long) {
        deleteDailyMessageUseCase.execute(id)
    }

    @Transactional
    fun update(id: Long, request: DailyMessageUpdateRequest) {
        updateDailyMessageUseCase.execute(request.toCommand(id))
    }

    fun findById(id: Long): DailyMessageResult {
        return getDailyMessageUseCase.execute(id, null)
    }
}
