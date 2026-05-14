## **[GET] /api/v1/chat-messages/{roomId}**

채팅방의 메시지 히스토리를 조회합니다.

---

### **1. 기본 정보**

| 항목 | 내용 |
| --- | --- |
| Method | `GET` |
| URL | `/api/v1/chat-messages/{roomId}` |
| Summary | 메시지 히스토리 조회 |
| Authentication | Bearer JWT (Clerk 발급 토큰 필수) |

---

### **2. 요청 (Request)**

#### **2.1 Headers**

| Name | Required | Example | Description |
| --- | --- | --- | --- |
| Authorization | Y | `Bearer eyJhbGci...` | Clerk에서 발급받은 유효한 JWT 토큰 |

#### **2.2 Path Parameter**

| Name | Required | Type | Description |
| --- | --- | --- | --- |
| roomId | Y | UUID | 조회할 채팅방의 고유 ID |

#### **2.3 Query Parameter**

| Name | Required | Type | Default | Description |
| --- | --- | --- | --- | --- |
| cursor | N | `OffsetDateTime` (ISO-8601) | - | 페이지네이션 기준 메시지의 created_at (이 시각 이전 메시지 조회) |
| limit | N | INT | 30 | 한 번에 조회할 메시지 수 |

---

### **3. 응답 (Response)**

#### **3.1 성공 (200 OK)**

- **Description**: 메시지 히스토리가 성공적으로 조회되었습니다.

    ```json
    {
      "roomId": "bbb-222",
      "messages": [
        {
          "messageId": "msg-040",
          "role": "user",
          "content": "북촌한옥마을 대신 인사동으로 바꿔줘",
          "createdAt": "2026-04-03T21:50:00"
        },
        {
          "messageId": "msg-039",
          "role": "assistant",
          "content": "북촌한옥마을을 인사동으로 변경했습니다!",
          "actionResult": {
            "itineraryId": "aaa-111",
            "destinations": [{"city": "서울", "start_date": "2026-05-01", "end_date": "2026-05-04"}],
            "startDate": "2026-05-01",
            "endDate": "2026-05-04",
            "totalDays": 4,
            "budget": 500000.00,
            "adultCount": 2,
            "childCount": 0,
            "childAges": []
          },
          "createdAt": "2026-04-03T21:49:30"
        },
        {
          "messageId": "msg-038",
          "role": "user",
          "content": "창덕궁 후원 예약은 어떻게 해?",
          "createdAt": "2026-04-03T21:40:00"
        },
        {
          "messageId": "msg-037",
          "role": "assistant",
          "content": "창덕궁 후원은 문화재청 홈페이지에서 사전 예약 가능합니다.",
          "createdAt": "2026-04-03T21:39:30"
        },
        {
          "messageId": "msg-036",
          "role": "user",
          "content": "2일차 남산타워 몇 시가 좋을까?",
          "createdAt": "2026-04-03T21:30:00"
        },
        {
          "messageId": "msg-035",
          "role": "assistant",
          "content": "일몰 시간대인 오후 6시 전후가 야경도 볼 수 있어서 추천드립니다.",
          "createdAt": "2026-04-03T21:29:30"
        },
        {
          "messageId": "msg-034",
          "role": "user",
          "content": "광장시장 근처 주차 가능해?",
          "createdAt": "2026-04-03T21:20:00"
        },
        {
          "messageId": "msg-033",
          "role": "assistant",
          "content": "광장시장 인근에 을지로 공영주차장 이용 가능합니다.",
          "createdAt": "2026-04-03T21:19:30"
        },
        {
          "messageId": "msg-032",
          "role": "user",
          "content": "경복궁 입장료 얼마야?",
          "createdAt": "2026-04-03T21:10:00"
        },
        {
          "messageId": "msg-031",
          "role": "assistant",
          "content": "경복궁 입장료는 성인 기준 3,000원입니다.",
          "createdAt": "2026-04-03T21:09:30"
        }
      ],
      "nextCursor": "2026-04-03T21:09:30", 
      "hasMore": true
    }
    ```


#### **3.2 인증 실패 (401 Unauthorized)**

- **Description**: JWT 토큰이 누락되었거나 만료, 서명 오류 등으로 유효하지 않은 경우입니다.

    ```json
    {
      "status": 401,
      "error": "Unauthorized",
      "message": "Invalid or expired token."
    }
    ```


#### **3.3 권한 없음 (403 Forbidden)**

- **Description:** 해당 채팅방의 소유자가 아닌 경우입니다.

    ```json
    {
      "status": 403,
      "error": "Forbidden",
      "message": "You do not have permission to access this chat room."
    }
    ```


#### **3.4 리소스 없음 (404 Not Found)**

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
5. **Query**: `room_id = roomId` 조건으로 chat_messages를 `created_at` 내림차순 정렬하여 조회합니다. `cursor`가 있으면 `created_at < :cursor` 조건으로 해당 시각 이전 데이터부터 조회합니다.
6. **Response**: 조회한 메시지 목록과 페이지네이션 정보를 반환합니다.

#### **4.2 DB 조회 구조 (chat_messages Table)**

**응답 메시지 아이템 필드**

| Field | Type | Description |
| --- | --- | --- |
| `messageId` | UUID | 메시지 고유 ID |
| `role` | String | 발화 주체 (`user` / `assistant`) |
| `content` | String | 메시지 본문 |
| `actionResult` | Object \| null | AI가 수행한 액션 결과 스냅샷. `user` 메시지 및 일반 대화(`chat` 타입) `assistant` 메시지는 `null`. `change`, `reservation`, `cancel`, `itinerary` 타입 처리 시 저장된 결과 객체 반환 |
| `createdAt` | String (ISO 8601) | 전송 일시 |

**DB 조회 구조 (chat_messages Table)**

| Column | Type | Description |
| --- | --- | --- |
| `id` | UUID | 메시지 고유 ID |
| `room_id` | UUID | 소속 채팅방 ID |
| `role` | VARCHAR(20) | 발화 주체 (`user` / `assistant` / `tool`) |
| `content` | TEXT | 메시지 본문 |
| `action_result` | JSONB | AI 액션 결과 스냅샷 (nullable) |
| `created_at` | TIMESTAMP | 전송 일시 |

> `embedding`은 응답에 포함하지 않습니다.
>

---

### **5. 호출 예시 (Example)**

```bash
curl -X GET "https://your-api-domain.com/api/v1/chat-messages/{roomId}?limit=50" \
  -H "Authorization: Bearer <clerk_jwt_token>"
```
