# VOC Insight - Customer-Driven 의사결정 지원 도구

## 프로젝트 개요

VOC Insight는 채널톡 해커톤 "HACKY-TALKY 챌린저스 4기"를 위해 개발된 고객 상담 데이터 분석 및 자동 리포트 생성 도구입니다. 채널톡의 핵심 가치인 "Customer Driven"을 실현하기 위해, 고객 접점 부서와 개발팀 사이의 소통 간극을 데이터로 메우는 것을 목표로 합니다.

### 해결하려는 문제

고객 상담을 직접 담당하는 CX팀은 고객의 불만사항, 버그 리포트, 기능 요청을 매일 접하지만, 이러한 VOC(Voice of Customer)가 개발팀이나 기획팀에 효과적으로 전달되지 못하는 경우가 많습니다. 그 결과 실제 고객이 겪는 문제의 우선순위와 팀 내부에서 인식하는 우선순위 사이에 괴리가 발생합니다.

데이터 가공이 필요하다는 것은 누구나 알지만, 리소스 부족으로 실행에 옮기지 못하는 것이 현실입니다. 채널톡의 상담 태그 데이터는 이미 고객의 목소리를 잘 대변하고 있지만, 이를 빠르게 집계하고 주기적으로 리포트화하여 팀 전체와 공유하는 기능이 부족합니다.

### 솔루션

VOC Insight는 채널톡 Open API를 활용하여 상담 태그 데이터를 자동으로 수집하고 분석합니다. 주기적으로 AI가 리포트를 생성하고, 이를 채널톡 도큐먼트로 발행하여 팀 전체가 고객의 목소리를 기반으로 의사결정할 수 있도록 돕습니다.

## 핵심 기능

### 1. 데이터 동기화
- **팀 정보 조회 및 업데이트**: 채널톡 API로 팀 정보 동기화
- **태그 정보 조회 및 업데이트**: 태그 계층 구조 포함, 재귀적 조회 지원
- **채팅 데이터 주기 수집**: 매일 1회 채팅 데이터 업데이트

### 2. 데이터 조회 및 통계
- **팀 조회**: 모든 팀 정보 조회
- **태그 조회**: 태그 계층 구조 조회 (상위 계층 ID로 하위 태그 재귀 조회)
- **채팅 통계**: 기간 및 태그 조건으로 채팅 개수 조회

### 3. 도큐먼트 주기 생성
- **주기 생성 설정 관리**: 대상 팀, 태그, 시작일, 주기(일 단위) 설정 가능
- **자동 리포트 생성**:
  1. 매일 스케줄러가 실행되어 설정된 조건으로 채팅 데이터 조회
  2. AI 서버로 데이터 전송하여 리포트 텍스트 생성
  3. Document API로 도큐먼트 생성
  4. 생성된 도큐먼트 링크를 대상 팀 채팅방에 자동 전송

## 기술 스택

### 백엔드
- **언어**: Kotlin 1.9.25
- **프레임워크**: Spring Boot 3.5.7
- **Java 버전**: Java 21
- **빌드 도구**: Gradle (Kotlin DSL)
- **데이터베이스**: H2 (개발용) / PostgreSQL (프로덕션 고려)
- **AI/ML**: 외부 AI 서버 연동
- **외부 연동**: Channel.io Open API v5, Channel.io Document API

### 프론트엔드
- **프레임워크**: React 18
- **차트 라이브러리**: Recharts (또는 Chart.js)

### 인프라
- **배포**: 로컬 서버 가동 + ngrok 공개 (해커톤용)
- **환경**: 로컬 개발 환경

## 아키텍처

```
┌──────────────────────────────────┐
│      채널톡 플랫폼 (고객 채팅)      │
│  UserChat + Message + Tags       │
└────────────┬─────────────────────┘
             │ Open API
             ▼
┌──────────────────────────────────┐
│    VOC Insight 백엔드             │
│  (Spring Boot + Kotlin)          │
│                                  │
│  1. 데이터 동기화 (팀/태그/채팅) │
│  2. 채팅 통계 API 제공            │
│  3. 주기 생성 설정 관리           │
│  4. 스케줄러 (매일 실행)          │
│     - 채팅 데이터 조회            │
│     - AI 서버 요청                │
│     - 도큐먼트 생성               │
│     - 팀 챗 전송                  │
└────────────┬─────────────────────┘
             │
             ├─► 채널톡 Open API (팀/태그/채팅)
             ├─► 채널톡 Document API (도큐먼트 생성)
             └─► AI 서버 (리포트 텍스트 생성)

             ▼
┌──────────────────────────────────┐
│       프론트엔드 (React)          │
│  - 팀/태그 업데이트 버튼          │
│  - 채팅 통계 조회                 │
│  - 주기 생성 설정 CRUD            │
└──────────────────────────────────┘
```

## API 명세

