## **[POST] /api/v1/chat-rooms/{roomId}/messages**

사용자 메시지를 전송하고 AI Agent의 응답을 스트리밍으로 반환합니다.

---

### **1. 기본 정보**

| 항목 | 내용 |
| --- | --- |
| Method | `POST` |
| URL | `/api/v1/chat-rooms/{roomId}/messages` |
| Summary | 메시지 전송 및 AI Agent 응답 스트리밍 |
| Authentication | Bearer JWT (Clerk 발급 토큰 필수) |
| Response Type | `text/event-stream` (SSE) |

---

### **2. 요청 (Request)**

#### **2.1 Headers**

| Name | Required | Example | Description |
| --- | --- | --- | --- |
| Authorization | Y | `Bearer eyJhbGci...` | Clerk에서 발급받은 유효한 JWT 토큰 |

#### **2.2 Path Parameter**

| Name | Required | Type | Description |
| --- | --- | --- | --- |
| roomId | Y | UUID | 메시지를 전송할 채팅방의 고유 ID |

#### **2.3 Body**

```json
{
  "content": "경복궁 대신 창덕궁으로 바꿔줘"
}
```

| Field | Required | Type | Description |
| --- | --- | --- | --- |
| content | Y | String | 사용자가 전송한 메시지 본문 |

---

### **3. 응답 (Response)**

#### **3.1 성공 - 스트리밍 청크 (200 OK, SSE)**

- **Description**: AI Agent가 토큰 단위로 응답을 스트리밍합니다.

##### SSE Event: `chunk`

```json
{
  "content": "창"
}
```

##### SSE Event: `chunk`

```json
{
  "content": "덕"
}
```

##### SSE Event: `chunk`

```json
{
  "content": "궁으로 변경했습니다!"
}
```

#### **3.2 성공 - 스트리밍 완료**

- **Description**: 스트리밍이 완료되면 최종 메타데이터를 포함한 `done` 이벤트가 전송됩니다.

##### 일반 메시지일 때

###### SSE Event: `done`

```json
{
  "userMessage": {
    "messageId": "msg-041",
    "role": "user",
    "content": "경복궁 대신 창덕궁으로 바꿔줘",
    "createdAt": "2026-04-03T22:00:00"
  },
  "assistantMessage": {
    "messageId": "msg-042",
    "role": "assistant",
    "content": "네, 어떤 도움이 필요하신가요?",
    "createdAt": "2026-04-03T22:00:05"
  }
}
```

##### itinerary 변경일 때

###### SSE Event: `done`

```json
{
  "userMessage": {
    "messageId": "msg-041",
    "role": "user",
    "content": "경복궁 대신 창덕궁으로 바꿔줘",
    "createdAt": "2026-04-03T22:00:00"
  },
  "assistantMessage": {
    "messageId": "msg-042",
    "role": "assistant",
    "content": "창덕궁으로 변경했습니다!",
    "createdAt": "2026-04-03T22:00:05"
  },
  "itinerary": {
    "itineraryId": "aaa-111",
    "startDate": "2026-05-01",
    "endDate": "2026-05-04",
    "dayPlans": {
      "2026-05-01": [
        {
          "index": 0,
          "time": "09:00 ~ 10:00",
          "place": "창덕궁",
          "note": "후원 투어 예약 필요",
          "status": "todo"
        }
      ]
    },
    "updatedAt": "2026-04-03T22:00:05"
  }
}
```

#### **3.3 인증 실패 (401 Unauthorized)**

- **Description**: JWT 토큰이 누락되었거나 만료, 서명 오류 등으로 유효하지 않은 경우입니다.

```json
{
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid or expired token."
}
```

#### **3.4 권한 없음 (403 Forbidden)**

- **Description**: 해당 채팅방의 소유자가 아닌 경우입니다.

```json
{
  "status": 403,
  "error": "Forbidden",
  "message": "You do not have permission to access this chat room."
}
```

