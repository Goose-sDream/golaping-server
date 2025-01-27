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
    MISSING_VOTE_UUID(false, "MISSING_VOTE_UUID", "투표 ID가 누락되었습니다."),
    NICKNAME_ALREADY_EXISTS_IN_VOTE(false, "NICKNAME_ALREADY_EXISTS_IN_VOTE", "해당 투표에서 이미 존재하는 닉네임입니다."),
    VOTE_NOT_FOUND(false, "VOTE_NOT_FOUND", "존재하지 않는 투표입니다."),
    INVALID_VOTE_UUID(false, "INVALID_VOTE_UUID", "유효하지 않은 vote uuid입니다."),
    EXPIRED_VOTE(false, "EXPIRED_VOTE", "종료된 투표입니다."),
    INVALID_REQUEST_FORMAT(false, "INVALID_REQUEST_FORMAT", "요청 형식이 올바르지 않습니다."),
    UNKNOWN_ACTION(false, "UNKNOWN_ACTION", "알 수 없는 요청 action 입니다."),
    MISSING_NICKNAME(false, "MISSING_NICKNAME", "닉네임이 누락되었습니다."),
    MISSING_WEBSOCKET_SESSION_ID(false, "MISSING_WEBSOCKET_SESSION_ID", "웹소켓 세션 ID가 누락되었습니다."),
    MISSING_SESSION_ID(false, "MISSING_SESSION_ID", "세션 ID가 누락되었습니다."),

    GENERAL_ERROR(false, "GENERAL_ERROR", "알 수 없는 오류가 발생했습니다.")
}
