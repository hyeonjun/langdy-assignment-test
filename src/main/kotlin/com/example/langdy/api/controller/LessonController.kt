package com.example.langdy.api.controller

import com.example.langdy.api.controller.request.FindAvailableTeachersRequest
import com.example.langdy.api.controller.request.CreateLessonRequest
import com.example.langdy.api.controller.response.FindAvailableTeacherResponse
import com.example.langdy.api.controller.response.CreateLessonResponse
import com.example.langdy.api.service.LessonService
import com.example.langdy.global.interceptor.AuthenticationInterceptor
import com.example.langdy.global.response.ResponseModel
import com.example.langdy.infra.lock.LessonLockService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/lessons")
class LessonController(
    private val lessonService: LessonService,
    private val lessonLockService: LessonLockService,
) {

    @PostMapping
    fun createLesson(
        @Valid @RequestBody request: CreateLessonRequest,
        @RequestAttribute(AuthenticationInterceptor.USER_ID) studentId: Long,
    ): ResponseModel<CreateLessonResponse> {
        val lessonId = lessonLockService.lockCreateLesson(request.teacherId, request.startAt) {
            lessonService.createLesson(studentId, request.courseId, request.teacherId, request.startAt)
        }
        val result = lessonService.getLessonDetail(lessonId)
        return ResponseModel.success(CreateLessonResponse.from(result))
    }

    @GetMapping("/available-teachers")
    fun getAvailableTeachers(
        @Valid @ModelAttribute request: FindAvailableTeachersRequest,
        @RequestAttribute(AuthenticationInterceptor.USER_ID) studentId: Long,
    ): ResponseModel<List<FindAvailableTeacherResponse>> {
        val results = lessonService.getAvailableTeachers(studentId, request.courseId, request.startAt)
        return ResponseModel.success(results.map { FindAvailableTeacherResponse.from(it) })
    }
}
