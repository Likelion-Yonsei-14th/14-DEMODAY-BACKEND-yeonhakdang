# 프론트엔드 전달 사항

## API 명세와 서버 주소

- API 명세: [API.md](API.md). 이 Markdown 파일을 공유하면 요청·응답 예시, 인증, 결제/열람 흐름과 오류 코드를 확인할 수 있습니다.
- 로컬 Base URL: `http://localhost:8080` — 백엔드를 각자 실행했을 때 사용합니다.
- REST API prefix: `/api` (예: `http://localhost:8080/api/products`).
- 공개 개발 서버 Base URL: **아직 미배포·미확정**. localhost는 백엔드 개발자의 PC에 원격 접속하는 주소가 아닙니다.
- Swagger UI 주소는 제공하지 않습니다. 현재 명세는 위 Markdown 문서입니다.

현재 실행에 PostgreSQL이 필요하며 [README 실행 절차](../README.md#로컬-실행)를 따릅니다. 공개 HTTPS 서버가 준비되면 위 Base URL과 프론트의 환경변수를 같은 주소로 갱신해야 합니다.

이전 프로젝트를 참고한 Render/Docker 배포 준비와 환경변수는 [DEPLOYMENT.md](DEPLOYMENT.md)에 정리했습니다. Dockerfile과 공개 상태 확인 API `/actuator/health`를 추가했으며, Render·DB의 실제 서비스 생성은 아직 하지 않았습니다.

## CORS·인증

두 Origin을 동시에 허용하도록 코드에 반영했습니다:

```text
https://yeonhakdang.vercel.app
http://localhost:3000
```

- credentials 허용. 모든 API fetch에 `credentials: 'include'` 사용.
- 메서드: GET, POST, PUT, DELETE, OPTIONS.
- 요청 헤더: Content-Type, X-CSRF-TOKEN, Idempotency-Key.
- 변경 요청 전 `GET /api/csrf`로 받은 `token`을 `X-CSRF-TOKEN` 헤더로 전송.
- 로그인 시작: 브라우저 전체 이동 `/oauth2/authorization/kakao` (카카오 설정이 준비된 서버에서 사용).

서버 환경변수:

```dotenv
CORS_ALLOWED_ORIGINS=https://yeonhakdang.vercel.app,http://localhost:3000
```

`FRONTEND_ORIGIN`은 로그인/결제 후 복귀할 프론트 주소 하나입니다. 배포 프론트로 복귀할 개발 서버라면 `https://yeonhakdang.vercel.app`로 설정합니다. CORS 변경은 서버를 새 빌드로 실행/배포해야 적용됩니다.

쿠키 인증이므로 공개 서버의 HTTPS·SameSite·도메인 구조도 함께 맞춰야 합니다. 프론트 Vercel 주소와 API가 다른 사이트이면 CORS 허용만으로 로그인 쿠키가 보장되지는 않습니다. 자세한 설정은 [운영 문서](OPERATIONS.md#쿠키vercel보안)를 참고하세요.

## 우선 연동 순서

1. `/api/products`, `/api/policies`로 공개 조회 연결 확인.
2. 카카오 로그인 → `/api/csrf` → `/api/me` → 필요한 동의 `/api/me/consents`.
3. `/api/results`로 결과 요약 저장 → `/api/orders`로 주문 생성.
4. 준비된 테스트 채널에서 `/prepare` → 포트원 SDK → `/complete`.
5. `/api/locker` 목록 조회 후 사용자가 실제로 여는 시점에 상품별 `/open` 호출.

카카오 실설정과 포트원 테스트 채널은 아직 미연결입니다. 개인화 유료 콘텐츠 생성 방식도 미확정이므로 기본 설정에서 `/prepare`는 `503 PAID_CONTENT_NOT_READY`입니다. 현재 단계의 명세·CORS 준비 완료와 외부 서비스까지 연결된 서버 제공 완료를 구분해 주세요.

## 팀에 전달할 메시지 초안

> API 명세는 `docs/API.md`로 전달드립니다. CORS는 `https://yeonhakdang.vercel.app`과 `http://localhost:3000` 두 곳을 허용하도록 코드에 반영했습니다. 쿠키 인증 방식이라 fetch에 `credentials: 'include'`가 필요하고, 변경 요청에는 `/api/csrf`에서 받은 토큰을 `X-CSRF-TOKEN`으로 보내주세요.
>
> 공개 서버는 아직 배포 전이라 현재 전달 가능한 주소는 로컬 실행용 `http://localhost:8080`입니다. 팀 공용 HTTPS 주소는 배포 환경이 준비되면 공유하겠습니다. 카카오·포트원 실연동과 유료 콘텐츠 생성은 준비 중이며, 결제 준비 API는 기본 차단 상태입니다.
