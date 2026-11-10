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

@Serializable
data class ProfileInfo(
    val id: String,
    val username: String,
    @SerialName("display_name") val displayName: String? = null,
    val bio: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("is_private") val isPrivate: Boolean = false,
    @SerialName("posts_count") val postsCount: Long = 0,
    @SerialName("followers_count") val followersCount: Long = 0,
    @SerialName("following_count") val followingCount: Long = 0,
    @SerialName("follow_status") val followStatus: String? = null
)
@Serializable data class MiniUser(val id: String, val username: String, @SerialName("display_name") val displayName: String? = null, @SerialName("avatar_url") val avatarUrl: String? = null)
@Serializable data class MediaUrl(val url: String, val position: Int = 0)
@Serializable data class PostCover(val id: String, @SerialName("post_media") val media: List<MediaUrl> = emptyList())
@Serializable data class NewFollow(@SerialName("follower_id") val followerId: String, @SerialName("followee_id") val followeeId: String)
@Serializable
data class ProfileUpdate(
    @SerialName("display_name") val displayName: String,
    val bio: String,
    val username: String,
    @SerialName("is_private") val isPrivate: Boolean
)
