---
name: new-api
description: 새 API 엔드포인트 추가 — Controller 메서드 + Service interface/impl 메서드 작성
argument-hint: [HTTP메서드] [경로] (예: POST /api/schedules)
allowed-tools: Read, Write, Edit, Glob
---

@docs/conventions.md 의 코딩 규칙을 준수한다.

## 입력
사용자가 HTTP 메서드와 경로를 제공한다. (예: `GET /api/schedules/{id}`)

## 절차

### 1. 기존 Controller 확인
- 해당 도메인의 Controller 파일을 Read로 읽는다.
- 없으면 사용자에게 `/new-domain` 먼저 실행할 것을 안내한다.

### 2. Controller 메서드 추가
아래 규칙에 맞게 메서드를 추가한다:
- 반환 타입: `Mono<Void>`, 상태코드는 `@ResponseStatus(HttpStatus.XXX)` 로 선언
- `@Operation(summary = "한 줄 설명")` 필수
- JPA 호출이 있으면 `Mono.fromCallable(...).subscribeOn(dbScheduler)` 패턴 사용

```
@{HttpMethod}Mapping("{path}")
@Operation(summary = "...")
@ResponseStatus(HttpStatus.OK)
public Mono<Void> {methodName}(...) {
    return {name}Service.method(...);
}
```

### 3. Service interface + ServiceImpl 메서드 추가
- `{Name}Service` interface에 메서드 선언
- `{Name}ServiceImpl`에 구현

### 4. 완료 후 안내
- 추가된 Controller 메서드 경로
- Flyway 마이그레이션이 필요한 경우 `/new-migration` 실행을 안내
