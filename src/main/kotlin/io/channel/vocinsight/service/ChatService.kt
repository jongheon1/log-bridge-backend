package io.channel.vocinsight.service

import com.fasterxml.jackson.databind.ObjectMapper
import io.channel.vocinsight.domain.Chat
import io.channel.vocinsight.dto.ChatStatisticsResponse
import io.channel.vocinsight.dto.TagSeries
import io.channel.vocinsight.dto.UserChatsResponse
import io.channel.vocinsight.external.channeltalk.ChannelTalkOpenApiClient
import io.channel.vocinsight.repository.ChatRepository
import io.channel.vocinsight.repository.TagRepository
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Service
@Transactional
class ChatService(
    private val chatRepository: ChatRepository,
    private val tagRepository: TagRepository,
    private val openApiClient: ChannelTalkOpenApiClient,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(ChatService::class.java)

    /**
     * 채팅 통계 조회 (차트용)
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @param tagIds 태그 ID 목록
     * @return 일별 태그별 채팅 개수 (차트용 형식)
     */
    fun getChatStatistics(startDate: LocalDateTime, endDate: LocalDateTime, tagIds: List<String>): ChatStatisticsResponse {
        // 1. 날짜 범위 내의 모든 날짜 생성
        val startLocalDate = startDate.toLocalDate()
        val endLocalDate = endDate.toLocalDate()
        val allDates = generateDateRange(startLocalDate, endLocalDate)

        // 2. DB에서 기간 내 모든 채팅 조회
        val allChats = chatRepository.findByChatCreatedAtBetween(startDate, endDate)

        // 3. tagIds로 태그 정보 조회
        val tags = tagRepository.findAllById(tagIds)
        val tagMap = tags.associateBy { it.id }

        // 4. 데이터 그룹화: Map<TagId, Map<Date, Count>>
        val dataByTag = mutableMapOf<String, MutableMap<LocalDate, Long>>()

        allChats.forEach { chat ->
            val date = chat.chatCreatedAt.toLocalDate()
            val chatTagNames = chat.tagNames

            tagIds.forEach { tagId ->
                val tagName = tagMap[tagId]?.name
                if (tagName != null && chatTagNames.contains(tagName)) {
                    dataByTag.computeIfAbsent(tagId) { mutableMapOf() }
                        .merge(date, 1L) { old, new -> old + new }
                }
            }
        }

        // 5. 각 태그별로 일별 데이터 생성 (없는 날짜는 0으로 채움)
        val series = tagIds.map { tagId ->
            val tagName = tagMap[tagId]?.name ?: tagId
            val tagData = dataByTag[tagId] ?: emptyMap()

            val data = allDates.map { date ->
                tagData[date]?.toInt() ?: 0
            }

            TagSeries(
                tagId = tagId,
                tagName = tagName,
                data = data
            )
        }

        // 6. 응답 생성
        return ChatStatisticsResponse(
            dates = allDates.map { it.toString() },
            series = series
        )
    }

    /**
     * 날짜 범위 생성
     */
    private fun generateDateRange(startDate: LocalDate, endDate: LocalDate): List<LocalDate> {
        val dates = mutableListOf<LocalDate>()
        var current = startDate

        while (!current.isAfter(endDate)) {
            dates.add(current)
            current = current.plusDays(1)
        }

        return dates
    }

    /**
     * 채팅 통계 조회 (단순 개수)
     */
    fun countChats(startDate: LocalDateTime, endDate: LocalDateTime, tagIds: List<String>?): Long {
        return if (tagIds.isNullOrEmpty()) {
            chatRepository.countByChatCreatedAtBetween(startDate, endDate)
        } else {
            // tagIds로 태그 정보 조회
            val tags = tagRepository.findAllById(tagIds)
            val tagNames = tags.map { it.name }.toSet()

            // 태그 필터링이 필요한 경우
            chatRepository.findByChatCreatedAtBetween(startDate, endDate)
                .count { chat ->
                    val chatTagNames = chat.tagNames
                    tagNames.any { tagName -> chatTagNames.contains(tagName) }
                }
                .toLong()
        }
    }

    /**
     * 채널톡 API로 채팅 데이터 동기화
     */
    fun syncChats(startDate: LocalDateTime?, endDate: LocalDateTime?) {
        logger.info("Starting chat sync...")

        try {
            val start = startDate ?: LocalDateTime.now().minusDays(7)
            val end = endDate ?: LocalDateTime.now()

            // 채널톡 API 호출 - /user-chats 엔드포인트 (limit 500)
            // 날짜를 Unix timestamp(밀리초)로 변환
            val startTimestamp = start.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endTimestamp = end.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

            val queryParams = mapOf(
                "since" to startTimestamp.toString(),
                "until" to endTimestamp.toString(),
                "limit" to "500"
            )

            val responseJson = openApiClient.get("/user-chats", queryParams)
            logger.debug("Chats API response: $responseJson")

            // JSON 파싱 - UserChatsResponse DTO 사용
            val response = objectMapper.readValue(responseJson, UserChatsResponse::class.java)

            response.userChats.forEach { userChat ->
                val chatCreatedAt = LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(userChat.createdAt),
                    ZoneId.systemDefault()
                )

                // 태그 이름 목록 (API 응답의 tags 필드)
                val tagNames = userChat.tags ?: emptyList()

                val chat = Chat(
                    id = userChat.id,
                    tagNames = tagNames,
                    chatCreatedAt = chatCreatedAt,
                    updatedAt = LocalDateTime.now()
                )
                chatRepository.save(chat)
                logger.debug("Saved chat: ${userChat.id} with ${tagNames.size} tags")
            }

            logger.info("Chat sync completed successfully. Total synced: ${response.userChats.size}")
        } catch (e: Exception) {
            logger.error("Failed to sync chats", e)
            throw e
        }
    }
}
