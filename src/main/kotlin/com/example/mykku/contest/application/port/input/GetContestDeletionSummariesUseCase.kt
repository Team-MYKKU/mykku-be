package com.example.mykku.contest.application.port.input

import com.example.mykku.contest.application.dto.ContestDeletionSummaryResult

interface GetContestDeletionSummariesUseCase {
    fun execute(contestIds: List<Long>): Map<Long, ContestDeletionSummaryResult>
}
