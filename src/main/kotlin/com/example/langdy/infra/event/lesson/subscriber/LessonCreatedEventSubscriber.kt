package com.example.langdy.infra.event.lesson.subscriber

import com.example.langdy.infra.event.lesson.LessonCreated
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class LessonCreatedEventSubscriber {

    private val logger = KotlinLogging.logger { }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handleLessonCreated(event: LessonCreated) {
        logger.info { "LessonCreatedEvent 수신 - lessonId=${event.lessonId}, studentId=${event.studentId}" }
        publishNotification(event)
    }

    private fun publishNotification(event: LessonCreated) {
        // 1. 데이터 재조회
        //    - lessonRepository.findById(event.lessonId) 로 Lesson 조회
        //    - lesson.student, lesson.teacher 에 접근해 LAZY 연관 초기화
        //      → 수신자: student.phone, student.name
        //      → 본문 변수: startAt, teacher.name

        // 2. AlimtalkMessageBuilder.sendNotification(phoneNumber, templateCode, reservedTime, title, content, buttons) 으로 ExternalMessage 조립
        //    val message = AlimtalkMessageBuilder().apply {
        //        this.templateCode = templateCode
        //        this.reservedTime = reservedTime
        //        this.addMessage(
        //            to = phoneNumber,
        //            title = title,
        //            content = content.trimMargin(),
        //            buttons = buttons
        //        )
        //    }.build()
        //    externalMessageApiService.sendAlimtalk(message)

        // 3. ExternalMessageApiService.sendAlimtalk(message) 호출
        //    - 내부적으로 NCloud SENS API 에 HTTP POST 요청
        //      POST https://sens.apigw.ntruss.com/alimtalk/v2/services/{serviceId}/messages
        //      헤더: HMAC-SHA256 서명 (x-ncp-apigw-timestamp, x-ncp-iam-access-key, x-ncp-apigw-signature-v2)
        //      바디: { plusFriendId, templateCode, messages: [{ to, content }] }
        //    - @Retryable: 실패 시 최대 3회 재시도, 지수 백오프 (1s → 2s → 4s)
        //    - @Recover: 3회 소진 후 최종 실패 시 SlackNotifier.sendAlert() 로 슬랙 알림 발송
        //      → 발송 실패가 예약 성공 이력에 영향을 주지 않도록 예외를 삼킴
    }
}
