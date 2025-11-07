# VOC Insight API 문서

## 개요

- **Base URL**:  https://a301ce025d78.ngrok-free.app
- **Content-Type**: `application/json`
- **날짜 형식**: ISO 8601 DateTime (`YYYY-MM-DDTHH:mm:ss`)

---

## 📊 Chat API (채팅 통계)

### 1. 채팅 통계 조회 (차트용)

차트 라이브러리(Recharts, Chart.js 등)에서 바로 사용 가능한 형식으로 데이터를 제공합니다.

**Endpoint**
```
GET /api/chats/statistics
```

**Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 | 예시 |
|---------|------|------|------|------|
| `startDate` | DateTime | ✅ | 조회 시작 날짜 | `2024-09-01T00:00:00` |
| `endDate` | DateTime | ✅ | 조회 종료 날짜 | `2024-11-30T23:59:59` |
| `tagIds` | String[] | ✅ | 태그 ID 목록 (쉼표 구분) | `2179328,2179332,2179335` |

**처리 로직**
1. 기간(`startDate`~`endDate`)으로 채팅 전체 조회
2. `tagIds`로 태그 정보 조회 (Tag 엔티티)
3. 태그의 `name`으로 채팅 필터링
4. 일별/태그별로 그룹화하여 반환

**Request 예시**
```http
GET /api/chats/statistics?startDate=2024-09-01T00:00:00&endDate=2024-11-30T23:59:59&tagIds=2179328,2179332,2179335
```

**Response 200 OK**
```json
{
  "dates": [
    "2024-09-01",
    "2024-09-02",
    "2024-09-03"
  ],
  "series": [
    {
      "tagId": "2179328",
      "tagName": "교환",
      "data": [5, 3, 7]
    },
    {
      "tagId": "2179332",
      "tagName": "배송",
      "data": [2, 4, 1]
    },
    {
      "tagId": "2179335",
      "tagName": "매장문의",
      "data": [0, 1, 3]
    }
  ]
}
```

**데이터 구조 설명**
- `dates`: x축에 표시할 날짜 배열 (문자열, "YYYY-MM-DD" 형식)
- `series`: 각 태그별 데이터 시리즈
  - `tagId`: 태그 고유 ID (요청한 tagIds와 매칭)
  - `tagName`: 태그 이름 (Tag 엔티티의 name, 차트 범례에 표시)
  - `data`: 각 날짜에 해당하는 채팅 개수 (숫자 배열, `dates`와 1:1 매칭)

**프론트엔드 사용 예시 (Recharts)**
```jsx
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend } from 'recharts';

// API 응답을 Recharts 형식으로 변환
const transformData = (apiResponse) => {
  return apiResponse.dates.map((date, index) => {
    const dataPoint = { date };
    apiResponse.series.forEach(series => {
      dataPoint[series.tagName] = series.data[index];
    });
    return dataPoint;
  });
};

// 사용
const chartData = transformData(response);
// 결과: [
//   { date: "2024-09-01", "교환": 5, "배송": 2, "매장문의": 0 },
//   { date: "2024-09-02", "교환": 3, "배송": 4, "매장문의": 1 },
//   ...
// ]

<LineChart data={chartData}>
  <XAxis dataKey="date" />
  <YAxis />
  <CartesianGrid strokeDasharray="3 3" />
  <Tooltip />
  <Legend />
  {response.series.map(series => (
    <Line
      key={series.tagId}
      type="monotone"
      dataKey={series.tagName}
      stroke={getColorByTag(series.tagId)}
    />
  ))}
</LineChart>
```

---

### 2. 채팅 개수 조회

특정 기간 및 태그 조건에 맞는 채팅의 총 개수를 반환합니다.

**Endpoint**
```
GET /api/chats/count
```

**Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 | 예시 |
|---------|------|------|------|------|
| `startDate` | DateTime | ✅ | 조회 시작 날짜 | `2024-09-01T00:00:00` |
| `endDate` | DateTime | ✅ | 조회 종료 날짜 | `2024-11-30T23:59:59` |
| `tagIds` | String[] | ❌ | 태그 ID 목록 (없으면 전체) | `2179328,2179332` |

**Request 예시**
```http
# 전체 채팅 개수
GET /api/chats/count?startDate=2024-09-01T00:00:00&endDate=2024-11-30T23:59:59

# 특정 태그 채팅 개수
GET /api/chats/count?startDate=2024-09-01T00:00:00&endDate=2024-11-30T23:59:59&tagIds=2179328,2179332
```

**Response 200 OK**
```json
{
  "count": 42
}
```

---

### 3. 채팅 데이터 동기화

채널톡 API에서 채팅 데이터를 가져와 로컬 DB에 저장합니다.

**Endpoint**
```
POST /api/chats/sync
```

**Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 | 기본값 |
|---------|------|------|------|--------|
| `startDate` | DateTime | ❌ | 동기화 시작 날짜 | 7일 전 |
| `endDate` | DateTime | ❌ | 동기화 종료 날짜 | 현재 |

