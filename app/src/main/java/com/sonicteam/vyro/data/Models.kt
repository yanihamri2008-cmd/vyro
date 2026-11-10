package com.sonicteam.vyro.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FeedPost(
    val id: String,
    @SerialName("author_id") val authorId: String,
    val username: String,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val caption: String? = null,
    @SerialName("created_at") val createdAt: String,
    val media: List<String> = emptyList(),
    @SerialName("like_count") val likeCount: Long = 0,
    @SerialName("comment_count") val commentCount: Long = 0,
    val liked: Boolean = false,
    val saved: Boolean = false
)

@Serializable data class NewPost(@SerialName("author_id") val authorId: String, val caption: String)
@Serializable data class IdRow(val id: String)
@Serializable data class NewMedia(@SerialName("post_id") val postId: String, val url: String, val position: Int = 0)
@Serializable data class UserPostRef(@SerialName("user_id") val userId: String, @SerialName("post_id") val postId: String)
@Serializable data class MiniProfile(val username: String, @SerialName("avatar_url") val avatarUrl: String? = null)
@Serializable
data class CommentRow(
    val id: String,
    val body: String,
    @SerialName("author_id") val authorId: String,
    @SerialName("created_at") val createdAt: String,
    val profiles: MiniProfile? = null
)
@Serializable data class NewComment(@SerialName("post_id") val postId: String, @SerialName("author_id") val authorId: String, val body: String)
