package com.example.mykku.contest.application.port.input

import com.example.mykku.contest.application.dto.UpdateContestCommand

interface UpdateContestUseCase {
    fun execute(command: UpdateContestCommand)
}
