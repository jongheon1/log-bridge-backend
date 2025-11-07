package io.channel.vocinsight.service

import io.channel.vocinsight.domain.DocumentSchedule
import io.channel.vocinsight.dto.CreateDocumentScheduleRequest
import io.channel.vocinsight.dto.DocumentScheduleResponse
import io.channel.vocinsight.repository.DocumentScheduleRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

@Service
@Transactional
class DocumentScheduleService(
    private val documentScheduleRepository: DocumentScheduleRepository,
    private val documentService: DocumentService,
    private val reportService: ReportService,
    private val aiServerClient: io.channel.vocinsight.external.ai.AIServerClient
) {
    private val logger = LoggerFactory.getLogger(DocumentScheduleService::class.java)

    /**
     * 도큐먼트 주기 생성 스케줄 생성
     */
    fun createSchedule(request: CreateDocumentScheduleRequest): DocumentScheduleResponse {
        val id = UUID.randomUUID().toString()
        val nextExecutionAt = request.startDate

        val schedule = DocumentSchedule(
            id = id,
            teamId = request.teamId,
            tagIds = request.tagIds,
            startDate = request.startDate,
            periodDays = request.periodDays,
            lastExecutedAt = nextExecutionAt,
            nextExecutionAt = nextExecutionAt,
            isActive = true
        )

        val saved = documentScheduleRepository.save(schedule)
        logger.info("Created document schedule: $id for team ${request.teamId}")

        return toResponse(saved)
    }

    /**
     * 모든 스케줄 조회
     */
    fun getAllSchedules(): List<DocumentScheduleResponse> {
        return documentScheduleRepository.findAll().map { toResponse(it) }
    }

    /**
     * 특정 스케줄 조회
     */
    fun getSchedule(id: String): DocumentScheduleResponse? {
        return documentScheduleRepository.findById(id)
            .map { toResponse(it) }
            .orElse(null)
    }

    /**
     * 특정 팀의 스케줄 조회
     */
    fun getSchedulesByTeamId(teamId: String): List<DocumentScheduleResponse> {
        return documentScheduleRepository.findByTeamId(teamId).map { toResponse(it) }
    }

    /**
     * 스케줄 삭제
     */
    fun deleteSchedule(id: String) {
        documentScheduleRepository.deleteById(id)
        logger.info("Deleted document schedule: $id")
    }

    /**
     * 활성화된 스케줄 조회 (스케줄러용)
     */
    fun getActiveSchedules(): List<DocumentSchedule> {
        return documentScheduleRepository.findByIsActiveTrue()
    }

    /**
     * 스케줄 실행 (테스트용 / 나중에 스케줄러에서 호출)
     */
    fun executeSchedule(id: String): String {
        val schedule = documentScheduleRepository.findById(id)
            .orElseThrow { RuntimeException("Schedule not found: $id") }

        logger.info("Executing schedule: $id for team ${schedule.teamId}")

        // 주간 리포트 HTML 테이블 생성
        val tableHtml = reportService.generateWeeklyReportHtml(
            tagIds = schedule.tagIds,
            periodDays = schedule.periodDays,
            weeks = 8
        )

        // AI 서버로 8주 데이터 전송하여 분석 텍스트 받아오기
        val aiDataJson = reportService.generateWeeklyDataForAI(
            tagIds = schedule.tagIds,
            periodDays = schedule.periodDays,
            weeks = 8
        )
        logger.info("Sending 8-week data to AI: $aiDataJson")

        val aiAnalysisHtml = aiServerClient.requestReportAnalysis(aiDataJson)

        // 최종 bodyHtml 조합
        val title = "VOC 주간 리포트"
        val subtitle = "기간: 최근 8주 데이터"
        val bodyHtml = """
            <style>
                body { font-family: 'Noto Sans KR', sans-serif; line-height: 1.6; }
                .greeting { background-color: #f8f9fa; padding: 15px; border-left: 4px solid #4CAF50; margin: 20px 0; }
                .insight { background-color: #fff; padding: 15px; margin: 10px 0; border-left: 3px solid #2196F3; }
            </style>
            <h1>📊 VOC 주간 리포트</h1>
            <p><strong>생성 시간:</strong> ${LocalDateTime.now()}</p>
            <p><strong>대상 태그:</strong> ${schedule.tagIds.joinToString(", ")}</p>
            <hr/>
            <h2>📈 8주간 추이 데이터</h2>
            $tableHtml
            <hr/>
            <h2>💡 AI 분석 인사이트</h2>
            $aiAnalysisHtml
        """.trimIndent()

        // 도큐먼트 생성 및 팀에 전송
        val documentUrl = documentService.createAndSendDocument(
            teamId = schedule.teamId,
            title = title,
            subtitle = subtitle,
            bodyHtml = bodyHtml
        )

        // 실행 정보 업데이트
        val executedAt = LocalDateTime.now()
        schedule.lastExecutedAt = executedAt
        schedule.nextExecutionAt = executedAt.plusDays(schedule.periodDays.toLong())
        schedule.updatedAt = executedAt
        documentScheduleRepository.save(schedule)

        logger.info("Schedule executed successfully: $id")
        return documentUrl
    }

    private fun toResponse(schedule: DocumentSchedule): DocumentScheduleResponse {
        return DocumentScheduleResponse(
            id = schedule.id,
            teamId = schedule.teamId,
            tagIds = schedule.tagIds,
            startDate = schedule.startDate,
            periodDays = schedule.periodDays,
            lastExecutedAt = schedule.lastExecutedAt,
            nextExecutionAt = schedule.nextExecutionAt,
            isActive = schedule.isActive,
            createdAt = schedule.createdAt,
            updatedAt = schedule.updatedAt
        )
    }
}
