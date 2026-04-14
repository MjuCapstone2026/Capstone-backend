# 채팅방 삭제

HTTP 요청 방식: DELETE
개발 상태: 진행중
도메인: CHAT-ROOMS
엔드포인트: /api/v1/chat-rooms/{roomId}
특이사항: 메시지, 일정 cascade
마지막 수정시각: 2026년 4월 14일 오후 2:26

## **[DELETE] /v1/chat-rooms/{roomId}**

현재 로그인한 사용자가 소유한 특정 채팅방을 삭제합니다.

채팅방 삭제 시 연결된 메시지와 현재 일정도 함께 삭제됩니다.

---

### **1. 기본 정보**

| 항목 | 내용 |
| --- | --- |
| Method | `DELETE` |
| URL | `/v1/chat-rooms/{roomId}` |
| Summary | 채팅방 삭제 |
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
| roomId | Y | UUID | 삭제할 채팅방의 고유 ID |

### **2.3 Body**

- 없음

---

### **3. 응답 (Response)**

### **3.1 성공 (200 OK)**

- **Description**: 채팅방이 성공적으로 삭제되었습니다.

```json
{
  "roomId": "9d12a7e5-2a7b-4e86-bf5c-2d1b50dcb1a4",
  "deleted": true
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
  "message": "You do not have permission to delete this chat room."
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

- **Description**: 채팅방 삭제 중 서버 내부 오류가 발생한 경우입니다.

```json
{
  "status": 500,
  "error": "Internal Server Error",
  "message": "Failed to delete chat room."
}
```

---

### **4. 비즈니스 로직 및 DB 스키마**

### **4.1 동작 흐름 (Sequence)**

1. **Token Parsing**: Authorization 헤더에서 JWT를 추출하고 검증합니다.
2. **Claim Extraction**: JWT의 `sub` 클레임을 추출하여 `clerk_id`로 사용합니다.
3. **Resource Check**: `roomId`로 `chat_rooms` 테이블을 조회합니다. 존재하지 않으면 404를 반환합니다.
4. **Authorization Check**: 조회한 채팅방의 `clerk_id`가 요청자의 `clerk_id`와 일치하는지 확인합니다. 일치하지 않으면 403을 반환합니다.
5. **Delete**: `chat_rooms`에서 해당 채팅방을 삭제합니다.
6. **Cascade Delete**: 외래키 제약에 의해 연결된 `chat_messages`, `itineraries`도 함께 삭제됩니다. `itineraries` 삭제 시 해당 일정을 참조하는 하위 데이터도 DB 정책에 따라 함께 정리됩니다.
7. **Return**: 삭제 성공 여부를 응답으로 반환합니다.

### **4.2 DB 삭제 구조 (`chat_rooms` Table)**

| Column | Type | Description |
| --- | --- | --- |
| `id` | UUID | 삭제 대상 채팅방 ID |
| `clerk_id` | VARCHAR(255) | 채팅방 소유자 식별자 |
| `created_at` | TIMESTAMP | 채팅방 생성 시각 |
| `updated_at` | TIMESTAMP | 채팅방 마지막 수정 시각 |

### **4.3 Cascade 영향 범위**

| 대상 테이블 | 연결 컬럼 | 삭제 방식 |
| --- | --- | --- |
| `chat_messages` | `room_id` | `chat_rooms` 삭제 시 함께 삭제 |
| `itineraries` | `room_id` | `chat_rooms` 삭제 시 함께 삭제 |
| `itinerary_logs` | `itinerary_id` | 연결된 `itineraries` 삭제 시 함께 삭제 |

> `reservations`는 `itinerary_id`를 통해 `itineraries`를 참조하지만, 현재 스키마상 `ON DELETE RESTRICT`이므로 실제 운영 시 채팅방 삭제 전에 예약 데이터 처리 정책을 팀 내에서 반드시 확정해야 합니다. 첨부 스키마에는 `reservations.itinerary_id REFERENCES itineraries(id) ON DELETE RESTRICT`로 적혀 있습니다.
> 

---

### **5. 호출 예시 (Example)**

```bash
curl -X DELETE https://your-api-domain.com/v1/chat-rooms/{roomId} \
  -H "Authorization: Bearer <clerk_jwt_token>"
```