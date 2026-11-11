package com.sonicteam.vyro.data

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
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

    // ---- messaging ----
    suspend fun inbox(): List<InboxItem> = supabase.postgrest.rpc("get_inbox").decodeList<InboxItem>()

    suspend fun startDm(other: String): String =
        supabase.postgrest.rpc("start_dm", buildJsonObject { put("other", other) }).decodeAs<String>()

    suspend fun createGroup(title: String, members: List<String>): String =
        supabase.postgrest.rpc("create_group", buildJsonObject {
            put("p_title", title)
            put("p_members", JsonArray(members.map { JsonPrimitive(it) }))
        }).decodeAs<String>()

    suspend fun messages(cid: String): List<MessageRow> =
        supabase.from("messages")
            .select(Columns.raw("id,conversation_id,sender_id,body,created_at,deleted,profiles(username,avatar_url)")) {
                filter { eq("conversation_id", cid) }
                order("created_at", Order.DESCENDING)
                limit(60)
            }.decodeList<MessageRow>()

    suspend fun sendMessage(cid: String, body: String) {
        val u = uid ?: return
        supabase.from("messages").insert(NewMessage(cid, u, body))
    }

    suspend fun markRead(cid: String) {
        supabase.postgrest.rpc("mark_read", buildJsonObject { put("p_cid", cid) })
    }

    suspend fun members(cid: String): List<MemberRow> =
        supabase.from("conversation_members")
            .select(Columns.raw("user_id,role,profiles(username,avatar_url)")) {
                filter { eq("conversation_id", cid) }
            }.decodeList<MemberRow>()

    suspend fun leaveConversation(cid: String) {
        val u = uid ?: return
        supabase.from("conversation_members").delete { filter { eq("conversation_id", cid); eq("user_id", u) } }
    }

    // ---- reports, bans, admin ----
    suspend fun report(type: String, targetId: String, reason: String) {
        val u = uid ?: return
        supabase.from("reports").insert(NewReport(u, type, targetId, reason))
    }

    suspend fun amIAdmin(): Boolean = supabase.postgrest.rpc("am_i_admin").decodeAs<Boolean>()

    suspend fun myBan(): BanInfo? = supabase.postgrest.rpc("my_ban").decodeList<BanInfo>().firstOrNull()

    suspend fun adminReports(): List<AdminReport> = supabase.postgrest.rpc("admin_reports").decodeList<AdminReport>()

    suspend fun adminUsers(q: String): List<AdminUser> =
        supabase.postgrest.rpc("admin_users", buildJsonObject { put("q", q) }).decodeList<AdminUser>()

    suspend fun adminBan(user: String, reason: String, days: Int?) {
        supabase.postgrest.rpc("admin_ban", buildJsonObject {
            put("p_user", user); put("p_reason", reason)
            if (days != null) put("p_days", days)
        })
    }

    suspend fun adminUnban(user: String) {
        supabase.postgrest.rpc("admin_unban", buildJsonObject { put("p_user", user) })
    }

    suspend fun adminDeleteContent(type: String, id: String) {
        if (type == "post") supabase.postgrest.rpc("admin_delete_post", buildJsonObject { put("p_post", id) })
        else if (type == "comment") supabase.postgrest.rpc("admin_delete_comment", buildJsonObject { put("p_comment", id) })
    }

    suspend fun adminResolve(report: String, status: String) {
        supabase.postgrest.rpc("admin_resolve_report", buildJsonObject { put("p_report", report); put("p_status", status) })
    }

    // ---- notifications ----
    suspend fun notifications(): List<NotifRow> {
        val me = uid ?: return emptyList()
        return supabase.from("notifications")
            .select(Columns.raw("id,type,post_id,read,created_at,actor:profiles!actor_id(username,avatar_url)")) {
                filter { eq("user_id", me) }
                order("created_at", Order.DESCENDING)
                limit(50)
            }.decodeList<NotifRow>()
    }

    suspend fun markNotificationsRead() {
        val me = uid ?: return
        supabase.from("notifications").update({ set("read", true) }) { filter { eq("user_id", me); eq("read", false) } }
    }
}
