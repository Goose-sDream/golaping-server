package com.goosesdream.golaping.common.exception

import com.goosesdream.golaping.common.enums.BaseResponseStatus
import com.goosesdream.golaping.common.enums.WebSocketResponseStatus
import java.time.LocalDateTime

data class WebSocketErrorResponse(
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val code: String,
    val message: String) {
    companion object {
        fun fromStatus(status: WebSocketResponseStatus): WebSocketErrorResponse {
            return WebSocketErrorResponse(
                code = status.code,
                message = status.message
            )
        }

        fun fromBaseStatus(status: BaseResponseStatus): WebSocketErrorResponse {
            return WebSocketErrorResponse(
                code = status.name,
                message = status.message
            )
        }
    }
}
