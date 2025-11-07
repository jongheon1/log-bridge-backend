package io.channel.vocinsight.service

import com.fasterxml.jackson.databind.ObjectMapper
import io.channel.vocinsight.domain.Tag
import io.channel.vocinsight.dto.ChatTagsResponse
import io.channel.vocinsight.external.channeltalk.ChannelTalkOpenApiClient
import io.channel.vocinsight.repository.TagRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional
class TagService(
    private val tagRepository: TagRepository,
    private val openApiClient: ChannelTalkOpenApiClient,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(TagService::class.java)

    /**
     * 모든 태그 조회
     */
    fun getAllTags(): List<Tag> {
        return tagRepository.findAll()
    }

    /**
     * 특정 태그의 하위 태그 재귀 조회
     */
    fun getTagsWithChildren(parentId: String): List<Tag> {
        val result = mutableListOf<Tag>()
        collectChildrenRecursively(parentId, result)
        return result
    }

    private fun collectChildrenRecursively(parentId: String, result: MutableList<Tag>) {
        val children = tagRepository.findByParentId(parentId)
        result.addAll(children)

        children.forEach { child ->
            collectChildrenRecursively(child.id, result)
        }
    }

    /**
     * 채널톡 API로 태그 정보 동기화
     */
    fun syncTags() {
        logger.info("Starting tag sync...")

        try {
            // 채널톡 API 호출 - /chat-tags 엔드포인트 (limit 500)
            val queryParams = mapOf("limit" to "500")
            val responseJson = openApiClient.get("/chat-tags", queryParams)
            logger.debug("Tags API response: $responseJson")

            // JSON 파싱 - ChatTagsResponse DTO 사용
            val response = objectMapper.readValue(responseJson, ChatTagsResponse::class.java)

            response.chatTags.forEach { chatTag ->
                val tag = Tag(
                    id = chatTag.id,
                    name = chatTag.name,
                    parentId = null,  // API 응답에 parentId가 없으므로 null
                    updatedAt = LocalDateTime.now()
                )
                tagRepository.save(tag)
                logger.debug("Saved tag: ${chatTag.id} - ${chatTag.name}")
            }

            logger.info("Tag sync completed successfully. Total tags synced: ${response.chatTags.size}")
        } catch (e: Exception) {
            logger.error("Failed to sync tags", e)
            throw e
        }
    }
}
