# CLAUDE.md - VOC Insight 프로젝트 가이드

## 프로젝트 개요

VOC Insight는 **채널톡 해커톤 "HACKY-TALKY 챌린저스 4기"**를 위해 개발되는 고객 상담 데이터 분석 및 자동 리포트 생성 도구입니다.

### 핵심 목표
- 채널톡의 핵심 가치인 **"Customer Driven"** 실현
- 고객 접점 부서(CX팀)와 개발팀 사이의 소통 간극을 데이터로 연결
- VOC(Voice of Customer) 데이터의 자동 수집, 분석, 주기적 리포트 생성을 통한 데이터 기반 의사결정 지원

### 해결하려는 문제
1. CX팀이 매일 접하는 고객의 불만사항, 버그 리포트, 기능 요청이 개발팀/기획팀에 효과적으로 전달되지 않음
2. 실제 고객 문제의 우선순위와 팀 내부 인식 우선순위 사이의 괴리
3. 데이터 가공의 필요성은 인지하나 리소스 부족으로 실행 불가
4. 채널톡의 상담 태그 데이터를 빠르게 집계하고 주기적으로 도큐먼트로 정리하는 기능 부재

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

## 프로젝트 구조

```
voc-insight/
├── src/
│   ├── main/
│   │   ├── kotlin/
│   │   │   └── io/channel/vocinsight/
│   │   │       ├── VocInsightApplication.kt           # 메인 애플리케이션
│   │   │       ├── config/                             # 설정 클래스
│   │   │       ├── controller/                         # REST API 컨트롤러
│   │   │       ├── service/                            # 비즈니스 로직
│   │   │       ├── repository/                         # 데이터 액세스
│   │   │       ├── domain/                             # 엔티티, DTO
│   │   │       ├── scheduler/                          # 스케줄러
│   │   │       └── external/                           # 외부 API 클라이언트
│   │   │           ├── channeltalk/                    # 채널톡 Open API
│   │   │           ├── document/                       # 채널톡 Document API
│   │   │           └── ai/                             # AI 서버 클라이언트
│   │   └── resources/
│   │       └── application.properties                  # 스프링 설정
│   └── test/
│       └── kotlin/
│           └── io/channel/vocinsight/
│               └── VocInsightApplicationTests.kt
├── build.gradle.kts                                    # 빌드 설정
├── settings.gradle.kts
├── README.md                                           # 프로젝트 상세 문서
└── CLAUDE.md                                           # 이 파일
```

## 핵심 기능

### 1. 데이터 동기화
- **팀 정보 조회 및 업데이트**: 채널톡 API로 팀 정보 동기화
- **태그 정보 조회 및 업데이트**: 태그 계층 구조 포함, 재귀적 조회 지원
- **채팅 데이터 주기 수집**: 매일 1회 채팅 데이터 업데이트

### 2. 데이터 조회 API
- **팀 조회**: 모든 팀 정보 조회
- **태그 조회**: 태그 계층 구조 조회 (상위 계층 ID로 하위 태그 재귀 조회)
- **채팅 통계**: 기간 및 태그 조건으로 채팅 개수 조회

### 3. 도큐먼트 주기 생성
- **주기 생성 설정 관리**: CRUD API 제공
  - 대상 팀, 태그, 시작일, 주기(일 단위) 설정
- **자동 리포트 생성**:
  1. 매일 스케줄러 실행
  2. 설정된 조건으로 채팅 데이터 조회
  3. AI 서버로 데이터 전송하여 리포트 텍스트 생성
  4. Document API로 도큐먼트 생성
  5. 생성된 도큐먼트 링크를 대상 팀에 전송

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

## 도메인 모델

### 팀 (Team)
- `id`: 팀 ID
- `name`: 팀 이름
- 채널톡 API로 정보 업데이트 필요

### 태그 (Tag)
- `id`: 태그 ID
- `name`: 태그 이름
- `parentId`: 상위 계층 태그 ID (nullable)
- 채널톡 API로 정보 업데이트 필요
- 조회 시 하위 태그를 재귀적으로 모두 조회

