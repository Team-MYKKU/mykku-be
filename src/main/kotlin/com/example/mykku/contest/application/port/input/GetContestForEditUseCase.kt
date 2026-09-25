package com.example.mykku.contest.application.port.input

import com.example.mykku.contest.application.dto.ContestEditResult

interface GetContestForEditUseCase {
    fun execute(contestId: Long): ContestEditResult
}
