# Langdy 아키텍처 문서

## 1. 개발 방법론

### TDD (Test-Driven Development)
모든 기능 개발은 **Red → Green → Refactor** 사이클로 진행한다.

```
1. Red    : 실패하는 테스트를 먼저 작성
2. Green  : 테스트를 통과하는 최소한의 코드 작성
3. Refactor: 동작을 유지하면서 코드 품질 개선
```

**테스트 레이어 구성**

| 레이어 | 테스트 종류 | 도구 |
|--------|-------------|------|
| 도메인 | 단위 테스트 (순수 Kotlin) | JUnit 5, Kotest |
| 서비스 | 단위 테스트 (Mock 의존성) | Mockito-Kotlin |
| 레포지토리 | 슬라이스 테스트 | @DataJpaTest, H2 |
| API | 통합 테스트 | @SpringBootTest, MockMvc |
| 동시성 | 병렬 요청 테스트 | CountDownLatch + ExecutorService |

**규칙**
- 테스트 없이 프로덕션 코드를 먼저 작성하지 않는다.
- 각 테스트는 독립적이며, 순서에 의존하지 않는다.
- 테스트 커버리지 목표: 도메인/서비스 레이어 80% 이상.

---

### DDD (Domain-Driven Design)
비즈니스 도메인을 중심으로 코드를 구성한다.

**패키지 구조 (레이어드 + DDD)**

```
com.example.langdy/
├── domain/          ← 핵심 도메인 (순수 비즈니스 로직)
│   ├── student/     ← 학생 Bounded Context
│   ├── teacher/     ← 선생님 Bounded Context
│   ├── course/      ← 수업 과정 Bounded Context
│   └── lesson/      ← 수업 예약 Bounded Context (핵심)
├── api/             ← 애플리케이션 서비스 (유스케이스 조합)
│   ├── teacher/
│   └── lesson/
├── infra/           ← 인프라 어댑터 (외부 시스템 연동)
│   ├── lock/        ← Redis 분산 락
│   └── notification/
├── global/          ← 공통 인프라 (예외, 리졸버)
└── config/          ← 설정
```

**설계 원칙**
- **도메인 순수성**: `domain/` 패키지는 Spring, JPA 외 외부 프레임워크 의존 최소화.
- **풍부한 도메인 모델**: 비즈니스 규칙은 Entity/Value Object 안에 캡슐화.
- **애플리케이션 서비스**: `api/` 레이어는 도메인 객체를 조합해 유스케이스를 완성. 자체 비즈니스 로직 없음.
- **Bounded Context 격리**: 각 도메인은 Long FK로 참조. `@ManyToOne` 크로스 컨텍스트 연관관계 금지.

---

## 2. 알림 발송 아키텍처 (TASK 3)

### 설계 목표
- 수업 예약 트랜잭션과 알림 발송을 **완전히 분리**한다.
- DB 롤백 시 알림이 발송되지 않도록 보장한다.
- 알림 발송 지연/실패가 예약 API 응답 시간에 영향을 주지 않는다.

### 전체 흐름

```
[Client]
  │ POST /api/v1/lessons
  ▼
[LessonController]
  │
  ▼
[LessonService.bookLesson()]
  │ @Transactional
  ├─ 유효성 검증
  ├─ [Redis 분산 락 획득]  ← LessonLockService.lockCreateLesson()
  ├─ 중복 예약 체크
  ├─ lessonRepository.save()   ← DB INSERT
  ├─ eventPublisher.publishEvent(LessonBookedEvent)  ← 이벤트 버퍼에 적재
  │   (트랜잭션 커밋 전까지 리스너 미실행)
  ├─ [Redis 락 해제]           ← stringRedisTemplate.unlink(key)
  └─ return BookLessonResponse  → 201 CREATED 응답
       │
       │ [트랜잭션 커밋 성공]
       ▼
[NotificationPublisher.onLessonBooked()]
  │ @TransactionalEventListener(AFTER_COMMIT)
  │ @Async  ← 별도 스레드 풀
  ▼
[publishNotification()]
  │ NCloud SENS API 호출
  ▼
[카카오 알림톡 발송]
  - 수신자: Student.phone
  - 템플릿: LESSON_BOOKED
  - 내용: "#{name}님, #{startAt} 수업이 예약되었습니다."
```

