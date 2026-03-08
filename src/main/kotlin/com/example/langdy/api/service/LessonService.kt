package com.example.langdy.api.service

import com.example.langdy.api.service.result.LessonResult
import com.example.langdy.api.service.result.TeacherResult
import com.example.langdy.domain.entity.Lesson
import com.example.langdy.domain.repository.CourseRepository
import com.example.langdy.domain.repository.LessonRepository
import com.example.langdy.domain.repository.StudentRepository
import com.example.langdy.domain.repository.TeacherRepository
import com.example.langdy.global.exception.LessonAlreadyBookedException
import com.example.langdy.global.exception.LessonEntityNotFoundException
import com.example.langdy.global.exception.LessonInvalidDateException
import com.example.langdy.infra.event.lesson.LessonCreated
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class LessonService(
    private val studentRepository: StudentRepository,
    private val teacherRepository: TeacherRepository,
    private val courseRepository: CourseRepository,
    private val lessonRepository: LessonRepository,
    private val eventPublisher: ApplicationEventPublisher,
) {
    companion object {
        private const val LESSON_DURATION_MINUTES = 20L
    }

    @Transactional
    fun createLesson(
        studentId: Long,
        courseId: Long,
        teacherId: Long,
        startAt: LocalDateTime,
    ): Long {
        validateStartTime(startAt)
        val endAt = startAt.plusMinutes(LESSON_DURATION_MINUTES)

        val student = studentRepository.findById(studentId).orElseThrow { LessonEntityNotFoundException() }
        val teacher = teacherRepository.findById(teacherId).orElseThrow { LessonEntityNotFoundException() }
        val course = courseRepository.findById(courseId).orElseThrow { LessonEntityNotFoundException() }

        val activeStatuses = listOf(Lesson.Status.BOOKED, Lesson.Status.DONE)

        if (lessonRepository.existsBookedByTeacherOrStudent(teacher, student, startAt, activeStatuses)) {
            throw LessonAlreadyBookedException()
        }

        val lesson = lessonRepository.save(
            Lesson.of(
                teacher = teacher,
                student = student,
                course = course,
                startAt = startAt,
                endAt = endAt,
            )
        )

        eventPublisher.publishEvent(
            LessonCreated(
                lessonId = lesson.id,
                studentId = lesson.student.id,
                teacherId = lesson.teacher.id,
                courseId = lesson.course.id,
            )
        )

        return lesson.id
    }

    @Transactional(readOnly = true)
    fun getLessonDetail(lessonId: Long): LessonResult {
        val lesson = lessonRepository.findById(lessonId).orElseThrow { LessonEntityNotFoundException() }
        return LessonResult.from(lesson)
    }

    @Transactional(readOnly = true)
    fun getAvailableTeachers(studentId: Long, courseId: Long, startAt: LocalDateTime): List<TeacherResult> {
        validateStartTime(startAt)

        val student = studentRepository.findById(studentId).orElseThrow { LessonEntityNotFoundException() }

        if (!courseRepository.existsById(courseId)) throw LessonEntityNotFoundException()

        val activeStatuses = listOf(Lesson.Status.BOOKED, Lesson.Status.DONE)

        if (lessonRepository.existsBookedByStudent(student, startAt, activeStatuses)) {
            throw LessonAlreadyBookedException()
        }

        val teachers = teacherRepository.findAvailableTeachers(startAt, activeStatuses)

        return teachers.map { TeacherResult.from(it) }
    }

    private fun validateStartTime(startAt: LocalDateTime) {
        if (startAt.minute != 0 && startAt.minute != 30) throw LessonInvalidDateException()
        if (startAt.isBefore(LocalDateTime.now())) throw LessonInvalidDateException()
    }
}
