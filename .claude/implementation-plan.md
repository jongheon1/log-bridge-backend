# VOC Insight 구현 계획

## 완료된 작업 (Phase 1)

### ✅ 프로젝트 문서 업데이트
- CLAUDE.md 수정: 새로운 프로젝트 계획 반영
- README.md 수정: Open API 기반 아키텍처로 변경

### ✅ API 클라이언트 구현
- `ChannelTalkOpenApiClient`: 팀, 태그, 채팅 조회용 클라이언트
  - 인증 방식: `x-access-key`, `x-access-secret` 헤더
  - 메서드: get(), post(), put(), delete()
  - 엔드포인트: `https://api.channel.io/open/v5`

- `ChannelTalkDocumentApiClient`: 도큐먼트 생성용 클라이언트
  - 인증 방식: Basic Auth (base64 encoding)
  - 메서드: get(), post(), put(), delete()
  - 엔드포인트: `https://document-api.channel.io`

- 설정 클래스: `OpenApiConfig`, `DocumentApiConfig`
- 환경 변수 설정: `application.yml`

---

## Phase 2: 데이터 동기화 구현

### 2.1 도메인 모델 설계 및 구현

#### 팀 (Team) 엔티티
```kotlin
@Entity
@Table(name = "teams")
data class Team(
    @Id
    val id: String,           // 채널톡 팀 ID
    val name: String,         // 팀 이름
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)
```

#### 태그 (Tag) 엔티티
```kotlin
@Entity
@Table(name = "tags")
data class Tag(
    @Id
    val id: String,                  // 채널톡 태그 ID
    val name: String,                // 태그 이름
    val parentId: String? = null,    // 상위 태그 ID (nullable)
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)
```

#### 채팅 (Chat) 엔티티
```kotlin
@Entity
@Table(name = "chats")
data class Chat(
    @Id
    val id: String,                       // 채널톡 채팅 ID
    @ElementCollection
    val tagIds: List<String> = emptyList(), // 태그 ID 목록
    val createdAt: LocalDateTime,         // 생성일시
    val updatedAt: LocalDateTime = LocalDateTime.now()
)
```

### 2.2 Repository 구현

```kotlin
interface TeamRepository : JpaRepository<Team, String>

interface TagRepository : JpaRepository<Tag, String> {
    fun findByParentId(parentId: String): List<Tag>
    fun findByParentIdIsNull(): List<Tag>  // 최상위 태그 조회
}

interface ChatRepository : JpaRepository<Chat, String> {
    // 기간 및 태그 조건으로 채팅 개수 조회
    @Query("SELECT COUNT(c) FROM Chat c WHERE c.createdAt BETWEEN :startDate AND :endDate")
    fun countByDateRange(startDate: LocalDateTime, endDate: LocalDateTime): Long

    // 특정 태그를 포함하는 채팅 개수 조회
    @Query("SELECT COUNT(c) FROM Chat c JOIN c.tagIds t WHERE t IN :tagIds AND c.createdAt BETWEEN :startDate AND :endDate")
    fun countByTagIdsAndDateRange(tagIds: List<String>, startDate: LocalDateTime, endDate: LocalDateTime): Long
}
```

### 2.3 Service 계층 구현

#### TeamService
- `syncTeams()`: 채널톡 API로 팀 정보 조회 및 DB 저장
  - API 호출: `GET /teams` (실제 엔드포인트는 API 문서 확인 필요)
  - 응답 파싱 후 Team 엔티티로 변환
  - 기존 데이터와 비교하여 업데이트 또는 신규 생성

#### TagService
- `syncTags()`: 채널톡 API로 태그 정보 조회 및 DB 저장
  - API 호출: `GET /tags`
  - 태그 계층 구조 파싱
  - 재귀적으로 하위 태그까지 모두 저장

- `getTagsWithChildren(parentId: String)`: 특정 태그의 하위 태그 재귀 조회
  - 재귀 함수로 모든 하위 태그 조회
  - 계층 구조를 유지하면서 DTO로 변환

#### ChatService
- `syncChats(startDate: LocalDateTime?, endDate: LocalDateTime?)`: 채널톡 API로 채팅 데이터 조회 및 DB 저장
  - API 호출: `GET /user-chats` (페이징 처리 필요)
  - 날짜 범위 지정하여 대량 데이터 효율적 조회
  - 배치 insert로 성능 최적화

