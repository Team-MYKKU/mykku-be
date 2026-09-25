package com.example.mykku.fannote.exception

import com.example.mykku.common.exception.BaseDomainException

class FanNoteException(
    errorCode: FanNoteErrorCode,
    additionalMessage: String? = null,
    cause: Throwable? = null
) : BaseDomainException(errorCode, additionalMessage, cause) {

    companion object {
        fun fanNoteNotFound(): FanNoteException =
            FanNoteException(FanNoteErrorCode.FAN_NOTE_NOT_FOUND)

        fun fanNotePageNotFound(): FanNoteException =
            FanNoteException(FanNoteErrorCode.FAN_NOTE_PAGE_NOT_FOUND)

        fun invalidKeepImageUrls(): FanNoteException =
            FanNoteException(FanNoteErrorCode.INVALID_KEEP_IMAGE_URLS)

        fun coverImageConflict(): FanNoteException =
            FanNoteException(FanNoteErrorCode.COVER_IMAGE_CONFLICT)
    }
}