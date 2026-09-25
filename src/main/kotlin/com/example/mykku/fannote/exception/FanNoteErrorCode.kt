package com.example.mykku.fannote.exception

import com.example.mykku.common.exception.DomainErrorCode
import org.springframework.http.HttpStatus

enum class FanNoteErrorCode(
    override val code: String,
    override val status: HttpStatus,
    override val message: String
) : DomainErrorCode {

    FAN_NOTE_NOT_FOUND("FN001", HttpStatus.NOT_FOUND, "덕질노트를 찾을 수 없습니다"),

    FAN_NOTE_PAGE_NOT_FOUND("FN002", HttpStatus.NOT_FOUND, "덕질노트 페이지를 찾을 수 없습니다"),

    INVALID_KEEP_IMAGE_URLS("FN101", HttpStatus.BAD_REQUEST, "유지할 페이지 이미지 목록에 이 덕질노트의 이미지가 아니거나 중복된 주소가 있습니다"),
    COVER_IMAGE_CONFLICT("FN102", HttpStatus.BAD_REQUEST, "새 커버 이미지와 커버 삭제를 함께 지정할 수 없습니다")
}
