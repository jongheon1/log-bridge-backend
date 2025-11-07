package io.channel.vocinsight.controller

import io.channel.vocinsight.dto.CreateDocumentScheduleRequest
import io.channel.vocinsight.dto.DocumentScheduleResponse
import io.channel.vocinsight.service.DocumentScheduleService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/document-schedules")
class DocumentScheduleController(
    private val documentScheduleService: DocumentScheduleService
) {

    /**
     * 모든 도큐먼트 주기 생성 스케줄 조회
     * GET /api/document-schedules
     */
    @GetMapping
    fun getAllSchedules(): List<DocumentScheduleResponse> {
        return documentScheduleService.getAllSchedules()
    }

    /**
     * 특정 도큐먼트 주기 생성 스케줄 조회
     * GET /api/document-schedules/{id}
     */
    @GetMapping("/{id}")
    fun getSchedule(@PathVariable id: String): ResponseEntity<DocumentScheduleResponse> {
        val schedule = documentScheduleService.getSchedule(id)
        return if (schedule != null) {
            ResponseEntity.ok(schedule)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * 특정 팀의 도큐먼트 주기 생성 스케줄 조회
     * GET /api/document-schedules/team/{teamId}
     */
    @GetMapping("/team/{teamId}")
    fun getSchedulesByTeam(@PathVariable teamId: String): List<DocumentScheduleResponse> {
        return documentScheduleService.getSchedulesByTeamId(teamId)
    }

    /**
     * 도큐먼트 주기 생성 스케줄 생성
     * POST /api/document-schedules
     */
    @PostMapping
    fun createSchedule(@RequestBody request: CreateDocumentScheduleRequest): ResponseEntity<DocumentScheduleResponse> {
        return try {
            val schedule = documentScheduleService.createSchedule(request)
            ResponseEntity.ok(schedule)
        } catch (e: Exception) {
            ResponseEntity.internalServerError().build()
        }
    }

    /**
     * 도큐먼트 주기 생성 스케줄 삭제
     * DELETE /api/document-schedules/{id}
     */
    @DeleteMapping("/{id}")
    fun deleteSchedule(@PathVariable id: String): ResponseEntity<Map<String, String>> {
        return try {
            documentScheduleService.deleteSchedule(id)
            ResponseEntity.ok(mapOf("message" to "Schedule deleted successfully"))
        } catch (e: Exception) {
            ResponseEntity.internalServerError()
                .body(mapOf("error" to (e.message ?: "Failed to delete schedule")))
        }
    }

    /**
     * 도큐먼트 주기 생성 스케줄 실행 (테스트용)
     * POST /api/document-schedules/{id}/execute
     */
    @PostMapping("/{id}/execute")
    fun executeSchedule(@PathVariable id: String): ResponseEntity<Map<String, String>> {
        return try {
            val documentUrl = documentScheduleService.executeSchedule(id)
            ResponseEntity.ok(mapOf(
                "message" to "Schedule executed successfully",
                "documentUrl" to documentUrl
            ))
        } catch (e: Exception) {
            ResponseEntity.internalServerError()
                .body(mapOf("error" to (e.message ?: "Failed to execute schedule")))
        }
    }
}
