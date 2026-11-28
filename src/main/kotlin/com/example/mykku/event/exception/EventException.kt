package com.example.mykku.event.exception

import com.example.mykku.common.exception.BaseDomainException

open class EventException(
    errorCode: EventErrorCode,
    additionalMessage: String? = null,
    cause: Throwable? = null
) : BaseDomainException(errorCode, additionalMessage, cause) {

    companion object {
        fun eventNotFound(): EventException = EventException(EventErrorCode.EVENT_NOT_FOUND)
        fun eventImageLimitExceeded(): EventException = EventException(EventErrorCode.EVENT_IMAGE_LIMIT_EXCEEDED)
        fun alreadyParticipated(): EventException = EventException(EventErrorCode.ALREADY_PARTICIPATED)
        fun eventNotActive(): EventException = EventException(EventErrorCode.EVENT_NOT_ACTIVE)
        fun eventNotExpired(): EventException = EventException(EventErrorCode.EVENT_NOT_EXPIRED)
        fun eventParticipationNotFound(): EventException =
            EventException(EventErrorCode.EVENT_PARTICIPATION_NOT_FOUND)
        fun participationNotBelongToEvent(): EventException =
            EventException(EventErrorCode.PARTICIPATION_NOT_BELONG_TO_EVENT)
        fun alreadyAttendedToday(cause: Throwable? = null): EventException =
            EventException(EventErrorCode.ALREADY_ATTENDED_TODAY, cause = cause)
        fun invalidKeepImageUrls(): EventException = EventException(EventErrorCode.INVALID_KEEP_IMAGE_URLS)
        fun periodLockedAfterWinnerSelected(): EventException =
            EventException(EventErrorCode.PERIOD_LOCKED_AFTER_WINNER_SELECTED)
        fun invalidEventPeriod(): EventException = EventException(EventErrorCode.INVALID_EVENT_PERIOD)
        fun invalidEventTypeSetting(): EventException = EventException(EventErrorCode.INVALID_EVENT_TYPE_SETTING)
        fun emptyWinners(): EventException = EventException(EventErrorCode.EMPTY_WINNERS)
        fun duplicateWinner(): EventException = EventException(EventErrorCode.DUPLICATE_WINNER)
        fun winnerNotAnnounced(): EventException = EventException(EventErrorCode.WINNER_NOT_ANNOUNCED)
        fun winnerAnnouncementNotFound(): EventException =
            EventException(EventErrorCode.WINNER_ANNOUNCEMENT_NOT_FOUND)
    }
}
