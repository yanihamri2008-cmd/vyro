package com.sonicteam.vyro.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sonicteam.vyro.data.FeedPost

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(vm: FeedViewModel) {
    var commentsFor by remember { mutableStateOf<FeedPost?>(null) }
    PullToRefreshBox(isRefreshing = vm.loading, onRefresh = { vm.load() }, modifier = Modifier.fillMaxSize()) {
        when {
            vm.error != null && vm.posts.isEmpty() -> Column(Modifier.fillMaxSize().padding(24.dp),
                Arrangement.Center, Alignment.CenterHorizontally) {
                Text(vm.error ?: "", color = Color(0xFFFF6B8A))
                TextButton({ vm.load() }) { Text("إعادة المحاولة") }
            }
            vm.posts.isEmpty() && !vm.loading -> Box(Modifier.fillMaxSize().padding(24.dp), Alignment.Center) {
                Text("لا توجد منشورات بعد. انشر أول منشور أو تابع أشخاصاً.")
            }
            else -> LazyColumn(Modifier.fillMaxSize()) {
                items(vm.posts, key = { it.id }) { p ->
                    PostCard(p, { vm.toggleLike(p) }, { vm.toggleSave(p) }, { commentsFor = p })
                }
            }
        }
    }
    commentsFor?.let { p ->
        CommentsSheet(p, onDismiss = { commentsFor = null }, onAdded = { vm.commentAdded(p.id) })
    }
}
