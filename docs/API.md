# API·프론트엔드 연동

기본 주소는 로컬 `http://localhost:8080`입니다. 모든 JSON은 UTF-8입니다. UUID는 서버 응답값을 사용합니다. DB 조회 응답 열 이름은 `snake_case`, 요청 DTO와 가공 응답은 아래의 `camelCase`입니다. 날짜는 ISO-8601 오프셋 포함 시각입니다. 예시 ID와 데이터는 설명용입니다.

공개 백엔드 주소는 아직 발급·배포되지 않았습니다. 위 localhost 주소는 각 개발자가 백엔드를 직접 실행했을 때 사용하는 주소입니다. 프론트팀 전달용 요약은 [FRONTEND_HANDOFF.md](FRONTEND_HANDOFF.md)에 있습니다.

기본 CORS 허용 Origin은 `https://yeonhakdang.vercel.app`, `http://localhost:3000` 두 개이며 쿠키 인증을 허용합니다. `CORS_ALLOWED_ORIGINS` 환경변수로 목록을 설정할 수 있습니다. `FRONTEND_ORIGIN`은 로그인·결제 복귀 목적지 하나를 지정하는 별도 설정입니다.

## 인증·CSRF

1. 브라우저 전체 이동으로 `GET /oauth2/authorization/kakao`를 엽니다. `kakao` 프로필에서만 활성화됩니다.
2. Spring Security가 state를 세션에 저장하고 인가 코드·토큰·사용자 정보를 서버에서 처리합니다. 콜백은 `/login/oauth2/code/kakao`입니다. 프론트에서 카카오 토큰이나 `loggedIn:true`를 보내지 않습니다.
3. 성공 시 설정한 프론트엔드 `/#9-1`로 복귀합니다. 기존 2-4·2-5 진행 상태 복원은 프론트에서 구현해야 합니다. 신규/미동의 사용자는 `/me`의 동의 목록을 확인하여 2-5로 안내합니다.
4. fetch는 `credentials:'include'`를 사용합니다. `GET /api/csrf`의 `{headerName:"X-CSRF-TOKEN",token:"..."}`를 받아 변경 요청 헤더에 넣습니다. 로그인·로그아웃·세션 만료 뒤 토큰을 갱신합니다. HttpOnly 쿠키를 JS로 읽지 않습니다.

```ts
const API = "http://localhost:8080";
let csrfToken = "";
async function refreshCsrf() {
  const r = await fetch(`${API}/api/csrf`, { credentials: "include" });
  csrfToken = (await r.json()).token;
}
async function api(path: string, method = "GET", body?: unknown, extra: Record<string,string> = {}) {
  const r = await fetch(`${API}/api${path}`, {
    method, credentials: "include",
    headers: { "Content-Type": "application/json", ...extra,
      ...(method === "GET" ? {} : { "X-CSRF-TOKEN": csrfToken }) },
    ...(body === undefined ? {} : { body: JSON.stringify(body) }),
  });
  if (!r.ok) throw await r.json();
  const text = await r.text();
  return text ? JSON.parse(text) : undefined;
}
```

위 코드는 이식용 예시이며 프론트엔드에 적용·실행하지 않았습니다.

## 엔드포인트

‘회원’은 활성 로그인 세션, ‘관리자’는 DB의 ADMIN 역할도 필요합니다. GET을 제외한 표의 모든 회원/관리자 변경 API에 CSRF 토큰이 필요합니다. 웹훅만 CSRF 대신 서명을 검증합니다.

