package com.example.mykku.feed.exception

import com.example.mykku.common.exception.DomainErrorCode
import com.example.mykku.feed.domain.entity.Feed
import com.example.mykku.feed.domain.entity.FeedComment
import com.example.mykku.feed.domain.entity.FeedTag
import com.example.mykku.feed.domain.vo.SearchKeyword
import org.springframework.http.HttpStatus

enum class FeedErrorCode(
    override val code: String,
    override val status: HttpStatus,
    override val message: String
) : DomainErrorCode {

    FEED_NOT_FOUND("FD001", HttpStatus.NOT_FOUND, "피드를 찾을 수 없습니다"),
    FEED_COMMENT_NOT_FOUND("FD002", HttpStatus.NOT_FOUND, "피드 댓글을 찾을 수 없습니다"),

    FEED_CONTENT_TOO_LONG("FD101", HttpStatus.BAD_REQUEST, "피드 내용은 ${Feed.CONTENT_MAX_LENGTH}자 이하여야 합니다"),
    FEED_IMAGE_LIMIT_EXCEEDED("FD102", HttpStatus.BAD_REQUEST, "피드 이미지는 ${Feed.IMAGE_MAX_COUNT}개 이하여야 합니다"),
    FEED_IMAGE_NOT_FOUND("FD103", HttpStatus.BAD_REQUEST, "삭제할 이미지를 찾을 수 없습니다"),
    FEED_TAG_LIMIT_EXCEEDED("FD104", HttpStatus.BAD_REQUEST, "피드 태그는 ${Feed.TAG_MAX_COUNT}개 이하여야 합니다"),
    FEED_COMMENT_CONTENT_TOO_LONG("FD105", HttpStatus.BAD_REQUEST, "피드 댓글은 ${FeedComment.CONTENT_MAX_LENGTH}자 이하여야 합니다"),
    TAG_TITLE_TOO_LONG("FD106", HttpStatus.BAD_REQUEST, "태그는 ${FeedTag.TITLE_MAX_LENGTH}자 이하여야 합니다"),
    TAG_INVALID_FORMAT("FD107", HttpStatus.BAD_REQUEST, "태그는 한글, 영문, 숫자만 사용할 수 있습니다"),
    IMAGE_INVALID_DIMENSIONS("FD108", HttpStatus.BAD_REQUEST, "이미지 크기가 유효하지 않습니다"),
    SEARCH_KEYWORD_EMPTY("FD109", HttpStatus.BAD_REQUEST, "검색어를 입력해주세요"),
    SEARCH_KEYWORD_TOO_LONG("FD110", HttpStatus.BAD_REQUEST, "검색어는 ${SearchKeyword.MAX_LENGTH}자 이하여야 합니다"),

    FEED_FORBIDDEN_ACCESS("FD201", HttpStatus.FORBIDDEN, "피드에 접근할 권한이 없습니다"),
    FEED_COMMENT_FORBIDDEN_ACCESS("FD202", HttpStatus.FORBIDDEN, "피드 댓글에 접근할 권한이 없습니다")
}
