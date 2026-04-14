## **[POST] api/v1/chat-rooms**

사용자의 새로운 여행 계획용 채팅방을 생성합니다.

---

### **1. 기본 정보**

| 항목 | 내용                          |
| --- |-----------------------------|
| Method | `POST`                      |
| URL | `api/v1/chat-rooms`           |
| Summary | 채팅방 생성                      |
| Authentication | Bearer JWT (Clerk 발급 토큰 필수) |

---

### **2. 요청 (Request)**

#### **2.1 Headers**

| Name | Required | Example | Description |
| --- | --- | --- | --- |
| Authorization | Y | `Bearer eyJhbGci...` | Clerk에서 발급받은 유효한 JWT 토큰 |

#### 2.2 Body

```json
{
  "destination": "제주도",
  "startDate": "2026-05-01",
  "endDate": "2026-05-03",
  "budget": 300000,
  "adultCount": 2,
  "childCount": 1,
  "childAges": [7],
}
```

| Field | Required | Type | Description |
| --- | --- | --- | --- |
| `destination` | Y | String | 여행지 |
| `startDate` | Y | Date | 여행 시작일 |
| `endDate` | Y | Date | 여행 종료일 |
| `budget` | N | Decimal | 예산 |
| `adultCount` | Y | Int | 성인 수 (최소 1) |
| `childCount` | N | Int | 아이 수 (기본값 0) |
| `childAges` | N | Int[] | 아이 나이 배열 (`childCount > 0`이면 필수) |

### **3. 응답 (Response)**

#### **3.1 성공 (201 Created)**

- **Description**: 채팅방이 성공적으로 생성되었습니다.

```json
{
  "roomId": "9d12a7e5-2a7b-4e86-bf5c-2d1b50dcb1a4",
  "itineraryId": "aaa-111",
  "createdAt": "2026-04-03T22:00:00",
  "updatedAt": "2026-04-03T22:00:00"
}
```

#### **3.2 잘못된 요청 (400 Bad Request)**

- **Description**: 필수 필드가 누락되었거나 유효하지 않은 값이 입력된 경우입니다.

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "childAges must be provided when childCount > 0."
}
```

#### **3.3 인증 실패 (401 Unauthorized)**

- **Description**: JWT 토큰이 누락되었거나 만료, 서명 오류 등으로 유효하지 않은 경우입니다.

```json
{
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid or expired token."
}
```

#### **3.4 리소스 없음 (404 Not Found)**

- **Description**: 서비스 DB에 해당 사용자가 존재하지 않는 경우입니다. 먼저 회원가입 동기화 API를 호출해야 합니다.

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "User not found. Please sign up first."
}
```

#### **3.5 서버 오류 (500 Internal Server Error)**

- **Description**: 채팅방 생성 중 서버 내부 오류가 발생한 경우입니다.

```json
{
  "status": 500,
  "error": "Internal Server Error",
  "message": "Failed to create chat room."
}
```

---

### 4. 비즈니스 로직 및 DB 스키마

#### 4.1 동작 흐름 (Sequence)

1. **Token Parsing**: Authorization 헤더에서 JWT를 추출하고 검증합니다.
2. **Claim Extraction**: JWT의 `sub` 클레임을 추출하여 `clerk_id`로 사용합니다.
3. **User Check**: `users` 테이블에서 해당 `clerk_id` 사용자가 존재하는지 확인합니다. 존재하지 않으면 404를 반환합니다.
4. **Validation**: `childCount > 0`인데 `childAges`가 누락되거나 길이가 맞지 않으면 400을 반환합니다.
5. **Insert chat_rooms**: `chat_rooms` 테이블에 새 채팅방을 생성합니다.
6. **Insert itineraries**: `itineraries` 테이블에 여행 일정을 초기화합니다. `day_plans`는 빈 객체 `{}`로 초기화합니다. → total_date, start_date, end_date, adults, chidlren, child_ages, destination, buget 값을 넣으면서 초기화
7. **Return**: 생성된 `roomId`, `itineraryId` 및 시각 정보를 반환합니다.

### 4.2 DB 저장 구조

**`chat_rooms` Table**

| Column | Value |
| --- | --- |
| `id` | `PK` |
| `clerk_id` | JWT `sub` 클레임 |
| `name` | 자동 생성: `"{N-1}박 {N}일 {destination} 여행"` |
| `ai_summary` | `NULL` |
| `preferences` | `NULL` |
| `created_at` | 생성일자 |
| `updated_at` | 업데이트일자 |

**`itineraries` Table**

| Column | Value |
| --- | --- |
| `id` | `PK` |
| `room_id` | 생성된 `chat_rooms.id` |
| `destination` | 요청 `destination` |
| `start_date` | 요청 `startDate` |
| `end_date` | 요청 `endDate` |
| `total_days` | `endDate - startDate + 1` 계산 |
| `budget` | 요청 `budget` |
| `adult_count` | 요청 `adultCount` |
| `child_count` | 요청 `childCount` |
| `child_ages` | 요청 `childAges` |
| `status` | `'draft'` |
| `day_plans` | `'{}'` |
| `created_at` | 생성일자 |
| `updated_at` | 업데이트일자 |

### 5. 호출 예시 (Example)

```bash
curl -X POST https://your-api-domain.com/v1/chat-rooms \
  -H "Authorization: Bearer <clerk_jwt_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "destination": "제주도",
    "startDate": "2026-05-01",
    "endDate": "2026-05-03",
    "budget": 300000,
    "adultCount": 2,
    "childCount": 1,
    "childAges": [7],
  }'
```