| 메서드·경로 | 인증 | 입력 / 출력 |
|---|---|---|
| GET `/actuator/health` | 공개 | `{status:"UP"}`; `/api` 접두사 없음, 내부 상세 비공개 |
| GET `/api/csrf` | 공개 | 토큰·헤더 이름 |
| GET `/api/products` | 공개 | `[{code,title,price}]`, KRW·부가세 포함 |
| GET `/api/policies` | 공개 | `{version,required,optional,ageVerification}`; 전문은 프론트 공개 페이지 |
| GET `/api/me` | 회원 | `{id,policyVersion,consents:[{item,version,accepted,recorded_at}]}` |
| POST `/api/me/consents` | 회원 | 동의 DTO / 빈 200 |
| POST `/api/logout` | 현재 세션 | 현재 세션 무효화 / 204 |
| DELETE `/api/me` | 회원 | 탈퇴·서비스 데이터 삭제 / 빈 200 |
| POST `/api/results` | 회원·필수동의 | 최소 무료 결과 / `{id,provenance}` |
| GET `/api/results` | 회원 | 본인 `{id,engine_version,summary,provenance,created_at}` 배열 |
| POST `/api/orders` | 회원·필수동의 | 주문 DTO + `Idempotency-Key` / 주문 |
| GET `/api/orders` | 회원 | 본인 `{id,product_code,amount,status,created_at,paid_at}` 배열 |
| GET `/api/orders/{id}` | 소유 회원 | 주문 상태·메타데이터; 포트원 재조회 없음 |
| POST `/api/orders/{id}/prepare` | 소유 회원 | `{method:"CARD"\|"KAKAO"\|"TOSS"}` / SDK 요청 객체 |
| POST `/api/orders/{id}/complete` | 소유 회원 | 본문 없음 / 서버가 포트원을 재조회한 주문 상태 |
| GET `/api/locker` | 회원 | 상품별 메타데이터; 콘텐츠·열람 기록 생성 없음 |
| POST `/api/orders/{id}/products/{kind}/open` | 소유 회원 | 콘텐츠 반환과 최초 열람 기록을 한 트랜잭션으로 수행 |
| POST `/api/orders/{id}/refunds` | 소유 회원 | `{reason}` / 환불 신청 |
| GET `/api/orders/{id}/refunds` | 소유 회원 | 최신 환불 신청 |
| POST `/api/webhooks/portone` | 공개+서명 | 원문 JSON·웹훅 헤더 / 빈 200 |
| PUT `/api/admin/orders/{id}/products/{kind}/content` | 관리자 | `{version,json}` / 빈 200 |
| GET `/api/admin/refunds` | 관리자 | 처리 내역 |
| POST `/api/admin/refunds/{id}/process` | 관리자 | 증빙·정책 확인 후 취소 실행 / 환불 상태 |
| POST `/api/admin/refunds/{id}/reject` | 관리자 | 심사 거절·예약 권한 복원 / 빈 200 |
| POST `/api/admin/refunds/{id}/reconcile` | 관리자 | `{cancellationId}` / 포트원 취소내역 대조 |
| POST `/api/admin/orders/{id}/reconcile` | 관리자 | 포트원 GET으로 최신화 |

타인 리소스와 없는 리소스는 모두 404입니다. 관리자 권한을 부여하는 일반 사용자 API는 없습니다. `process`는 실제 취소 API를 호출하는 기능이므로 이 작업에서는 실행하지 않았습니다.

## 동의·결과 저장

버전은 `/policies`에서 받습니다. 필수 항목 3개와 선택 항목을 개별 저장합니다. 동일 항목의 가장 최근 기록을 적용합니다. 나이는 **자기 신고**이며 인증된 생년월일이 아닙니다.

```json
{"version":"2026-10-29-draft","age14":true,"terms":true,"privacy":true,"marketing":false}
```

무료 결과는 원본 생일·출생시간·이름·성별을 받지 않습니다. 기존 `analysis` 전체 대신 오행과 추천 학과 ID·점수만 보냅니다.

```json
{"engineVersion":"frontend-435e2f6","counts":{"wood":2,"fire":2,"earth":2,"metal":1,"water":1},"top":[{"id":"학과표의-id","score":85}]}
```

응답은 `{ "id":"서버-UUID", "provenance":"CLIENT_UNVERIFIED" }`입니다. 오행 키는 목→wood, 화→fire, 토→earth, 금→metal, 수→water이고 합계는 6 또는 8입니다. 이는 형식 검증일 뿐 사주 검증이 아닙니다.

조회 응답의 `summary`는 JSON 문자열입니다. 학과 설명 등은 프론트의 버전별 표에서 복원해야 합니다. 이 최소 요약만으로 현재 무료 결과 화면 전체를 정확히 복원하지는 못합니다. 이전 계산 전체 복원이 필요하면 보관 스냅샷 범위와 콘텐츠 버전 관리 방식을 추가 합의해야 합니다.

## 주문·결제

`Idempotency-Key: checkout_임의UUID` 헤더와 함께 아래를 보냅니다. 같은 키·본문은 같은 주문을 반환하고, 다른 본문은 409입니다. 결제 재시도·새로고침에서 키를 유지하세요.

