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

    // ─── Spring Event 한계 및 Outbox + Kafka 전환 전략 ────────────────────────
    //
    // [Spring Event 의 한계]
    //
    // 현재 구조(@TransactionalEventListener + @Async)는 단일 서버 인메모리 이벤트이므로
    // 아래 상황에서 이벤트 유실이 발생한다.
    //
    //   1. AFTER_COMMIT 직후, @Async 스레드 실행 전 서버가 재시작되면 이벤트 소멸
    //   2. 앱 인스턴스가 여러 개(수평 확장)인 경우 특정 인스턴스에서만 이벤트 발생
    //   3. 알림 발송 실패 후 재시도 소진 시 해당 이벤트를 영구적으로 복구할 수단 없음
    //
    // ─────────────────────────────────────────────────────────────────────────
    //
    // [Outbox 패턴 + Kafka 전환 구조]
    //
    // 핵심 아이디어:
    //   이벤트를 인메모리가 아닌 DB에 먼저 저장(Outbox 테이블)하고,
    //   별도 프로세스가 Outbox를 읽어 Kafka 토픽으로 발행한다.
    //   이벤트 저장과 비즈니스 트랜잭션이 같은 DB 트랜잭션 안에 묶이므로
    //   "저장됐는데 이벤트 유실" 또는 "이벤트 발행됐는데 DB 롤백" 시나리오가 사라진다.
    //
    //
    // [전체 흐름]
    //
    //   LessonService.createLesson() — @Transactional
    //     │
    //     ├─ lessonRepository.save()                 ← lessons 테이블 INSERT
    //     └─ outboxRepository.save(OutboxEvent)      ← outbox_events 테이블 INSERT
    //          (같은 트랜잭션 → 원자적 보장)
    //          │
    //          │ [트랜잭션 커밋]
    //          ▼
    //   OutboxPoller (Scheduled, 별도 스레드)
    //     │  주기적으로 outbox_events 에서 status = PENDING 레코드 조회
    //     │  → KafkaProducer.send("lesson.created", OutboxEvent)
    //     │  → 발행 성공 시 status = PUBLISHED 로 업데이트
    //     │  → 발행 실패 시 status = PENDING 유지 → 다음 폴링 때 재시도
    //          │
    //          ▼
    //   Kafka Topic: "lesson.created"
    //          │
    //          ▼
    //   LessonCreatedKafkaConsumer (@KafkaListener)
    //     │  컨슈머 그룹: "notification-group"
    //     │  → AlimtalkMessageBuilder 로 ExternalMessage 조립
    //     │  → ExternalMessageApiService.sendAlimtalk() 호출
    //     │  → 발송 성공 시 offset commit
    //     │  → 발송 실패 시 offset commit 안 함 → Kafka 가 재전달 (at-least-once)
    //
    //
    // [Outbox 테이블 구조]
    //
    //   outbox_events
    //     id          BIGINT PK
    //     aggregate   VARCHAR   — 이벤트 발생 대상 (예: "lesson")
    //     event_type  VARCHAR   — 이벤트 종류 (예: "LESSON_CREATED")
    //     payload     TEXT      — JSON 직렬화된 이벤트 데이터 (lessonId, studentId 등)
    //     status      VARCHAR   — PENDING | PUBLISHED | FAILED
    //     created_at  DATETIME
    //
    //
    // [인프라 추가 구성 (docker-compose)]
    //
    //   zookeeper : Kafka 클러스터 메타데이터 관리
    //   kafka     : 토픽 브로커. lesson.created 토픽 파티션 3, replication-factor 1(로컬)
    //
    //   서비스 연결:
    //     app → kafka:9092 (producer + consumer)
    //     OutboxPoller → DB (outbox_events 폴링)
    //
    //
    // [현재 구조 대비 개선 효과]
    //
    //   | 항목                  | Spring Event (현재)     | Outbox + Kafka          |
    //   |-----------------------|------------------------|-------------------------|
    //   | 이벤트 유실            | 서버 재시작 시 유실 가능  | DB에 영속 → 유실 없음    |
    //   | 수평 확장             | 인스턴스별 독립 이벤트    | Kafka 컨슈머 그룹으로 통합 |
    //   | 재처리                | 재시도 소진 시 복구 불가  | offset 미commit → 재전달  |
    //   | 발행/구독 결합도       | 동일 JVM 강결합          | 토픽 기준 완전 분리       |
}
