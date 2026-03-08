package com.example.langdy.domain.repository

import com.example.langdy.domain.entity.Lesson
import com.example.langdy.domain.entity.QLesson
import com.example.langdy.domain.entity.QTeacher
import com.example.langdy.domain.entity.Teacher
import com.querydsl.jpa.JPAExpressions
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime

interface TeacherRepository : JpaRepository<Teacher, Long>, TeacherRepositoryCustom

interface TeacherRepositoryCustom {
    fun findAvailableTeachers(startAt: LocalDateTime, statuses: List<Lesson.Status>): List<Teacher>
}

class TeacherRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : TeacherRepositoryCustom {

    override fun findAvailableTeachers(startAt: LocalDateTime, statuses: List<Lesson.Status>): List<Teacher> {
        val lesson = QLesson.lesson
        val teacher = QTeacher.teacher
        return queryFactory
            .selectFrom(teacher)
            .where(
                teacher.id.notIn(
                    JPAExpressions.select(lesson.teacher.id)
                        .from(lesson)
                        .where(
                            lesson.startAt.eq(startAt),
                            lesson.status.`in`(statuses)
                        )
                )
            )
            .fetch()
    }
}
