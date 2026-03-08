package com.example.langdy.global.exception

import com.example.langdy.global.response.ResponseModel
import jakarta.servlet.http.HttpServletRequest
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(ApplicationException::class)
    fun handleApplicationException(request: HttpServletRequest, e: ApplicationException): ResponseModel<Any> {
        return ResponseModel.error(
            status = ResponseModel.Status(e.errorType.status),
            code = e.errorType.errorCode,
            message = e.customMessage ?: e.errorType.messageCode(),
        )
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(request: HttpServletRequest, e: MethodArgumentNotValidException): ResponseModel<Any> {
        val errorMessages = e.bindingResult.allErrors.joinToString(",") { it.defaultMessage.toString() }
        return ResponseModel.error(
            status = ResponseModel.Status.BAD_REQUEST,
            message = errorMessages,
        )
    }

    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleDataIntegrityViolationException(request: HttpServletRequest, e: DataIntegrityViolationException): ResponseModel<Any> {
        return ResponseModel.error(
            status = ResponseModel.Status.ERROR,
            message = e.message ?: "DB ERROR",
        )
    }
}
