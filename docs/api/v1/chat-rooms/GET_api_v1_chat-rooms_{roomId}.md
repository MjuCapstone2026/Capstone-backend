## **[GET] api/v1/chat-rooms/{roomId}**

현재 로그인한 사용자가 소유한 특정 채팅방의 메타 정보를 조회합니다.

필요 시 연결된 현재 여행 일정의 `itineraryId`도 함께 반환합니다.

---

### **1. 기본 정보**

| 항목 | 내용                          |
| --- |-----------------------------|
| Method | `GET`                       |
| URL | `api/v1/chat-rooms/{roomId}`  |
| Summary | 채팅방 상세 조회                   |
| Authentication | Bearer JWT (Clerk 발급 토큰 필수) |

---

### **2. 요청 (Request)**

### **2.1 Headers**

| Name | Required | Example | Description |
| --- | --- | --- | --- |
| Authorization | Y | `Bearer eyJhbGci...` | Clerk에서 발급받은 유효한 JWT 토큰 |

### **2.2 Path Parameter**

| Name | Required | Type | Description |
| --- | --- | --- | --- |
| roomId | Y | UUID | 조회할 채팅방의 고유 ID |

### **2.3 Body**

- 없음

---

### **3. 응답 (Response)**

### **3.1 성공 (200 OK)**

- **Description**: 채팅방 상세 정보를 성공적으로 조회했습니다.

```json
{
  "roomId": "9d12a7e5-2a7b-4e86-bf5c-2d1b50dcb1a4",
  "aiSummary": "오사카 3박 4일 여행 계획 중",
  "preferences": {
    "budget": "economy",
    "style": "food"
  },
  "itineraryId": "be4d9d2d-1d84-4b1b-bf4d-1ac6b9cc7f22",
  "createdAt": "2026-04-01T10:00:00",
  "updatedAt": "2026-04-03T22:00:00"
}
```

- **연결된 현재 일정이 아직 없는 경우 예시**

```json
{
  "roomId": "9d12a7e5-2a7b-4e86-bf5c-2d1b50dcb1a4",
  "aiSummary": null,
  "preferences": null,
  "itineraryId": null,
  "createdAt": "2026-04-01T10:00:00",
  "updatedAt": "2026-04-01T10:00:00"
}
```

### **3.2 인증 실패 (401 Unauthorized)**

- **Description**: JWT 토큰이 누락되었거나 만료, 서명 오류 등으로 유효하지 않은 경우입니다.

```json
{
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid or expired token."
}
```

### **3.3 권한 없음 (403 Forbidden)**

- **Description**: 해당 채팅방의 소유자가 아닌 경우입니다.

```json
{
  "status": 403,
  "error": "Forbidden",
  "message": "You do not have permission to access this chat room."
}
```

### **3.4 리소스 없음 (404 Not Found)**

- **Description**: 해당 `roomId`에 해당하는 채팅방이 존재하지 않는 경우입니다.

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Chat room not found."
}
```

### **3.5 서버 오류 (500 Internal Server Error)**

- **Description**: 채팅방 상세 조회 중 서버 내부 오류가 발생한 경우입니다.

```json
{
  "status": 500,
  "error": "Internal Server Error",
  "message": "Failed to fetch chat room."
}
```

---

### **4. 비즈니스 로직 및 DB 스키마**

### **4.1 동작 흐름 (Sequence)**

1. **Token Parsing**: Authorization 헤더에서 JWT를 추출하고 검증합니다.
2. **Claim Extraction**: JWT의 `sub` 클레임을 추출하여 `clerk_id`로 사용합니다.
3. **Resource Check**: `roomId`로 `chat_rooms` 테이블을 조회합니다. 존재하지 않으면 404를 반환합니다.
4. **Authorization Check**: 조회한 채팅방의 `clerk_id`가 요청자의 `clerk_id`와 일치하는지 확인합니다. 일치하지 않으면 403을 반환합니다.
5. **Join Itinerary**: `chat_rooms.id = itineraries.room_id` 조건으로 현재 연결된 일정이 있는지 조회합니다. 현재 구조상 `itineraries.room_id`는 UNIQUE이므로 최대 1건만 존재합니다.
6. **Return**: 채팅방 메타 정보와 `itineraryId`를 함께 반환합니다. 일정이 없으면 `itineraryId`는 `null`입니다.

### **4.2 DB 조회 구조 (`chat_rooms`, `itineraries`)**

| Column | Type | Description |
| --- | --- | --- |
| `chat_rooms.id` | UUID | 채팅방 고유 ID |
| `chat_rooms.clerk_id` | VARCHAR(255) | 채팅방 소유자 식별자 |
| `chat_rooms.ai_summary` | TEXT | AI가 요약한 대화 맥락 |
| `chat_rooms.preferences` | JSONB | 사용자 여행 선호도 |
| `chat_rooms.created_at` | TIMESTAMP | 채팅방 생성 시각 |
| `chat_rooms.updated_at` | TIMESTAMP | 채팅방 마지막 수정 시각 |
| `itineraries.id` | UUID | 연결된 현재 일정 ID |
| `itineraries.room_id` | UUID | 채팅방과 1:1 연결된 FK |

---

### **5. 호출 예시 (Example)**

```bash
curl -X GET https://your-api-domain.com/v1/chat-rooms/{roomId} \
  -H "Authorization: Bearer <clerk_jwt_token>"
```