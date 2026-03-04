# 📍 MJU Capstone Backend Spec
> **명지대학교 자연캠퍼스 가이드 및 사용자 맞춤형 여행 일정을 설계하는 스마트 AI 에이전트**

---

## 🛠 1. Tech Stack & Versions
* **Language:** Java 21 (LTS)
* **Framework:** Spring Boot 4.0.3(WebFlux/Reactive)
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

---

## 📖 5. Swagger API 문서 (Documentation)
서버를 실행한 후 아래 주소에서 API 명세서를 확인하고 직접 테스트할 수 있습니다.

* **Swagger UI 주소:** [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
* **API Docs (JSON):** [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)