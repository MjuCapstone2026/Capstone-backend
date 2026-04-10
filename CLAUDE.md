# Capstone Backend — Claude 지침

## 참고 문서
- @docs/conventions.md — 코딩 스타일 & 테스트 패턴 (상세)
- @docs/db-schema.md — DB 스키마 & Flyway 이력
- @docs/api/ — API 명세서 (엔드포인트별, 파일명: `{HTTP메서드}_v1_{도메인}_{액션}.md`)
  - 예) `docs/api/v1/users/POST_v1_users_signup.md`
  - 특정 엔드포인트 작업 시 해당 파일을 먼저 읽는다

---

## 기술 스택 요약
- **Java 21 / Spring Boot 3.5.x WebFlux**
- **DB:** PostgreSQL(JPA, 동기) + Redis(Reactive, 비동기)
- **인증:** Clerk JWT (OAuth2 Resource Server)
- **마이그레이션:** Flyway / **문서:** SpringDoc OpenAPI(WebFlux)

---

## 핵심 코딩 규칙

### 패키지 구조
새 도메인은 반드시 아래 구조를 따른다:
```
domain/{name}/
├── controller/  {Name}Controller.java
├── service/     {Name}Service.java (interface) + {Name}ServiceImpl.java
├── repository/  {Name}Repository.java  (JpaRepository)
└── entity/      {Name}.java
```

### 의존성 주입
- 필드 주입(`@Autowired`) 금지 — 반드시 생성자 주입
- Lombok `@RequiredArgsConstructor` 사용

### Reactive / Blocking 혼용 패턴
JPA(블로킹) 호출은 반드시 아래 패턴으로 격리:
```
Mono.fromCallable(() -> repository.save(entity))
    .subscribeOn(dbScheduler)
```
- `dbScheduler`는 `DatabaseConfig`에서 주입받는다
- 절대 WebFlux 이벤트 루프 스레드에서 직접 JPA 호출 금지

### Entity
- `@NoArgsConstructor(access = AccessLevel.PROTECTED)` 필수
- 생성은 정적 팩터리 메서드 `{Name}.of(...)` 사용

---

## 에러 처리 규칙

모든 에러는 `{"status": N, "error": "...", "message": "..."}` 형식으로 반환한다.

| 상황 | 핸들러 |
|------|--------|
| JWT 없음/만료 (401) | `SecurityErrorHandler` |
| 권한 없음 (403) | `SecurityErrorHandler` |
| 비즈니스 예외 (404, 500 등) | `GlobalExceptionHandler` |

새 예외 추가 시 `global/exception/` 패키지만 수정한다. SecurityConfig는 건드리지 않는다.

---

## 핵심 테스트 규칙

| 레이어 | 애노테이션 | 외부 서비스 |
|--------|-----------|------------|
| Service | `@ExtendWith(MockitoExtension.class)` | `@Mock` Repository |
| Controller | `@SpringBootTest` + `@AutoConfigureWebTestClient` + `@ActiveProfiles("test")` + `@TestPropertySource("file:.env")` | H2 + `.env` 자격증명 + `mockJwt()` |
| Config/인프라 | `@SpringBootTest` + `@TestPropertySource(locations = "file:.env")` | 실제 Supabase/Redis |

- 리액티브 검증은 `StepVerifier` 사용
- Service 단위 테스트에서 Scheduler는 `Schedulers.immediate()`로 교체
- JWT는 `SecurityMockServerConfigurers.mockJwt()` 사용

---

## 커밋 컨벤션
```
type(#이슈번호) : 한글 설명
```
| type | 사용 시점 |
|------|----------|
| `feat` | 새 기능 |
| `fix` | 버그 수정 |
| `test` | 테스트 코드 |
| `refactor` | 리팩토링 |
| `setting` | 환경/설정 변경 |
| `docs` | 문서 작성/수정 |
| `CICD` | 배포 파이프라인 |
| `perf` | 성능 개선 |

예시: `feat(#12) : 일정 조회 API 추가`
