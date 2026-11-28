package com.example.mykku.event.application.port.input

import com.example.mykku.event.application.dto.AttendEventCommand
import com.example.mykku.event.application.dto.MyEventStatusResult
import com.example.mykku.member.domain.entity.Member

interface AttendEventUseCase {
    fun execute(command: AttendEventCommand, member: Member): MyEventStatusResult
}
