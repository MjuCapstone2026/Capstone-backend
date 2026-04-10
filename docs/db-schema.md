# 여행 비서 AI Agent — DB 테이블 명세서

> **기술 스택**: PostgreSQL 16 + pgvector  
> **인증**: Clerk (외부) → users 테이블에 미러링  
> **총 테이블 수**: 8개

---

## 목차

1. [users](#1-users)
2. [chat_rooms](#2-chat_rooms)
3. [chat_messages](#3-chat_messages)
4. [itineraries](#4-itineraries)
5. [itinerary_logs](#5-itinerary_logs)
6. [itinerary_check_logs](#6-itinerary_check_logs)
7. [reservations](#7-reservations)
8. [reservation_histories](#8-reservation_histories)
9. [관계 요약](#관계-요약)
10. [설계 주요 결정 사항](#설계-주요-결정-사항)

---

## 1. users

Clerk에서 관리하는 사용자 정보를 로컬 DB에 미러링하는 테이블입니다.

| 컬럼명 | 타입 | 제약 | 설명 |
|---|---|---|---|
| `clerk_id` | VARCHAR(255) | PK | Clerk 발급 고유 ID (`user_2N...` 형태) |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT now() | 로컬 DB 최초 등록 일시 |

---

## 2. chat_rooms

사용자 1명이 여러 여행 계획을 가질 수 있으므로, 1개의 채팅방 = 1개의 여행 계획 컨텍스트로 설계합니다.

| 컬럼명 | 타입 | 제약 | 설명 |
|---|---|---|---|
| `id` | UUID | PK | 채팅방 고유 ID |
| `clerk_id` | VARCHAR(255) | NOT NULL, FK → users | 소유자 |
| `ai_summary` | TEXT | | AI가 생성한 대화 요약 |
| `preferences` | JSONB | | 사용자 여행 선호도 `{"budget":"economy","style":"adventure"}` |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT now() | 생성 일시 |
| `updated_at` | TIMESTAMP | NOT NULL, DEFAULT now() | 마지막 수정 일시 |

**인덱스**: `clerk_id`

---

## 3. chat_messages

채팅방 내 모든 대화 메시지를 저장합니다. `embedding`은 과거 대화 문맥을 벡터 유사도 검색할 때 사용합니다.

| 컬럼명 | 타입 | 제약 | 설명 |
|---|---|---|---|
| `id` | UUID | PK | 메시지 고유 ID |
| `room_id` | UUID | NOT NULL, FK → chat_rooms | 소속 채팅방 |
| `role` | VARCHAR(20) | NOT NULL, CHECK IN ('user','assistant','tool') | 발화 주체 |
| `content` | TEXT | NOT NULL | 메시지 본문 |
| `embedding` | vector(1536) | | 메시지 임베딩 벡터 (pgvector) |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT now() | 전송 일시 |

**인덱스**: `room_id`, `embedding` (IVFFlat, cosine)

---

## 4. itineraries

AI Agent가 생성·수정한 여행 일정입니다. `day_plans`는 실제 날짜를 Key로 사용하는 JSONB 구조입니다.

| 컬럼명 | 타입 | 제약 | 설명 |
|---|---|---|---|
| `id` | UUID | PK | 일정 고유 ID |
| `room_id` | UUID | NOT NULL, UNIQUE, FK → chat_rooms | 연결된 채팅방 |
| `total_days` | INT | NOT NULL, CHECK >= 1 | 총 여행 일수 |
| `start_date` | DATE | | 여행 시작일 |
| `end_date` | DATE | | 여행 종료일 |
| `status` | VARCHAR(20) | NOT NULL, DEFAULT 'draft' | 일정 상태 (`draft` / `completed`) |
| `day_plans` | JSONB | NOT NULL, DEFAULT '{}' | 날짜별 일정 상세 |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT now() | 생성 일시 |
| `updated_at` | TIMESTAMP | NOT NULL, DEFAULT now() | 마지막 수정 일시 |

**day_plans 구조 예시**

```json
{
  "2026-05-01": [
    {"time": "09:00", "place": "경복궁", "type": "sightseeing", "note": "한복 대여 추천"},
    {"time": "12:30", "place": "광장시장", "type": "meal", "note": "빈대떡 필수"}
  ],
  "2026-05-02": [
    {"time": "10:00", "place": "남산타워", "type": "sightseeing", "note": ""}
  ]
}
```

**인덱스**: `day_plans` (GIN)

---

## 5. itinerary_logs

`itineraries`가 수정될 때마다 변경 전 `day_plans` 스냅샷을 쌓는 이력 테이블입니다.

| 컬럼명 | 타입 | 제약 | 설명 |
|---|---|---|---|
| `id` | UUID | PK | 로그 고유 ID |
| `itinerary_id` | UUID | NOT NULL, FK → itineraries | 대상 일정 |
| `start_date` | DATE | 변경 전 여행 시작일 스냅샷 |
| `end_date` | DATE | 변경 전 여행 종료일 스냅샷 |
| `day_plans` | JSONB | NOT NULL | 변경 전 day_plans 스냅샷 |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT now() | 변경 발생 일시 |

**인덱스**: `itinerary_id`

---

## 6. itinerary_check_logs(보류)

AI Agent가 날씨·항공 변동 등을 주기적으로 점검한 결과를 기록합니다.

| 컬럼명 | 타입 | 제약 | 설명 |
|---|---|---|---|
| `id` | UUID | PK | 로그 고유 ID |
| `itinerary_id` | UUID | NOT NULL, FK → itineraries | 대상 일정 (변경: room_id → itinerary_id) |
| `check_date` | DATE | NOT NULL | 점검 기준 날짜 |
| `check_result` | TEXT | | 점검 결과 요약 |
| `proposed_changes` | JSONB | | AI가 제안한 변경 사항 |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT now() | 점검 실행 일시 |

**인덱스**: `itinerary_id`

---

## 7. reservations

예약 완료된 정보를 저장합니다. 항공·숙소·렌트카를 단일 테이블로 관리하며, 유형별 상세는 `detail` JSONB로 처리합니다.

| 컬럼명 | 타입 | 제약 | 설명 |
|---|---|---|---|
| `id` | UUID | PK | 예약 고유 ID |
| `itinerary_id` | UUID | NOT NULL, FK → itineraries | 연결된 일정 |
| `type` | VARCHAR(20) | NOT NULL, CHECK IN ('flight','accommodation','car_rental') | 예약 유형 |
| `status` | VARCHAR(20) | NOT NULL, DEFAULT 'confirmed' | 예약 상태 (`confirmed` / `changed` / `cancelled`) |
| `booked_by` | VARCHAR(10) | NOT NULL, DEFAULT 'user', CHECK IN ('user','ai') | 예약 주체 (`user`: 사용자 직접 / `ai`: AI Agent) |
| `booking_url` | TEXT | | AI가 사용자에게 제공한 예약 링크 |
| `external_ref_id` | VARCHAR(255) | | 외부 예약 번호 |
| `detail` | JSONB | NOT NULL, DEFAULT '{}' | 유형별 상세 정보 |
| `total_price` | DECIMAL(12,2) | | 총 결제 금액 |
| `currency` | VARCHAR(3) | DEFAULT 'KRW' | 통화 코드 |
| `reserved_at` | TIMESTAMP | | 예약 완료 일시 |
| `cancelled_at` | TIMESTAMP | | 취소 일시 |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT now() | 레코드 생성 일시 |
| `updated_at` | TIMESTAMP | NOT NULL, DEFAULT now() | 마지막 수정 일시 |

> `clerk_id` FK 제거 — `itinerary_id → room_id → clerk_id` 조인으로 사용자 조회 가능

**detail 구조 예시**

```json
// 항공권
{
  "airline": "대한항공",
  "flight_no": "KE123",
  "departure": {"airport": "ICN", "datetime": "2026-05-01T09:00:00"},
  "arrival":   {"airport": "NRT", "datetime": "2026-05-01T11:30:00"},
  "seat_class": "economy",
  "passengers": [{"name": "홍길동", "passport": "M12345678"}]
}

// 숙소
{
  "hotel_name": "롯데호텔 도쿄",
  "room_type": "디럭스 더블",
  "check_in": "2026-05-01",
  "check_out": "2026-05-03",
  "guests": 2
}

// 렌트카
{
  "company": "Hertz",
  "car_model": "Toyota Camry",
  "pickup":  {"location": "NRT T1", "datetime": "2026-05-01T13:00:00"},
  "dropoff": {"location": "NRT T1", "datetime": "2026-05-03T11:00:00"}
}
```

**인덱스**: `itinerary_id`, `status`, `detail` (GIN)

---

## 8. reservation_histories

예약 생성·변경·취소 이력을 추적하는 감사 로그(Audit Log)입니다.

| 컬럼명 | 타입 | 제약 | 설명 |
|---|---|---|---|
| `id` | UUID | PK | 이력 고유 ID |
| `reservation_id` | UUID | NOT NULL, FK → reservations | 대상 예약 |
| `action` | VARCHAR(20) | NOT NULL, CHECK IN ('created','changed','cancelled') | 액션 유형 |
| `previous_status` | VARCHAR(20) | | 변경 전 상태 |
| `new_status` | VARCHAR(20) | | 변경 후 상태 |
| `changed_detail` | JSONB | | 변경된 필드 스냅샷 |
| `reason` | TEXT | | 변경·취소 사유 |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT now() | 이력 생성 일시 |

**인덱스**: `reservation_id`

---

## 관계 요약

```
users ──< chat_rooms ──< chat_messages
                    ──< itineraries ──< itinerary_logs
                                   ──< itinerary_check_logs
                                   ──< reservations ──< reservation_histories
```

| 관계 | 카디널리티 | 설명 |
|---|---|---|
| users → chat_rooms | 1:N | 사용자는 여러 채팅방(여행 계획)을 가질 수 있음 |
| chat_rooms → chat_messages | 1:N | 채팅방에 여러 메시지 포함 |
| chat_rooms → itineraries | 1:1 | 채팅방에서 일정이 생성됨 |
| itineraries → itinerary_logs | 1:N | 일정 수정 시 변경 전 스냅샷 누적 |
| itineraries → itinerary_check_logs | 1:N | 일정 단위로 점검 로그 누적 |
| itineraries → reservations | 1:N | 하나의 일정에 여러 예약 포함 |
| reservations → reservation_histories | 1:N | 예약 변경 이력 추적 |

---

## 설계 주요 결정 사항

### 통합 예약 테이블
항공·숙소·렌트카를 각각 별도 테이블로 분리하지 않고, `type` 컬럼과 `detail` JSONB로 통합 관리합니다.

### reservations.clerk_id 제거
`itinerary_id → itineraries → room_id → chat_rooms → clerk_id` 조인으로 사용자를 특정할 수 있으므로 중복 저장을 제거했습니다.

### itinerary_check_logs FK 변경
`room_id` 대신 `itinerary_id`로 직접 연결해 불필요한 조인을 줄이고, 어떤 버전의 일정을 점검했는지 명확히 합니다.

### itinerary_logs 추가
`itineraries` 수정 시 변경 전 `day_plans`를 스냅샷으로 쌓아 일정 히스토리를 관리합니다.

### reservations.status 단순화
예약 완료된 데이터만 저장하는 테이블이므로 `confirmed`, `changed`, `cancelled`만 유지합니다.