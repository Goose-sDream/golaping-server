package com.goosesdream.golaping.common.enums

enum class WebSocketResponseStatus(
    val isSuccess: Boolean,
    val code: String,
    val message: String
) {

    /**
     * 요청 성공
     */
    SUCCESS(true, "SUCCESS", "요청에 성공하였습니다."),

    /**
     * Request 오류
     */
    // vote
    MISSING_VOTE_UUID(false, "MISSING_VOTE_UUID", "투표 ID가 누락되었습니다."),
    VOTE_NOT_FOUND(false, "VOTE_NOT_FOUND", "존재하지 않는 투표입니다."),
    INVALID_VOTE_UUID(false, "INVALID_VOTE_UUID", "유효하지 않은 vote uuid 입니다."),
    EXPIRED_VOTE(false, "EXPIRED_VOTE", "종료된 투표입니다."),
    USER_VOTE_LIMIT_EXCEEDED(false, "USER_VOTE_LIMIT_EXCEEDED", "투표 제한 횟수를 초과하였습니다."),

    // user
    MISSING_NICKNAME(false, "MISSING_NICKNAME", "닉네임이 누락되었습니다."),

    // websocket session
    MISSING_WEBSOCKET_SESSION_ID(false, "MISSING_WEBSOCKET_SESSION_ID", "웹소켓 세션 ID가 누락되었습니다."),
    MISSING_PRINCIPAL(false, "MISSING_PRINCIPAL", "유저를 식별하는 Principal 구현체가 없습니다."),

    // vote option
    MISSING_SELECTED_OPTION(false, "MISSING_SELECTED_OPTION", "선택된 옵션이 누락되었습니다."),
    VOTE_OPTION_NOT_FOUND(false, "VOTE_OPTION_NOT_FOUND", "존재하지 않는 투표 옵션입니다."),

    // general
    GENERAL_ERROR(false, "GENERAL_ERROR", "알 수 없는 오류가 발생했습니다.")
}