### 1. 팀 관리
- `GET /api/teams`: 모든 팀 조회
- `POST /api/teams/sync`: 채널톡 API로 팀 정보 동기화

### 2. 태그 관리
- `GET /api/tags`: 모든 태그 조회 (계층 구조 포함)
- `GET /api/tags/{id}/children`: 특정 태그의 하위 태그 재귀 조회
- `POST /api/tags/sync`: 채널톡 API로 태그 정보 동기화

### 3. 채팅 통계
- `GET /api/chats/count?startDate=...&endDate=...&tagIds=...`: 기간 및 태그 조건으로 채팅 개수 조회
- `POST /api/chats/sync`: 채널톡 API로 채팅 데이터 동기화

### 4. 도큐먼트 주기 생성
- `GET /api/document-schedules`: 모든 주기 생성 설정 조회
- `GET /api/document-schedules/{id}`: 특정 주기 생성 설정 조회
- `POST /api/document-schedules`: 주기 생성 설정 생성
- `PUT /api/document-schedules/{id}`: 주기 생성 설정 수정
- `DELETE /api/document-schedules/{id}`: 주기 생성 설정 삭제

## 채널톡 API 인증

### 1. Open API (팀, 태그, 채팅 조회)
- **엔드포인트**: `https://api.channel.io/open/v5`
- **인증 방식**: 헤더에 `x-access-key`, `x-access-secret` 추가

```bash
curl -X GET "https://api.channel.io/open/v5/channel" \
  -H "accept: application/json" \
  -H "x-access-key: YOUR_ACCESS_KEY" \
  -H "x-access-secret: YOUR_ACCESS_SECRET"
```

### 2. Document API (도큐먼트 생성)
- **엔드포인트**: `https://document-api.channel.io`
- **인증 방식**: Basic Auth (base64 encoding of `access_key:access_secret`)

```bash
curl -X POST "https://document-api.channel.io/..." \
  -H "Authorization: Basic <credentials>" \
  -H "Accept: application/json"
```

## 시작하기

### 사전 요구사항
- Java 21
- Kotlin 1.9.25
- Gradle

### 환경 설정

1. 프로젝트 클론
```bash
git clone <repository-url>
cd voc-insight
```

2. 환경 변수 설정 (`src/main/resources/application.properties`)
```properties
# 채널톡 Open API
channeltalk.open-api.base-url=https://api.channel.io/open/v5
channeltalk.open-api.access-key=YOUR_ACCESS_KEY
channeltalk.open-api.access-secret=YOUR_ACCESS_SECRET

# 채널톡 Document API
channeltalk.document-api.base-url=https://document-api.channel.io
channeltalk.document-api.access-key=YOUR_DOC_ACCESS_KEY
channeltalk.document-api.access-secret=YOUR_DOC_ACCESS_SECRET

# AI 서버
ai.server.base-url=YOUR_AI_SERVER_URL
```

3. 애플리케이션 실행
```bash
./gradlew bootRun
```

4. API 테스트
```bash
curl http://localhost:8080/api/teams
```

## 구현 계획

### Phase 1: API 클라이언트 구현 (현재 단계)
- [x] Spring Boot 프로젝트 초기 설정
- [ ] Document API Client 구현 (Basic Auth)
- [ ] Open API Client 구현 (x-access-key/secret)
- [ ] 환경 변수로 API Key 관리

### Phase 2: 데이터 동기화
- [ ] 팀 정보 조회 및 저장
- [ ] 태그 정보 조회 및 저장 (계층 구조)
- [ ] 채팅 데이터 조회 및 저장
- [ ] 동기화 API 구현 (프론트엔드 업데이트 버튼)

### Phase 3: 프론트엔드 API
- [ ] 팀 조회 API
- [ ] 태그 조회 API (재귀)
- [ ] 채팅 통계 API (기간, 태그 조건)
- [ ] 도큐먼트 주기 생성 CRUD API

### Phase 4: 자동 리포트 생성
- [ ] 도큐먼트 주기 생성 도메인 설계
- [ ] AI 서버 클라이언트 구현
- [ ] 도큐먼트 생성 로직 구현
- [ ] 팀 챗 메시지 전송 기능

### Phase 5: 스케줄러
- [ ] 매일 1회 실행되는 스케줄러 구현
- [ ] 채팅 데이터 자동 동기화
- [ ] 도큐먼트 주기 생성 작업 스레드로 실행
- [ ] 에러 핸들링 및 재시도 로직

## 참고 자료

- [채널톡 Open API v5](https://api-doc.channel.io/)
- [채널톡 Document API](https://document-api.channel.io/swagger/index.html)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Kotlin Documentation](https://kotlinlang.org/docs/home.html)

---

**마지막 업데이트**: 2025-11-07
**현재 Phase**: Phase 1 (API 클라이언트 구현)
**예상 완료 시간**: Phase 1 완료까지 1~2시간
