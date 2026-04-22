## **[GET] /api/v1/reservations**

현재 로그인한 사용자의 예약 목록을 조회합니다.

예약 유형(`type`)과 예약 상태(`status`)로 필터링할 수 있습니다.

---

### **1. 기본 정보**

| 항목 | 내용 |
| --- | --- |
| Method | `GET` |
| URL | `/api/v1/reservations` |
| Summary | 내 예약 목록 조회 |
| Authentication | Bearer JWT (Clerk 발급 토큰 필수) |

---

### **2. 요청 (Request)**

### **2.1 Headers**

| Name | Required | Example | Description |
| --- | --- | --- | --- |
| Authorization | Y | `Bearer eyJhbGci...` | Clerk에서 발급받은 유효한 JWT 토큰 |

### **2.2 Query Parameter**

| Name | Required | Type | Example | Description |
| --- | --- | --- | --- | --- |
| type | N | String | `flight` | 예약 유형 필터 |
| status | N | String | `confirmed` | 예약 상태 필터 |
- `type` 허용 값: `flight`, `accommodation`, `car_rental`
- `status` 허용 값: `confirmed`, `changed`, `cancelled`

### **2.3 Body**

- 없음

---

### **3. 응답 (Response)**

### **3.1 성공 (200 OK)**

- **Description**: 현재 로그인한 사용자의 예약 목록을 성공적으로 조회했습니다.

  ```json
  {
    "reservations": [
      {
        "reservationId": "c3a7db7a-3b93-4b50-a667-4ac922e2ff11",
        "itineraryId": "be4d9d2d-1d84-4b1b-bf4d-1ac6b9cc7f22",
        "type": "flight",
        "status": "confirmed",
        "bookedBy": "user",
        "bookingUrl": "https://booking.example.com/flight/123",
        "externalRefId": "KE12345678",
        "detail": {
          "airline": "Korean Air",
          "flight_no": "KE721",
          "departure": {
            "airport": "ICN",
            "datetime": "2026-05-01T09:00:00"
          },
          "arrival": {
            "airport": "NRT",
            "datetime": "2026-05-01T11:30:00"
          }
        },
        "totalPrice": 320000.00,
        "currency": "KRW",
        "reservedAt": "2026-04-03T21:30:00Z",
        "cancelledAt": null,
        "createdAt": "2026-04-03T21:20:00Z",
        "updatedAt": "2026-04-03T21:30:00Z"
      },
      {
        "reservationId": "7f2f7e4f-5d29-47f7-8561-8ff22f0b2fd8",
        "itineraryId": "3db203a5-d4fa-41b8-b30f-1d4cf58ce0e6",
        "type": "flight",
        "status": "confirmed",
        "bookedBy": "ai",
        "bookingUrl": "https://booking.example.com/flight/456",
        "externalRefId": "OZ98765432",
        "detail": {
          "airline": "Asiana",
          "flight_no": "OZ108",
          "departure": {
            "airport": "FCO",
            "datetime": "2026-06-10T12:20:00"
          },
          "arrival": {
            "airport": "ICN",
            "datetime": "2026-06-11T06:20:00"
          }
        },
        "totalPrice": 890000.00,
        "currency": "KRW",
        "reservedAt": "2026-04-05T10:10:00Z",
        "cancelledAt": null,
        "createdAt": "2026-04-05T10:00:00Z",
        "updatedAt": "2026-04-05T10:10:00Z"
      }
    ]
  }
  ```

- **예약이 없는 경우 예시**

  ```json
  {
    "reservations": []
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

### **3.4 잘못된 요청 (400 Bad Request)**

- **Description**: `type` 또는 `status` 값이 허용된 값이 아닌 경우입니다. 각 파라미터는 별도로 검증되어 독립적으로 오류를 반환합니다.

  ```json
  {
    "status": 400,
    "error": "Bad Request",
    "message": "type must be one of: flight, accommodation, car_rental."
  }
  ```

  ```json
  {
    "status": 400,
    "error": "Bad Request",
    "message": "status must be one of: confirmed, changed, cancelled."
  }
  ```

### **3.5 서버 오류 (500 Internal Server Error)**

- **Description**: 예약 목록 조회 중 서버 내부 오류가 발생한 경우입니다.

  ```json
  {
    "status": 500,
    "error": "Internal Server Error",
    "message": "Failed to get reservations."
  }
  ```

---

### **4. 비즈니스 로직 및 DB 스키마**

### **4.1 동작 흐름 (Sequence)**

1. **Token Parsing**: Authorization 헤더에서 JWT를 추출하고 검증합니다.
2. **Claim Extraction**: JWT의 `sub` 클레임을 추출하여 `clerk_id`로 사용합니다.
3. **User Check**: `users` 테이블에서 해당 `clerk_id` 사용자가 존재하는지 확인합니다. 존재하지 않으면 404를 반환합니다.
4. **Validation**: Query Parameter의 `type`, `status` 값이 허용된 값인지 검증합니다. 허용되지 않은 값이면 400을 반환합니다.
5. **Join Query**: `users → chat_rooms → itineraries → reservations` 순으로 조인하여 현재 로그인한 사용자의 예약 목록만 조회합니다.
6. **Filter**: `type`, `status`가 전달된 경우 해당 조건으로 추가 필터링합니다.
7. **Sort**: 예약 목록은 최근 생성 순 또는 최근 수정 순(`updated_at DESC`)으로 정렬하여 반환합니다.
8. **Return**: 예약 목록을 응답으로 반환합니다. 조건에 맞는 예약이 없으면 빈 배열 `[]`을 반환합니다.

### **4.2 DB 조회 구조 (`reservations` Table)**

| Column | Type | Description |
| --- | --- | --- |
| `id` | UUID | 예약 고유 ID |
| `itinerary_id` | UUID | 연결된 일정 ID |
| `type` | VARCHAR(20) | 예약 유형 (`flight`, `accommodation`, `car_rental`) |
| `status` | VARCHAR(20) | 예약 상태 (`confirmed`, `changed`, `cancelled`) |
| `booked_by` | VARCHAR(10) | 예약 주체 (`user`, `ai`) |
| `booking_url` | TEXT | 예약 링크 |
| `external_ref_id` | VARCHAR(255) | 외부 예약 번호 |
| `detail` | JSONB | 예약 상세 정보 |
| `total_price` | DECIMAL(12,2) | 총 결제 금액 |
| `currency` | VARCHAR(3) | 통화 코드 |
| `reserved_at` | TIMESTAMP | 예약 완료 시각 |
| `cancelled_at` | TIMESTAMP | 예약 취소 시각 |
| `created_at` | TIMESTAMP | 생성 시각 |
| `updated_at` | TIMESTAMP | 마지막 수정 시각 |

---

### **5. 호출 예시 (Example)**

```bash
curl -X GET "https://your-api-domain.com/api/v1/reservations?type=flight&status=confirmed" \
  -H "Authorization: Bearer <clerk_jwt_token>"
```