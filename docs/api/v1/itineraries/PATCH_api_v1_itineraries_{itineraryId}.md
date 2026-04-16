## **[PATCH] /api/v1/itineraries/{itineraryId}**

사용자가 UI에서 직접 수정한 여행 기본 정보를 반영합니다.

### **1. 기본 정보**

| 항목 | 내용 |
| --- | --- |
| Method | `PATCH` |
| URL | `/api/v1/itineraries/{itineraryId}` |
| Summary | 여행 기본 정보 수정 |
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
| itineraryId | Y | UUID | 수정할 일정의 고유 ID |

#### **2.3 Body**

모든 필드는 선택입니다. 변경이 필요한 필드만 포함합니다.

> `destination`은 수정 불가 필드입니다. 요청 body에 포함되더라도 무시합니다. 여행지를 변경하려면 새 채팅방에서 일정을 생성해야 합니다.

```json
{
  "startDate": "2026-05-01",
  "endDate": "2026-05-03",
  "budget": 300000,
  "adultCount": 2,
  "childCount": 1,
  "childAges": [7]
}
```

| Field | Required | Type | Description |
| --- | --- | --- | --- |
| `startDate` | N | DATE | 변경할 여행 시작일 |
| `endDate` | N | DATE | 변경할 여행 종료일 |
| `budget` | N | Decimal | 변경할 예산 |
| `adultCount` | N | Int | 변경할 성인 수 (최솟값: 1) |
| `childCount` | N | Int | 변경할 아이 수 (최솟값: 0). `childAges`와 항상 함께 전달해야 함 |
| `childAges` | N | Int[] | 변경할 아이 나이 배열. `childCount`와 항상 함께 전달해야 함. 배열 길이는 `childCount`와 일치해야 하며, `childCount`가 0이면 빈 배열(`[]`) |

---

### **3. 응답 (Response)**

#### **3.1 성공 (200 OK)**

- **Description**: 일정 기본 정보가 성공적으로 변경되었습니다.

    ```json
    {
      "itineraryId": "aaa-111",
      "destination": "제주도",
      "startDate": "2026-05-01",
      "endDate": "2026-05-03",
      "totalDays": 3,
      "budget": 300000,
      "adultCount": 2,
      "childCount": 1,
      "childAges": [7],
      "updatedAt": "2026-04-03T22:00:00"
    }
    ```

**응답 필드 설명**

| Field | Type | Description |
| --- | --- | --- |
| `itineraryId` | UUID | 일정 고유 ID |
| `destination` | String | 목적지 (수정 불가, 기존값 그대로 반환) |
| `startDate` | String (YYYY-MM-DD) | 여행 시작일 |
| `endDate` | String (YYYY-MM-DD) | 여행 종료일 |
| `totalDays` | Integer | 총 여행 일수 (`endDate - startDate + 1`) |
| `budget` | Number \| null | 예산. 미설정 시 `null` |
| `adultCount` | Integer | 어른 수 |
| `childCount` | Integer | 아이 수 |
| `childAges` | Array | 아이 나이 목록 |
| `updatedAt` | String (ISO 8601) | 마지막 수정 일시 |

> `budget`을 요청에 포함하지 않거나 `null`로 전달한 경우 기존 값이 유지됩니다. `budget`이 설정된 적 없는 일정은 `"budget": null`로 반환됩니다. `null`은 "사용자가 예산을 지정하지 않음"을 의미하며, AI 서버는 예산 제약 없이 일정을 생성합니다.


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

**`startDate`가 `endDate`보다 늦음** (요청값 또는 기존값과 비교)
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "startDate must not be later than endDate."
}
```

**`adultCount`가 1 미만**
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "adultCount must be at least 1."
}
```

**`childCount`와 `childAges` 중 하나만 요청에 포함**
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "childCount and childAges must be provided together."
}
```

**`childAges` 배열 길이가 `childCount`와 불일치**
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "childAges length must match childCount."
}
```

**요청 필드가 모두 기존 값과 동일한 경우**
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "No changes detected. The submitted values are identical to the current data."
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

#### **3.6 사용자 없음 (404 Not Found)**

