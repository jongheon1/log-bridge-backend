package io.channel.vocinsight.service

import com.fasterxml.jackson.databind.ObjectMapper
import io.channel.vocinsight.domain.Team
import io.channel.vocinsight.dto.GroupsResponse
import io.channel.vocinsight.external.channeltalk.ChannelTalkOpenApiClient
import io.channel.vocinsight.repository.TeamRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional
class TeamService(
    private val teamRepository: TeamRepository,
    private val openApiClient: ChannelTalkOpenApiClient,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(TeamService::class.java)

    /**
     * 모든 팀 조회
     */
    fun getAllTeams(): List<Team> {
        return teamRepository.findAll()
    }

    /**
     * 채널톡 API로 팀 정보 동기화
     */
    fun syncTeams() {
        logger.info("Starting team sync...")

        try {
            // 채널톡 API 호출 - /groups 엔드포인트
            val responseJson = openApiClient.get("/groups")
            logger.debug("Teams API response: $responseJson")

            // JSON 파싱 - GroupsResponse DTO 사용
            val response = objectMapper.readValue(responseJson, GroupsResponse::class.java)

            response.groups.forEach { group ->
                val team = Team(
                    id = group.id,
                    name = group.name,
                    updatedAt = LocalDateTime.now()
                )
                teamRepository.save(team)
                logger.debug("Saved team: ${group.id} - ${group.name}")
            }

            logger.info("Team sync completed successfully. Total teams: ${response.groups.size}")
        } catch (e: Exception) {
            logger.error("Failed to sync teams", e)
            throw e
        }
    }
}
