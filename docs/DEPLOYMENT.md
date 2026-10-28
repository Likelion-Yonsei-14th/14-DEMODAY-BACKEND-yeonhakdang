# 이전 프로젝트 참고 및 배포 준비

이 문서는 로컬 배포 준비와 비교 결과입니다. Render/Supabase 서비스나 운영 DB를 생성·변경하지 않았으며 실제 배포 URL은 아직 없습니다.

## 14th-hackathon-team-2에서 확인한 구성

| 역할 | 저장소 설정 | 근거 |
|---|---|---|
| 백엔드 호스팅 | Render Web Service, Docker runtime, 서비스 이름 `wellness-backend`, Singapore 설정 | 저장소 루트 `render.yaml` |
| Java 이미지 | Temurin 21 JDK로 빌드 → 21 JRE로 실행, 비관리자 사용자 | `backend/Dockerfile` |
| DB | Supabase PostgreSQL, Hikari 연결 풀, Flyway 마이그레이션 | `docs/DEPLOYMENT.md`, `backend/src/main/resources/application.yml` |
| 인증·스토리지·실시간 | Supabase Auth/JWT, Storage, Realtime | `backend/README.md`, 배포 문서 |
| 상태 확인 | `/actuator/health` | `render.yaml` |
| 포트 | `${PORT:8080}` | `application.yml` |
| CORS | `APP_CORS_ALLOWED_ORIGINS` 환경변수 | `application.yml` |
| CI·배포 트리거 | GitHub Actions에서 Java 21 `test check`; Render는 커밋 배포 설정 | `.github/workflows/backend-ci.yml`, `render.yaml` |
| 프론트 API 주소 | `VITE_API_BASE_URL`, 없으면 `http://localhost:8080` | `frontend/src/services/apiClient.ts` |

위는 저장소에서 확인한 구성입니다. 당시 실제 운영 서비스의 상태·비용·계정·리전과 발급된 URL을 Render 콘솔에서 확인한 것은 아닙니다. 공개 소스·배포 문서에서는 확정된 `*.onrender.com` API 주소가 발견되지 않았습니다. `wellness-backend`라는 이름만 보고 URL을 만들어 전달하면 안 됩니다. 기존 프로젝트의 실제 접속키를 읽거나 복사하지 않았습니다.

## 연학당에 적용한 준비

- 루트 `Dockerfile`: Java 21 다단계 빌드, 비관리자 사용자로 JAR 실행.
- `.dockerignore`: 로컬 비밀값·Git·빌드 결과·테스트를 이미지 빌드 컨텍스트에서 제외.
- `${PORT:8080}`: Render가 주입하는 포트 지원, 기본 로컬 포트 8080.
- `GET /actuator/health`: 로그인 없이 상태 확인, DB 등 내부 상세 비공개. 웹 노출은 health만 허용.
- CORS: `https://yeonhakdang.vercel.app`, `http://localhost:3000` 동시 허용.

연학당은 이미 카카오 직접 로그인과 서버 세션을 사용합니다. 이전 프로젝트의 Supabase Auth/JWT·Storage·Realtime까지 이식할 필요는 없습니다. Render(Docker) + **연학당 전용 PostgreSQL** 구성을 후보로 사용할 수 있으며, PostgreSQL 공급자로 Supabase 또는 Render Postgres 등을 별도 선택하면 됩니다. 기존 해커톤 DB·계정을 그대로 연결하지 마세요.

## Render를 선택했을 때의 절차

계정, 이용 플랜/비용, DB 공급자, 리전, 공개 서버 생성은 아직 미확정입니다. 아래는 검토 후 실제 배포할 때의 절차이며 이번 작업에서 실행하지 않았습니다.

1. 연학당 백엔드 변경사항을 팀 저장소에 검토·반영합니다. 현재 파일은 로컬 작업 상태이므로 Render가 원격 Git에서 가져갈 수 있는 상태를 먼저 만들어야 합니다.
2. 연학당 전용 DB를 준비하고 JDBC URL·계정·비밀번호·TLS를 확인합니다. 시작 시 Flyway가 해당 DB에 테이블을 생성하므로 다른 프로젝트 DB를 지정하면 안 됩니다.
3. Render에서 Git 저장소를 연결한 Web Service를 선택하고 runtime **Docker**, Root Directory는 백엔드 저장소 루트, Dockerfile Path는 `./Dockerfile`, Health Check Path는 `/actuator/health`로 설정합니다.
4. 플랜과 리전은 사용자가 선택합니다. 해커톤 설정의 Singapore를 개인정보 이전 검토 없이 자동 복제하지 않습니다. 초기 배포는 수동으로 확인하고 자동 배포는 팀 브랜치/검증 절차에 맞춰 결정합니다.
5. 아래 환경변수를 Render에 입력합니다. 비밀값은 파일·브라우저 코드·채팅에 붙이지 않습니다.

