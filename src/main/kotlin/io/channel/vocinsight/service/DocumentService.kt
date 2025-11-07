package io.channel.vocinsight.service

import com.fasterxml.jackson.databind.ObjectMapper
import io.channel.vocinsight.dto.CreateArticleRequest
import io.channel.vocinsight.dto.CreateArticleResponse
import io.channel.vocinsight.external.channeltalk.ChannelTalkOpenApiClient
import io.channel.vocinsight.external.document.ChannelTalkDocumentApiClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class DocumentService(
    private val documentApiClient: ChannelTalkDocumentApiClient,
    private val openApiClient: ChannelTalkOpenApiClient,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(DocumentService::class.java)

    companion object {
        private const val CHANNELS_ID = "218772"
        private const val SPACE_ID = "15307"
        private const val AUTHOR_ID = "86563"
    }

    /**
     * 도큐먼트를 생성하고 팀 챗에 링크 전송
     */
    fun createAndSendDocument(teamId: String, title: String, subtitle: String, bodyHtml: String): String {
        // 1. 도큐먼트 생성
        val articleResponse = createArticle(title, subtitle, bodyHtml)

        // 2. URL 생성
        val documentUrl = generateDocumentUrl(
            articleResponse.article.id,
            articleResponse.revision.id
        )

        // 3. 팀 챗에 메시지 전송
        sendMessageToTeam(teamId, documentUrl)

        logger.info("Document created and sent to team $teamId: $documentUrl")
        return documentUrl
    }

    /**
     * 도큐먼트 생성
     */
    private fun createArticle(title: String, subtitle: String, bodyHtml: String): CreateArticleResponse {
        val request = CreateArticleRequest(
            authorId = AUTHOR_ID,
            bodyHtml = bodyHtml,
            name = title,
            subtitle = subtitle,
            title = title
        )

        val requestJson = objectMapper.writeValueAsString(request)
        val responseJson = documentApiClient.post("/spaces/\$me/articles", requestJson)

        return objectMapper.readValue(responseJson, CreateArticleResponse::class.java)
    }

    /**
     * 도큐먼트 URL 생성
     */
    private fun generateDocumentUrl(articleId: String, revisionId: String): String {
        return "https://desk.channel.io/#/channels/$CHANNELS_ID/document/spaces/$SPACE_ID/articles/$articleId/revisions/$revisionId?revisionFrom=all"
    }

    /**
     * 팀 챗에 메시지 전송
     */
    private fun sendMessageToTeam(teamId: String, message: String) {
        val requestBody = """
            {
              "plainText": "$message"
            }
        """.trimIndent()

        openApiClient.post("/groups/$teamId/messages", requestBody)
        logger.info("Message sent to team $teamId")
    }
}