- `countChats(startDate: LocalDateTime, endDate: LocalDateTime, tagIds: List<String>?)`: 채팅 통계 조회
  - 기간 및 태그 조건으로 채팅 개수 반환

### 2.4 Controller 구현

```kotlin
@RestController
@RequestMapping("/api/teams")
class TeamController(private val teamService: TeamService) {

    @GetMapping
    fun getTeams(): List<TeamDto> = teamService.getAllTeams()

    @PostMapping("/sync")
    fun syncTeams(): ResponseEntity<String> {
        teamService.syncTeams()
        return ResponseEntity.ok("Teams synced successfully")
    }
}

@RestController
@RequestMapping("/api/tags")
class TagController(private val tagService: TagService) {

    @GetMapping
    fun getTags(): List<TagDto> = tagService.getAllTags()

    @GetMapping("/{id}/children")
    fun getTagChildren(@PathVariable id: String): List<TagDto> {
        return tagService.getTagsWithChildren(id)
    }

    @PostMapping("/sync")
    fun syncTags(): ResponseEntity<String> {
        tagService.syncTags()
        return ResponseEntity.ok("Tags synced successfully")
    }
}

@RestController
@RequestMapping("/api/chats")
class ChatController(private val chatService: ChatService) {

    @GetMapping("/count")
    fun getChatCount(
        @RequestParam startDate: String,
        @RequestParam endDate: String,
        @RequestParam(required = false) tagIds: List<String>?
    ): Map<String, Long> {
        val count = chatService.countChats(
            LocalDateTime.parse(startDate),
            LocalDateTime.parse(endDate),
            tagIds
        )
        return mapOf("count" to count)
    }

    @PostMapping("/sync")
    fun syncChats(
        @RequestParam(required = false) startDate: String?,
        @RequestParam(required = false) endDate: String?
    ): ResponseEntity<String> {
        chatService.syncChats(
            startDate?.let { LocalDateTime.parse(it) },
            endDate?.let { LocalDateTime.parse(it) }
        )
        return ResponseEntity.ok("Chats synced successfully")
    }
}
```

---

## Phase 3: 도큐먼트 주기 생성 기능

### 3.1 도메인 설계

#### DocumentSchedule 엔티티
```kotlin
@Entity
@Table(name = "document_schedules")
data class DocumentSchedule(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    val targetTeamId: String,              // 대상 팀 ID

    @ElementCollection
    val tagIds: List<String> = emptyList(), // 조회할 태그 ID 목록

    val startDate: LocalDate,               // 시작일
    val periodDays: Int,                    // 주기 (일 단위)
    val lastExecutedAt: LocalDateTime? = null, // 마지막 실행 시간

    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)
```

### 3.2 Repository 및 Service

```kotlin
interface DocumentScheduleRepository : JpaRepository<DocumentSchedule, Long> {
    fun findAllByOrderByIdAsc(): List<DocumentSchedule>
}

@Service
class DocumentScheduleService(
    private val repository: DocumentScheduleRepository
) {
    fun getAllSchedules(): List<DocumentScheduleDto> =
        repository.findAllByOrderByIdAsc().map { it.toDto() }

    fun getSchedule(id: Long): DocumentScheduleDto? =
        repository.findById(id).orElse(null)?.toDto()

    fun createSchedule(dto: CreateDocumentScheduleDto): DocumentScheduleDto {
        val schedule = dto.toEntity()
        return repository.save(schedule).toDto()
    }

    fun updateSchedule(id: Long, dto: UpdateDocumentScheduleDto): DocumentScheduleDto? {
        val schedule = repository.findById(id).orElse(null) ?: return null
        val updated = schedule.copy(
            targetTeamId = dto.targetTeamId ?: schedule.targetTeamId,
            tagIds = dto.tagIds ?: schedule.tagIds,
            startDate = dto.startDate ?: schedule.startDate,
            periodDays = dto.periodDays ?: schedule.periodDays,
            updatedAt = LocalDateTime.now()
        )
        return repository.save(updated).toDto()
    }

    fun deleteSchedule(id: Long) {
        repository.deleteById(id)
    }
}
```

### 3.3 Controller

