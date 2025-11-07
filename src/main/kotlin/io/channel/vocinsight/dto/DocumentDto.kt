package io.channel.vocinsight.dto

data class CreateArticleRequest(
    val authorId: String,
    val bodyHtml: String,
    val language: String = "ko",
    val name: String,
    val subtitle: String,
    val title: String
)

data class CreateArticleResponse(
    val article: Article,
    val revision: Revision
)

data class Article(
    val id: String,
    val spaceId: String,
    val currentRevisionId: String
)

data class Revision(
    val id: String
)

data class SendMessageRequest(
    val plainText: String
)
