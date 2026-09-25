package com.example.mykku.event.exception

import com.example.mykku.common.exception.DomainErrorCode
import org.springframework.http.HttpStatus

enum class EventErrorCode(
    override val code: String,
    override val status: HttpStatus,
    override val message: String
) : DomainErrorCode {
    EVENT_NOT_FOUND("EV001", HttpStatus.NOT_FOUND, "이벤트를 찾을 수 없습니다"),

    EVENT_IMAGE_LIMIT_EXCEEDED("EV101", HttpStatus.BAD_REQUEST, "이벤트 이미지는 최대 10개까지 등록할 수 있습니다"),
    EVENT_NOT_ACTIVE("EV102", HttpStatus.BAD_REQUEST, "진행 중인 이벤트가 아닙니다"),
    EVENT_NOT_EXPIRED("EV103", HttpStatus.BAD_REQUEST, "아직 종료되지 않은 이벤트입니다"),
    INVALID_KEEP_IMAGE_URLS("EV104", HttpStatus.BAD_REQUEST, "유지할 이미지 목록에 이 이벤트의 이미지가 아니거나 중복된 주소가 있습니다"),
    PERIOD_LOCKED_AFTER_WINNER_SELECTED("EV105", HttpStatus.BAD_REQUEST, "당첨자를 선정한 뒤에는 이벤트 기간을 바꿀 수 없습니다"),
    INVALID_WINNER_MEMBER_IDS("EV106", HttpStatus.BAD_REQUEST, "당첨자로 지정할 수 없는 회원 아이디가 있습니다"),
    INVALID_EVENT_PERIOD("EV107", HttpStatus.BAD_REQUEST, "이벤트 시작일은 종료일보다 늦을 수 없습니다"),

    ALREADY_PARTICIPATED("EV301", HttpStatus.CONFLICT, "이미 참여한 이벤트입니다"),
    EVENT_PARTICIPATION_NOT_FOUND("EV302", HttpStatus.NOT_FOUND, "이벤트 참여 정보를 찾을 수 없습니다"),
    PARTICIPATION_NOT_BELONG_TO_EVENT("EV303", HttpStatus.BAD_REQUEST, "해당 이벤트의 참여 정보가 아닙니다"),

    EMPTY_WINNERS("EV401", HttpStatus.BAD_REQUEST, "당첨자 목록은 비어있을 수 없습니다"),
    DUPLICATE_WINNER("EV402", HttpStatus.BAD_REQUEST, "중복된 당첨자가 있습니다"),
    WINNER_NOT_ANNOUNCED("EV403", HttpStatus.BAD_REQUEST, "아직 당첨자가 발표되지 않았습니다"),
    WINNER_ANNOUNCEMENT_NOT_FOUND("EV404", HttpStatus.NOT_FOUND, "이벤트 당첨자 발표 공지를 찾을 수 없습니다")
}
