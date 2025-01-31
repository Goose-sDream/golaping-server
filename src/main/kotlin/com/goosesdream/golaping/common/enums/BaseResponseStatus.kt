package com.goosesdream.golaping.common.enums

import org.springframework.http.HttpStatus

enum class BaseResponseStatus(
    val isSuccess: Boolean,
    val httpStatus: HttpStatus,
    val message: String
) {
    /**
     * 요청 성공
     */
    SUCCESS(true, HttpStatus.OK, "요청에 성공하였습니다."),

    /**
     * Request 오류
     */
    // user
    INVALID_USER(false, HttpStatus.BAD_REQUEST, "유효하지 않은 user 입니다."),
    NICKNAME_ALREADY_EXISTS_IN_VOTE(false, HttpStatus.BAD_REQUEST, "해당 투표에서 이미 존재하는 닉네임입니다."),
    INVALID_NICKNAME(false, HttpStatus.BAD_REQUEST, "유효하지 않은 닉네임 형식입니다."),
    USER_ALREADY_PARTICIPANT(false, HttpStatus.BAD_REQUEST, "이미 참여한 사용자입니다."),
    NOT_CREATOR(false, HttpStatus.FORBIDDEN, "투표 생성자가 아닙니다."),

    // participant
    PARTICIPANT_NOT_FOUND(false, HttpStatus.BAD_REQUEST, "존재하지 않는 참여자입니다."),

    // vote
    INVALID_VOTE_TYPE(false, HttpStatus.BAD_REQUEST, "투표 타입이 올바르지 않습니다."),
    INVALID_TIME_LIMIT(false, HttpStatus.BAD_REQUEST, "투표 제한 시간은 0분보다 커야합니다."),
    ALREADY_EXIST_CHANNEL(false, HttpStatus.CONFLICT, "이미 존재하는 채널입니다."),
    VOTE_NOT_FOUND(false, HttpStatus.BAD_REQUEST, "존재하지 않는 투표입니다."),
    EXPIRED_VOTE(false, HttpStatus.BAD_REQUEST, "만료된 투표입니다."),

    // vote option
    INVALID_OPTION_TEXT(false, HttpStatus.BAD_REQUEST, "유효하지 않은 option text 입니다."),
    INVALID_OPTION_COLOR(false, HttpStatus.BAD_REQUEST, "유효하지 않은 option color 입니다."),
    VOTE_OPTION_NOT_FOUND(false, HttpStatus.BAD_REQUEST, "투표 옵션이 존재하지 않습니다."),

    // session
    INVALID_SESSION(false, HttpStatus.BAD_REQUEST, "유효하지 않은 세션입니다."),
    UNAUTHORIZED(false, HttpStatus.UNAUTHORIZED, "인증되지 않은 사용자입니다."),
    INVALID_VOTE_UUID(false, HttpStatus.BAD_REQUEST, "유효하지 않은 vote uuid 입니다."),
    MISSING_SESSION_ID(false, HttpStatus.BAD_REQUEST, "세션 ID가 없습니다."),

    /**
     * Response 오류
     */
    // user

    // vote

    /**
     * DB, Server 오류
     */
    DATABASE_ERROR(false, HttpStatus.INTERNAL_SERVER_ERROR, "데이터베이스 연결에 실패했습니다."),
    SERVER_ERROR(false, HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다."),
    DUPLICATED_RESOURCE(false, HttpStatus.CONFLICT, "데이터가 이미 존재합니다.");
}