**Request 예시**
```http
# 최근 7일 동기화
POST /api/chats/sync

# 특정 기간 동기화
POST /api/chats/sync?startDate=2024-09-01T00:00:00&endDate=2024-11-30T23:59:59
```

**Response 200 OK**
```json
{
  "message": "Chats synced successfully"
}
```

**Response 500 Internal Server Error**
```json
{
  "error": "Failed to sync chats"
}
```

---

### 4. 가짜 데이터 로드 (테스트용)

로컬에서 테스트할 때 사용. `mock-chats.json` 파일의 데이터를 DB에 로드합니다.

**Endpoint**
```
POST /api/chats/load-mock-data
```

**Request 예시**
```http
POST /api/chats/load-mock-data
```

**Response 200 OK**
```json
{
  "message": "Mock chats loaded successfully",
  "count": 120
}
```

---

## 🏷️ Tag API (태그)

### 1. 모든 태그 조회

시스템에 등록된 모든 태그를 조회합니다.

**Endpoint**
```
GET /api/tags
```

**Request 예시**
```http
GET /api/tags
```

**Response 200 OK**
```json
[
  {
    "id": "2179328",
    "name": "교환",
    "parentId": null,
    "createdAt": "2024-11-08T10:00:00",
    "updatedAt": "2024-11-08T10:00:00"
  },
  {
    "id": "2179332",
    "name": "배송",
    "parentId": null,
    "createdAt": "2024-11-08T10:00:00",
    "updatedAt": "2024-11-08T10:00:00"
  }
]
```

**데이터 구조**
- `id`: 태그 고유 ID
- `name`: 태그 이름
- `parentId`: 상위 태그 ID (계층 구조 지원, null이면 최상위)
- `createdAt`: 생성 시간
- `updatedAt`: 수정 시간

---

### 2. 특정 태그의 하위 태그 조회

계층 구조가 있는 태그의 경우, 특정 태그의 모든 하위 태그를 재귀적으로 조회합니다.

**Endpoint**
```
GET /api/tags/{id}/children
```

**Path Parameters**

| 파라미터 | 타입 | 설명 | 예시 |
|---------|------|------|------|
| `id` | String | 태그 ID | `2179328` |

**Request 예시**
```http
GET /api/tags/2179328/children
```

**Response 200 OK**
```json
[
  {
    "id": "2179329",
    "name": "교환/사이즈",
    "parentId": "2179328",
    "createdAt": "2024-11-08T10:00:00",
    "updatedAt": "2024-11-08T10:00:00"
  }
]
```

---

### 3. 태그 동기화

채널톡 API에서 태그 데이터를 가져와 로컬 DB에 저장합니다.

**Endpoint**
```
POST /api/tags/sync
```

**Request 예시**
```http
POST /api/tags/sync
```

**Response 200 OK**
```json
{
  "message": "Tags synced successfully"
}
```

**Response 500 Internal Server Error**
```json
{
  "error": "Failed to sync tags"
}
```

---

## 👥 Team API (팀)

### 1. 모든 팀 조회

시스템에 등록된 모든 팀을 조회합니다.

**Endpoint**
```
GET /api/teams
```

**Request 예시**
```http
GET /api/teams
```

**Response 200 OK**
```json
[
  {
    "id": "100000",
    "name": "CS팀",
    "createdAt": "2024-11-08T10:00:00",
    "updatedAt": "2024-11-08T10:00:00"
  },
  {
    "id": "100001",
    "name": "개발팀",
    "createdAt": "2024-11-08T10:00:00",
    "updatedAt": "2024-11-08T10:00:00"
  }
]
```

**데이터 구조**
- `id`: 팀 고유 ID
- `name`: 팀 이름
- `createdAt`: 생성 시간
- `updatedAt`: 수정 시간

---

### 2. 팀 동기화

채널톡 API에서 팀 데이터를 가져와 로컬 DB에 저장합니다.

**Endpoint**
```
POST /api/teams/sync
```

**Request 예시**
```http
POST /api/teams/sync
```

**Response 200 OK**
```json
{
  "message": "Teams synced successfully"
}
```

**Response 500 Internal Server Error**
```json
{
  "error": "Failed to sync teams"
}
```

---

## 📅 Document Schedule API (도큐먼트 주기 생성)

### 1. 스케줄 생성
```
POST /api/document-schedules
```

**Request Body**
```json
{
  "teamId": "100000",
  "tagIds": ["2179328", "2179332"],
  "startDate": "2025-11-10T00:00:00",
  "periodDays": 7
}
```

**Response 200 OK**
```json
{
  "id": "uuid",
  "teamId": "100000",
  "tagIds": ["2179328", "2179332"],
  "startDate": "2025-11-10T00:00:00",
  "periodDays": 7,
  "lastExecutedAt": null,
  "nextExecutionAt": "2025-11-10T00:00:00",
  "isActive": true,
  "createdAt": "2025-11-08T10:00:00",
  "updatedAt": "2025-11-08T10:00:00"
}
```