#### **3.5 리소스 없음 (404 Not Found)**

- **Description**: 해당 roomId에 해당하는 채팅방이 존재하지 않는 경우입니다.

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Chat room not found."
}
```

---

### **4. 비즈니스 로직 및 DB 스키마**

#### **4.1 동작 흐름 (Sequence)**

1. **Token Parsing**: Authorization 헤더에서 JWT를 추출하고 검증합니다.
2. **Claim Extraction**: JWT의 `sub` 클레임을 추출하여 `clerk_id`로 사용합니다.
3. **Resource Check**: `roomId`로 chat_rooms 테이블을 조회합니다. 존재하지 않으면 404를 반환합니다.
4. **Authorization Check**: `chat_rooms.clerk_id`가 요청자의 `clerk_id`와 일치하는지 확인합니다. 일치하지 않으면 403을 반환합니다.
5. **FastAPI 호출**: WebClient로 FastAPI AI Agent에 사용자 메시지 및 `room_id`를 전달하고 SSE 스트리밍 요청을 보냅니다.
6. **스트리밍 전달**: FastAPI로부터 수신한 토큰을 Flux로 프론트엔드에 실시간 전달합니다.
7. **Embedding 생성**: 스트리밍 완료 후 FastAPI에서 사용자 메시지 및 assistant 메시지 본문을 기준으로 임베딩 벡터를 생성합니다.
8. **스트리밍 완료 후 분기 처리 (Java 서버에서 처리)**: FastAPI 응답의 `type`을 기준으로 DB 저장을 분기합니다. `done` 이벤트에 embedding 벡터를 포함하여 Java로 전달합니다. `userMessage` / `assistantMessage`는 항상 저장합니다.
    - `"chat"` → chat_messages 저장 + embedding 저장. `memory`가 있으면 `chat_rooms.ai_summary` / `preferences` 갱신. 추가 도메인 처리 없이 종료합니다.
    - `"itinerary"` → chat_messages 저장 + embedding 저장 + itineraries 수정 + itinerary_logs 스냅샷 저장
        - 저장 전 각 날짜의 아이템 배열을 `time` 오름차순으로 정렬하여 저장합니다.
    - `"change"` → chat_messages 저장 + embedding 저장 + itineraries 기본 정보 수정 + itinerary_logs 스냅샷 저장
    - `"reservation"` → chat_messages 저장 + embedding 저장 + reservations 저장 + reservation_histories 저장
    - `"cancel"` → chat_messages 저장 + embedding 저장 + reservations 취소 처리 + reservation_histories 저장
    - `null` → 하위 호환. `log.warn` 후 `"chat"`과 동일하게 처리합니다.
    - unknown type → `log.error` 후 `502 Bad Gateway`를 반환합니다. FastAPI ↔ Spring Boot 계약 위반으로 간주합니다.
9. **done 이벤트 전송**: 저장 완료 후 최종 메타데이터를 `done` 이벤트로 프론트엔드에 전송합니다. 아이템 배열은 `time` 오름차순으로 DB에 저장되며, 백엔드가 `index`를 부여하여 반환합니다. `dayPlans`에도 `index`를 추가하여 프론트에 반환합니다.

#### **4.2 DB 저장 구조**

**chat_messages Table**

| Column | Type | Description |
| --- | --- | --- |
| `room_id` | UUID | 소속 채팅방 ID |
| `role` | VARCHAR(20) | `user` / `assistant` |
| `content` | TEXT | 메시지 본문 |
| `embedding` | vector(1536) | 메시지 임베딩 벡터 |
| `created_at` | TIMESTAMP | 전송 일시 |

---

### **5. 호출 예시 (Example)**

```bash
curl -X POST https://your-api-domain.com/v1/chat-rooms/{roomId}/messages \
  -H "Authorization: Bearer <clerk_jwt_token>" \
  -H "Content-Type: application/json" \
  -d '{"content": "경복궁 대신 창덕궁으로 바꿔줘"}'
```
