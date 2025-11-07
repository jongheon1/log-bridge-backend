package io.channel.vocinsight.controller

import io.channel.vocinsight.dto.ChatStatisticsResponse
import io.channel.vocinsight.service.ChatService
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/chats")
class ChatController(
    private val chatService: ChatService
) {

    /**
     * 채팅 통계 조회 (차트용)
     * GET /api/chats/statistics?startDate=2025-01-01T00:00:00&endDate=2025-01-07T23:59:59&tagIds=tag_001,tag_002,tag_003
     *
     * 응답 형식:
     * {
     *   "dates": ["2025-01-01", "2025-01-02", "2025-01-03"],
     *   "series": [
     *     {
     *       "tagId": "tag_001",
     *       "tagName": "버그",
     *       "data": [5, 3, 7]
     *     }
     *   ]
     * }
     */
    @GetMapping("/statistics")
    fun getChatStatistics(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) startDate: LocalDateTime,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) endDate: LocalDateTime,
        @RequestParam tagIds: List<String>
    ): ChatStatisticsResponse {
        return chatService.getChatStatistics(startDate, endDate, tagIds)
    }

    /**
     * 채팅 개수 조회 (기간 및 태그 조건)
     * GET /api/chats/count?startDate=2025-01-01T00:00:00&endDate=2025-01-07T23:59:59&tagIds=tag_001,tag_002
     */
    @GetMapping("/count")
    fun getChatCount(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) startDate: LocalDateTime,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) endDate: LocalDateTime,
        @RequestParam(required = false) tagIds: List<String>?
    ): Map<String, Long> {
        val count = chatService.countChats(startDate, endDate, tagIds)
        return mapOf("count" to count)
    }

    /**
     * 채널톡 API로 채팅 데이터 동기화
     * POST /api/chats/sync?startDate=2025-01-01T00:00:00&endDate=2025-01-07T23:59:59
     */
    @PostMapping("/sync")
    fun syncChats(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) startDate: LocalDateTime?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) endDate: LocalDateTime?
    ): ResponseEntity<Map<String, String>> {
        return try {
            chatService.syncChats(startDate, endDate)
            ResponseEntity.ok(mapOf("message" to "Chats synced successfully"))
        } catch (e: Exception) {
            ResponseEntity.internalServerError()
                .body(mapOf("error" to (e.message ?: "Failed to sync chats")))
        }
    }
}
