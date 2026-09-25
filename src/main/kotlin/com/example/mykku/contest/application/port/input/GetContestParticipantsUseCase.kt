package com.example.mykku.contest.application.port.input

import com.example.mykku.contest.application.dto.ContestParticipantsResult
import com.example.mykku.contest.application.dto.GetContestParticipantsQuery

interface GetContestParticipantsUseCase {
    fun execute(query: GetContestParticipantsQuery): ContestParticipantsResult
}
