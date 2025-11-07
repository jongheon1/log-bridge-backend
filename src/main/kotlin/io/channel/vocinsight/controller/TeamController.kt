package io.channel.vocinsight.controller

import io.channel.vocinsight.domain.Team
import io.channel.vocinsight.service.TeamService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/teams")
class TeamController(
    private val teamService: TeamService
) {

    /**
     * 모든 팀 조회
     * GET /api/teams
     */
    @GetMapping
    fun getTeams(): List<Team> {
        return teamService.getAllTeams()
    }

    /**
     * 채널톡 API로 팀 정보 동기화
     * POST /api/teams/sync
     */
    @PostMapping("/sync")
    fun syncTeams(): ResponseEntity<Map<String, String>> {
        return try {
            teamService.syncTeams()
            ResponseEntity.ok(mapOf("message" to "Teams synced successfully"))
        } catch (e: Exception) {
            ResponseEntity.internalServerError()
                .body(mapOf("error" to (e.message ?: "Failed to sync teams")))
        }
    }
}