```dotenv
DB_URL=jdbc:postgresql://<실제-DB-호스트>:<포트>/<DB명>?sslmode=verify-full
DB_USER=<DB계정>
DB_PASSWORD=<DB비밀번호>
CORS_ALLOWED_ORIGINS=https://yeonhakdang.vercel.app,http://localhost:3000
FRONTEND_ORIGIN=https://yeonhakdang.vercel.app
COOKIE_SECURE=true
COOKIE_SAME_SITE=None
SALES_ENABLED=false
```

위 None은 Vercel 도메인과 Render API 도메인이 서로 다른 사이트인 개발 구성을 가정합니다. 브라우저의 제3자 쿠키 차단 문제가 남을 수 있으므로 인증 실기기 검증과 같은 사이트 도메인/프록시 결정이 필요합니다. `CORS_ALLOWED_ORIGINS`와 로그인·결제 복귀 목적지 `FRONTEND_ORIGIN`은 별도입니다.

6. DB 연결 후 첫 공개 API 확인까지는 카카오 프로필 없이 실행할 수 있습니다. 공개 `/api/products`, `/api/policies`, `/api/csrf`, `/actuator/health`를 확인합니다. 보호된 API는 401이 정상입니다.
7. Render가 **실제로 발급한 HTTPS URL**을 복사해 프론트팀에 Base URL로 전달합니다. 이 시점 전에는 주소를 추측하지 않습니다.
8. 카카오 로그인까지 연동할 때 `SPRING_PROFILES_ACTIVE=kakao`, 키·시크릿, `KAKAO_REDIRECT_URI=https://<실제-API-호스트>/login/oauth2/code/kakao`를 설정하고 카카오 콘솔에 일치하는 URI를 등록합니다. 포트원은 별도 테스트 채널·웹훅 설정 후 검증합니다.

## DB 연결에서 이전 설정과 다른 점

이전 배포 문서는 Supabase transaction pooler 사용을 안내합니다. 현재 공식 문서는 지속 실행 백엔드에 direct connection(연결 가능한 네트워크) 또는 IPv4 환경의 session pooler를 안내하며, transaction pooler는 서버리스 등 짧은 연결에 적합한 선택으로 구분합니다. 따라서 이전 JDBC 주소를 그대로 복사하지 말고 Spring 상시 실행·Flyway에 맞게 확인해야 합니다. [Supabase 연결 문서](https://supabase.com/docs/guides/database/connecting-to-postgres).

Supabase를 선택하면 이 서비스의 주문·회원·세션 테이블이 Data API에 공개되지 않도록 전용 비공개 스키마/노출 설정·테이블 권한·RLS를 설계한 뒤 마이그레이션해야 합니다. 현재 일반 PostgreSQL용 스키마를 Supabase public 영역에 바로 생성하고 운영 준비가 끝났다고 간주하지 않습니다. 연결 수·백업·DB/앱 리전·위탁/국외 이전도 확인 대상입니다.

## 실행·검증 범위

```powershell
# Docker가 설치된 개발 환경에서 실행
docker build -t yeonhakdang-backend .
# 실제 비밀값을 출력하지 않는 로컬 전용 env 파일을 별도 준비한 뒤:
docker run --rm -p 8080:8080 --env-file .env yeonhakdang-backend
```

컨테이너의 localhost는 호스트 DB가 아닙니다. Windows Docker Desktop에서 호스트의 개발 DB에 연결한다면 해당 환경에 맞는 `host.docker.internal` 주소 또는 같은 Docker 네트워크의 DB 서비스 이름을 사용합니다. 개발 PC의 `.env`가 Spring에 자동으로 로드되는 것은 아니지만 위 `docker run --env-file`은 값을 컨테이너 환경변수로 주입합니다.

이 작업 환경에는 Docker 실행기가 없어 이미지 빌드·컨테이너 기동을 실행하지 못했습니다. JAR 빌드와 CORS·상태 확인 API는 Spring 통합 테스트로 검증합니다. 공개 배포·외부 DB 연결은 미실행입니다.

Render 무료 웹 서비스는 유휴 시 중지될 수 있어 첫 응답/웹훅 지연을 고려해야 하고, 무료 Render Postgres에는 만료 제한이 있습니다. 결제 운영용 플랜은 별도로 결정해야 합니다. [Render 무료 서비스 제한](https://render.com/docs/free).

공식 확인 자료: [Docker 배포](https://render.com/docs/docker), [Web Service와 포트](https://render.com/docs/web-services), [상태 확인](https://render.com/docs/health-checks).
