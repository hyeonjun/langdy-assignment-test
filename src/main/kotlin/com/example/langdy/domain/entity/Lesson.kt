package com.example.langdy.domain.entity

import com.example.langdy.domain.base.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

@Entity
@Table(
    name = "lessons",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_lessons_teacher_start_status",
            columnNames = ["teacher_id", "start_at", "status"]
        ),
        UniqueConstraint(
            name = "uk_lessons_student_start_status",
            columnNames = ["student_id", "start_at", "status"]
        )
    ]
)
class Lesson (
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    var teacher: Teacher,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    var student: Student,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    var course: Course,

    @Column(name = "start_at", nullable = false)
    var startAt: LocalDateTime,

    @Column(name = "end_at", nullable = false)
    var endAt: LocalDateTime,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: Status = Status.BOOKED,
) : BaseEntity() {

    enum class Status {
        BOOKED, CANCELLED, DONE
    }

    fun cancel() {
        status = Status.CANCELLED
    }

    fun complete() {
        status = Status.DONE
    }

    companion object {
        fun of(
            teacher: Teacher,
            student: Student,
            course: Course,
            startAt: LocalDateTime,
            endAt: LocalDateTime,
        ) = Lesson(
            teacher = teacher,
            student = student,
            course = course,
            startAt = startAt,
            endAt = endAt,
        )
    }
}
