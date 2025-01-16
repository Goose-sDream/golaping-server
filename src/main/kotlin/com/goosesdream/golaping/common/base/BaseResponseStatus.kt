package com.goosesdream.golaping.common.base

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

    // vote
    INVALID_VOTE_TYPE(false, HttpStatus.BAD_REQUEST, "투표 타입이 올바르지 않습니다."),
    INVALID_TIME_LIMIT(false, HttpStatus.BAD_REQUEST, "투표 제한 시간이 유효하지 않습니다."),
    ALREADY_EXIST_CHANNEL(false, HttpStatus.BAD_REQUEST, "이미 존재하는 채널입니다."),

    // session
    INVALID_SESSION(false, HttpStatus.BAD_REQUEST, "유효하지 않은 세션입니다."),
    UNAUTHORIZED(false, HttpStatus.UNAUTHORIZED, "인증되지 않은 사용자입니다."),
    INVALID_VOTE_UUID(false, HttpStatus.BAD_REQUEST, "유효하지 않은 vote uuid입니다."),
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
