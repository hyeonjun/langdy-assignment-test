package com.example.langdy.api.controller.response

import com.example.langdy.api.service.result.TeacherResult

data class FindAvailableTeacherResponse(
    val id: Long,
    val name: String,
) {
    companion object Companion {
        fun from(result: TeacherResult) = FindAvailableTeacherResponse(
            id = result.id,
            name = result.name,
        )
    }
}
