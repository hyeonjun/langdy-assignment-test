package com.example.langdy.infra.event.lesson.subscriber

import com.example.langdy.infra.event.lesson.LessonBooked
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class LessonBookedEventSubscriber {

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun onLessonBooked(event: LessonBooked) {
        publishNotification(event)
    }

    private fun publishNotification(event: LessonBooked) {
        // TODO: NCloud SENS 카카오 알림톡 발송 구현
    }
}
