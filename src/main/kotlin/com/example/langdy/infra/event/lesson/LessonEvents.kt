package com.example.langdy.infra.event.lesson

data class LessonCreated(
    val lessonId: Long,
    val studentId: Long,
    val teacherId: Long,
    val courseId: Long,
)