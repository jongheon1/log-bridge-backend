# 변경 내역

## 2025-11-07 - 토큰 캐싱 및 구조 개선

### 1. application.properties → application.yml 변경

**이유:** YAML이 더 읽기 쉽고 구조화되어 있음

**Before:**
```properties
spring.application.name=voc-insight
channeltalk.app-id=6909d92d6fd14badaca2
```

**After:**
```yaml
spring:
  application:
    name: voc-insight

channeltalk:
  app-id: 6909d92d6fd14badaca2
  app-secret: VqNWN_ZMnYGAhgo0f3ttfQ==
  api-base-url: https://app-store-api.channel.io
  signing-key: a5787da5115bb886ba9592d0c2eeb3edeb4fc0e93eabe208c535357de58f4338
```

### 2. 토큰 캐싱 구현 (Map 기반)

**새 파일:**
- `TokenCache.kt` - ConcurrentHashMap 기반 토큰 캐싱

**특징:**
- App-level token: 캐시 키 `"app"`
- Channel-level token: 캐시 키 `channelId`
- 만료 시간 자동 체크 (1분 버퍼)
- Thread-safe (ConcurrentHashMap)

**로그 예시:**
```
Issuing new app token...
App token issued and cached
// 30분 내 재사용
Using cached app token
```

### 3. channelId 구분 처리

**ChannelTalkClient 메서드:**

#### issueAppToken()
```kotlin
// channelId 없이 발급
// 용도: Command 등록, 앱 레벨 작업
fun issueAppToken(): TokenResponse
```

#### issueChannelToken(channelId)
```kotlin
// channelId 포함하여 발급
// 용도: 메시지 전송, 채널별 작업
fun issueChannelToken(channelId: String): TokenResponse
```

**API 요청 차이:**

App Token:
```json
{
  "method": "issueToken",
  "params": {
    "secret": "..."
  }
}
```

Channel Token:
```json
{
  "method": "issueToken",
  "params": {
    "secret": "...",
    "channelId": "channel123"
  }
}
```

### 4. ChannelTalkService 개선

**Before:**
```kotlin
fun registerCommands() {
    val token = client.issueToken().result  // 매번 발급
    client.registerCommands(token.accessToken, commands)
}
```

**After:**
```kotlin
fun registerCommands() {
    val token = getAppToken()  // 캐시 사용
    client.registerCommands(token.accessToken, commands)
}

fun getAppToken(): TokenResponseResult {
    // 1. 캐시 확인
    tokenCache.getAppToken()?.let { return it.token }

    // 2. 없으면 발급
    val token = client.issueAppToken().result

    // 3. 캐시 저장
    tokenCache.putAppToken(token)
    return token
}
```

### 5. 응답 구조 수정 (이중 직렬화 제거)

**Before (잘못됨):**
```kotlin
// 이중 직렬화
val json1 = objectMapper.writeValueAsString(wamResult)
val json2 = objectMapper.writeValueAsString(functionResult)
FunctionResponse(result = json2)  // String 타입
```

**After (올바름):**
```kotlin
// 객체 직접 사용
val wamAttributes = WamAttributes(appId, name, wamArgs)
val functionResult = FunctionResult("wam", wamAttributes)
FunctionResponse(result = functionResult)  // 객체 타입
```

**응답 차이:**

Before (잘못됨):
```json
{
  "result": "{\"type\":\"wam\",\"attributes\":\"...\"}"
}
```

After (올바름):
```json
{
  "result": {
    "type": "wam",
    "attributes": {
      "appId": "...",
      "name": "tutorial",
      "wamArgs": {...}
    }
  }
}
```

### 6. 로깅 개선

**application.yml에 추가:**
```yaml
logging:
  level:
    io.channel.vocinsight: DEBUG
```

**로그 예시:**
```
Issuing new app token...
App token issued and cached
Registering commands...
Commands registered successfully
Received function request: method=tutorial
WAM args: {rootMessageId=test123, broadcast=false, ...}
Returning WAM response
```

## 주요 변경 파일

### 새로 추가된 파일
- `src/main/resources/application.yml` (properties 대체)
- `src/main/kotlin/.../service/TokenCache.kt`
- `.claude/token-caching-guide.md`
- `.claude/response-structure-fix.md`
- `CHANGES.md` (이 파일)

### 수정된 파일
- `src/main/kotlin/.../client/ChannelTalkClient.kt`
  - `issueToken()` → `issueAppToken()`, `issueChannelToken()`
- `src/main/kotlin/.../service/ChannelTalkService.kt`
  - TokenCache 사용
  - `getAppToken()`, `getChannelToken()` 추가
- `src/main/kotlin/.../dto/FunctionResponse.kt`
  - `result: String?` → `result: FunctionResult?`
  - `attributes: String` → `attributes: Any`
- `src/main/kotlin/.../controller/FunctionController.kt`
  - 이중 직렬화 제거
  - 객체 직접 반환
- `CLAUDE.md`
  - 내부 문서 링크 추가

### 삭제된 파일
- `src/main/resources/application.properties`

## 테스트 방법

### 1. 빌드
```bash
./gradlew clean build
```

### 2. 서버 실행
```bash
./gradlew bootRun
```

### 3. 로그 확인
```
Issuing new app token...
App token issued and cached
Registering commands...
Commands registered successfully
```

### 4. 로컬 테스트
```bash
curl -X PUT http://localhost:8080/functions \
  -H "Content-Type: application/json" \
  -d '{
    "method": "tutorial",
    "params": {"trigger": {"type": "command", "attributes": {}}},
    "context": {"channel": {"id": "test"}, "caller": {"type": "manager", "id": "test"}}
  }'
```

**기대 결과:**
```json
{
  "result": {
    "type": "wam",
    "attributes": {
      "appId": "6909d92d6fd14badaca2",
      "name": "tutorial",
      "wamArgs": {...}
    }
  }
}
```

### 5. 채널톡 테스트
1. 채널톡 Desk 열기
2. `/tutorial` 입력
3. WAM 페이지 열리는지 확인
4. "Get Random Number" 버튼 클릭

## 성능 개선

### Before (토큰 캐싱 없음)
- Command 등록할 때마다 토큰 발급
- 불필요한 API 호출

### After (토큰 캐싱 있음)
- 30분 동안 토큰 재사용
- API 호출 횟수 대폭 감소

**예시:**
```
서버 시작 → 토큰 발급 (1회)
10분 후 Command 재등록 → 캐시 사용 (0회)
40분 후 Command 재등록 → 토큰 재발급 (1회)
```

## 다음 단계

현재 구현 완료:
- [x] 토큰 캐싱
- [x] channelId 구분
- [x] application.yml 변환
- [x] 응답 구조 수정

추가 구현 필요:
- [ ] Webhook 수신 로직
- [ ] AI 태그 분류
- [ ] VOC 데이터 저장
- [ ] 실제 대시보드 UI
- [ ] 리포트 생성 기능

## 참고 문서

- [토큰 캐싱 가이드](.claude/token-caching-guide.md)
- [응답 구조 수정 내역](.claude/response-structure-fix.md)
- [채널톡 API 구현 가이드](.claude/channeltalk-api-guide.md)
- [트러블슈팅 가이드](TROUBLESHOOTING.md)
