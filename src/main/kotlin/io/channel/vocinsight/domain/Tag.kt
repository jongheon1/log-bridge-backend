package io.channel.vocinsight.domain

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "tags")
data class Tag(
    @Id
    @Column(length = 50)
    val id: String = "",

    @Column(nullable = false, length = 200)
    var name: String = "",

    @Column(length = 50)
    var parentId: String? = null,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
