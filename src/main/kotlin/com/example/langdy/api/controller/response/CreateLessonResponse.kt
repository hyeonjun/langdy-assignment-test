package com.example.langdy.api.controller.response

import com.example.langdy.api.service.result.LessonResult
import com.example.langdy.domain.entity.Lesson
import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDateTime

data class CreateLessonResponse(
    val lessonId: Long,

    val courseId: Long,
    val courseName: String,

    val teacherId: Long,
    val teacherName: String,

    val studentId: Long,
    val studentName: String,

    @field:JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    val startAt: LocalDateTime,
    @field:JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    val endAt: LocalDateTime,
    val status: Lesson.Status,
) {
    companion object Companion {
        fun from(result: LessonResult) = CreateLessonResponse(
            lessonId = result.lessonId,
            courseId = result.courseId,
            courseName = result.courseName,
            teacherId = result.teacherId,
            teacherName = result.teacherName,
            studentId = result.studentId,
            studentName = result.studentName,
            startAt = result.startAt,
            endAt = result.endAt,
            status = result.status,
        )
    }
}
