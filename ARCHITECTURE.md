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
| 서비스 | 단위 테스트 (Mock 의존성) | JUnit 5, Mockito-Kotlin |
| API | 슬라이스 테스트 | @WebMvcTest, MockMvc |
| 동시성 | 병렬 요청 테스트 | CountDownLatch + ExecutorService |

**규칙**
- 테스트 없이 프로덕션 코드를 먼저 작성하지 않는다.
- 각 테스트는 독립적이며, 순서에 의존하지 않는다.
- 테스트 커버리지 목표: 도메인/서비스 레이어 80% 이상.

---

### DDD (Domain-Driven Design)
비즈니스 도메인을 중심으로 코드를 구성한다.

**실제 패키지 구조**

```
com.example.langdy/
├── domain/                  ← 핵심 도메인 (엔티티 + 레포지토리)
│   ├── entity/
│   │   ├── Student.kt       ← 학생 (os: IOS/ANDROID)
│   │   ├── Teacher.kt       ← 선생님
│   │   ├── Course.kt        ← 수업 과정
│   │   └── Lesson.kt        ← 수업 예약 (핵심, BOOKED/CANCELLED/DONE)
│   ├── repository/
│   │   ├── StudentRepository.kt
│   │   ├── TeacherRepository.kt
│   │   ├── CourseRepository.kt
│   │   └── LessonRepository.kt
│   └── base/
│       └── BaseEntity.kt    ← createdAt, updatedAt (JPA Auditing)
├── api/                     ← 애플리케이션 서비스 (유스케이스 조합)
│   ├── controller/
│   │   ├── LessonController.kt
│   │   ├── request/
│   │   │   ├── CreateLessonRequest.kt
│   │   │   └── FindAvailableTeachersRequest.kt
│   │   └── response/
│   │       ├── CreateLessonResponse.kt
│   │       └── FindAvailableTeacherResponse.kt
│   └── service/
│       ├── LessonService.kt
│       └── result/
│           ├── LessonResult.kt
│           └── TeacherResult.kt
├── infra/                   ← 인프라 어댑터
│   ├── lock/
│   │   └── LessonLockService.kt   ← Redis 분산 락
│   └── event/lesson/
│       ├── LessonEvents.kt        ← LessonCreated 이벤트 정의
│       └── subscriber/
│           └── LessonCreatedEventSubscriber.kt  ← 알림 발송 (AFTER_COMMIT + @Async)
├── global/                  ← 공통 인프라
│   ├── response/
│   │   └── ResponseModel.kt       ← 통일된 API 응답 래퍼
│   ├── exception/
│   │   ├── ErrorType.kt           ← 에러 타입 인터페이스
│   │   ├── ApplicationException.kt
│   │   ├── LessonException.kt     ← sealed class + enum LessonErrorType
│   │   └── GlobalExceptionHandler.kt
│   └── interceptor/
│       └── AuthenticationInterceptor.kt  ← X-Student-Id 헤더 인증
└── config/
    ├── SecurityConfig.kt    ← CSRF 비활성화, frameOptions 비활성화
    ├── WebMvcConfig.kt      ← AuthenticationInterceptor 등록
    ├── QueryDslConfig.kt    ← JPAQueryFactory Bean
    └── JpaAuditingConfig.kt ← @EnableJpaAuditing
```

**설계 원칙**
- **애플리케이션 서비스**: `api/service` 레이어는 도메인 객체를 조합해 유스케이스를 완성. 자체 비즈니스 로직 없음.
- **인터셉터 인증**: Spring Security는 모든 요청 허용, 실제 인증은 `AuthenticationInterceptor`가 `X-Student-Id` 헤더로 처리.
- **통일 응답 포맷**: 모든 API는 `ResponseModel<T>` 래퍼로 반환. HTTP 상태는 항상 200, 비즈니스 상태는 `status` 필드로 구분.

---

## 2. API 명세

### 공통

**요청 헤더**

| 헤더 | 필수 | 설명 |
|------|------|------|
| `X-Student-Id` | 필수 | 학생 ID (Long) |

**응답 포맷**

```json
{
  "status": 0,
  "data": { ... }
}
```

성공 시 `status: 0`, 실패 시 비즈니스 에러 코드 반환.

