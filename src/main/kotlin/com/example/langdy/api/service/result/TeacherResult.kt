package com.example.langdy.api.service.result

import com.example.langdy.domain.entity.Teacher

data class TeacherResult(
    val id: Long,
    val name: String,
) {
    companion object Companion {
        fun from(teacher: Teacher) = TeacherResult(
            id = teacher.id,
            name = teacher.name,
        )
    }
}
