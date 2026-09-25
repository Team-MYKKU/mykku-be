package com.example.mykku.event.application.port.input

import com.example.mykku.event.application.dto.UpdateEventCommand

interface UpdateEventUseCase {
    fun execute(command: UpdateEventCommand)
}
