package com.example.langdy.infra.event.lesson

data class LessonBooked(
    val lessonId: Long,
    val studentId: Long,
    val teacherId: Long,
    val courseId: Long,
)