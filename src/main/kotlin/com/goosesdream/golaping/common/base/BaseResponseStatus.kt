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
    INVALID_USER(false, HttpStatus.BAD_REQUEST, "유저 정보가 올바르지 않습니다."),

    // vote
    INVALID_VOTE_TYPE(false, HttpStatus.BAD_REQUEST, "투표 타입이 올바르지 않습니다."),
    INVALID_TIME_LIMIT(false, HttpStatus.BAD_REQUEST, "투표 제한 시간이 유효하지 않습니다."),

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
