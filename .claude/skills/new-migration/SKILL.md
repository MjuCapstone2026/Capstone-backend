---
name: new-migration
description: 다음 Flyway 마이그레이션 SQL 파일 생성 — 로컬 파일 스캔으로 버전 자동 결정
argument-hint: [테이블/변경 설명 (영문 snake_case)]
allowed-tools: Glob, Read, Write
---

## 절차

1. `src/main/resources/db/migration/` 디렉토리의 파일 목록을 Glob으로 스캔한다.
2. `V{숫자}__*.sql` 패턴에서 가장 높은 버전 번호를 찾는다.
3. 다음 버전 번호 = 현재 최대 + 1
4. 사용자가 설명을 전달했으면 그대로 사용, 없으면 물어본다.
5. 아래 네이밍 규칙에 맞게 빈 SQL 파일을 생성한다.
6. 파일 생성 후 경로와 버전을 사용자에게 알린다.

## 네이밍 규칙
```
V{버전}__{snake_case_설명}.sql
예) V2__add_schedules_table.sql
    V3__add_index_on_users_clerk_id.sql
```
- `V` 반드시 대문자
- 언더스코어 2개(`__`)
- 설명은 영문 소문자 + 언더스코어
- **한 번 생성된 파일은 절대 수정 금지** (Flyway 체크섬 오류 발생)

## 생성할 파일 템플릿
```sql
-- V{버전}__{설명}.sql
-- 작성자: {작성자}
-- 설명: {변경 내용 한 줄 요약}

```
