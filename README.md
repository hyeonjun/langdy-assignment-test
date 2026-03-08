# Langdy

수업 예약 서비스 백엔드 API. 학생이 원하는 시간에 가능한 선생님을 조회하고 수업을 예약할 수 있다.

---

## 실행 방법

### 사전 요구사항

- Docker & Docker Compose
- (로컬 직접 실행 시) JDK 21, Redis

### Docker로 실행 (권장)

```bash
docker-compose up --build
```

앱이 기동되면 `http://localhost:8080` 에서 API를 사용할 수 있다.

> **재빌드 없이 재시작:**
> ```bash
> docker-compose up
> ```
> 소스 변경 후에는 `--build` 옵션을 붙여야 반영된다.

### 로컬에서 직접 실행

Redis가 `localhost:6379`에서 실행 중이어야 한다.

```bash
# Redis 실행 (Docker로 Redis만 띄우기)
docker run -d -p 6379:6379 redis:7-alpine

# 애플리케이션 실행
./gradlew bootRun
```

---

## H2 콘솔

인메모리 DB 데이터를 브라우저에서 직접 확인할 수 있다.

- **URL**: `http://localhost:8080/h2-console`
- **JDBC URL**: `jdbc:h2:mem:langdy`
- **Username**: `sa`
- **Password**: (비어 있음)

> 앱을 재시작하면 DB가 초기화된다 (`ddl-auto: create-drop`).

---

## API 사용법

모든 요청에 `X-Student-Id` 헤더가 필요하다.

### 가용 선생님 조회

```bash
GET /api/v1/lessons/available-teachers
```

```bash
curl -X GET "http://localhost:8080/api/v1/lessons/available-teachers?courseId=1&startAt=2026-03-10+09:00:00" \
  -H "X-Student-Id: 1"
```

**Query Parameters**

| 파라미터 | 설명 | 예시 |
|----------|------|------|
| `courseId` | 수업 과정 ID | `1` |
| `startAt` | 수업 시작 시간 (`yyyy-MM-dd HH:mm:ss`) | `2026-03-10 09:00:00` |

**응답 예시**

```json
{
  "status": 0,
  "data": [
    { "id": 1, "name": "선생님A" },
    { "id": 2, "name": "선생님B" }
  ]
}
```

### 수업 예약

```bash
POST /api/v1/lessons
```

```bash
curl -X POST "http://localhost:8080/api/v1/lessons" \
  -H "Content-Type: application/json" \
  -H "X-Student-Id: 1" \
  -d '{
    "courseId": 1,
    "teacherId": 1,
    "startAt": "2026-03-10T09:00:00"
  }'
```

**Request Body**

| 필드 | 설명 | 예시 |
|------|------|------|
| `courseId` | 수업 과정 ID | `1` |
| `teacherId` | 선생님 ID | `1` |
| `startAt` | 수업 시작 시간 (`yyyy-MM-dd'T'HH:mm:ss`) | `2026-03-10T09:00:00` |

**응답 예시**

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

### 비즈니스 규칙

- `startAt`은 반드시 **미래 시간**이어야 한다.
- `startAt`의 **분은 0 또는 30**이어야 한다 (정각/30분 단위).
- 수업 시간은 `startAt`부터 **20분**.
- 학생 또는 선생님이 해당 시간에 이미 예약이 있으면 예약 불가.

### 에러 응답

```json
{
  "status": 400,
  "error": {
    "code": "LS0005",
    "message": "이미 예약된 수업이 있습니다."
  }
}
```

| 코드 | 의미 |
|------|------|
| `LS0001` | `X-Student-Id` 헤더 누락 또는 잘못된 형식 |
| `LS0003` | 존재하지 않는 학생/선생님/수업과정 ID |
| `LS0004` | 유효하지 않은 시간 (정각·30분 아님, 또는 과거 시간) |
| `LS0005` | 해당 시간에 이미 예약 존재 |
| `LS0007` | 동시 요청 처리 중 (잠시 후 재시도) |

---

## 테스트 방법

### 전체 테스트 실행

```bash
./gradlew test
```

### 테스트 결과 확인

```bash
# 테스트 실행 후 HTML 리포트 열기
open build/reports/tests/test/index.html
```

### 특정 테스트 클래스만 실행

```bash
# 서비스 단위 테스트
./gradlew test --tests "com.example.langdy.api.service.LessonServiceTest"

# 컨트롤러 슬라이스 테스트
./gradlew test --tests "com.example.langdy.api.controller.LessonControllerTest"
```

### 테스트 구성

| 파일 | 종류 | 설명 |
|------|------|------|
| `LessonServiceTest` | 단위 테스트 | Mockito로 의존성 Mock. 비즈니스 로직 검증 (유효성, 중복 체크, 정상 생성) |
| `LessonControllerTest` | 슬라이스 테스트 (`@WebMvcTest`) | MockMvc로 HTTP 요청/응답 검증. 헤더 인증, 파라미터 검증, 에러 코드 확인 |

**`LessonServiceTest` 주요 케이스**

- `getAvailableTeachers`: 정상 조회, 유효하지 않은 시간, 존재하지 않는 student/course, 학생 중복 예약, 빈 결과
- `createLesson`: 유효하지 않은 시간, 존재하지 않는 student/teacher/course, 중복 예약, 정상 생성 + 이벤트 발행 확인

**`LessonControllerTest` 주요 케이스**

- `X-Student-Id` 헤더 누락 → `status: 400`
- 필수 파라미터 누락 → `status: 400`
- 서비스 예외별 에러 코드 매핑 검증 (`LS0003`, `LS0004`, `LS0005`)
- 정상 응답 구조 확인

---

## 프로젝트 구조

```
src/
├── main/kotlin/com/example/langdy/
│   ├── domain/         # 엔티티 (Student, Teacher, Course, Lesson) + Repository
│   ├── api/            # Controller + Service + Request/Response DTO
│   ├── infra/
│   │   ├── lock/       # Redis 분산 락 (LessonLockService)
│   │   └── event/      # 도메인 이벤트 + 비동기 구독자 (알림 발송)
│   ├── global/
│   │   ├── response/   # ResponseModel (통일 응답 래퍼)
│   │   ├── exception/  # LessonErrorType, GlobalExceptionHandler
│   │   └── interceptor/ # AuthenticationInterceptor (X-Student-Id)
│   └── config/         # Security, WebMvc, QueryDSL, JPA Auditing
└── test/kotlin/com/example/langdy/
    ├── api/controller/ # LessonControllerTest
    └── api/service/    # LessonServiceTest
```

자세한 아키텍처는 [ARCHITECTURE.md](./ARCHITECTURE.md)를 참고.
