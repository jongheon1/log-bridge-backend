package io.channel.vocinsight.external.channeltalk

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

/**
 * 채널톡 Open API 클라이언트
 * 팀, 태그, 채팅 데이터 조회를 위한 클라이언트
 *
 * 인증 방식: x-access-key, x-access-secret 헤더 사용
 * 엔드포인트: https://api.channel.io/open/v5
 */
@Component
class ChannelTalkOpenApiClient(
    @Value("\${channeltalk.open-api.base-url}") private val baseUrl: String,
    @Value("\${channeltalk.open-api.access-key}") private val accessKey: String,
    @Value("\${channeltalk.open-api.access-secret}") private val accessSecret: String
) {
    private val logger = LoggerFactory.getLogger(ChannelTalkOpenApiClient::class.java)
    private val restClient: RestClient = RestClient.builder()
        .baseUrl(baseUrl)
        .defaultHeader("x-access-key", accessKey)
        .defaultHeader("x-access-secret", accessSecret)
        .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
        .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
        .build()

    /**
     * GET 요청 실행
     * @param path API 경로 (예: "/channel", "/tags")
     * @param queryParams 쿼리 파라미터 (nullable)
     * @return 응답 문자열
     */
    fun get(path: String, queryParams: Map<String, String>? = null): String {
        val url = buildUrl(path, queryParams)
        logger.debug("Executing GET request to: $baseUrl$url")

        val response = restClient.get()
            .uri(url)
            .retrieve()
            .body(String::class.java)

        logger.debug("GET response received")
        return response ?: ""
    }

    /**
     * POST 요청 실행
     * @param path API 경로
     * @param body 요청 바디 (JSON 문자열)
     * @return 응답 문자열
     */
    fun post(path: String, body: String): String {
        logger.debug("Executing POST request to: $baseUrl$path")

        val response = restClient.post()
            .uri(path)
            .body(body)
            .retrieve()
            .body(String::class.java)

        logger.debug("POST response received")
        return response ?: ""
    }

    /**
     * PUT 요청 실행
     * @param path API 경로
     * @param body 요청 바디 (JSON 문자열)
     * @return 응답 문자열
     */
    fun put(path: String, body: String): String {
        logger.debug("Executing PUT request to: $baseUrl$path")

        val response = restClient.put()
            .uri(path)
            .body(body)
            .retrieve()
            .body(String::class.java)

        logger.debug("PUT response received")
        return response ?: ""
    }

    /**
     * DELETE 요청 실행
     * @param path API 경로
     * @return 응답 문자열
     */
    fun delete(path: String): String {
        logger.debug("Executing DELETE request to: $baseUrl$path")

        val response = restClient.delete()
            .uri(path)
            .retrieve()
            .body(String::class.java)

        logger.debug("DELETE response received")
        return response ?: ""
    }

    /**
     * URL 생성 (쿼리 파라미터 포함)
     */
    private fun buildUrl(path: String, queryParams: Map<String, String>?): String {
        if (queryParams.isNullOrEmpty()) {
            return path
        }

        val queryString = queryParams.entries.joinToString("&") { (key, value) ->
            "$key=$value"
        }
        return "$path?$queryString"
    }
}
