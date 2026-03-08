package com.example.langdy.domain.repository

import com.example.langdy.domain.entity.Lesson
import com.example.langdy.domain.entity.Teacher
import com.example.langdy.domain.entity.Student
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime

interface LessonRepository : JpaRepository<Lesson, Long> {

    fun existsByTeacherAndStartAtAndStatusIn(
        teacher: Teacher,
        startAt: LocalDateTime,
        statuses: List<Lesson.Status>,
    ): Boolean

    fun existsByStudentAndStartAtAndStatusIn(
        student: Student,
        startAt: LocalDateTime,
        statuses: List<Lesson.Status>,
    ): Boolean
}
