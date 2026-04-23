## **[POST] /api/v1/reservations**

예약 링크 제공 시 예약 레코드를 생성합니다.

---

### **1. 기본 정보**

| 항목 | 내용 |
| --- | --- |
| Method | `POST` |
| URL | `/api/v1/reservations` |
| Summary | 예약 생성 |
| Authentication | Bearer JWT (Clerk 발급 토큰 필수) |

---

### **2. 요청 (Request)**

### **2.1 Headers**

| Name | Required | Example | Description |
| --- | --- | --- | --- |
| Authorization | Y | `Bearer eyJhbGci...` | Clerk에서 발급받은 유효한 JWT 토큰 |

### **2.2 Body**

```json
{
  "itineraryId": "be4d9d2d-1d84-4b1b-bf4d-1ac6b9cc7f22",
  "type": "flight",
  "bookedBy": "ai",
  "bookingUrl": "https://booking.example.com/flight/123",
  "externalRefId": "KE12345678",
  "detail": {
    "airline": "대한항공",
    "flight_no": "KE123",
    "departure": {"airport": "ICN", "datetime": "2026-05-01T09:00:00"},
    "arrival": {"airport": "NRT", "datetime": "2026-05-01T11:30:00"},
    "seat_class": "economy",
    "passengers": [{"name": "홍길동", "passport": "M12345678"}]
  },
  "totalPrice": 320000.00,
  "currency": "KRW",
  "reservedAt": "2026-04-03T21:20:00+09:00"
}
```

| Field | Required | Type | Description |
| --- | --- | --- | --- |
| `itineraryId` | Y | UUID | 연결된 일정 ID |
| `type` | Y | String | `flight` | `accommodation` | `car_rental` |
| `bookedBy` | Y | String | `user` | `ai` |
| `bookingUrl` | N | String | 예약 링크 |
| `externalRefId` | N | String | 외부 예약 번호 |
| `detail` | Y | JSONB | 유형별 상세 정보 |
| `totalPrice` | N | Decimal | 총 결제 금액 |
| `currency` | N | String | 통화 코드 (기본값 `KRW`) |
| `reservedAt` | N | ISO-8601 with offset | 예약 완료 일시. **타임존 오프셋 필수** (예: `2026-04-03T21:20:00+09:00`) |

---

### **3. 응답 (Response)**

### **3.1 성공 (201 Created)**

- **Description**: 예약이 성공적으로 생성되었습니다.
    
    ```json
    {
      "reservationId": "c3a7db7a-3b93-4b50-a667-4ac922e2ff11",
      "itineraryId": "be4d9d2d-1d84-4b1b-bf4d-1ac6b9cc7f22",
      "type": "flight",
      "status": "confirmed",
      "reservedAt": "2026-04-03T21:20:00Z",
      "createdAt": "2026-04-03T12:20:10+09:00",
      "updatedAt": "2026-04-03T12:20:10+09:00"
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

- **Description**: 해당 일정의 소유자가 아닌 경우입니다.
    
    ```json
    {
      "status": 403,
      "error": "Forbidden",
      "message": "You do not have permission to create a reservation for this itinerary."
    }
    ```
    

### **3.4 잘못된 요청 (400 Bad Request)**

- **Description**: 필수 필드가 누락되었거나 허용되지 않은 값인 경우입니다.
    
    
    | 케이스 | message |
    | --- | --- |
    | `type` 허용값 위반 | `type must be one of: flight, accommodation, car_rental.` |
    | `bookedBy` 허용값 위반 | `bookedBy must be one of: user, ai.` |
    | `detail` 필수 키 누락 또는 빈 값 | `detail.{field} is required for type '{type}'.` |
    | `detail.{parent}` 객체 아님 | `detail.{parent} must be an object for type '{type}'.` |
    | `detail` 중첩 필수 키 누락 또는 빈 값 | `detail.{parent}.{field} is required for type '{type}'.` |
    | `passengers` 빈 배열 또는 비리스트 | `detail.passengers must be a non-empty array for type 'flight'.` |
    | `passengers[n]` name/passport 누락 또는 빈 값 | `detail.passengers[n] must include name and passport.` |
    
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
    	"message": "bookedBy must be one of: user, ai."
    }
    ```
    
    ```json
    {
      "status": 400,
      "error": "Bad Request",
      "message": "detail.departure.airport is required for type 'flight'."
    }
    ```
    

### **3.5 리소스 없음 (404 Not Found)**

