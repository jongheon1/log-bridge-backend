package io.channel.vocinsight.repository

import io.channel.vocinsight.domain.Tag
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TagRepository : JpaRepository<Tag, String> {
    fun findByParentId(parentId: String): List<Tag>
    fun findByParentIdIsNull(): List<Tag>
}
