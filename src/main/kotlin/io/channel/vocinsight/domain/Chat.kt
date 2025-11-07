package io.channel.vocinsight.domain

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime

@Entity
@Table(name = "chats")
class Chat(
    @Id
    @Column(length = 50)
    val id: String = "",

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tag_names", columnDefinition = "JSON")
    var tagNames: List<String> = emptyList(),

    @Column(nullable = false)
    val chatCreatedAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
