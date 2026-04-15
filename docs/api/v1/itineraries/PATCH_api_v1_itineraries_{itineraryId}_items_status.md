## **[PATCH] /api/v1/itineraries/{itineraryId}/items/status**

사용자가 일정 아이템의 완료 여부를 체크/해제할 때 호출합니다.

---

### **1. 기본 정보**

| 항목 | 내용 |
| --- | --- |
| Method | `PATCH` |
| URL | `/api/v1/itineraries/{itineraryId}/items/status` |
| Summary | 일정 아이템 상태 변경 |
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
| itineraryId | Y | UUID | 대상 일정의 고유 ID |

#### **2.3 Body**

```json
{
  "date": "2026-05-01",
  "index": 0,
  "status": "done"
}
```

| Field | Required | Type | Description |
| --- | --- | --- | --- |
| `date` | Y | string | 아이템이 속한 날짜 (YYYY-MM-DD) |
| `index` | Y | integer | 해당 날짜 아이템을 `time`의 시작 시각 오름차순으로 정렬했을 때의 순서 (0부터 시작). GET 응답의 `day_plans[date][index]`와 대응됨 |
| `status` | Y | string | 변경할 `day_plans` 아이템의 완료 여부. `todo`(예정) / `done`(완료) |

---

### **3. 응답 (Response)**

#### **3.1 성공 (200 OK)**

```json
{
  "itineraryId": "aaa-111",
  "date": "2026-05-01",
  "index": 0,
  "status": "done",
  "updatedAt": "2026-04-03T22:00:00"
}
```

#### **3.2 인증 실패 (401 Unauthorized)**

```json
{
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid or expired token."
}
```

#### **3.3 권한 없음 (403 Forbidden)**

```json
{
  "status": 403,
  "error": "Forbidden",
  "message": "You do not have permission to update this itinerary."
}
```

#### **3.4 잘못된 요청 (400 Bad Request)**

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid status value. Must be 'todo' or 'done'."
}
```

#### **3.5 일정 없음 (404 Not Found)**

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Itinerary not found."
}
```

#### **3.6 아이템 없음 (404 Not Found)**

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Item not found."
}
```

---

### **4. 비즈니스 로직 및 DB 스키마**

#### **4.1 동작 흐름 (Sequence)**

1. **Token Parsing**: Authorization 헤더에서 JWT를 추출하고 검증합니다.
2. **Claim Extraction**: JWT의 `sub` 클레임을 추출하여 `clerk_id`로 사용합니다.
3. **Resource Check**: `itineraryId`로 itineraries 테이블을 조회합니다. 존재하지 않으면 404를 반환합니다.
4. **Authorization Check**: `room_id → chat_rooms.clerk_id`가 요청자의 `clerk_id`와 일치하는지 확인합니다. 일치하지 않으면 403을 반환합니다.
5. **Validation**: 요청 body의 `status`가 `todo` / `done` 중 하나인지 검증합니다. 아니면 400을 반환합니다.
6. **Item Check**: `day_plans[date]` 배열을 `time`의 시작 시각 오름차순으로 정렬하여 `index`번째 아이템이 존재하는지 확인합니다. `date` 키가 없거나 `index`가 배열 길이 이상이면 404(`Item not found`)를 반환합니다.
7. **Update**: 정렬된 배열의 `index`번째 아이템의 `status`를 요청값으로 변경합니다. 변경된 배열을 `day_plans`에 다시 저장하고 `updated_at`을 갱신합니다.

#### **4.2 `time` 필드 형식 및 정렬 기준**

`day_plans` 내 각 아이템의 `time` 필드는 `"HH:MM ~ HH:MM"` 형식의 시간 범위입니다 (24시간제, 예: `"09:00 ~ 12:00"`).

- **정렬 기준**: `time`의 시작 시각(첫 번째 `HH:MM`)을 기준으로 오름차순 정렬합니다.
- **중복 보장**: 동일 날짜의 아이템은 시간 범위가 서로 겹치지 않음이 보장됩니다. (아이템 추가/수정 시 겹침 검증이 이루어집니다.) 따라서 시작 시각 기준 정렬은 항상 안정적이고 유일한 순서를 보장합니다.

#### **4.3 DB 업데이트 구조**

`day_plans` 전체를 덮어쓰지 않고 해당 아이템의 `status`만 변경합니다.

```sql
UPDATE itineraries
SET
  day_plans = jsonb_set(
    day_plans,
    ARRAY[:date::text],
    :updatedArray::jsonb
  ),
  updated_at = NOW()
WHERE id = :itineraryId;
```

`:updatedArray` = 백엔드에서 index번째 아이템 status 변경 후 직렬화한 배열.

#### **4.4 스냅샷**

status 변경은 단순 체크/해제 액션이므로 `itinerary_logs` 스냅샷 저장 대상에서 제외합니다.

---

### **5. 호출 예시 (Example)**

```bash
curl -X PATCH https://your-api-domain.com/api/v1/itineraries/{itineraryId}/items/status \
  -H "Authorization: Bearer <clerk_jwt_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "date": "2026-05-01",
    "index": 0,
    "status": "done"
  }'
```