### 채팅 (Chat)
- `id`: 채팅 ID
- `tagIds`: 태그 ID 목록
- `createdAt`: 생성일시
- 매일 1회 채널톡 API로 업데이트

### 도큐먼트 주기 생성 요청 (DocumentSchedule)
- `id`: 요청 ID
- `targetTeamId`: 대상 팀 ID
- `tagIds`: 조회할 태그 ID 목록
- `startDate`: 시작일
- `periodDays`: 주기 (일 단위)
- 스케줄러가 이 테이블을 조회하여 각 작업을 스레드로 실행

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
- **예시**:
  ```bash
  curl -X GET "https://api.channel.io/open/v5/channel" \
    -H "accept: application/json" \
    -H "x-access-key: YOUR_ACCESS_KEY" \
    -H "x-access-secret: YOUR_ACCESS_SECRET"
  ```

### 2. Document API (도큐먼트 생성)
- **엔드포인트**: `https://document-api.channel.io`
- **인증 방식**: Basic Auth (base64 encoding of `access_key:access_secret`)
- **예시**:
  ```bash
  curl -X POST "https://document-api.channel.io/..." \
    -H "Authorization: Basic <credentials>" \
    -H "Accept: application/json"
  ```

## 개발 단계

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

## 개발 시 주의사항

### 1. 채널톡 API 인증
- API Key 및 Secret은 환경 변수로 관리 (`application.properties` 또는 `.env`)
- Open API와 Document API는 인증 방식이 다르므로 별도 클라이언트 구현

### 2. 데이터베이스
- 태그 계층 구조를 재귀 조회할 수 있도록 설계
- 채팅 데이터 조회 시 날짜 범위와 태그 필터링을 위한 인덱스 최적화
- 도큐먼트 주기 생성 설정 삭제 시 자동으로 스케줄러에서 제외

### 3. 스케줄러
- 매일 정해진 시간에 실행
- 각 도큐먼트 생성 작업은 별도 스레드로 실행하여 병렬 처리
- 작업 실패 시 로그 기록 및 알림

### 4. 도큐먼트 링크 생성
- 도큐먼트 생성 성공 시 다음 형식의 링크 생성:
  ```
  https://dest.channel.io/#/channels/{channelsId}/document/spaces/{spacesId}/articles/{articlesId}/revisions/{revisionsId}?revisionFrom=all
  ```
- 대상 팀 채팅방에 링크 메시지 전송

## 참고 자료

### 공식 문서
- [채널톡 Open API v5](https://api-doc.channel.io/)
- [채널톡 Document API](https://document-api.channel.io/swagger/index.html)

### 기술 스택
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Kotlin Documentation](https://kotlinlang.org/docs/home.html)

## 코드 작성 가이드

### Kotlin 코딩 컨벤션
- Spring Boot의 Kotlin 컨벤션 준수
- Data class 활용 (DTO, Entity)
- Extension function 적극 활용
- Null safety 활용 (`?.`, `?:`)

### 패키지 구조
```
io.channel.vocinsight/
├── config/                  # 설정 클래스
├── controller/              # REST API 컨트롤러
├── service/                 # 비즈니스 로직
├── repository/              # 데이터 액세스
├── domain/                  # 엔티티, DTO
├── scheduler/               # 스케줄러
└── external/                # 외부 API 클라이언트
    ├── channeltalk/         # 채널톡 Open API
    ├── document/            # 채널톡 Document API
    └── ai/                  # AI 서버 클라이언트
```

## 다음 액션 아이템

### 즉시 진행 (Phase 1)
1. Document API Client 구현
2. Open API Client 구현
3. 환경 변수 설정

### 이후 계획 (Phase 2~5)
1. 팀, 태그, 채팅 데이터 동기화 구현
2. 프론트엔드용 조회 API 구현
3. 도큐먼트 주기 생성 CRUD
4. AI 서버 연동 및 도큐먼트 생성 로직
5. 스케줄러 구현

---

**마지막 업데이트**: 2025-11-07
**현재 Phase**: Phase 1 (API 클라이언트 구현)
**예상 완료 시간**: Phase 1 완료까지 1~2시간
