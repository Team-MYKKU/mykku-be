package com.example.mykku.contest.exception

import com.example.mykku.common.exception.DomainErrorCode
import com.example.mykku.contest.domain.entity.Contest
import com.example.mykku.contest.domain.entity.ContestTag
import org.springframework.http.HttpStatus

enum class ContestErrorCode(
    override val code: String,
    override val status: HttpStatus,
    override val message: String
) : DomainErrorCode {

    CONTEST_NOT_FOUND("CN001", HttpStatus.NOT_FOUND, "콘테스트를 찾을 수 없습니다"),
    CONTEST_WINNER_NOT_FOUND("CN002", HttpStatus.NOT_FOUND, "콘테스트 수상자를 찾을 수 없습니다"),
    PARTICIPATION_NOT_FOUND("CN003", HttpStatus.NOT_FOUND, "콘테스트 참여 정보를 찾을 수 없습니다"),
    WINNER_ANNOUNCEMENT_NOT_FOUND("CN004", HttpStatus.NOT_FOUND, "콘테스트 수상자 발표 공지를 찾을 수 없습니다"),

    CONTEST_IMAGE_LIMIT_EXCEEDED("CN101", HttpStatus.BAD_REQUEST, "콘테스트 이미지는 ${Contest.IMAGE_MAX_COUNT}개 이하여야 합니다"),
    CONTEST_TAG_LIMIT_EXCEEDED("CN102", HttpStatus.BAD_REQUEST, "콘테스트 태그는 ${Contest.TAG_MAX_COUNT}개 이하여야 합니다"),
    TAG_TITLE_TOO_LONG("CN104", HttpStatus.BAD_REQUEST, "태그는 ${ContestTag.TITLE_MAX_LENGTH}자 이하여야 합니다"),
    TAG_INVALID_FORMAT("CN105", HttpStatus.BAD_REQUEST, "태그는 한글, 영문, 숫자만 사용할 수 있습니다"),
    INVALID_WINNER_RANK("CN106", HttpStatus.BAD_REQUEST, "수상 순위는 1, 2, 3 중 하나여야 합니다"),
    INVALID_WINNER_COUNT("CN107", HttpStatus.BAD_REQUEST, "수상자는 1~3명이어야 합니다"),
    DUPLICATE_WINNER_RANK("CN108", HttpStatus.BAD_REQUEST, "중복된 수상 순위가 있습니다"),
    PARTICIPATION_NOT_BELONG_TO_CONTEST("CN109", HttpStatus.BAD_REQUEST, "해당 참여작은 이 콘테스트에 속하지 않습니다"),
    CONTEST_NOT_EXPIRED("CN110", HttpStatus.BAD_REQUEST, "종료되지 않은 콘테스트입니다"),
    WINNER_NOT_ANNOUNCED("CN111", HttpStatus.BAD_REQUEST, "아직 수상자가 발표되지 않았습니다"),
    CONTEST_TAG_REQUIRED("CN113", HttpStatus.BAD_REQUEST, "콘테스트 태그는 1개 이상이어야 합니다"),
    DUPLICATE_WINNER_PARTICIPATION("CN114", HttpStatus.BAD_REQUEST, "같은 참여작을 중복해서 선정할 수 없습니다"),
    DUPLICATE_WINNER_MEMBER("CN115", HttpStatus.BAD_REQUEST, "한 회원은 한 콘테스트에서 한 번만 수상할 수 있습니다"),

    NOT_WINNER_OWNER("CN201", HttpStatus.FORBIDDEN, "수상 소감을 수정할 권한이 없습니다"),

    ALREADY_PARTICIPATED_WITH_FEED("CN301", HttpStatus.CONFLICT, "이 피드로 이미 참여한 콘테스트입니다")
}