- **Description**: JWT는 유효하지만 서비스 DB에 해당 사용자가 존재하지 않는 경우입니다.

    ```json
    {
      "status": 404,
      "error": "Not Found",
      "message": "User not found. Please sign up first."
    }
    ```


---

### **4. 비즈니스 로직 및 DB 스키마**

#### **4.1 동작 흐름 (Sequence)**

1. **Token Parsing**: Authorization 헤더에서 JWT를 추출하고 검증합니다.
2. **Claim Extraction**: JWT의 `sub` 클레임을 추출하여 `clerk_id`로 사용합니다.
3. **User Check**: `users` 테이블에서 해당 `clerk_id`가 존재하는지 확인합니다. 존재하지 않으면 404를 반환합니다.
4. **Resource Check**: `itineraryId`로 itineraries 테이블을 조회합니다. 존재하지 않으면 404를 반환합니다.
5. **Authorization Check**: `room_id → chat_rooms.clerk_id`가 요청자의 `clerk_id`와 일치하는지 확인합니다. 일치하지 않으면 403을 반환합니다.
6. **Validation**: 아래 항목을 검증합니다. 위반 시 400을 반환합니다.
   - `startDate` / `endDate` 중 하나만 요청에 포함된 경우, 나머지는 DB의 기존값으로 대체하여 비교합니다. 유효한 `startDate`가 유효한 `endDate`보다 늦으면 400을 반환합니다.
   - `adultCount`가 1 미만이면 400을 반환합니다.
   - `childCount`와 `childAges` 중 하나만 요청에 포함된 경우 400을 반환합니다. `childAges`의 배열 길이가 `childCount`와 일치하지 않으면 400을 반환합니다.
7. **No Change Check**: 요청에 포함된 모든 필드가 기존 값과 동일하면 400을 반환합니다. `null` 필드는 "변경 없음"으로 취급합니다.
8. **Snapshot**: 수정 전 `destination`, `budget`, `adult_count`, `child_count`, `child_ages`, `total_days`, `start_date`, `end_date`, `day_plans` 값을 `itinerary_logs` 테이블에 저장합니다.
9. **Update**: 요청에 포함된 필드만 `itineraries`에 업데이트하고 `updated_at`을 갱신합니다.
   - `startDate` / `endDate` 중 하나라도 변경된 경우 유효한 두 날짜로 `total_days`를 재계산합니다 (`endDate - startDate + 1`).
   - 날짜 범위가 **넓어진** 경우: 새로 추가된 날짜를 빈 배열(`[]`)로 `day_plans`에 추가합니다. (예: `"2026-05-04": []`)
   - 날짜 범위가 **좁아진** 경우: 범위 밖으로 잘린 날짜의 `day_plans` 항목을 삭제합니다.
10. **Response**: 갱신된 일정 기본 정보를 반환합니다.

#### **4.2 DB 업데이트 구조**

| Column | Type | Description |
| --- | --- | --- |
| `start_date` | DATE | 요청에 포함된 경우 업데이트 |
| `end_date` | DATE | 요청에 포함된 경우 업데이트 |
| `total_days` | INT | `startDate` / `endDate` 중 하나라도 변경된 경우 재계산 (`endDate - startDate + 1`, 편측 요청 시 나머지는 기존값 사용) |
| `budget` | DECIMAL(12,2) | 요청에 포함된 경우 업데이트 |
| `adult_count` | INT | 요청에 포함된 경우 업데이트 |
| `child_count` | INT | 요청에 포함된 경우 업데이트 |
| `child_ages` | JSONB | 요청에 포함된 경우 업데이트 |
| `day_plans` | JSONB | 날짜 범위 변경 시 업데이트: 넓어진 경우 새 날짜 키를 `[]`로 추가, 좁아진 경우 범위 밖 키 삭제 |
| `updated_at` | TIMESTAMP | 현재 시각으로 업데이트 |

---

### **5. 호출 예시 (Example)**

```bash
curl -X PATCH https://your-api-domain.com/api/v1/itineraries/{itineraryId} \
  -H "Authorization: Bearer <clerk_jwt_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "startDate": "2026-05-01",
    "endDate": "2026-05-03",
    "budget": 300000,
    "adultCount": 2,
    "childCount": 1,
    "childAges": [7]
  }'
```
