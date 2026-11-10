package com.sonicteam.vyro.data

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object Repo {
    val uid: String? get() = supabase.auth.currentUserOrNull()?.id
    val email: String? get() = supabase.auth.currentUserOrNull()?.email

    suspend fun feed(): List<FeedPost> =
        supabase.postgrest.rpc("get_feed", buildJsonObject { put("page_size", 30) }).decodeList<FeedPost>()

    suspend fun setLiked(postId: String, on: Boolean) {
        val u = uid ?: return
        if (on) supabase.from("likes").insert(UserPostRef(u, postId))
        else supabase.from("likes").delete { filter { eq("user_id", u); eq("post_id", postId) } }
    }

    suspend fun setSaved(postId: String, on: Boolean) {
        val u = uid ?: return
        if (on) supabase.from("saves").insert(UserPostRef(u, postId))
        else supabase.from("saves").delete { filter { eq("user_id", u); eq("post_id", postId) } }
    }

    suspend fun comments(postId: String): List<CommentRow> =
        supabase.from("comments")
            .select(Columns.raw("id,body,author_id,created_at,profiles(username,avatar_url)")) {
                filter { eq("post_id", postId) }
                order("created_at", Order.ASCENDING)
            }.decodeList<CommentRow>()

    suspend fun addComment(postId: String, body: String) {
        val u = uid ?: return
        supabase.from("comments").insert(NewComment(postId, u, body))
    }

    suspend fun createPost(caption: String, images: List<ByteArray>) {
        val u = uid ?: error("غير مسجّل الدخول")
        val post = supabase.from("posts").insert(NewPost(u, caption)) { select() }.decodeSingle<IdRow>()
        images.forEachIndexed { i, bytes ->
            val path = "$u/${post.id}/$i.jpg"
            supabase.storage.from("posts").upload(path, bytes)
            val url = supabase.storage.from("posts").publicUrl(path)
            supabase.from("post_media").insert(NewMedia(post.id, url, i))
        }
    }

    // ---- profiles, follow, search ----
    suspend fun profile(userId: String): ProfileInfo =
        supabase.postgrest.rpc("get_profile", buildJsonObject { put("p_user", userId) })
            .decodeList<ProfileInfo>().first()

    suspend fun userPosts(userId: String): List<PostCover> =
        supabase.from("posts")
            .select(Columns.raw("id,post_media(url,position)")) {
                filter { eq("author_id", userId) }
                order("created_at", Order.DESCENDING)
            }.decodeList<PostCover>()

    suspend fun searchProfiles(q: String): List<MiniUser> {
        val me = uid ?: return emptyList()
        val term = q.trim()
        return supabase.from("profiles")
            .select(Columns.raw("id,username,display_name,avatar_url")) {
                filter {
                    if (term.isNotEmpty()) ilike("username", "%$term%")
                    neq("id", me)
                }
                order("created_at", Order.DESCENDING)
                limit(30)
            }.decodeList<MiniUser>()
    }

    suspend fun follow(target: String) {
        val me = uid ?: return
        supabase.from("follows").insert(NewFollow(me, target))
    }

    suspend fun unfollow(target: String) {
        val me = uid ?: return
        supabase.from("follows").delete { filter { eq("follower_id", me); eq("followee_id", target) } }
    }

    suspend fun updateProfile(u: ProfileUpdate) {
        val me = uid ?: return
        supabase.from("profiles").update(u) { filter { eq("id", me) } }
    }

    suspend fun uploadAvatar(bytes: ByteArray) {
        val me = uid ?: return
        val path = "$me/avatar-${System.currentTimeMillis()}.jpg"
        supabase.storage.from("avatars").upload(path, bytes)
        val url = supabase.storage.from("avatars").publicUrl(path)
        supabase.from("profiles").update({ set("avatar_url", url) }) { filter { eq("id", me) } }
    }
}