**에러 응답**

```json
{
  "status": 400,
  "error": {
    "code": "LS0005",
    "message": "이미 예약된 수업이 있습니다."
  }
}
```

### GET `/api/v1/lessons/available-teachers`

수업 가능한 선생님 목록 조회.

**Query Parameters**

| 파라미터 | 타입 | 필수 | 형식 |
|----------|------|------|------|
| `courseId` | Long | 필수 | - |
| `startAt` | LocalDateTime | 필수 | `yyyy-MM-dd HH:mm:ss` |

**비즈니스 규칙**
- `startAt`은 미래 시간이어야 한다.
- `startAt`의 분은 0 또는 30이어야 한다 (정각/30분 단위).
- 학생이 해당 시간에 이미 예약이 있으면 `LS0005` 에러.

**응답 예시 (200 OK)**

```json
{
  "status": 0,
  "data": [
    { "id": 1, "name": "선생님A" },
    { "id": 2, "name": "선생님B" }
  ]
}
```

### POST `/api/v1/lessons`

수업 예약.

**Request Body**

```json
{
  "courseId": 1,
  "teacherId": 1,
  "startAt": "2026-03-10T09:00:00"
}
```

**비즈니스 규칙**
- `startAt`은 미래 시간이어야 한다.
- `startAt`의 분은 0 또는 30이어야 한다.
- 선생님 또는 학생이 해당 시간에 이미 예약 중이면 `LS0005` 에러.
- 수업 시간은 `startAt`부터 20분.

**응답 예시 (200 OK)**

```json
{
  "status": 0,
  "data": {
    "lessonId": 42,
    "teacherName": "선생님A",
    "courseName": "영어",
    "startAt": "2026-03-10T09:00:00",
    "endAt": "2026-03-10T09:20:00"
  }
}
```

### 에러 코드표

| 코드 | HTTP Status (ResponseModel) | 의미 |
|------|-----------------------------|------|
| `LS0000` | 500 | 시스템 오류 |
| `LS0001` | 400 | 인증 실패 (X-Student-Id 헤더 누락/잘못됨) |
| `LS0002` | 400 | 잘못된 요청 |
| `LS0003` | 404 | 엔티티 없음 (학생/선생님/수업과정/수업 ID 불일치) |
| `LS0004` | 400 | 유효하지 않은 시간 (정각/30분 아님, 과거 시간) |
| `LS0005` | 400 | 이미 예약된 시간 |
| `LS0006` | 400 | 중복 예약 (DB UNIQUE 제약 위반) |
| `LS0007` | 400 | 동시 요청 처리 중 (Redis 락 획득 실패) |

---

## 3. 알림 발송 아키텍처

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
  │ lessonLockService.lockCreateLesson() — Redis 락 획득
  ▼
[LessonService.createLesson()]
  │ @Transactional
  ├─ 유효성 검증 (시간 형식, 미래 여부)
  ├─ Student / Teacher / Course 조회
  ├─ 중복 예약 체크 (teacher OR student 기준)
  ├─ lessonRepository.save()   ← DB INSERT
  ├─ eventPublisher.publishEvent(LessonCreated)  ← 이벤트 버퍼에 적재
  │   (트랜잭션 커밋 전까지 리스너 미실행)
  └─ return lessonId
       │
       │ [Redis 락 해제 → getLessonDetail() → 201 응답]
       │
       │ [트랜잭션 커밋 성공]
       ▼
[LessonCreatedEventSubscriber.handleLessonCreated()]
  │ @Async — 별도 스레드
  │ @TransactionalEventListener(AFTER_COMMIT)
  │ @Transactional(REQUIRES_NEW)
  ▼
[publishNotification()]
  └─ TODO: NCloud SENS 카카오 알림톡 발송
