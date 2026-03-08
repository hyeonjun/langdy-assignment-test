package com.example.langdy.infra.lock

import com.example.langdy.global.exception.LessonWaitingProcessingException
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDateTime

@Service
class LessonLockService(
    private val stringRedisTemplate: StringRedisTemplate,
) {
    fun <T> lockCreateLesson(teacherId: Long, startAt: LocalDateTime, block: () -> T) = lock("lesson:create:${teacherId}:${startAt}", block)

    private fun <T> lock(key: String, block: () -> T): T {
        val locked = stringRedisTemplate.opsForValue()
            .setIfAbsent(key, "lock", Duration.ofSeconds(3)) ?: false
        if (!locked) throw LessonWaitingProcessingException()
        try {
            return block()
        } finally {
            stringRedisTemplate.unlink(key)
        }
    }
}