### 핵심 결정 사항

#### `@TransactionalEventListener(phase = AFTER_COMMIT)` 사용 이유
| 상황 | 동작 |
|------|------|
| 트랜잭션 커밋 성공 | 리스너 실행 → 알림 발송 |
| 트랜잭션 롤백 | 리스너 **미실행** → 알림 미발송 (올바른 동작) |
| `@EventListener` 사용 시 (잘못된 방법) | 롤백되어도 알림 발송됨 |

#### `@Async` 사용 이유
- 알림톡 API 호출(외부 네트워크 I/O)이 예약 응답 P99에 영향을 주지 않음.
- `@EnableAsync` + `ThreadPoolTaskExecutor` Bean 등록 필요.

#### 장애 격리
- `publishNotification()` 내부 예외는 catch 후 로깅만 수행.
- 알림 발송 실패가 예약 성공 이력에 영향 없음.

### NCloud SENS 카카오 알림톡 발송 구현 상세

#### 인증 (HMAC-SHA256)
```
timestamp  = System.currentTimeMillis().toString()
method     = "POST"
url        = "/alimtalk/v2/services/{serviceId}/messages"
message    = "{method}\n{url}\n{timestamp}\n{accessKey}"
signature  = Base64(HMAC-SHA256(secretKey.toByteArray(), message.toByteArray()))

요청 헤더:
  Content-Type             : application/json
  x-ncp-apigw-timestamp    : {timestamp}
  x-ncp-iam-access-key     : {accessKey}
  x-ncp-apigw-signature-v2 : {signature}
```

#### 요청 엔드포인트
```
POST https://sens.apigw.ntruss.com/alimtalk/v2/services/{serviceId}/messages
```

#### 요청 바디
```json
{
  "plusFriendId": "@langdy",
  "templateCode": "LESSON_BOOKED",
  "messages": [
    {
      "to": "01012345678",
      "content": "홍길동님, 2026-03-10 09:00 수업이 예약되었습니다.",
      "buttons": [
        {
          "type": "WL",
          "name": "예약 확인",
          "linkMobile": "https://langdy.com/lessons/42"
        }
      ]
    }
  ]
}
```

#### 신뢰성 전략
| 전략 | 설명 |
|------|------|
| 재시도 | 최대 3회, 지수 백오프(1s → 2s → 4s) |
| 멱등성 | 요청에 lessonId 포함 → 중복 발송 방지 |
| 실패 로깅 | `notification_logs` 테이블에 SUCCESS/FAILED 기록 |
| 재발송 | 실패 건 배치 또는 관리자 콘솔에서 재처리 |

### 도메인 이벤트 설계

```kotlin
// 이벤트: 영속성 컨텍스트 의존 제거를 위해 필요한 값만 포함
data class LessonBookedEvent(
    val lessonId: Long,
    val studentId: Long,
    val teacherId: Long,
    val courseId: Long,
)

// 발행: LessonService.bookLesson() 내부
eventPublisher.publishEvent(LessonBookedEvent(...))

// 수신: NotificationPublisher.onLessonBooked()
@Async
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
fun onLessonBooked(event: LessonBookedEvent) { ... }
```

---

## 3. 동시성 제어 전략

**3중 방어 레이어**

| 레이어 | 수단 | 역할 |
|--------|------|------|
| Redis 분산 락 | `LessonLockService.lockCreateLesson()` | 동일 선생님+시간 요청을 직렬화, 선착순 1건만 통과 |
| 애플리케이션 | `existsByTeacherIdAndStartAtAndStatusIn()` | 99% 케이스 조기 차단, 명확한 에러 메시지 |
| DB | UNIQUE 제약조건 `(teacher_id, start_at, status)` | 동시 요청 최종 안전장치 |

