# VOC Insight 설정 가이드

## 1. 채널톡 앱 설정

### 1.1 Developer Portal에서 앱 등록

1. [채널톡 Developer Portal](https://developers.channel.io/)에 접속
2. "New App" 클릭하여 새 앱 생성
3. 앱 이름: `VOC Insight` (또는 원하는 이름)

### 1.2 App Secret 발급

1. 앱 설정 페이지에서 "Issue" 버튼 클릭
2. App Secret 복사 (나중에 `application.properties`에 사용)
3. **주의**: App Secret은 절대 노출하지 말 것!

### 1.3 Server Settings 설정

앱 설정 → General Settings → Server Settings에서 다음을 설정:

- **Function Endpoint URL**: `https://your-domain.com/functions`
- **WAM Endpoint URL**: `https://your-domain.com/resource/wam`

로컬 개발 시 ngrok 사용:
```bash
ngrok http 8080
```

ngrok에서 제공하는 URL (예: `https://abc123.ngrok.io`)을 사용:
- Function Endpoint: `https://abc123.ngrok.io/functions`
- WAM Endpoint: `https://abc123.ngrok.io/resource/wam`

### 1.4 Signing Key 생성

1. Server Settings에서 "Generate" 버튼 클릭
2. Signing Key 복사 (Function 요청 검증용)

## 2. 애플리케이션 설정

### 2.1 환경변수 설정 (필수)

민감한 정보는 `.env` 파일로 관리합니다.

#### 1. `.env` 파일 생성

```bash
cp .env.example .env
```

#### 2. `.env` 파일 수정

`.env` 파일을 열어서 실제 값으로 수정:

```bash
# Database Configuration
DB_USERNAME=your_db_username
DB_PASSWORD=your_db_password

# ChannelTalk Configuration
CHANNELTALK_APP_ID=your_app_id
CHANNELTALK_APP_SECRET=your_app_secret
CHANNELTALK_SIGNING_KEY=your_signing_key

# ChannelTalk Open API
CHANNELTALK_OPEN_API_ACCESS_KEY=your_open_api_access_key
CHANNELTALK_OPEN_API_ACCESS_SECRET=your_open_api_access_secret

# ChannelTalk Document API
CHANNELTALK_DOCUMENT_API_ACCESS_KEY=your_document_api_access_key
CHANNELTALK_DOCUMENT_API_ACCESS_SECRET=your_document_api_access_secret

# Claude API Configuration
CLAUDE_API_KEY=your_claude_api_key
```

#### 3. 환경변수 로드

실행하기 전에 환경변수를 로드:

```bash
source load-env.sh
```

#### 설정 값 찾는 방법:

- **CHANNELTALK_APP_ID**: Developer Portal의 앱 페이지 URL에서 확인
- **CHANNELTALK_APP_SECRET**: 1.2 단계에서 발급받은 값
- **CHANNELTALK_SIGNING_KEY**: 1.4 단계에서 생성한 값
- **CHANNELTALK_OPEN_API_ACCESS_KEY/SECRET**: Open API 설정에서 발급
- **CHANNELTALK_DOCUMENT_API_ACCESS_KEY/SECRET**: Document API 설정에서 발급
- **CLAUDE_API_KEY**: Claude Console에서 발급받은 API 키

### 2.2 데이터베이스 설정

MySQL 데이터베이스를 준비:

```bash
# Docker로 MySQL 실행 (권장)
docker-compose up -d

# 또는 직접 MySQL 설치 후 데이터베이스 생성
mysql -u root -p
CREATE DATABASE vocinsight;
CREATE USER 'vocuser'@'localhost' IDENTIFIED BY 'vocpass1234';
GRANT ALL PRIVILEGES ON vocinsight.* TO 'vocuser'@'localhost';
FLUSH PRIVILEGES;
```

## 3. 빌드 및 실행

### 3.1 빌드

```bash
./gradlew clean build
```

### 3.2 실행

```bash
./gradlew bootRun
```

또는 JAR 파일 직접 실행:
```bash
java -jar build/libs/voc-insight-0.0.1-SNAPSHOT.jar
```

### 3.3 로그 확인

서버가 정상적으로 시작되면 다음 로그가 출력됩니다:

```
Issuing access token...
Token issued successfully
Registering commands...
Commands registered successfully
```

에러가 발생하면:
- App Secret이 올바른지 확인
- App ID가 올바른지 확인
- 인터넷 연결 확인

## 4. ngrok 설정 (로컬 개발)

### 4.1 ngrok 설치

```bash
# Homebrew (Mac)
brew install ngrok

# 또는 https://ngrok.com/download 에서 다운로드
```

### 4.2 ngrok 실행

```bash
ngrok http 8080
```

### 4.3 URL 확인

터미널에 출력된 Forwarding URL을 복사:
```
Forwarding  https://abc123.ngrok.io -> http://localhost:8080
```

이 URL을 채널톡 Developer Portal의 Server Settings에 입력

## 5. 채널톡에서 테스트

### 5.1 Command 확인

1. 채널톡 Desk에 로그인
2. 채팅창에서 `/` 입력
3. `voc-dashboard` 명령어가 나타나는지 확인

### 5.2 WAM 실행

1. `/voc-dashboard` 명령어 입력 (또는 클릭)
2. VOC Insight Dashboard WAM이 열리는지 확인
3. "Get Random Number" 버튼 클릭
4. 랜덤 숫자가 표시되는지 확인

## 6. 트러블슈팅

### Command가 등록되지 않음

- 서버 로그에서 에러 확인
- App Secret이 올바른지 확인
- 채널톡 Developer Portal에서 App 권한 확인

### WAM이 열리지 않음

- ngrok URL이 올바른지 확인
- Function Endpoint가 제대로 설정되었는지 확인
- 브라우저 콘솔에서 에러 확인

### "Get Random Number" 버튼이 작동하지 않음

- 브라우저 개발자 도구 → Network 탭에서 요청 확인
- `/api/random` 엔드포인트가 응답하는지 확인
- CORS 설정 확인 (필요시 추가)

### ngrok 세션 만료

ngrok 무료 버전은 세션이 만료될 수 있습니다:
1. ngrok을 재시작
2. 새 URL을 채널톡 Developer Portal에 다시 설정

## 7. 다음 단계

기본 구현이 완료되었습니다. 이제 다음을 추가할 수 있습니다:

- [ ] 데이터베이스 연동
- [ ] Webhook 수신 로직
- [ ] AI 태그 분류 기능
- [ ] 실제 VOC 대시보드 UI
- [ ] 리포트 생성 기능

자세한 내용은 [README.md](README.md)와 [CLAUDE.md](CLAUDE.md)를 참고하세요.

## 8. 참고 자료

- [채널톡 Developer Portal](https://developers.channel.io/developers/en)
- [채널톡 App Tutorial](https://developers.channel.io/en/articles/Getting-Started-Tutorial-516161ed)
- [채널톡 인증 가이드](https://developers.channel.io/en/articles/Authentication-e7c2fb6f)
- [채널톡 Function 가이드](https://developers.channel.io/en/articles/Function-77250b17)
- [채널톡 Command 가이드](https://developers.channel.io/en/articles/Command-b3d200dc)
- [채널톡 WAM 가이드](https://developers.channel.io/en/articles/WAM-059680de)