### 2. 모든 스케줄 조회
```
GET /api/document-schedules
```

### 3. 특정 스케줄 조회
```
GET /api/document-schedules/{id}
```

### 4. 특정 팀의 스케줄 조회
```
GET /api/document-schedules/team/{teamId}
```

### 5. 스케줄 삭제
```
DELETE /api/document-schedules/{id}
```

### 6. 스케줄 실행 (테스트용)
```
POST /api/document-schedules/{id}/execute
```

**Response 200 OK**
```json
{
  "message": "Schedule executed successfully",
  "documentUrl": "https://dest.channel.io/#/channels/218772/document/spaces/15307/articles/463621/revisions/735112?revisionFrom=all"
}
```

---

## 📝 사용 시나리오

### 시나리오 1: 초기 데이터 세팅

1. 가짜 채팅 데이터 로드
```http
POST /api/chats/load-mock-data
```

2. 태그 목록 조회
```http
GET /api/tags
```

3. 9월 한 달간 "교환" 태그 통계 확인
```http
GET /api/chats/statistics?startDate=2024-09-01T00:00:00&endDate=2024-09-30T23:59:59&tagIds=2179328
```

### 시나리오 2: 실시간 대시보드 구현

1. 페이지 로드 시 기본 데이터 가져오기
```http
# 모든 팀 조회
GET /api/teams

# 모든 태그 조회
GET /api/tags
```

2. 사용자가 날짜 범위와 태그 선택
```javascript
const startDate = "2024-09-01T00:00:00";
const endDate = "2024-11-30T23:59:59";
const selectedTagIds = ["2179328", "2179332", "2179335"]; // Tag 엔티티의 ID
```

3. 통계 데이터 요청 (tagIds 사용)
```http
GET /api/chats/statistics?startDate=2024-09-01T00:00:00&endDate=2024-11-30T23:59:59&tagIds=2179328,2179332,2179335
```

**처리 과정:**
- 서버는 `tagIds`로 Tag 엔티티 조회
- Tag의 `name` 필드로 Chat 데이터 필터링
- 일별/태그별로 집계하여 반환

4. 응답을 차트에 렌더링
```javascript
// 응답 데이터를 차트 라이브러리에 전달
renderChart(response);
```

### 시나리오 3: 정기 데이터 동기화

매일 자동으로 최신 데이터 동기화:

```javascript
// 매일 오전 1시에 실행
async function syncAllData() {
  // 1. 팀 동기화
  await fetch('/api/teams/sync', { method: 'POST' });

  // 2. 태그 동기화
  await fetch('/api/tags/sync', { method: 'POST' });

  // 3. 최근 7일 채팅 동기화
  await fetch('/api/chats/sync', { method: 'POST' });
}
```

---

## 🔍 에러 처리

모든 API는 실패 시 다음 형식으로 에러를 반환합니다:

**Response 500 Internal Server Error**
```json
{
  "error": "에러 메시지"
}
```

**프론트엔드 에러 처리 예시**
```javascript
try {
  const response = await fetch('/api/chats/statistics?...');
  const data = await response.json();

  if (!response.ok) {
    throw new Error(data.error || '알 수 없는 오류가 발생했습니다.');
  }

  // 정상 처리
  renderChart(data);
} catch (error) {
  console.error('API 호출 실패:', error.message);
  showErrorToast(error.message);
}
```

---

## 📌 주의사항

1. **날짜 형식**: 반드시 ISO 8601 형식 (`YYYY-MM-DDTHH:mm:ss`)을 사용하세요.
   - ✅ 올바른 예시: `2024-09-01T00:00:00`
   - ❌ 잘못된 예시: `2024-09-01`, `09/01/2024`

2. **태그 ID 구분**: 여러 태그를 조회할 때는 쉼표(`,`)로 구분하세요.
   - ✅ 올바른 예시: `tagIds=2179328,2179332,2179335`
   - ❌ 잘못된 예시: `tagIds=2179328&tagIds=2179332`
   - **중요**: `tagIds`는 Tag 엔티티의 실제 ID(예: "2179328")이며, 서버는 이 ID로 Tag를 조회한 후 Tag의 `name` 필드로 Chat 데이터를 필터링합니다.

3. **데이터 동기화**: `sync` API는 시간이 걸릴 수 있으니 로딩 UI를 표시하세요.

4. **차트 데이터**: `dates` 배열과 각 `series.data` 배열의 길이는 항상 동일합니다.

5. **빈 데이터**: 특정 날짜에 채팅이 없으면 `0`으로 표시됩니다.

---

## 🛠️ 개발 환경

- **로컬 서버**: `http://localhost:8080`
- **프로덕션**: TBD

---

**마지막 업데이트**: 2024-11-08
