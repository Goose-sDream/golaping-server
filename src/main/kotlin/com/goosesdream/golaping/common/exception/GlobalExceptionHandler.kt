package com.goosesdream.golaping.common.exception

import com.goosesdream.golaping.common.base.BaseException
import com.goosesdream.golaping.common.enums.BaseResponseStatus
import org.hibernate.exception.ConstraintViolationException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

@RestControllerAdvice
class GlobalExceptionHandler : ResponseEntityExceptionHandler() {

    @ExceptionHandler(ConstraintViolationException::class, DataIntegrityViolationException::class)
    protected fun handleDataException(): ResponseEntity<HttpErrorResponse> {
        return HttpErrorResponse.toResponseEntity(BaseResponseStatus.DUPLICATED_RESOURCE)
    }

    @ExceptionHandler(BaseException::class)
    protected fun handleCustomException(e: BaseException): ResponseEntity<HttpErrorResponse> {
        return HttpErrorResponse.toResponseEntity(e.status)
    }
}