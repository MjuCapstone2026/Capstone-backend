## [POST] /api/users/signup

Clerk 인증 정보를 기반으로 서비스 데이터베이스에 사용자를 등록합니다. 멱등성(Idempotent)이 보장되어 여러 번 호출해도 안전합니다.

### 1. 기본 정보

| **항목** | **내용** |
| --- | --- |
| **Method** | `POST` |
| **URL** | `/api/users/signup` |
| **Summary** | 회원가입 및 사용자 동기화 |
| **Authentication** | **Bearer JWT** (Clerk 발급 토큰 필수) |

---

### 2. 요청 (Request)

#### 2.1 Headers

| **Name** | **Required** | **Example** | **Description** |
| --- | --- | --- | --- |
| **Authorization** | Y | `Bearer eyJhbGci...` | Clerk에서 발급받은 유효한 JWT 토큰 |

#### 2.2 Body

- **None** (본문 데이터 없음)

---

### 3. 응답 (Response)

#### 3.1 성공 (200 OK)

- **Description**: 회원가입이 성공적으로 완료되었거나, 이미 가입된 사용자입니다.
- **Body**: (No Body)

#### 3.2 인증 실패 (401 Unauthorized)

- **Description**: JWT 토큰이 누락되었거나 만료, 서명 오류 등으로 유효하지 않은 경우입니다.

    ```json
    {
      "status": 401,
      "error": "Unauthorized",
      "message": "Invalid or expired token."
    }
    ```


---

### 4. 비즈니스 로직 및 DB 스키마

#### 4.1 동작 흐름 (Sequence)

1. **Token Parsing**: `Authorization` 헤더에서 JWT를 추출하고 검증합니다.
2. **Claim Extraction**: JWT의 `sub` 클레임을 추출하여 `clerk_id`로 사용합니다.
3. **Conflict Check**: `users` 테이블에 해당 `clerk_id`가 존재하는지 확인합니다.
4. **Action**:
    - 존재하지 않을 경우: `INSERT` 수행 (새 사용자 등록)
    - 존재할 경우: 추가 작업 없이 성공 반환 (멱등성 유지)

#### 4.2 DB 저장 구조 (`users` Table)

| **Column** | **Type** | **Constraints** | **Description** |
| --- | --- | --- | --- |
| **clerk_id** | `VARCHAR` | **Primary Key** | Clerk에서 제공하는 고유 사용자 ID (`sub`) |
| **created_at** | `TIMESTAMPTZ` | `DEFAULT NOW()` | 데이터 생성 일시 (서버/DB 시간) |

---

### 5. 호출 예시 (Example)

```bash
curl -X POST https://your-api-domain.com/api/users/signup \
  -H "Authorization: Bearer <clerk_jwt_token>"
```