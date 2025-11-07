package io.channel.vocinsight.dto

import java.time.LocalDate

/**
 * 차트용 채팅 통계 응답 DTO
 * x축: 날짜, y축: 채팅 개수, 각 선: 태그
 */
data class ChatStatisticsResponse(
    val dates: List<String>,  // ["2025-01-01", "2025-01-02", ...]
    val series: List<TagSeries>
)

data class TagSeries(
    val tagId: String,
    val tagName: String,
    val data: List<Int>  // 각 날짜별 채팅 개수
)

/**
 * 일별 태그별 채팅 개수 집계 결과
 */
data class DailyTagCount(
    val date: LocalDate,
    val tagId: String,
    val count: Long
)
