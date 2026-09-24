package com.example.mykku.contest.exception

import com.example.mykku.common.exception.BaseDomainException

class ContestException(
    errorCode: ContestErrorCode,
    additionalMessage: String? = null,
    cause: Throwable? = null
) : BaseDomainException(errorCode, additionalMessage, cause) {

    companion object {
        fun contestNotFound(): ContestException = ContestException(ContestErrorCode.CONTEST_NOT_FOUND)

        fun contestImageLimitExceeded(): ContestException = ContestException(ContestErrorCode.CONTEST_IMAGE_LIMIT_EXCEEDED)

        fun contestTagLimitExceeded(): ContestException = ContestException(ContestErrorCode.CONTEST_TAG_LIMIT_EXCEEDED)

        fun tagTitleTooLong(): ContestException = ContestException(ContestErrorCode.TAG_TITLE_TOO_LONG)

        fun tagInvalidFormat(): ContestException = ContestException(ContestErrorCode.TAG_INVALID_FORMAT)


        fun contestWinnerNotFound(): ContestException = ContestException(ContestErrorCode.CONTEST_WINNER_NOT_FOUND)

        fun alreadyParticipatedWithFeed(): ContestException = ContestException(ContestErrorCode.ALREADY_PARTICIPATED_WITH_FEED)

        fun invalidWinnerRank(): ContestException = ContestException(ContestErrorCode.INVALID_WINNER_RANK)

        fun invalidWinnerCount(): ContestException = ContestException(ContestErrorCode.INVALID_WINNER_COUNT)

        fun duplicateWinnerRank(): ContestException = ContestException(ContestErrorCode.DUPLICATE_WINNER_RANK)

        fun participationNotFound(): ContestException = ContestException(ContestErrorCode.PARTICIPATION_NOT_FOUND)

        fun participationNotBelongToContest(): ContestException = ContestException(ContestErrorCode.PARTICIPATION_NOT_BELONG_TO_CONTEST)

        fun notWinnerOwner(): ContestException = ContestException(ContestErrorCode.NOT_WINNER_OWNER)

        fun contestNotExpired(): ContestException = ContestException(ContestErrorCode.CONTEST_NOT_EXPIRED)

        fun winnerNotAnnounced(): ContestException = ContestException(ContestErrorCode.WINNER_NOT_ANNOUNCED)

        fun winnerAnnouncementNotFound(): ContestException = ContestException(ContestErrorCode.WINNER_ANNOUNCEMENT_NOT_FOUND)
    }
}
