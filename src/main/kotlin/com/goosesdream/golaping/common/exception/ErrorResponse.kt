package com.goosesdream.golaping.common.exception

import com.goosesdream.golaping.common.base.BaseResponseStatus
import org.springframework.http.ResponseEntity
import java.time.LocalDateTime

data class ErrorResponse(
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val status: Int,
    val error: String,
    val code: String,
    val message: String
) {

    companion object {
        fun toResponseEntity(status: BaseResponseStatus): ResponseEntity<ErrorResponse> {
            return ResponseEntity
                .status(status.httpStatus)
                .body(
                    ErrorResponse(
                        status = status.httpStatus.value(),
                        error = status.httpStatus.name,
                        code = status.name,
                        message = status.message
                    )
                )
        }
    }
}
