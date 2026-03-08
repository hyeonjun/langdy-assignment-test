package com.example.langdy.domain.repository

import com.example.langdy.domain.entity.Lesson
import com.example.langdy.domain.entity.QLesson
import com.example.langdy.domain.entity.Student
import com.example.langdy.domain.entity.Teacher
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime

interface LessonRepository : JpaRepository<Lesson, Long>, LessonRepositoryCustom

interface LessonRepositoryCustom {
    fun existsBookedByTeacherOrStudent(
        teacher: Teacher,
        student: Student,
        startAt: LocalDateTime,
        statuses: List<Lesson.Status>,
    ): Boolean

    fun existsBookedByStudent(
        student: Student,
        startAt: LocalDateTime,
        statuses: List<Lesson.Status>,
    ): Boolean
}

class LessonRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : LessonRepositoryCustom {

    private val lesson = QLesson.lesson

    override fun existsBookedByTeacherOrStudent(
        teacher: Teacher,
        student: Student,
        startAt: LocalDateTime,
        statuses: List<Lesson.Status>,
    ): Boolean {
        return queryFactory
            .selectOne()
            .from(lesson)
            .where(
                lesson.startAt.eq(startAt),
                lesson.status.`in`(statuses),
                lesson.teacher.eq(teacher).or(lesson.student.eq(student))
            )
            .fetchFirst() != null
    }

    override fun existsBookedByStudent(
        student: Student,
        startAt: LocalDateTime,
        statuses: List<Lesson.Status>,
    ): Boolean {
        return queryFactory
            .selectOne()
            .from(lesson)
            .where(
                lesson.startAt.eq(startAt),
                lesson.status.`in`(statuses),
                lesson.student.eq(student)
            )
            .fetchFirst() != null
    }
}
