package com.example.langdy.infra.event.lesson.subscriber

import com.example.langdy.infra.event.lesson.LessonCreated
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class LessonCreatedEventSubscriber {

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handleLessonCreated(event: LessonCreated) {
        publishNotification(event)
    }

    private fun publishNotification(event: LessonCreated) {
        // TODO: NCloud SENS 카카오 알림톡 발송 구현
    }
}
