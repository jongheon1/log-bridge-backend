package io.channel.vocinsight.controller

import io.channel.vocinsight.domain.Tag
import io.channel.vocinsight.service.TagService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/tags")
class TagController(
    private val tagService: TagService
) {

    /**
     * 모든 태그 조회
     * GET /api/tags
     */
    @GetMapping
    fun getTags(): List<Tag> {
        return tagService.getAllTags()
    }

    /**
     * 특정 태그의 하위 태그 재귀 조회
     * GET /api/tags/{id}/children
     */
    @GetMapping("/{id}/children")
    fun getTagChildren(@PathVariable id: String): List<Tag> {
        return tagService.getTagsWithChildren(id)
    }

    /**
     * 채널톡 API로 태그 정보 동기화
     * POST /api/tags/sync
     */
    @PostMapping("/sync")
    fun syncTags(): ResponseEntity<Map<String, String>> {
        return try {
            tagService.syncTags()
            ResponseEntity.ok(mapOf("message" to "Tags synced successfully"))
        } catch (e: Exception) {
            ResponseEntity.internalServerError()
                .body(mapOf("error" to (e.message ?: "Failed to sync tags")))
        }
    }
}
