## **[GET] /api/v1/chat-rooms**

현재 로그인한 사용자의 채팅방 목록을 조회합니다.

---

### **1. 기본 정보**

| 항목 | 내용                          |
| --- |-----------------------------|
| Method | `GET`                       |
| URL | `/api/v1/chat-rooms` |
| Summary | 내 채팅방 목록 조회                 |
| Authentication | Bearer JWT (Clerk 발급 토큰 필수) |

---

### **2. 요청 (Request)**

### **2.1 Headers**

| Name | Required | Example | Description |
| --- | --- | --- | --- |
| Authorization | Y | `Bearer eyJhbGci...` | Clerk에서 발급받은 유효한 JWT 토큰 |

### **2.2 Query Parameter**

- 없음

### **2.3 Body**

- 없음

---

### **3. 응답 (Response)**

### **3.1 성공 (200 OK)**

- **Description**: 현재 로그인한 사용자의 채팅방 목록을 성공적으로 조회했습니다.
- **Body**:

```json
{
  "rooms": [
    {
      "roomId": "9d12a7e5-2a7b-4e86-bf5c-2d1b50dcb1a4",
      "name": "3박 4일 오사카 여행",
      "clerkId": "user_2N...",
      "aiSummary": "오사카 3박 4일 여행 계획 중",
      "preferences": {
        "budget": "economy",
        "style": "food"
      },
      "createdAt": "2026-04-01T10:00:00",
      "updatedAt": "2026-04-03T22:00:00"
    },
    {
      "roomId": "15fdb4a4-7d9a-4b7c-9bcb-8d9a4f45be21",
      "name": "1박 2일 부산 여행",
      "clerkId": "user_2N...",
      "aiSummary": null,
      "preferences": null,
      "createdAt": "2026-04-02T14:30:00",
      "updatedAt": "2026-04-02T14:30:00"
    }
  ]
}
```

> 사용자의 채팅방이 하나도 없는 경우, `rooms`는 빈 배열 `[]` 로 반환합니다.
> 

예시:

```json
{
  "rooms": []
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

### **3.3 사용자 없음 (404 Not Found)**

- **Description**: JWT는 유효하지만 서비스 DB에 해당 사용자가 존재하지 않는 경우입니다.

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "User not found. Please sign up first."
}
```

### **3.4 서버 오류 (500 Internal Server Error)**

- **Description**: 채팅방 목록 조회 중 서버 내부 오류가 발생한 경우입니다.

```json
{
  "status": 500,
  "error": "Internal Server Error",
  "message": "Failed to fetch chat rooms."
}
```

---

### **4. 비즈니스 로직 및 DB 스키마**

### **4.1 동작 흐름 (Sequence)**

1. **Token Parsing**: Authorization 헤더에서 JWT를 추출하고 검증합니다.
2. **Claim Extraction**: JWT의 `sub` 클레임을 추출하여 `clerk_id`로 사용합니다.
3. **User Check**: `users` 테이블에서 해당 `clerk_id` 사용자가 존재하는지 확인합니다. 존재하지 않으면 404를 반환합니다.
4. **Query**: `chat_rooms` 테이블에서 요청자의 `clerk_id`와 일치하는 채팅방 목록을 조회합니다.
5. **Sort**: 채팅방 목록은 최근 수정 순(`updated_at DESC`)으로 정렬하여 반환합니다.
6. **Return**: 조회된 채팅방 목록을 응답으로 반환합니다.

### **4.2 DB 조회 구조 (`chat_rooms` Table)**

| Column | Type | Description |
| --- | --- | --- |
| `id` | UUID | 채팅방 고유 ID |
| `clerk_id` | VARCHAR(255) | 채팅방 소유자 식별자 |
| `ai_summary` | TEXT | AI가 요약한 대화 맥락 |
| `preferences` | JSONB | 사용자 여행 선호도 |
| `created_at` | TIMESTAMP | 생성 시각 |
| `updated_at` | TIMESTAMP | 마지막 수정 시각 |

---

### **5. 호출 예시 (Example)**

```bash
curl -X GET https://your-api-domain.com/api/v1/chat-rooms \
  -H "Authorization: Bearer <clerk_jwt_token>"
```