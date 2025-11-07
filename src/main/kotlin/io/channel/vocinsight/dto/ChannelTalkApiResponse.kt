package io.channel.vocinsight.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/**
 * 채널톡 Groups API 응답
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class GroupsResponse(
    val groups: List<GroupDto>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GroupDto(
    val id: String,
    val name: String
)

/**
 * 채널톡 Chat Tags API 응답
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class ChatTagsResponse(
    val next: String?,
    val chatTags: List<ChatTagDto>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ChatTagDto(
    val id: String,
    val name: String,
    val key: String,
    val colorVariant: String?,
    val description: String?,
    val createdAt: Long
)

/**
 * 채널톡 User Chats API 응답
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class UserChatsResponse(
    val userChats: List<UserChatDto>,
    val chatTags: List<ChatTagDto>?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class UserChatDto(
    val id: String,
    val tags: List<String>?,  // 태그 ID 배열
    val createdAt: Long,
    val updatedAt: Long
)
