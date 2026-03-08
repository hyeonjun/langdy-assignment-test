package com.example.langdy.api.controller

import com.example.langdy.api.service.LessonService
import com.example.langdy.api.service.result.TeacherResult
import com.example.langdy.config.SecurityConfig
import com.example.langdy.global.exception.LessonAlreadyBookedException
import com.example.langdy.global.exception.LessonEntityNotFoundException
import com.example.langdy.global.exception.LessonInvalidDateException
import com.example.langdy.infra.lock.LessonLockService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.time.LocalDateTime

@WebMvcTest(LessonController::class)
@Import(SecurityConfig::class)
class LessonControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @MockitoBean lateinit var lessonService: LessonService
    @MockitoBean lateinit var lessonLockService: LessonLockService

    private val studentId = 1L
    private val courseId = 1L
    private val validStartAt = LocalDateTime.now().plusDays(1).withMinute(0).withSecond(0).withNano(0)

    // ─── GET /api/v1/lessons/available-teachers ─────────────────────────────

    @Test
    fun `getAvailableTeachers - 정상 응답`() {
        val teachers = listOf(TeacherResult(id = 1L, name = "선생님A"), TeacherResult(id = 2L, name = "선생님B"))
        `when`(lessonService.getAvailableTeachers(studentId, courseId, validStartAt)).thenReturn(teachers)

        mockMvc.get("/api/v1/lessons/available-teachers") {
            param("courseId", courseId.toString())
            param("startAt", validStartAt.toString())
            header("X-Student-Id", studentId.toString())
        }.andExpect {
            status { isOk() }
            jsonPath("$.status") { value(0) }
            jsonPath("$.data.length()") { value(2) }
            jsonPath("$.data[0].name") { value("선생님A") }
        }
    }

    @Test
    fun `getAvailableTeachers - X-Student-Id 헤더 없으면 400`() {
        mockMvc.get("/api/v1/lessons/available-teachers") {
            param("courseId", courseId.toString())
            param("startAt", validStartAt.toString())
        }.andExpect {
            status { isOk() }
            jsonPath("$.status") { value(400) }
        }
    }

    @Test
    fun `getAvailableTeachers - courseId 없으면 검증 실패`() {
        mockMvc.get("/api/v1/lessons/available-teachers") {
            param("startAt", validStartAt.toString())
            header("X-Student-Id", studentId.toString())
        }.andExpect {
            status { isOk() }
            jsonPath("$.status") { value(400) }
        }
    }

    @Test
    fun `getAvailableTeachers - startAt 없으면 검증 실패`() {
        mockMvc.get("/api/v1/lessons/available-teachers") {
            param("courseId", courseId.toString())
            header("X-Student-Id", studentId.toString())
        }.andExpect {
            status { isOk() }
            jsonPath("$.status") { value(400) }
        }
    }

    @Test
    fun `getAvailableTeachers - 학생이 이미 예약한 경우 400`() {
        `when`(lessonService.getAvailableTeachers(studentId, courseId, validStartAt))
            .thenThrow(LessonAlreadyBookedException())

        mockMvc.get("/api/v1/lessons/available-teachers") {
            param("courseId", courseId.toString())
            param("startAt", validStartAt.toString())
            header("X-Student-Id", studentId.toString())
        }.andExpect {
            status { isOk() }
            jsonPath("$.status") { value(400) }
            jsonPath("$.error.code") { value("LS0005") }
        }
    }

    @Test
    fun `getAvailableTeachers - 유효하지 않은 시간이면 400`() {
        `when`(lessonService.getAvailableTeachers(studentId, courseId, validStartAt))
            .thenThrow(LessonInvalidDateException())

        mockMvc.get("/api/v1/lessons/available-teachers") {
            param("courseId", courseId.toString())
            param("startAt", validStartAt.toString())
            header("X-Student-Id", studentId.toString())
        }.andExpect {
            status { isOk() }
            jsonPath("$.status") { value(400) }
            jsonPath("$.error.code") { value("LS0004") }
        }
    }

    @Test
    fun `getAvailableTeachers - 존재하지 않는 엔티티면 404`() {
        `when`(lessonService.getAvailableTeachers(studentId, courseId, validStartAt))
            .thenThrow(LessonEntityNotFoundException())

        mockMvc.get("/api/v1/lessons/available-teachers") {
            param("courseId", courseId.toString())
            param("startAt", validStartAt.toString())
            header("X-Student-Id", studentId.toString())
        }.andExpect {
            status { isOk() }
            jsonPath("$.status") { value(404) }
            jsonPath("$.error.code") { value("LS0003") }
        }
    }

    @Test
    fun `getAvailableTeachers - 가용 선생님이 없으면 빈 배열 반환`() {
        `when`(lessonService.getAvailableTeachers(studentId, courseId, validStartAt)).thenReturn(emptyList())

        mockMvc.get("/api/v1/lessons/available-teachers") {
            param("courseId", courseId.toString())
            param("startAt", validStartAt.toString())
            header("X-Student-Id", studentId.toString())
        }.andExpect {
            status { isOk() }
            jsonPath("$.status") { value(0) }
            jsonPath("$.data.length()") { value(0) }
        }
    }
}