```

### 핵심 결정 사항

#### `@TransactionalEventListener(phase = AFTER_COMMIT)`

| 상황 | 동작 |
|------|------|
| 트랜잭션 커밋 성공 | 리스너 실행 → 알림 발송 |
| 트랜잭션 롤백 | 리스너 **미실행** → 알림 미발송 (올바른 동작) |
| `@EventListener` 사용 시 (잘못된 방법) | 롤백되어도 알림 발송됨 |

#### `@Async`
- 알림톡 API 호출(외부 네트워크 I/O)이 예약 응답 P99에 영향을 주지 않음.

#### `@Transactional(REQUIRES_NEW)`
- 별도 트랜잭션으로 알림 발송 실패가 예약 트랜잭션에 영향을 주지 않음.

---

## 4. 동시성 제어 전략

**3중 방어 레이어**

| 레이어 | 수단 | 역할 |
|--------|------|------|
| Redis 분산 락 | `LessonLockService.lockCreateLesson()` | 동일 선생님+시간 요청을 직렬화, 선착순 1건만 통과 |
| 애플리케이션 | `existsBookedByTeacherOrStudent()` | 99% 케이스 조기 차단, 명확한 에러 메시지 |
| DB | UNIQUE 제약조건 `(teacher_id, start_at, status)` / `(student_id, start_at, status)` | 동시 요청 최종 안전장치 |

### Redis 분산 락 설계

**도입 배경**: DB UNIQUE 제약만으로는 동시 요청이 `INSERT` 시점에 경합하여 `DataIntegrityViolationException`이 발생한다. Redis 분산 락으로 요청을 직렬화하면 DB 레이어까지 도달하는 중복 요청 자체를 차단할 수 있다.

**락 키 설계**
```
lesson:create:{teacherId}:{startAt}
```

**TTL 전략**
- `setIfAbsent(key, "lock", Duration.ofSeconds(3))` — 3초 TTL.
- TTL 내 처리 완료 시 `unlink(key)`로 즉시 해제.
- 서버 장애로 `finally` 미실행 시 TTL 만료 후 자동 해제.

**락 획득 실패 시**: `LessonWaitingProcessingException` (LS0007, 400) 반환.

---

## 5. 인프라 구성

### 기술 스택

| 분류 | 기술 |
|------|------|
| Language | Kotlin 1.9.25 |
| Framework | Spring Boot 3.5.11 |
| ORM | Spring Data JPA + QueryDSL 5.1.0 |
| Database | H2 (인메모리, MODE=MySQL) |
| Cache / Lock | Redis 7 (Spring Data Redis) |
| Security | Spring Security (모든 요청 허용, 커스텀 인터셉터 인증) |
| Build | Gradle (Kotlin JVM plugin) |
| Runtime | JDK 21 (eclipse-temurin:21) |
| Container | Docker + Docker Compose |

### Docker Compose 구성

```
┌─────────────────────────────┐
│        docker network        │
│                              │
│  ┌──────────┐  ┌──────────┐ │
│  │   app    │  │  redis   │ │
│  │  :8080   │──│  :6379   │ │
│  └──────────┘  └──────────┘ │
└─────────────────────────────┘
```

- **app**: Spring Boot 애플리케이션. `SPRING_DATA_REDIS_HOST=redis` 환경변수로 Redis 연결.
- **redis**: 분산 락 전용 인메모리 저장소. `redis-data` 볼륨으로 영속성 확보.
- **H2**: 인메모리 DB. Docker 실행 시 앱 컨테이너 내부에서 동작 (`SPRING_DATASOURCE_URL` 주입).

### Dockerfile (멀티 스테이지 빌드)

```dockerfile
# Stage 1: 빌드
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
RUN ./gradlew dependencies --no-daemon   # 의존성 레이어 캐시
COPY src src
RUN ./gradlew bootJar -x test --no-daemon

# Stage 2: 실행
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

- `build.gradle`이 변경되지 않으면 의존성 다운로드 레이어 캐시 재사용.
- JRE-only 이미지로 최종 이미지 크기 최소화.

### application.yml 주요 설정

```yaml
spring:
  data.redis:
    host: ${SPRING_DATA_REDIS_HOST:localhost}  # 로컬: localhost, Docker: redis
    port: 6379
  datasource:
    url: jdbc:h2:mem:langdy;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
  h2.console:
    enabled: true
    path: /h2-console
    settings.web-allow-others: true   # Docker 컨테이너 외부 접근 허용
  jpa:
    hibernate.ddl-auto: create-drop   # 앱 기동 시 스키마 자동 생성/삭제
    show-sql: true
```
