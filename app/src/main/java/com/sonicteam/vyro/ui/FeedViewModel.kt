package com.sonicteam.vyro.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonicteam.vyro.data.FeedPost
import com.sonicteam.vyro.data.Repo
import kotlinx.coroutines.launch

class FeedViewModel : ViewModel() {
    var posts by mutableStateOf<List<FeedPost>>(emptyList())
    var loading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)

    init { load() }

    fun load() {
        viewModelScope.launch {
            loading = true; error = null
            try { posts = Repo.feed() } catch (e: Exception) { error = e.message ?: "تعذّر تحميل الخلاصة" }
            loading = false
        }
    }

    private fun update(id: String, f: (FeedPost) -> FeedPost) {
        posts = posts.map { if (it.id == id) f(it) else it }
    }

    fun toggleLike(p: FeedPost) {
        val on = !p.liked
        update(p.id) { it.copy(liked = on, likeCount = it.likeCount + if (on) 1 else -1) }
        viewModelScope.launch {
            try { Repo.setLiked(p.id, on) } catch (e: Exception) { update(p.id) { p } }
        }
    }

    fun toggleSave(p: FeedPost) {
        val on = !p.saved
        update(p.id) { it.copy(saved = on) }
        viewModelScope.launch {
            try { Repo.setSaved(p.id, on) } catch (e: Exception) { update(p.id) { p } }
        }
    }

    fun commentAdded(id: String) = update(id) { it.copy(commentCount = it.commentCount + 1) }
}
