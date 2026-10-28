# 연학당 백엔드

Java 21 · Spring Boot 4.0.9 · Spring MVC · Spring Security · Spring JDBC · Flyway · PostgreSQL.

회원, 동의, 결과 요약 저장, 서버 가격 주문, 포트원 V2 검증, 상품별 권한, 최초 열람, 환불 신청과 관리자 처리를 구현했습니다. **개인화 유료 콘텐츠 생성기는 아직 원본 프론트엔드에도 없습니다.** `SALES_ENABLED=false`가 기본값이며 결제 준비는 `503 PAID_CONTENT_NOT_READY`를 반환합니다. 완료된 인프라와 서비스 오픈 준비 상태를 구분해 주세요.

사주·만세력·학과 추천 계산을 Java로 복제하지 않았습니다. 프론트엔드가 계산한 최소 요약은 `CLIENT_UNVERIFIED`로 보관합니다. 서버는 결제와 접근 권한을 검증하며 계산 결과의 진실성을 보증하지 않습니다.

## 문서

- [프론트엔드 전달용 요약](docs/FRONTEND_HANDOFF.md): API 명세, 서버 주소 상태, CORS·인증 안내
- [이전 프로젝트 비교·배포 준비](docs/DEPLOYMENT.md): Render/Docker/Supabase 구성과 연학당 적용 절차
- [조사 결과와 원문 충돌](docs/AUDIT.md): ZIP 4개 문서, 프론트엔드, 배포 사이트 조사
- [API와 프론트엔드 연동](docs/API.md): 인증, 요청·응답, 오류, 결제·열람 흐름
- [운영과 미확정 사항](docs/OPERATIONS.md): DB 비교, 쿠키, 환불 복구, 개인정보, 외부 설정
- [최종 작업 보고](docs/REPORT.md): 요청한 7개 항목 순서로 구현·검증·미완료 정리

## 로컬 실행

JDK 21, Docker Compose를 준비합니다. Gradle Wrapper가 포함되어 있습니다. 운영 DB나 외부 계정을 생성하는 명령은 없습니다.

```powershell
docker compose up -d postgres
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.12' # 설치 경로에 맞게 수정
$env:DB_PASSWORD = 'local-development-only'
$env:COOKIE_SECURE = 'false' # 로컬 HTTP에서만
.\gradlew.bat bootRun
```

Linux/macOS에서는 `bash gradlew bootRun`을 사용합니다. 환경변수 목록은 [.env.example](.env.example)에 있습니다. Spring은 `.env`를 자동으로 읽지 않습니다. IDE나 실행 셸에서 명시적으로 주입하세요. 비밀값을 Git에 넣지 마세요.

카카오 없이 실행하면 OAuth 로그인 경로가 활성화되지 않으며 인증이 필요한 API는 401을 반환합니다. **일반 실행에 모의 로그인이나 결제 승인 API는 없습니다.** 모의 객체는 테스트 소스에만 있습니다.

카카오 테스트 시 REST API 키, 클라이언트 시크릿, 등록한 콜백 URI를 주입한 뒤:

```powershell
.\gradlew.bat bootRun --args='--spring.profiles.active=kakao'
# 브라우저에서 http://localhost:8080/oauth2/authorization/kakao 로 이동
```

콜백 URI 예: `http://localhost:8080/login/oauth2/code/kakao`. 콘솔에 정확히 일치하도록 등록해야 합니다. 외부 서비스 설정을 이 프로젝트가 대신 변경하지 않습니다.

## 검증

```powershell
.\gradlew.bat test bootJar
# Docker가 있는 환경에서 실제 PostgreSQL 마이그레이션·행 잠금 검증:
.\gradlew.bat postgresTest
```

기본 `test`는 H2 PostgreSQL 호환 모드, 실제 Spring 컨텍스트·MVC·Flyway와 모의 포트원을 사용합니다. `GatewayContractTest`는 로컬 HTTP 서버로 요청 형식을 검증합니다. `postgresTest`는 같은 서비스 시나리오를 Testcontainers PostgreSQL에서 실행합니다. H2 결과를 실제 PostgreSQL 또는 PG 검증 결과로 간주하면 안 됩니다.

## 구조와 선택 이유

`ApiController`/`AdminController`/`WebhookController` → 업무별 Service → `Store` Repository 순서입니다. `RefundPolicy`는 금액과 기간 계산을 담당하고 `PaymentGateway`가 외부 결제 경계입니다. JDBC를 사용해 주문·회원 행 잠금과 SQL 제약을 명시했습니다.

JDK 21은 이미 설치된 LTS이며 소스·도구 체인은 21로 고정했습니다. Spring Boot 4.0.9는 공식 안정 버전이며, Gradle 8.14.3은 공식 요구사항을 충족하고 기존 작업 환경의 Wrapper와 일치합니다. 최신 버전이라는 주장은 아닙니다. [공식 호환성 자료](https://docs.spring.io/spring-boot/4.0/system-requirements.html).

외부 설정 변경, 배포, 실결제·실환불은 수행하지 않았습니다.
