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

    GENERAL_ERROR(false, "GENERAL_ERROR", "알 수 없는 오류가 발생했습니다.")
}