```kotlin
@RestController
@RequestMapping("/api/document-schedules")
class DocumentScheduleController(
    private val service: DocumentScheduleService
) {
    @GetMapping
    fun getAllSchedules() = service.getAllSchedules()

    @GetMapping("/{id}")
    fun getSchedule(@PathVariable id: Long) =
        service.getSchedule(id) ?: ResponseEntity.notFound().build()

    @PostMapping
    fun createSchedule(@RequestBody dto: CreateDocumentScheduleDto) =
        ResponseEntity.ok(service.createSchedule(dto))

    @PutMapping("/{id}")
    fun updateSchedule(@PathVariable id: Long, @RequestBody dto: UpdateDocumentScheduleDto) =
        service.updateSchedule(id, dto)?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.notFound().build()

    @DeleteMapping("/{id}")
    fun deleteSchedule(@PathVariable id: Long): ResponseEntity<Void> {
        service.deleteSchedule(id)
        return ResponseEntity.noContent().build()
    }
}
```

---

## Phase 4: AI 서버 연동 및 도큐먼트 생성

### 4.1 AI 서버 클라이언트 구현

```kotlin
@Component
class AiServerClient(
    @Value("\${ai.server.base-url}") private val baseUrl: String,
    private val restTemplate: RestTemplate = RestTemplate()
) {
    /**
     * 채팅 데이터를 AI 서버로 전송하여 리포트 텍스트 생성
     * @param chats 분석할 채팅 데이터
     * @return 생성된 리포트 텍스트
     */
    fun generateReport(chats: List<ChatData>): String {
        val requestBody = mapOf(
            "chats" to chats,
            "requestType" to "weekly_report"
        )

        val response = restTemplate.postForObject(
            "$baseUrl/generate-report",
            requestBody,
            Map::class.java
        )

        return response?.get("text") as? String ?: ""
    }
}

data class ChatData(
    val id: String,
    val messages: List<String>,
    val tags: List<String>,
    val createdAt: String
)
```

### 4.2 도큐먼트 생성 서비스

```kotlin
@Service
class DocumentGenerationService(
    private val documentApiClient: ChannelTalkDocumentApiClient,
    private val chatRepository: ChatRepository,
    private val aiServerClient: AiServerClient,
    private val teamRepository: TeamRepository
) {
    /**
     * 도큐먼트 생성 및 팀 챗 전송
     */
    fun generateAndSendDocument(schedule: DocumentSchedule) {
        // 1. 채팅 데이터 조회
        val endDate = LocalDateTime.now()
        val startDate = endDate.minusDays(schedule.periodDays.toLong())
        val chats = chatRepository.findByTagIdsAndDateRange(
            schedule.tagIds,
            startDate,
            endDate
        )

        // 2. AI 서버로 데이터 전송하여 리포트 텍스트 생성
        val chatData = chats.map { it.toChatData() }
        val reportText = aiServerClient.generateReport(chatData)

        // 3. 도큐먼트 생성
        val documentId = createDocument(reportText)

        // 4. 팀 챗에 도큐먼트 링크 전송
        val documentLink = buildDocumentLink(documentId)
        sendMessageToTeam(schedule.targetTeamId, documentLink)
    }

    private fun createDocument(content: String): String {
        // Document API를 사용하여 도큐먼트 생성
        // POST /open/v1/spaces/{spaceId}/articles
        val requestBody = """
        {
            "title": "VOC Weekly Report ${LocalDate.now()}",
            "content": "$content",
            "status": "published"
        }
        """.trimIndent()

        val response = documentApiClient.post("/open/v1/spaces/me/articles", requestBody)
        // 응답에서 documentId 추출
        return extractDocumentId(response)
    }

    private fun buildDocumentLink(documentId: String): String {
        // https://dest.channel.io/#/channels/{channelsId}/document/spaces/{spacesId}/articles/{articlesId}/revisions/{revisionsId}?revisionFrom=all
        return "https://dest.channel.io/#/channels/{channelId}/document/spaces/{spaceId}/articles/$documentId/revisions/{revisionId}?revisionFrom=all"
    }

    private fun sendMessageToTeam(teamId: String, message: String) {
        // Open API를 사용하여 팀 챗에 메시지 전송
        // POST /open/v5/teams/{teamId}/messages
        // 실제 API 엔드포인트는 문서 확인 필요
    }

    private fun extractDocumentId(response: String): String {
        // JSON 파싱하여 documentId 추출
        // 예: {"id": "doc123", ...}
        return "doc123" // placeholder
    }
}
```

