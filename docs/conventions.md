# Conventions — 코딩 스타일 & 테스트 패턴

## 1. 아키텍처

### 레이어 구조
```
domain/{name}/
├── controller/   HTTP 요청/응답, 위임만 담당
├── service/      비즈니스 로직 (interface + impl 분리)
├── repository/   데이터 접근 (JpaRepository 확장)
└── entity/       JPA 도메인 객체
global/config/    공통 설정 빈 (DB, Redis, Security, WebClient 등)
```

### SOLID 적용
| 원칙 | 적용 방식 |
|------|-----------|
| SRP | 레이어별 단일 책임 (Controller는 HTTP만, Service는 비즈니스만) |
| OCP | Service는 interface 기반 — 구현체 교체 가능 |
| LSP | `{Name}ServiceImpl`은 `{Name}Service` 계약을 완전히 준수, 사전조건 강화 금지 |
| DIP | Controller → `{Name}Service` (interface) 의존, impl에 직접 의존 금지 |
| ISP | Service interface는 최소한의 메서드만 선언 |

---

## 2. 코딩 규칙

### 의존성 주입
```
// 올바른 방식
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;  // interface 타입
}

// 금지
@Autowired
private UserService userService;
```

### Entity 작성
```
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)  // 필수
public class User {

    @Id
    private String id;

    // 정적 팩터리 메서드로만 생성
    public static User of(String clerkId) {
        User user = new User();
        user.id = clerkId;
        return user;
    }
}
```

### Reactive + JPA 혼용 패턴 (핵심)
WebFlux 환경에서 JPA(블로킹)를 사용할 때 반드시 아래 패턴 사용:
```
// 올바른 방식 — dbScheduler 스레드풀로 격리
Mono.fromCallable(() -> userRepository.save(user))
    .subscribeOn(dbScheduler)

// 절대 금지 — 이벤트 루프 블로킹
return Mono.just(userRepository.save(user));
```

`dbScheduler`는 `DatabaseConfig`에서 빈으로 등록된 `Scheduler`:
```
// DatabaseConfig.java
@Bean
public Scheduler dbScheduler(@Value("${DB_POOL_SIZE:20}") int poolSize) {
    return Schedulers.fromExecutor(Executors.newFixedThreadPool(poolSize));
}
```

### Controller 응답 형식
```
@RestController
@RequestMapping("/api/{name}s")
@RequiredArgsConstructor
@Tag(name = "{Name} API")
public class {Name}Controller {

    private final {Name}Service {name}Service;

    @PostMapping("{path}")
    @Operation(summary = "...")
    @ResponseStatus(HttpStatus.OK)
    public Mono<Void> methodName(...) {
        return {name}Service.method(...);
    }
}
```

### Flyway 마이그레이션 네이밍
```
V{버전}__{snake_case_설명}.sql
예) V1__create_users_table.sql
    V2__add_schedules_table.sql
```
- `V` 대문자, 언더스코어 2개(`__`), 버전은 순차 증가
- 한 번 적용된 파일은 절대 수정 금지

---

## 3. 테스트 패턴

### Service 단위 테스트
```
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    UserRepository userRepository;

    @InjectMocks
    UserServiceImpl userService;

    @BeforeEach
    void injectScheduler() throws Exception {
        // 비동기 코드를 동기적으로 테스트하기 위해 immediate() 주입
        Field f = UserServiceImpl.class.getDeclaredField("dbScheduler");
        f.setAccessible(true);
        f.set(userService, Schedulers.immediate());
    }

    @Test
    void 신규유저_회원가입_저장호출() {
        given(userRepository.findById("new_id")).willReturn(Optional.empty());

        StepVerifier.create(userService.signup("new_id"))
                .verifyComplete();

        verify(userRepository).save(any(User.class));
    }
}
```

### Controller 슬라이스 테스트
```
@SpringBootTest
@AutoConfigureWebTestClient
@ActiveProfiles("test")
@TestPropertySource(locations = "file:.env")
class UserControllerTest {

    @Autowired
    WebTestClient webTestClient;

    @MockBean
    UserService userService;

    @Test
    void 유효한JWT_200반환() {
        given(userService.signup(any())).willReturn(Mono.empty());

        webTestClient
            .mutateWith(SecurityMockServerConfigurers.mockJwt())
            .post().uri("/api/users/signup")
            .exchange()
            .expectStatus().isOk();
    }

    @Test
    void JWT없음_401반환() {
        webTestClient
            .post().uri("/api/users/signup")
            .exchange()
            .expectStatus().isUnauthorized();
    }
}
```

### 테스트 환경 설정
`application-test.properties` — DB만 H2로 전환, 나머지는 `.env`에서 공급:
- **DB** → H2 인메모리 (PostgreSQL 모드), Flyway 비활성화
- **Redis / Clerk JWT / AI URL** → `.env` 실제 자격증명 사용

모든 `@SpringBootTest` 기반 테스트는 `@TestPropertySource(locations = "file:.env")`를 함께 선언한다.
전체 테스트 스위트 실행 시 `.env`가 필수다.

### 테스트 원칙 요약
| 레이어 | 종류 | DB | 외부 서비스 |
|--------|------|----|------------|
| Service | Unit (Mockito) | Mock | Mock |
| Controller | Slice (SpringBootTest) | H2 | .env 자격증명 + mockJwt() |
| Config/인프라 | Integration | 실제 Supabase | 실제 Redis |

- 리액티브 반환값은 항상 `StepVerifier`로 검증
- Scheduler는 단위 테스트에서 `Schedulers.immediate()`로 교체
