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
}
