package com.example.mykku.event.exception

class InvalidWinnerMemberIdsException(
    val invalidMemberIds: List<String>
) : EventException(EventErrorCode.INVALID_WINNER_MEMBER_IDS)
