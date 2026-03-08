package com.example.langdy.global.response

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonValue
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ResponseModel<T>(
    val status: Status,
    val message: String? = null,
    val data: T? = null,
    val error: Error? = null,
) {
    data class Status(@JsonValue val code: Int) {
        companion object {
            @JvmField
            val SUCCESS = Status(0)

            @JvmField
            val ERROR = Status(500)

            @JvmField
            val BAD_REQUEST = Status(400)

            @JvmField
            val AUTHENTICATION_REQUIRED = Status(401)

            @JvmField
            val NOT_FOUND = Status(404)
        }
    }

    data class Error(val code: String, val message: String)

    companion object {
        fun success(message: String? = null) = ResponseModel<Any>(Status.SUCCESS, message)

        fun <T> success(data: T) = ResponseModel(
            status = Status.SUCCESS,
            data = data,
        )

        fun error(status: Status = Status.ERROR, code: String, message: String) = ResponseModel<Any>(
            status = status,
            message = message,
            error = Error(code = code, message = message),
        )

        fun error(status: Status = Status.ERROR, message: String) = ResponseModel<Any>(status, message)
    }
}