```json
{"resultId":"00000000-0000-0000-0000-000000000001","product":"package","universities":["연세대학교","고려대학교"],"relation":"선배","policyVersion":"2026-10-29-draft","orderConsent":true,"refundConsent":true}
```

`product`: `compat`, `benefactor`, `charm`, `package`. 궁합·패키지는 대학 1~6개, 귀인·패키지는 `선배|동기|교수님|연인`이 필요합니다. 다른 상품에서는 빈 배열·빈 문자열을 보냅니다. 가격이나 결제 성공 주장은 받지 않습니다. 주문마다 입력·권한을 분리하여 다른 대학/관계로 다시 구매한 결과와 구분합니다.

주문 응답 예시(주요 필드):

```json
{"id":"00000000-0000-0000-0000-000000000002","product_code":"package","amount":3790,"currency":"KRW","status":"CREATED","payment_id":"yhd-00000000-0000-0000-0000-000000000002","paid_at":null}
```

`prepare` 응답 예시:

```json
{"storeId":"상점ID","channelKey":"테스트채널","paymentId":"yhd-00000000-0000-0000-0000-000000000002","orderName":"3종 패키지","totalAmount":3790,"currency":"CURRENCY_KRW","payMethod":"EASY_PAY","easyPay":{"easyPayProvider":"KAKAOPAY"},"customData":{"orderId":"00000000-0000-0000-0000-000000000002"},"redirectUrl":"https://프론트도메인/?checkoutOrder=00000000-0000-0000-0000-000000000002","forceRedirect":true}
```

SDK 통화 `CURRENCY_KRW`와 REST 응답 `KRW`를 구분합니다. PG별 추가 고객 필수값은 계약 채널의 공식 안내에 따라 보완해야 합니다. 카드번호를 자체 입력받거나 백엔드로 보내지 마세요.

흐름:

1. 실제 로그인 → CSRF 갱신 → 필수 동의 → 무료 결과 저장.
2. 대학·관계 선택 → 주문/환불 제한 **두 동의** → 서버 주문 → 준비 데이터.
3. `@portone/browser-sdk/v2`의 `requestPayment(serverPayload)` 호출. 현재 기본 설정에서는 미준비 콘텐츠로 prepare가 차단됩니다.
4. 모바일 복귀 URL의 `checkoutOrder`로 서버 `complete`를 호출합니다. 반환값 방식도 지원하려면 프론트에서 같은 처리를 합니다. URL/SDK의 성공 주장으로 구매 플래그를 설정하지 않습니다. hash 화면 복귀·실패 UI는 프론트 작업입니다.
5. 서버가 상태·원금·통화·상점·paymentId·customData.orderId를 검증한 뒤 권한을 부여합니다. 웹훅과 완료 요청은 같은 동기화 경로를 사용합니다.
6. 결제 완료의 ‘나중에 보기’는 `/locker`만 조회합니다. 실제 열기 버튼에서 `POST .../open`을 호출하고 응답을 그립니다. preload/prefetch에서 호출하지 마세요.

## 보관함·유료 콘텐츠

```json
[{"order_id":"00000000-0000-0000-0000-000000000002","product_code":"compat","status":"ACTIVE","first_viewed_at":null,"content_version":null,"content_ready":false}]
```

`open`은 콘텐츠가 없으면 409 `CONTENT_PENDING`이며 열람 시각을 기록하지 않습니다. 준비된 응답 예시:

```json
{"content":{"title":"검수된 콘텐츠"},"contentVersion":"approved-v1","firstViewedAt":"2026-10-28T13:00:00Z"}
```

유료 콘텐츠 내부 스키마·생성 규칙은 미정입니다. 관리자 등록 `json`은 JSON 객체를 담은 문자열(48,000자 이내)입니다. 일반 회원은 등록할 수 없고 이미 등록된 결과를 덮어쓸 수 없습니다. 목록·주문 응답에 유료 원문은 포함되지 않습니다. 환불 완료 상품은 보관함에서 제외됩니다.

응답 준비와 열람 시각은 원자적으로 처리하지만 네트워크 단절·탭 종료 후 실제 화면 표시 여부까지 증명하지는 못합니다. 전달서의 ‘처음 표시’와 이 증거를 동일하게 취급할지 정책 검토가 필요합니다. 재열람은 최초 시각을 유지합니다.