### Redis 분산 락 설계

**도입 배경**: DB UNIQUE 제약만으로는 동시 요청이 `INSERT` 시점에 경합하여 `DataIntegrityViolationException`이 발생한다. Redis 분산 락으로 요청을 직렬화하면 DB 레이어까지 도달하는 중복 요청 자체를 차단할 수 있다.

**락 키 설계**
```
langdy:lesson:create:{teacherId}:{startAt}
```
- 선생님 ID + 수업 시작 시간 조합으로 키를 구성한다.
- 동일한 (선생님, 시간) 조합의 동시 요청만 직렬화하여, 다른 선생님/시간의 요청은 독립적으로 처리된다.

**TTL 전략**
- `setIfAbsent(key, "lock", Duration.ofSeconds(3))` — 3초 TTL.
- TTL 내에 처리가 완료되면 `unlink(key)`로 즉시 해제한다.
- 서버 장애로 `finally` 블록이 실행되지 않더라도 TTL 만료 시 자동 해제된다.

**`LessonLockService` 동작**
```kotlin
fun <T> lockCreateLesson(teacherId: Long, startAt: LocalDateTime, block: () -> T): T {
    val key = "langdy:lesson:create:${teacherId}:${startAt}"
    val locked = stringRedisTemplate.opsForValue()
        .setIfAbsent(key, "lock", Duration.ofSeconds(3)) ?: false
    if (!locked) throw BusinessException(ErrorCode.LESSON_LOCK_CONFLICT)
    try {
        return block()
    } finally {
        stringRedisTemplate.unlink(key)
    }
}
```

**에러 코드**

| 에러 코드 | HTTP 상태 | 발생 조건 |
|-----------|-----------|-----------|
| `LESSON_LOCK_CONFLICT` | 409 CONFLICT | Redis 락 획득 실패 (동일 요청 처리 중) |
| `TEACHER_ALREADY_BOOKED` | 409 CONFLICT | 앱 레이어 중복 체크 실패 |
| `DUPLICATE_LESSON` | 409 CONFLICT | DB UNIQUE 제약 위반 |

`DataIntegrityViolationException` → `GlobalExceptionHandler`에서 409 CONFLICT 반환.

---

## 4. 인프라 구성

### Docker Compose (`docker-compose.yml`)

```yaml
services:
  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    volumes:
      - redis-data:/data

  app:
    build: .
    ports:
      - "8080:8080"
    depends_on:
      - redis
    environment:
      SPRING_DATA_REDIS_HOST: redis
      SPRING_DATASOURCE_URL: jdbc:h2:mem:langdy;MODE=MySQL;DB_CLOSE_DELAY=-1
```

- Redis 7-alpine: 분산 락에 사용하는 인메모리 저장소.
- `depends_on: redis`: 앱 컨테이너가 Redis 준비 후 기동됨을 보장.
- `SPRING_DATA_REDIS_HOST`: 컨테이너 네트워크 내 서비스 이름으로 Redis 접속.

### Dockerfile

```dockerfile
FROM eclipse-temurin:21-jre-alpine
```

- JRE 21 + Alpine Linux 기반으로 이미지 크기를 최소화한다.

### application.yml — Redis 설정

```yaml
spring:
  data:
    redis:
      host: ${SPRING_DATA_REDIS_HOST:localhost}
      port: 6379
```

- 환경변수 `SPRING_DATA_REDIS_HOST`로 호스트를 주입받으며, 로컬 실행 시 기본값 `localhost` 사용.

---

## 5. API 명세

| Task | Method | URL | 헤더 | 응답 코드 |
|------|--------|-----|------|-----------|
| #1 | GET | `/api/v1/teachers/available?courseId=1&startAt=2026-03-10T09:00:00` | - | 200 |
| #2 | POST | `/api/v1/lessons` | `X-Student-Id: {id}` | 201 |

### 에러 응답 형식
```json
{
  "code": "TEACHER_ALREADY_BOOKED",
  "message": "해당 선생님은 이미 예약된 시간입니다."
}
```
