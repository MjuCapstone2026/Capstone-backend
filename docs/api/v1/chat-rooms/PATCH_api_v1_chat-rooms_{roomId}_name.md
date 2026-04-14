## **[PATCH] api/v1/chat-rooms/{roomId}/name**

현재 로그인한 사용자가 소유한 특정 채팅방의 이름을 수정합니다.

---

### **1. 기본 정보**

| 항목 | 내용 |
| --- | --- |
| Method | `PATCH` |
| URL | `api/v1/chat-rooms/{roomId}/name` |
| Summary | 채팅방 이름 수정 |
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
| roomId | Y | UUID | 이름을 수정할 채팅방의 고유 ID |

### **2.3 Body**

```json
{
  "name": "오사카 3박 4일 여행"
}
```

| Field | Required | Type | Description |
| --- | --- | --- | --- |
| `name` | Y | String | 변경할 채팅방 이름 |

---

### **3. 응답 (Response)**

### **3.1 성공 (200 OK)**

- **Description**: 채팅방 이름이 성공적으로 수정되었습니다.

```json
{
  "roomId": "9d12a7e5-2a7b-4e86-bf5c-2d1b50dcb1a4",
  "name": "오사카 3박 4일 여행",
  "updatedAt": "2026-04-03T22:00:00"
}
```

### **3.2 잘못된 요청 (400 Bad Request)**

- **Description**: 필수 필드가 누락되었거나 유효하지 않은 값이 입력된 경우입니다.

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "name must not be blank."
}
```

### **3.3 인증 실패 (401 Unauthorized)**

- **Description**: JWT 토큰이 누락되었거나 만료, 서명 오류 등으로 유효하지 않은 경우입니다.

```json
{
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid or expired token."
}
```

### **3.4 권한 없음 (403 Forbidden)**

- **Description**: 해당 채팅방의 소유자가 아닌 경우입니다.

```json
{
  "status": 403,
  "error": "Forbidden",
  "message": "You do not have permission to update this chat room."
}
```

### **3.5 리소스 없음 (404 Not Found)**

- **Description**: 해당 `roomId`에 해당하는 채팅방이 존재하지 않는 경우입니다.

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Chat room not found."
}
```

### **3.6 서버 오류 (500 Internal Server Error)**

- **Description**: 채팅방 이름 수정 중 서버 내부 오류가 발생한 경우입니다.

```json
{
  "status": 500,
  "error": "Internal Server Error",
  "message": "Failed to update chat room name."
}
```

---

### **4. 비즈니스 로직 및 DB 스키마**

### **4.1 동작 흐름 (Sequence)**

1. **Token Parsing**: Authorization 헤더에서 JWT를 추출하고 검증합니다.
2. **Claim Extraction**: JWT의 `sub` 클레임을 추출하여 `clerk_id`로 사용합니다.
3. **Validation**: 요청 바디의 `name`이 비어있으면 400을 반환합니다.
4. **Resource Check**: `roomId`로 `chat_rooms` 테이블을 조회합니다. 존재하지 않으면 404를 반환합니다.
5. **Authorization Check**: 조회한 채팅방의 `clerk_id`가 요청자의 `clerk_id`와 일치하는지 확인합니다. 일치하지 않으면 403을 반환합니다.
6. **Update**: `chat_rooms` 테이블의 `name` 컬럼을 업데이트하고 `updated_at`을 현재 시각으로 갱신합니다.
7. **Return**: 수정된 `roomId`, `name`, `updatedAt`을 반환합니다.

### **4.2 DB 수정 구조 (`chat_rooms` Table)**

| Column | Type | Description |
| --- | --- | --- |
| `id` | UUID | 채팅방 고유 ID |
| `clerk_id` | VARCHAR(255) | 채팅방 소유자 식별자 |
| `name` | VARCHAR(255) | 수정할 채팅방 이름 |
| `updated_at` | TIMESTAMP | 마지막 수정 시각 (자동 갱신) |

---

### **5. 호출 예시 (Example)**

```bash
curl -X PATCH https://your-api-domain.com/v1/chat-rooms/{roomId}/name \
  -H "Authorization: Bearer <clerk_jwt_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "오사카 3박 4일 여행"
  }'
```