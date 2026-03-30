# 📍 MJU Capstone Backend Spec
> **명지대학교 자연캠퍼스 가이드 및 사용자 맞춤형 여행 일정을 설계하는 스마트 AI 에이전트**

---

## 🛠 1. Tech Stack & Versions
* **Language:** Java 21 (LTS)
* **Framework:** Spring Boot 3.5.13(WebFlux/Reactive)
* **Build Tool:** Gradle 8.11.1
* **Main DB:** PostgreSQL (Supabase) - 동기식(JPA) 격리 운용
* **Cache/Session:** Redis (Upstash)- 비동기식(Reactive) 운용
* **Migration:** Flyway 10.x
* **Documentation:** Swagger (SpringDoc OpenAPI 2.x - WebFlux 버전)

---

## 📦 2. Core Libraries
* `spring-boot-starter-web`: REST API 구현
* `spring-boot-starter-data-jpa`: 데이터베이스 ORM
* `spring-boot-starter-data-redis-reactive`: Reactive Redis를 통한 비동기 캐싱
* `spring-dotenv`: `.env` 환경 변수 자동 로드
* `lombok`: 코드 자동 생성 (Getter, Setter 등)
* `springdoc-openapi-starter-webflux-ui`: WebFlux 전용 Swagger UI

---

## ☕ 3. IntelliJ에서 Java 21 설치 단계
팀원 중 Java 21이 설치되지 않은 경우 아래 단계를 따르세요.

1. **Project Structure** 실행 (`Ctrl + Alt + Shift + S`)
2. **Project** 탭 -> **SDK** 항목에서 `Add SDK` > `Download JDK...` 클릭
3. **Version:** `21` 선택
4. **Vendor:** `Amazon Corretto` 또는 `Oracle OpenJDK` 선택 후 `Download`
5. **Gradle 설정 변경:** `Settings` > `Build, Execution, Deployment` > `Build Tools` > `Gradle`에서 **Gradle JVM**을 방금 다운로드한 Java 21로 변경


---

## 🧪 4. 테스트 명령어 (Terminal)
터미널(PowerShell/CMD)에서 프로젝트 루트 경로로 이동 후 입력합니다.

* **전체 테스트 실행 (환경 변수 로드 포함):**
  ```powershell
  .\gradlew.bat clean test
  ```

---

## 🗃 5. DB 마이그레이션 (Flyway)

### 자동 실행
Flyway는 **애플리케이션 시작 시 자동으로 마이그레이션을 실행**합니다. 별도의 명령어 없이 서버를 구동하면 됩니다.

```powershell
.\gradlew.bat bootRun
```

### 마이그레이션 파일 위치
```
src/main/resources/db/migration/
```

### 파일 네이밍 규칙
```
V{버전}__{설명}.sql
예) V1__create_users_table.sql
    V2__add_schedule_table.sql
```
> `V` (대문자) + 버전 번호 + `__` (언더스코어 2개) + 설명 + `.sql`

### 현재 마이그레이션 파일
| 파일 | 설명 |
|------|------|
| `V1__create_users_table.sql` | `users` 테이블 생성 (clerk_id, created_at) |

### Flyway Gradle 플러그인으로 수동 실행 (선택)
`build.gradle`에 플러그인을 추가하면 터미널에서 직접 명령어를 실행할 수 있습니다.

```groovy
// build.gradle plugins 블록에 추가
id 'org.flywaydb.flyway' version '10.20.1'

// flyway 설정 블록 추가 (최상단 수준)
flyway {
    url      = System.getenv('DB_URL')
    user     = System.getenv('DB_USERNAME')
    password = System.getenv('DB_PASSWORD')
}
```

플러그인 추가 후 사용 가능한 명령어:

```powershell
# 마이그레이션 실행
.\gradlew.bat flywayMigrate

# 현재 마이그레이션 적용 상태 확인
.\gradlew.bat flywayInfo

# 체크섬 불일치 등 오류 복구 (개발 환경에서만 사용)
.\gradlew.bat flywayRepair

# 모든 테이블 삭제 후 재마이그레이션 (절대 운영 환경에서 사용 금지)
.\gradlew.bat flywayClean flywayMigrate
```

---

## 📖 6. Swagger API 문서 (Documentation)
서버를 실행한 후 아래 주소에서 API 명세서를 확인하고 직접 테스트할 수 있습니다.

* **Swagger UI 주소:** [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
* **API Docs (JSON):** [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)
