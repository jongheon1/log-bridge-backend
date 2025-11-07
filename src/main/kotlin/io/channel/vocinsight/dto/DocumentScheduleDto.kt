package io.channel.vocinsight.dto

import java.time.LocalDateTime

data class CreateDocumentScheduleRequest(
    val teamId: String,
    val tagIds: List<String>,
    val startDate: LocalDateTime,
    val periodDays: Int
)

data class DocumentScheduleResponse(
    val id: String,
    val teamId: String,
    val tagIds: List<String>,
    val startDate: LocalDateTime,
    val periodDays: Int,
    val lastExecutedAt: LocalDateTime?,
    val nextExecutionAt: LocalDateTime,
    val isActive: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)
