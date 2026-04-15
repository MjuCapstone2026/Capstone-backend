## **[PATCH] /api/v1/itineraries/{itineraryId}/status**

여행 일정의 상태를 변경합니다.

---

### **1. 기본 정보**

| 항목 | 내용 |
| --- | --- |
| Method | `PATCH` |
| URL | `/api/v1/itineraries/{itineraryId}/status` |
| Summary | 여행 일정 상태 변경 |
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
| itineraryId | Y | UUID | 상태를 변경할 일정의 고유 ID |

#### **2.3 Body**

```json
{
  "status": "completed"
}
```

| Field | Required | Type | Constraints | Description |
| --- | --- | --- | --- | --- |
| status | Y | String | `draft` / `completed` | 변경할 일정 상태 (`draft`: 예정, `completed`: 완료) |

---

### **3. 응답 (Response)**

#### **3.1 성공 (200 OK)**

- **Description**: 일정 상태가 성공적으로 변경되었습니다.

    ```json
    {
      "itineraryId": "aaa-111",
      "status": "completed",
      "updatedAt": "2026-04-03T22:00:00"
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

- **Description**: 해당 일정의 소유자가 아닌 경우입니다.

    ```json
    {
      "status": 403,
      "error": "Forbidden",
      "message": "You do not have permission to update this itinerary."
    }
    ```


#### **3.4 잘못된 요청 (400 Bad Request)**

- **Description**: `status` 값이 허용된 값이 아닌 경우입니다.

    ```json
    {
      "status": 400,
      "error": "Bad Request",
      "message": "status must be one of: draft, completed."
    }
    ```


#### **3.5 리소스 없음 (404 Not Found)**

- **Description**: 해당 `itineraryId`에 해당하는 일정이 존재하지 않는 경우입니다.

    ```json
    {
      "status": 404,
      "error": "Not Found",
      "message": "Itinerary not found."
    }
    ```


---

### **4. 비즈니스 로직 및 DB 스키마**

#### **4.1 동작 흐름 (Sequence)**

1. **Token Parsing**: Authorization 헤더에서 JWT를 추출하고 검증합니다.
2. **Claim Extraction**: JWT의 `sub` 클레임을 추출하여 `clerk_id`로 사용합니다.
3. **Resource Check**: `itineraryId`로 itineraries 테이블을 조회합니다. 존재하지 않으면 404를 반환합니다.
4. **Authorization Check**: 조회한 itinerary의 `room_id → chat_rooms.clerk_id`가 요청자의 `clerk_id`와 일치하는지 확인합니다. 일치하지 않으면 403을 반환합니다.
5. **Validation**: 요청 `status` 값이 `draft` / `completed` 중 하나인지 검증합니다. 아니면 400을 반환합니다.
6. **Update**: `itineraries.status` 및 `updated_at`을 업데이트합니다.

#### **4.2 DB 업데이트 구조 (itineraries Table)**

| Column | Type | Description |
| --- | --- | --- |
| `status` | VARCHAR(20) | 변경할 상태값 (`draft`: 예정 / `completed`: 완료) |
| `updated_at` | TIMESTAMP | 현재 시각으로 업데이트 |

---

### **5. 호출 예시 (Example)**

```bash
curl -X PATCH https://your-api-domain.com/api/v1/itineraries/{itineraryId}/status \
  -H "Authorization: Bearer <clerk_jwt_token>" \
  -H "Content-Type: application/json" \
  -d '{"status": "completed"}'
```