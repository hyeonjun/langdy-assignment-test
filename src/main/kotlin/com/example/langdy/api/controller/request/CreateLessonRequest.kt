package com.example.langdy.api.controller.request

import com.fasterxml.jackson.annotation.JsonFormat
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime

data class CreateLessonRequest(
    @field:NotNull(message = "courseId는 필수입니다.")
    val courseId: Long,

    @field:NotNull(message = "teacherId는 필수입니다.")
    val teacherId: Long,

    @field:NotNull(message = "startAt은 필수입니다.")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    val startAt: LocalDateTime,
)
