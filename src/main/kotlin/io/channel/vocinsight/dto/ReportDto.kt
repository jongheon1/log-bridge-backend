package io.channel.vocinsight.dto

data class ReportDataRequest(
    val period: PeriodInfo,
    val summary: String,
    val weeklyData: List<WeeklyTagData>
)

data class PeriodInfo(
    val start: String,
    val end: String,
    val totalWeeks: Int
)

data class WeeklyTagData(
    val weekNumber: Int,
    val weekStart: String,
    val weekEnd: String,
    val tags: List<TagDataInfo>
)

data class TagDataInfo(
    val tagName: String,
    val count: Int
)
