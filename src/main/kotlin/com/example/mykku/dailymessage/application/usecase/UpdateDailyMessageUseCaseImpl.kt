package com.example.mykku.dailymessage.application.usecase

import com.example.mykku.dailymessage.application.dto.DailyMessageSummaryResult
import com.example.mykku.dailymessage.application.dto.UpdateDailyMessageCommand
import com.example.mykku.dailymessage.application.port.input.UpdateDailyMessageUseCase
import com.example.mykku.dailymessage.application.port.output.DailyMessageRepository
import com.example.mykku.dailymessage.domain.vo.DailyMessageId
import com.example.mykku.dailymessage.exception.DailyMessageException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UpdateDailyMessageUseCaseImpl(
    private val dailyMessageRepository: DailyMessageRepository
) : UpdateDailyMessageUseCase {

    @Transactional
    override fun execute(command: UpdateDailyMessageCommand): DailyMessageSummaryResult {
        val id = DailyMessageId.of(command.id)
        val dailyMessage = dailyMessageRepository.findById(id)
            ?: throw DailyMessageException.dailyMessageNotFound()
        if (dailyMessageRepository.findAllByDate(command.date).any { it.id != id }) {
            throw DailyMessageException.dailyMessageDateAlreadyExists()
        }
        val updated = dailyMessage.update(command.title, command.content, command.date)
        return DailyMessageSummaryResult.from(dailyMessageRepository.save(updated))
    }
}
