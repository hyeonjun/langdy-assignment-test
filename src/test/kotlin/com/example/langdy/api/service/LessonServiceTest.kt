package com.example.langdy.api.service

import com.example.langdy.domain.entity.Course
import com.example.langdy.domain.entity.Lesson
import com.example.langdy.domain.entity.Student
import com.example.langdy.domain.entity.Teacher
import com.example.langdy.domain.repository.CourseRepository
import com.example.langdy.domain.repository.LessonRepository
import com.example.langdy.domain.repository.StudentRepository
import com.example.langdy.domain.repository.TeacherRepository
import com.example.langdy.global.exception.LessonAlreadyBookedException
import com.example.langdy.global.exception.LessonEntityNotFoundException
import com.example.langdy.global.exception.LessonInvalidDateException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.any
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.context.ApplicationEventPublisher
import java.time.LocalDateTime
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class LessonServiceTest {

    @Mock lateinit var studentRepository: StudentRepository
    @Mock lateinit var teacherRepository: TeacherRepository
    @Mock lateinit var courseRepository: CourseRepository
    @Mock lateinit var lessonRepository: LessonRepository
    @Mock lateinit var eventPublisher: ApplicationEventPublisher

    @InjectMocks
    lateinit var lessonService: LessonService

    private val activeStatuses = listOf(Lesson.Status.BOOKED, Lesson.Status.DONE)
    private val futureTime = LocalDateTime.now().plusDays(1).withMinute(0).withSecond(0).withNano(0)

    private lateinit var student: Student
    private lateinit var teacher: Teacher
    private lateinit var course: Course

    @BeforeEach
    fun setUp() {
        student = Student(name = "학생", os = Student.Os.IOS)
        teacher = Teacher(name = "선생님")
        course = Course(name = "영어")
    }

    // ─── getAvailableTeachers ───────────────────────────────────────────────

    @Test
    fun `getAvailableTeachers - 정상 조회`() {
        val teacher2 = Teacher(name = "선생님2")
        `when`(studentRepository.findById(1L)).thenReturn(Optional.of(student))
        `when`(courseRepository.existsById(1L)).thenReturn(true)
        `when`(lessonRepository.existsBookedByStudent(student, futureTime, activeStatuses)).thenReturn(false)
        `when`(teacherRepository.findAvailableTeachers(futureTime, activeStatuses)).thenReturn(listOf(teacher, teacher2))

        val result = lessonService.getAvailableTeachers(1L, 1L, futureTime)

        assertEquals(2, result.size)
    }

    @Test
    fun `getAvailableTeachers - 분이 0도 30도 아니면 LessonInvalidDateException`() {
        val invalidTime = LocalDateTime.now().plusDays(1).withMinute(15)

        assertThrows(LessonInvalidDateException::class.java) {
            lessonService.getAvailableTeachers(1L, 1L, invalidTime)
        }
    }

    @Test
    fun `getAvailableTeachers - 과거 시간이면 LessonInvalidDateException`() {
        val pastTime = LocalDateTime.now().minusDays(1).withMinute(0).withSecond(0).withNano(0)

        assertThrows(LessonInvalidDateException::class.java) {
            lessonService.getAvailableTeachers(1L, 1L, pastTime)
        }
    }

    @Test
    fun `getAvailableTeachers - 존재하지 않는 studentId이면 LessonEntityNotFoundException`() {
        `when`(studentRepository.findById(999L)).thenReturn(Optional.empty())

        assertThrows(LessonEntityNotFoundException::class.java) {
            lessonService.getAvailableTeachers(999L, 1L, futureTime)
        }
    }

    @Test
    fun `getAvailableTeachers - 존재하지 않는 courseId이면 LessonEntityNotFoundException`() {
        `when`(studentRepository.findById(1L)).thenReturn(Optional.of(student))
        `when`(courseRepository.existsById(999L)).thenReturn(false)

        assertThrows(LessonEntityNotFoundException::class.java) {
            lessonService.getAvailableTeachers(1L, 999L, futureTime)
        }
    }

    @Test
    fun `getAvailableTeachers - 학생이 이미 해당 시간에 예약했으면 LessonAlreadyBookedException`() {
        `when`(studentRepository.findById(1L)).thenReturn(Optional.of(student))
        `when`(courseRepository.existsById(1L)).thenReturn(true)
        `when`(lessonRepository.existsBookedByStudent(student, futureTime, activeStatuses)).thenReturn(true)

        assertThrows(LessonAlreadyBookedException::class.java) {
            lessonService.getAvailableTeachers(1L, 1L, futureTime)
        }
    }

    @Test
    fun `getAvailableTeachers - 모든 선생님이 예약된 경우 빈 리스트 반환`() {
        `when`(studentRepository.findById(1L)).thenReturn(Optional.of(student))
        `when`(courseRepository.existsById(1L)).thenReturn(true)
        `when`(lessonRepository.existsBookedByStudent(student, futureTime, activeStatuses)).thenReturn(false)
        `when`(teacherRepository.findAvailableTeachers(futureTime, activeStatuses)).thenReturn(emptyList())

        val result = lessonService.getAvailableTeachers(1L, 1L, futureTime)

        assertEquals(0, result.size)
    }

    // ─── createLesson ───────────────────────────────────────────────────────

    @Test
    fun `createLesson - 분이 0도 30도 아니면 LessonInvalidDateException`() {
        val invalidTime = LocalDateTime.now().plusDays(1).withMinute(15)

        assertThrows(LessonInvalidDateException::class.java) {
            lessonService.createLesson(1L, 1L, 1L, invalidTime)
        }
    }

    @Test
    fun `createLesson - 과거 시간이면 LessonInvalidDateException`() {
        val pastTime = LocalDateTime.now().minusDays(1).withMinute(0).withSecond(0).withNano(0)

        assertThrows(LessonInvalidDateException::class.java) {
            lessonService.createLesson(1L, 1L, 1L, pastTime)
        }
    }

    @Test
    fun `createLesson - 존재하지 않는 studentId이면 LessonEntityNotFoundException`() {
        `when`(studentRepository.findById(999L)).thenReturn(Optional.empty())

        assertThrows(LessonEntityNotFoundException::class.java) {
            lessonService.createLesson(999L, 1L, 1L, futureTime)
        }
    }

    @Test
    fun `createLesson - 존재하지 않는 teacherId이면 LessonEntityNotFoundException`() {
        `when`(studentRepository.findById(1L)).thenReturn(Optional.of(student))
        `when`(teacherRepository.findById(999L)).thenReturn(Optional.empty())

        assertThrows(LessonEntityNotFoundException::class.java) {
            lessonService.createLesson(1L, 1L, 999L, futureTime)
        }
    }

    @Test
    fun `createLesson - 존재하지 않는 courseId이면 LessonEntityNotFoundException`() {
        `when`(studentRepository.findById(1L)).thenReturn(Optional.of(student))
        `when`(teacherRepository.findById(1L)).thenReturn(Optional.of(teacher))
        `when`(courseRepository.findById(999L)).thenReturn(Optional.empty())

        assertThrows(LessonEntityNotFoundException::class.java) {
            lessonService.createLesson(1L, 999L, 1L, futureTime)
        }
    }

    @Test
    fun `createLesson - 선생님 또는 학생이 이미 해당 시간에 예약했으면 LessonAlreadyBookedException`() {
        `when`(studentRepository.findById(1L)).thenReturn(Optional.of(student))
        `when`(teacherRepository.findById(1L)).thenReturn(Optional.of(teacher))
        `when`(courseRepository.findById(1L)).thenReturn(Optional.of(course))
        `when`(lessonRepository.existsBookedByTeacherOrStudent(teacher, student, futureTime, activeStatuses)).thenReturn(true)

        assertThrows(LessonAlreadyBookedException::class.java) {
            lessonService.createLesson(1L, 1L, 1L, futureTime)
        }
    }

    @Test
    fun `createLesson - 정상 생성 후 lessonId 반환`() {
        val savedLesson = Lesson.of(teacher = teacher, student = student, course = course, startAt = futureTime, endAt = futureTime.plusMinutes(20))
        `when`(studentRepository.findById(1L)).thenReturn(Optional.of(student))
        `when`(teacherRepository.findById(1L)).thenReturn(Optional.of(teacher))
        `when`(courseRepository.findById(1L)).thenReturn(Optional.of(course))
        `when`(lessonRepository.existsBookedByTeacherOrStudent(teacher, student, futureTime, activeStatuses)).thenReturn(false)
        `when`(lessonRepository.save(any(Lesson::class.java))).thenReturn(savedLesson)

        val lessonId = lessonService.createLesson(1L, 1L, 1L, futureTime)

        assertEquals(savedLesson.id, lessonId)
        verify(eventPublisher).publishEvent(any(Any::class.java))
    }
}
