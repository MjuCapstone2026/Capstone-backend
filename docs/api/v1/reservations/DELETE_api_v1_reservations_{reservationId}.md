## **[DELETE] /api/v1/reservations/{reservationId}**

예약 레코드를 완전히 삭제합니다. (데이터 Hard Delete)

---

### **1. 기본 정보**

| 항목 | 내용 |
| --- | --- |
| Method | `DELETE` |
| URL | `/api/v1/reservations/{reservationId}` |
| Summary | 예약 삭제 |
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
| reservationId | Y | UUID | 삭제할 예약의 고유 ID |

---

## **3. 응답 (Response)**

### **3.1 성공 (200 OK)**

- **Description**: 예약이 성공적으로 삭제되었습니다.
    
    ```json
    {
      "reservationId": "c3a7db7a-3b93-4b50-a667-4ac922e2ff11",
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

- **Description**: 해당 예약의 소유자가 아닌 경우입니다.
    
    ```json
    {
      "status": 403,
      "error": "Forbidden",
      "message": "You do not have permission to delete this reservation."
    }
    ```
    

### **3.4 리소스 없음 (404 Not Found)**

- **Description**: 해당 `reservationId`에 해당하는 예약이 존재하지 않는 경우입니다.
    
    ```json
    {
      "status": 404,
      "error": "Not Found",
      "message": "Reservation not found."
    }
    ```
    

---

## **4. 비즈니스 로직 및 DB 스키마**

### **4.1 동작 흐름 (Sequence)**

1. **Token Parsing**: Authorization 헤더에서 JWT를 추출하고 검증합니다.
2. **Claim Extraction**: JWT의 `sub` 클레임을 추출하여 `clerk_id`로 사용합니다.
3. **Resource Check**: `reservationId`로 reservations 테이블을 조회합니다. 존재하지 않으면 404를 반환합니다.
4. **Authorization Check**: `itinerary_id → room_id → chat_rooms.clerk_id`가 요청자의 `clerk_id`와 일치하는지 확인합니다. 일치하지 않으면 403을 반환합니다.
5. **Delete**: `reservations` 테이블에서 해당 레코드를 삭제합니다.

### **4.2 DB 삭제 구조**

`reservations` 테이블에서 `id = reservationId` 조건으로 레코드를 삭제합니다.

---

## **5. 호출 예시 (Example)**

```json
curl -X DELETE https://your-api-domain.com/v1/reservations/{reservationId} \
  -H "Authorization: Bearer <clerk_jwt_token>"
```