## 환불

```json
{"reason":"CHANGE_OF_MIND"}
```

사유: `CHANGE_OF_MIND`, `INPUT_ERROR`, `NOT_PROVIDED`, `DUPLICATE_PAYMENT`, `SYSTEM_FAILURE`, `NOT_AS_DESCRIBED`, `MINOR_WITHOUT_CONSENT`. 사용자에게 환불액·예외 승인 플래그를 받지 않습니다.

응답 주요 필드: `{id,order_id,reason,status,amount,created_at,reviewed_at,reviewed_by,cancellation_id}`. `amount`는 정상 계산 또는 예외 심사 시 남은 결제잔액의 제안이며 승인 사실이 아닙니다. 활성 신청 중 반복 요청은 같은 신청을 반환합니다. 완료 후 같은 사유 재요청도 같은 결과를 반환합니다. 부분환불 후 별도 예외 사유는 남은 금액으로 새 심사를 받을 수 있습니다.

정상 환불은 미열람 권한만 즉시 예약합니다. 이미 본 패키지 상품은 이용할 수 있습니다. 예외 신청은 남은 권한을 예약하고 운영자가 근거·기간을 확인합니다. 거절하면 예약을 해제합니다.

MVP 콘솔 수동 취소도 지원합니다. 신청 생성 → 운영자 정책 확인 → 포트원 콘솔에서 정확한 금액 취소 → `cancellationId`를 관리자 `reconcile`로 제출합니다. 서버가 원 결제·취소 ID·금액·성공 상태를 재조회해야 완료됩니다. HTTP 200만으로 성공 처리하지 않습니다. 취소 타임아웃 후 `process` 재호출은 차단되고 재확인만 허용됩니다.

## 오류·웹훅

오류는 `{ "code":"ENTITLEMENT_NOT_ACTIVE" }` 형식입니다. 내부 예외·결제수단·비밀값은 반환하지 않습니다.

| HTTP | 대표 code | 처리 |
|---:|---|---|
| 400 | `INVALID_REQUEST`, `WEBHOOK_STORE_MISMATCH` | 입력 형식 확인 |
| 401 | `UNAUTHENTICATED`, `ACCOUNT_INACTIVE`, `INVALID_WEBHOOK_SIGNATURE` | 재로그인/설정 확인 |
| 403 | `FORBIDDEN` | CSRF·권한 확인 |
| 404 | `NOT_FOUND` | 타인·없는 리소스 동일 처리 |
| 409 | `REQUIRED_CONSENT`, `PURCHASE_CONSENT_REQUIRED` | 버전·동의 UI 갱신 |
| 409 | `IDEMPOTENCY_CONFLICT`, `ORDER_NOT_PAYABLE` | 키·본문·상태 확인 |
| 409 | `CONTENT_PENDING`, `ENTITLEMENT_NOT_ACTIVE`, `ORDER_NOT_ACCESSIBLE` | 준비/환불/검토 상태 안내 |
| 409 | `REFUND_WINDOW_EXPIRED`, `ALREADY_VIEWED` | 정책 안내·상담 |
| 409 | `PROVIDER_REVIEW_REQUIRED`, `REFUND_ALREADY_PROCESSED_OR_UNKNOWN` | 운영자 확인, 취소 재실행 금지 |
| 413 | `REQUEST_TOO_LARGE` | JSON 64KiB 이내 |
| 502 | `PORTONE_UNAVAILABLE`, `INVALID_PROVIDER_PAYMENT_TIME` | 실패 단정 대신 재조회 |
| 503 | `PAID_CONTENT_NOT_READY`, `CHANNEL_NOT_CONFIGURED`, `PORTONE_NOT_CONFIGURED`, `WEBHOOK_NOT_CONFIGURED` | 서비스/외부 설정 준비 |
| 500 | `INTERNAL_ERROR` | 일반 오류 표시 |

웹훅은 JSON 원문과 `webhook-id`, `webhook-timestamp`, `webhook-signature`를 사용합니다. ±5분과 HMAC-SHA256을 검증하고 알 수 없는 이벤트/추가 필드를 무시합니다. 포트원 재조회 실패는 5xx와 수신 기록 롤백으로 재시도를 허용합니다. 유효한 같은 이벤트는 한 번만 처리하며 상품 권한에도 DB 기본키가 있습니다.
