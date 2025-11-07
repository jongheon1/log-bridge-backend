package io.channel.vocinsight.repository

import io.channel.vocinsight.domain.Chat
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.LocalDateTime

@Repository
interface ChatRepository : JpaRepository<Chat, String> {

    /**
     * 기간으로 채팅 개수 조회
     */
    fun countByChatCreatedAtBetween(startDate: LocalDateTime, endDate: LocalDateTime): Long

    /**
     * 특정 태그를 포함하는 채팅 개수 조회 (Service에서 처리하도록 변경)
     */
    @Query("SELECT COUNT(c) FROM Chat c WHERE c.chatCreatedAt BETWEEN :startDate AND :endDate")
    fun countByTagIdsInAndChatCreatedAtBetween(
        @Param("tagIds") tagIds: List<String>,
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): Long

    /**
     * 기간으로 채팅 조회
     */
    fun findByChatCreatedAtBetween(startDate: LocalDateTime, endDate: LocalDateTime): List<Chat>
}
