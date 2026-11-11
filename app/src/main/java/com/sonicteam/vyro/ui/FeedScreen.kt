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
import com.sonicteam.vyro.data.Repo
import kotlinx.coroutines.launch

private val reportReasons = listOf("محتوى مسيء", "احتيال أو بريد مزعج", "عنف", "انتهاك حقوق", "أخرى")

@Composable
fun FeedScreen(
    vm: FeedViewModel,
    onProfile: (String) -> Unit = {},
    onNotice: (String) -> Unit = {},
    header: @Composable () -> Unit = {}
) {
    var commentsFor by remember { mutableStateOf<FeedPost?>(null) }
    var reportFor by remember { mutableStateOf<FeedPost?>(null) }
    val scope = rememberCoroutineScope()
    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize()) {
            item { header() }
            if (vm.error != null && vm.posts.isEmpty()) item {
                Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(vm.error ?: "", color = Color(0xFFFF6B8A))
                    TextButton({ vm.load() }) { Text("إعادة المحاولة") }
                }
            } else if (vm.posts.isEmpty() && !vm.loading) item {
                Text("لا توجد منشورات بعد. انشر أول منشور أو تابع أشخاصاً.", Modifier.padding(24.dp))
            }
            items(vm.posts, key = { it.id }) { p ->
                PostCard(p, { vm.toggleLike(p) }, { vm.toggleSave(p) }, { commentsFor = p },
                    { onProfile(p.authorId) }, { reportFor = p })
            }
        }
        if (vm.loading) LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter))
    }
    commentsFor?.let { p ->
        CommentsSheet(p, onDismiss = { commentsFor = null }, onAdded = { vm.commentAdded(p.id) })
    }
    reportFor?.let { p ->
        AlertDialog(
            onDismissRequest = { reportFor = null },
            title = { Text("سبب الإبلاغ") },
            text = {
                Column {
                    reportReasons.forEach { r ->
                        TextButton({
                            reportFor = null
                            scope.launch {
                                try { Repo.report("post", p.id, r); onNotice("تم إرسال البلاغ. شكراً لك.") }
                                catch (e: Exception) { onNotice("تعذّر إرسال البلاغ") }
                            }
                        }, Modifier.fillMaxWidth()) { Text(r) }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton({ reportFor = null }) { Text("إلغاء") } }
        )
    }
}
