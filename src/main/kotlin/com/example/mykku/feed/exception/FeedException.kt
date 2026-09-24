package com.example.mykku.feed.exception

import com.example.mykku.common.exception.BaseDomainException

class FeedException(
    errorCode: FeedErrorCode,
    additionalMessage: String? = null,
    cause: Throwable? = null
) : BaseDomainException(errorCode, additionalMessage, cause) {

    companion object {
        fun feedNotFound(): FeedException = FeedException(FeedErrorCode.FEED_NOT_FOUND)

        fun feedForbiddenAccess(): FeedException = FeedException(FeedErrorCode.FEED_FORBIDDEN_ACCESS)

        fun feedContentTooLong(): FeedException = FeedException(FeedErrorCode.FEED_CONTENT_TOO_LONG)

        fun feedImageLimitExceeded(): FeedException = FeedException(FeedErrorCode.FEED_IMAGE_LIMIT_EXCEEDED)

        fun feedImageNotFound(): FeedException = FeedException(FeedErrorCode.FEED_IMAGE_NOT_FOUND)

        fun feedTagLimitExceeded(): FeedException = FeedException(FeedErrorCode.FEED_TAG_LIMIT_EXCEEDED)

        fun feedCommentNotFound(): FeedException = FeedException(FeedErrorCode.FEED_COMMENT_NOT_FOUND)

        fun feedCommentContentTooLong(): FeedException = FeedException(FeedErrorCode.FEED_COMMENT_CONTENT_TOO_LONG)

        fun feedCommentForbiddenAccess(): FeedException = FeedException(FeedErrorCode.FEED_COMMENT_FORBIDDEN_ACCESS)

        fun tagTitleTooLong(): FeedException = FeedException(FeedErrorCode.TAG_TITLE_TOO_LONG)

        fun tagInvalidFormat(): FeedException = FeedException(FeedErrorCode.TAG_INVALID_FORMAT)

        fun imageInvalidDimensions(): FeedException = FeedException(FeedErrorCode.IMAGE_INVALID_DIMENSIONS)

        fun searchKeywordEmpty(): FeedException = FeedException(FeedErrorCode.SEARCH_KEYWORD_EMPTY)

        fun searchKeywordTooLong(): FeedException = FeedException(FeedErrorCode.SEARCH_KEYWORD_TOO_LONG)
    }
}