package com.goosesdream.golaping.common.exception

import com.goosesdream.golaping.common.enums.BaseResponseStatus
import org.springframework.http.ResponseEntity
import java.time.LocalDateTime

data class HttpErrorResponse(
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val status: Int,
    val error: String,
    val code: String,
    val message: String
) {

    companion object {
        fun fromStatus(status: BaseResponseStatus): HttpErrorResponse {
            return HttpErrorResponse(
                status = status.httpStatus.value(),
                error = status.httpStatus.name,
                code = status.name,
                message = status.message
            )
        }

        fun toResponseEntity(status: BaseResponseStatus): ResponseEntity<HttpErrorResponse> {
            return ResponseEntity
                .status(status.httpStatus)
                .body(
                    HttpErrorResponse(
                        status = status.httpStatus.value(),
                        error = status.httpStatus.name,
                        code = status.name,
                        message = status.message
                    )
                )
        }
    }
}
