package com.example.mykku.event.application.port.input

interface DeleteEventUseCase {
    fun execute(eventId: Long)
}