---

## Phase 5: 스케줄러 구현

### 5.1 스케줄러 설정

```kotlin
@Configuration
@EnableScheduling
class SchedulerConfig
```

### 5.2 스케줄러 구현

```kotlin
@Component
class DocumentScheduler(
    private val documentScheduleRepository: DocumentScheduleRepository,
    private val documentGenerationService: DocumentGenerationService,
    private val chatService: ChatService
) {
    private val logger = LoggerFactory.getLogger(DocumentScheduler::class.java)

    /**
     * 매일 오전 9시에 실행
     * 1. 채팅 데이터 자동 동기화
     * 2. 도큐먼트 주기 생성 작업 실행
     */
    @Scheduled(cron = "0 0 9 * * *")
    fun executeDaily() {
        logger.info("Starting daily scheduler...")

        // 1. 채팅 데이터 동기화
        try {
            chatService.syncChats(startDate = null, endDate = null)
            logger.info("Chat sync completed")
        } catch (e: Exception) {
            logger.error("Failed to sync chats", e)
        }

        // 2. 도큐먼트 주기 생성 작업 실행
        val schedules = documentScheduleRepository.findAllByOrderByIdAsc()
        logger.info("Found ${schedules.size} document schedules")

        schedules.forEach { schedule ->
            if (shouldExecute(schedule)) {
                // 각 작업을 별도 스레드로 실행
                Thread {
                    try {
                        logger.info("Executing document generation for schedule ${schedule.id}")
                        documentGenerationService.generateAndSendDocument(schedule)

                        // 마지막 실행 시간 업데이트
                        documentScheduleRepository.save(
                            schedule.copy(lastExecutedAt = LocalDateTime.now())
                        )
                        logger.info("Document generation completed for schedule ${schedule.id}")
                    } catch (e: Exception) {
                        logger.error("Failed to generate document for schedule ${schedule.id}", e)
                    }
                }.start()
            }
        }
    }

    /**
     * 스케줄 실행 여부 판단
     * 시작일 이후이고, 주기에 맞는 날짜인지 확인
     */
    private fun shouldExecute(schedule: DocumentSchedule): Boolean {
        val today = LocalDate.now()

        // 시작일 이후가 아니면 실행하지 않음
        if (today.isBefore(schedule.startDate)) {
            return false
        }

        // 마지막 실행 시간이 없으면 실행
        val lastExecuted = schedule.lastExecutedAt?.toLocalDate() ?: return true

        // 마지막 실행 후 주기만큼 경과했는지 확인
        val daysSinceLastExecution = java.time.temporal.ChronoUnit.DAYS.between(lastExecuted, today)
        return daysSinceLastExecution >= schedule.periodDays
    }
}
```

---

## 추가 고려사항

### 1. 에러 핸들링
- 각 API 호출에 대한 예외 처리
- 재시도 로직 (Retry with exponential backoff)
- 실패한 작업에 대한 알림 (Slack, Email 등)

### 2. 페이징 처리
- 채팅 데이터 동기화 시 대량 데이터 페이징 처리
- 커서 기반 페이징 고려

### 3. 성능 최적화
- 배치 insert/update 사용
- 데이터베이스 인덱스 설정
- 캐싱 전략 (Redis 등)

### 4. 보안
- API Key를 환경 변수로 관리
- HTTPS 사용
- 입력 검증 및 SQL Injection 방지

### 5. 모니터링
- 스케줄러 실행 로그
- API 호출 성공/실패 메트릭
- 데이터베이스 성능 모니터링

### 6. 테스트
- 단위 테스트 (Service, Repository)
- 통합 테스트 (Controller)
- API 클라이언트 Mock 테스트

---

## 다음 단계

1. **Phase 2 시작**: 도메인 모델 설계 및 Repository 구현
2. **데이터베이스 설정**: H2 또는 PostgreSQL 연결 설정
3. **API 엔드포인트 확인**: 채널톡 API 문서에서 실제 엔드포인트 확인
4. **프론트엔드 개발**: React 앱 개발 시작

**예상 소요 시간**: Phase 2~5 완료까지 2~3일
