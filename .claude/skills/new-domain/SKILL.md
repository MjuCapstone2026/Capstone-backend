---
name: new-domain
description: 새 도메인 모듈 생성 — controller/service/serviceImpl/repository/entity 뼈대 코드 작성
---

@docs/conventions.md 의 코딩 규칙을 준수하여 새 도메인을 생성한다.

## 입력
사용자가 도메인 이름을 제공한다. (예: `schedule`, `place`)

## 생성할 파일 목록
아래 4개 파일을 `src/main/java/com/mju/capstone_backend/domain/{name}/` 아래에 생성한다.

### 1. entity/{Name}.java
- `@Entity`, `@Table(name = "{name}s")`
- `@NoArgsConstructor(access = AccessLevel.PROTECTED)`
- `@Getter`, `@RequiredArgsConstructor`
- 정적 팩터리 메서드 `{Name}.of(...)` 포함
- PK는 `@Id @GeneratedValue`

### 2. repository/{Name}Repository.java
- `JpaRepository<{Name}, Long>` 상속 (PK 타입은 엔티티에 맞게)
- 추가 쿼리 메서드가 있으면 interface에 선언

### 3. service/{Name}Service.java + service/{Name}ServiceImpl.java
- `{Name}Service`는 interface
- `{Name}ServiceImpl`은 `@Service` + `@RequiredArgsConstructor`
- JPA 호출은 반드시 `Mono.fromCallable(() -> ...).subscribeOn(dbScheduler)` 패턴 사용
- `dbScheduler`는 생성자 주입

### 4. controller/{Name}Controller.java
- `@RestController`, `@RequestMapping("/api/{name}s")`
- `@RequiredArgsConstructor`
- `@Tag(name = "{Name} API")` (Swagger)
- 반환 타입은 `Mono<Void>`, 상태코드는 `@ResponseStatus(HttpStatus.OK)` 사용

## 생성 후 안내
- Flyway 마이그레이션 파일(`V{n}__{설명}.sql`) 작성이 필요하면 사용자에게 알린다.
- 테스트 파일 뼈대도 생성할지 사용자에게 묻는다.
