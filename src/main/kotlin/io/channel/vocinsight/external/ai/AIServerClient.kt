package io.channel.vocinsight.external.ai

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class AIServerClient(
    @Value("\${claude.api-key}") private val apiKey: String,
    @Value("\${claude.model}") private val model: String,
    @Value("\${claude.system-prompt}") private val systemPrompt: String,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(AIServerClient::class.java)
    private val baseUrl = "https://api.anthropic.com/v1"

    private val restClient: RestClient = RestClient.builder()
        .baseUrl(baseUrl)
        .defaultHeader("x-api-key", apiKey)
        .defaultHeader("anthropic-version", "2023-06-01")
        .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
        .build()

    /**
     * Claude API에 리포트 분석 요청
     * @param reportData 리포트 데이터 (JSON 문자열)
     * @return AI 분석 결과 텍스트
     */
    fun requestReportAnalysis(reportData: String): String {
        logger.info("Requesting Claude API analysis...")
        logger.debug("Report data: $reportData")

        return try {
            val requestBody = createClaudeRequest(reportData)
            val requestJson = objectMapper.writeValueAsString(requestBody)

            logger.debug("Claude request: $requestJson")

            val response = restClient.post()
                .uri("/messages")
                .body(requestJson)
                .retrieve()
                .body(String::class.java)

            logger.debug("Claude response: $response")

            // 응답에서 content 추출
            val responseMap = objectMapper.readValue(response, Map::class.java)
            val content = responseMap["content"] as? List<*>
            val firstContent = content?.firstOrNull() as? Map<*, *>
            val text = firstContent?.get("text") as? String

            logger.info("AI analysis completed successfully")
            text ?: "AI 분석 결과를 받지 못했습니다."
        } catch (e: Exception) {
            logger.error("Failed to get AI analysis", e)
            "AI 분석 중 오류가 발생했습니다: ${e.message}"
        }
    }

    /**
     * Claude API 요청 바디 생성
     */
    private fun createClaudeRequest(reportData: String): Map<String, Any> {
        return mapOf(
            "model" to model,
            "max_tokens" to 4096,
            "system" to systemPrompt,
            "messages" to listOf(
                mapOf(
                    "role" to "user",
                    "content" to reportData
                )
            )
        )
    }
}
