package com.example.langdy.api.service.result

import com.example.langdy.domain.entity.Lesson
import java.time.LocalDateTime

data class LessonResult(
    val lessonId: Long,

    val courseId: Long,
    val courseName: String,

    val teacherId: Long,
    val teacherName: String,

    val studentId: Long,
    val studentName: String,

    val startAt: LocalDateTime,
    val endAt: LocalDateTime,
    val status: Lesson.Status,
) {
    companion object Companion {
        fun from(lesson: Lesson) = LessonResult(
            lessonId = lesson.id,
            courseId = lesson.course.id,
            courseName = lesson.course.name,
            teacherId = lesson.teacher.id,
            teacherName = lesson.teacher.name,
            studentId = lesson.student.id,
            studentName = lesson.student.name,
            startAt = lesson.startAt,
            endAt = lesson.endAt,
            status = lesson.status,
        )
    }
}
