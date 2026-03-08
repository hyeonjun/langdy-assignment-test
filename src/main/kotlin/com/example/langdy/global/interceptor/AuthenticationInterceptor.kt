package com.example.langdy.global.interceptor

import com.example.langdy.global.exception.LessonUnauthorizedException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

@Component
class AuthenticationInterceptor : HandlerInterceptor {

    companion object {
        const val USER_ID = "userId"
    }

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        val studentId = request.getHeader("X-Student-Id")
            ?: throw LessonUnauthorizedException()
        studentId.toLongOrNull() ?: throw LessonUnauthorizedException()
        request.setAttribute(USER_ID, studentId.toLong())
        return true
    }
}
