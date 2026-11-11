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

@Serializable
data class InboxItem(
    val id: String, val kind: String, val title: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("last_body") val lastBody: String? = null,
    @SerialName("last_at") val lastAt: String? = null,
    val unread: Long = 0,
    @SerialName("peer_id") val peerId: String? = null,
    @SerialName("peer_username") val peerUsername: String? = null,
    @SerialName("peer_avatar") val peerAvatar: String? = null
)
@Serializable
data class MessageRow(
    val id: String,
    @SerialName("conversation_id") val conversationId: String,
    @SerialName("sender_id") val senderId: String,
    val body: String,
    @SerialName("created_at") val createdAt: String,
    val deleted: Boolean = false,
    val profiles: MiniProfile? = null
)
@Serializable data class NewMessage(@SerialName("conversation_id") val conversationId: String, @SerialName("sender_id") val senderId: String, val body: String)
@Serializable data class MemberRow(@SerialName("user_id") val userId: String, val role: String, val profiles: MiniProfile? = null)
@Serializable data class NewReport(@SerialName("reporter_id") val reporterId: String, @SerialName("target_type") val targetType: String, @SerialName("target_id") val targetId: String, val reason: String)
@Serializable data class BanInfo(val reason: String? = null, @SerialName("expires_at") val expiresAt: String? = null)
@Serializable
data class AdminReport(
    val id: String, val reporter: String? = null,
    @SerialName("target_type") val targetType: String,
    @SerialName("target_id") val targetId: String,
    val preview: String? = null,
    @SerialName("target_user") val targetUser: String? = null,
    @SerialName("target_username") val targetUsername: String? = null,
    val reason: String? = null,
    @SerialName("created_at") val createdAt: String
)
@Serializable data class AdminUser(val id: String, val username: String, @SerialName("display_name") val displayName: String? = null, val banned: Boolean = false)

@Serializable
data class NotifRow(
    val id: String, val type: String,
    @SerialName("post_id") val postId: String? = null,
    val read: Boolean = false,
    @SerialName("created_at") val createdAt: String,
    val actor: MiniProfile? = null
)