- **Description**: JWT는 유효하지만 서비스 DB에 해당 사용자가 존재하지 않거나, `itineraryId`에 해당하는 일정이 존재하지 않는 경우입니다.

    ```json
    {
      "status": 404,
      "error": "Not Found",
      "message": "User not found. Please sign up first."
    }
    ```

    ```json
    {
      "status": 404,
      "error": "Not Found",
      "message": "Itinerary not found."
    }
    ```
    
### **3.6 서버 내부 오류 (500 Internal Server Error)**

- **Description**: 요청은 유효하지만, 서버 내 데이터 간의 참조 관계가 깨져 있거나 데이터가 유실된 경우입니다.
    - 일정과 연결된 **채팅방(ChatRoom)** 데이터가 없는 경우:
        
        ```json
        {
          "status": 500,
          "error": "Internal Server Error",
          "message": "Related chat room data is missing."
        }
        ```
    - 기타 예상치 못한 서버 내부 오류 경우:
        
        ```json
        {
          "status": 500,
          "error": "Internal Server Error",
          "message": "Failed to create reservation."
        }
        ```

---

### **4. 비즈니스 로직 및 DB 스키마**

### **4.1 동작 흐름 (Sequence)**

1. **Token Parsing**: Authorization 헤더에서 JWT를 추출하고 검증합니다.
2. **Claim Extraction**: JWT의 `sub` 클레임을 추출하여 `clerk_id`로 사용합니다.
3. **User Check**: `users` 테이블에서 해당 `clerk_id` 사용자가 존재하는지 확인합니다. 존재하지 않으면 404를 반환합니다.
4. **Validation**: `type`, `bookedBy` 값이 허용된 값인지 검증합니다. 허용되지 않은 값이면 400을 반환합니다.
5. **Resource Check**: `itineraryId`로 itineraries 테이블을 조회합니다. 존재하지 않으면 404를 반환합니다.
6. **Authorization Check**: `room_id → chat_rooms.clerk_id`가 요청자의 `clerk_id`와 일치하는지 확인합니다. 일치하지 않으면 403을 반환합니다.
7. **Detail Validation**: `type`에 따라 `detail` 필수 키를 검증합니다. 누락 또는 빈 값이면 400을 반환합니다.
8. **Insert**: `reservations` 테이블에 레코드를 생성합니다. `status`는 `confirmed`로 초기화합니다.

### **4.2 detail 필수 필드 (type별)**

**flight**

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `airline` | String | 항공사명 |
| `flight_no` | String | 편명 |
| `departure.airport` | String | 출발 공항 코드 |
| `departure.datetime` | String | 출발 일시 |
| `arrival.airport` | String | 도착 공항 코드 |
| `arrival.datetime` | String | 도착 일시 |
| `seat_class` | String | 좌석 등급 |
| `passengers[].name` | String | 탑승객 이름 |
| `passengers[].passport` | String | 탑승객 여권번호 |

**accommodation**

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `hotel_name` | String | 숙소명 |
| `room_type` | String | 객실 유형 |
| `check_in` | String | 체크인 날짜 |
| `check_out` | String | 체크아웃 날짜 |
| `guests` | Number | 투숙 인원 |

**car_rental**

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `company` | String | 렌터카 업체명 |
| `car_model` | String | 차량 모델 |
| `pickup.location` | String | 픽업 장소 |
| `pickup.datetime` | String | 픽업 일시 |
| `dropoff.location` | String | 반납 장소 |
| `dropoff.datetime` | String | 반납 일시 |

### **4.3 DB 저장 구조 (reservations Table)**

| Column | Type | Description |
| --- | --- | --- |
| `itinerary_id` | UUID | 연결된 일정 ID |
| `type` | VARCHAR(20) | 예약 유형 |
| `status` | VARCHAR(20) | `confirmed` 로 초기화 |
| `booked_by` | VARCHAR(10) | 예약 주체 |
| `booking_url` | TEXT | 예약 링크 |
| `external_ref_id` | VARCHAR(255) | 외부 예약 번호 |
| `detail` | JSONB | 유형별 상세 정보 |
| `total_price` | DECIMAL(12,2) | 총 결제 금액 |
| `currency` | VARCHAR(3) | 통화 코드 |
| `reserved_at` | TIMESTAMP | 예약 완료 일시 |

---

### **5. 호출 예시 (Example)**

```bash
curl -X POST https://your-api-domain.com/api/v1/reservations \
  -H "Authorization: Bearer <clerk_jwt_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "itineraryId": "be4d9d2d-1d84-4b1b-bf4d-1ac6b9cc7f22",
    "type": "flight",
    "bookedBy": "ai",
    "bookingUrl": "https://booking.example.com/flight/123",
    "detail": {...},
    "totalPrice": 320000.00
  }'
```