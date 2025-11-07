package io.channel.vocinsight.domain

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime

@Entity
@Table(name = "document_schedules")
class DocumentSchedule(
    @Id
    @Column(length = 50)
    val id: String = "",

    @Column(name = "team_id", length = 50, nullable = false)
    var teamId: String = "",

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tag_ids", columnDefinition = "JSON", nullable = false)
    var tagIds: List<String> = emptyList(),

    @Column(name = "start_date", nullable = false)
    var startDate: LocalDateTime = LocalDateTime.now(),

    @Column(name = "period_days", nullable = false)
    var periodDays: Int = 1,

    @Column(name = "last_executed_at")
    var lastExecutedAt: LocalDateTime? = null,

    @Column(name = "next_execution_at", nullable = false)
    var nextExecutionAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
