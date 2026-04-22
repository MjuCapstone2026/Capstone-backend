## **[PATCH] /api/v1/reservations/{reservationId}**

예약 상태 변경, detail 수정, 가격 등을 업데이트합니다.

---

### **1. 기본 정보**

| 항목 | 내용 |
| --- | --- |
| Method | `PATCH` |
| URL | `/api/v1/reservations/{reservationId}` |
| Summary | 예약 수정 |
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
| reservationId | Y | UUID | 수정할 예약의 고유 ID |

### **2.3 Body**

`status` 값에 따라 필요한 필드가 달라집니다.

**Case 1. `status: "cancelled"` — 예약 취소**

```json
{
  "status": "cancelled",
  "cancelledAt": "2026-04-10T10:00:00"
}
```

| Field | Required | Type | Description |
| --- | --- | --- | --- |
| `status` | Y | String | `cancelled` 고정 |
| `cancelledAt` | Y | TIMESTAMP | 취소 일시 |

**Case 2. `status: "changed"` — 예약 변경**

```json
{
  "status": "changed",
  "detail": {
    "airline": "아시아나",
    "flight_no": "OZ108",
    "departure": {"airport": "ICN", "datetime": "2026-05-02T09:00:00"},
    "arrival": {"airport": "NRT", "datetime": "2026-05-02T11:30:00"},
    "seat_class": "economy",
    "passengers": [{"name": "홍길동", "passport": "M12345678"}]
  },
  "totalPrice": 290000.00,
  "currency": "KRW",
  "reservedAt": "2026-04-10T10:00:00"
}
```

| Field | Required | Type | Description |
| --- | --- | --- | --- |
| `status` | Y | String | `changed` 고정 |
| `detail` | Y | JSONB | 변경된 예약 상세 정보 |
| `totalPrice` | Y | Decimal | 변경된 총 결제 금액 |
| `currency` | N | String | 통화 코드 (기본값 `KRW`) |
| `reservedAt` | Y | TIMESTAMP | 변경된 예약 완료 일시 |

---

### **3. 응답 (Response)**

### **3.1 성공 (200 OK)**

- **Description**: 예약이 성공적으로 수정되었습니다.
    - **Case 1. `status: "cancelled"`**
        
        ```json
        {
          "reservationId": "c3a7db7a-3b93-4b50-a667-4ac922e2ff11",
          "status": "cancelled",
          "cancelledAt": "2026-04-10T10:00:00",
          "updatedAt": "2026-04-10T10:00:00"
        }
        ```
        
    - **Case 2. `status: "changed"`**
        
        ```json
        {
          "reservationId": "c3a7db7a-3b93-4b50-a667-4ac922e2ff11",
          "status": "changed",
          "reservedAt": "2026-04-10T10:00:00",
          "updatedAt": "2026-04-10T10:00:00"
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

- **Description**: 해당 예약의 소유자가 아닌 경우입니다.
    
    ```json
    {
      "status": 403,
      "error": "Forbidden",
      "message": "You do not have permission to update this reservation."
    }
    ```
    

### **3.4 잘못된 요청 (400 Bad Request)**

- **Description**: 필수 필드가 누락되었거나 허용되지 않은 값인 경우입니다.
    - `status` 값이 허용되지 않은 경우:
        
        ```json
        {
          "status": 400,
          "error": "Bad Request",
          "message": "status must be one of: cancelled, changed."
        }
        ```
        
    - `status: cancelled`인데 `cancelledAt` 누락된 경우:
        
        ```json
        {
          "status": 400,
          "error": "Bad Request",
          "message": "cancelledAt is required when status is cancelled."
        }
        ```
        
    - `status: changed`인데 필수 필드 누락된 경우:
        
        ```json
        {
          "status": 400,
          "error": "Bad Request",
          "message": "detail, totalPrice, reservedAt are required when status is changed."
        }
        ```
        

### **3.5 리소스 없음 (404 Not Found)**

- **Description**: 해당 `reservationId`에 해당하는 예약이 존재하지 않는 경우입니다.
    
    ```json
    {
      "status": 404,
      "error": "Not Found",
      "message": "Reservation not found."
    }
    ```
    

---

### **4. 비즈니스 로직 및 DB 스키마**

### **4.1 동작 흐름 (Sequence)**

1. **Token Parsing**: Authorization 헤더에서 JWT를 추출하고 검증합니다.
2. **Claim Extraction**: JWT의 `sub` 클레임을 추출하여 `clerk_id`로 사용합니다.
3. **Resource Check**: `reservationId`로 reservations 테이블을 조회합니다. 존재하지 않으면 404를 반환합니다.
4. **Authorization Check**: `itinerary_id → room_id → chat_rooms.clerk_id`가 요청자의 `clerk_id`와 일치하는지 확인합니다. 일치하지 않으면 403을 반환합니다.
5. **Validation**: `status` 값이 `cancelled` / `changed` 중 하나인지 확인합니다. 아니면 400을 반환합니다.
    - `status: cancelled` → `cancelledAt` 필수 확인. 누락 시 400 반환.
    - `status: changed` → `detail`, `totalPrice`, `reservedAt` 필수 확인. 누락 시 400 반환.
6. **Update**: 요청에 포함된 필드만 업데이트하고 `updated_at`을 갱신합니다.

### **4.2 DB 업데이트 구조 (reservations Table)**

***Case 1. `cancelled`***

| Column | Type | Description |
| --- | --- | --- |
| `status` | VARCHAR(20) | `cancelled`로 업데이트 |
| `cancelled_at` | TIMESTAMP | 취소 일시로 업데이트 |
| `updated_at` | TIMESTAMP | 현재 시각으로 업데이트 |

***Case 2. `changed`***

| Column | Type | Description |
| --- | --- | --- |
| `status` | VARCHAR(20) | `changed`로 업데이트 |
| `detail` | JSONB | 변경된 상세 정보로 업데이트 |
| `total_price` | DECIMAL(12,2) | 변경된 금액으로 업데이트 |
| `currency` | VARCHAR(3) | 요청에 포함된 경우 업데이트 |
| `reserved_at` | TIMESTAMP | 변경된 예약 일시로 업데이트 |
| `updated_at` | TIMESTAMP | 현재 시각으로 업데이트 |

---

### **5. 호출 예시 (Example)**

```json
# 취소
curl -X PATCH https://your-api-domain.com/v1/reservations/{reservationId} \
  -H "Authorization: Bearer <clerk_jwt_token>" \
  -H "Content-Type: application/json" \
  -d '{"status": "cancelled", "cancelledAt": "2026-04-10T10:00:00"}'

# 변경
curl -X PATCH https://your-api-domain.com/v1/reservations/{reservationId} \
  -H "Authorization: Bearer <clerk_jwt_token>" \
  -H "Content-Type: application/json" \
  -d '{"status": "changed", "detail": {...}, "totalPrice": 290000.00, "reservedAt": "2026-04-10T10:00:00"}'
```