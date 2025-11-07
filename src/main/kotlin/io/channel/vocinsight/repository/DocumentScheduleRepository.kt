package io.channel.vocinsight.repository

import io.channel.vocinsight.domain.DocumentSchedule
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface DocumentScheduleRepository : JpaRepository<DocumentSchedule, String> {
    fun findByTeamId(teamId: String): List<DocumentSchedule>
    fun findByIsActiveTrue(): List<DocumentSchedule>
}
