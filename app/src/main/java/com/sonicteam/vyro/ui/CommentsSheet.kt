package com.sonicteam.vyro.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonicteam.vyro.data.CommentRow
import com.sonicteam.vyro.data.FeedPost
import com.sonicteam.vyro.data.Repo
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsSheet(post: FeedPost, onDismiss: () -> Unit, onAdded: () -> Unit) {
    var list by remember { mutableStateOf<List<CommentRow>>(emptyList()) }
    var text by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(true) }
    var err by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun reload() {
        try { list = Repo.comments(post.id); err = null } catch (e: Exception) { err = e.message }
        busy = false
    }
    LaunchedEffect(post.id) { reload() }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().heightIn(min = 280.dp, max = 560.dp).padding(horizontal = 16.dp).imePadding()) {
            Text("التعليقات", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Box(Modifier.weight(1f, fill = true)) {
                if (busy) CircularProgressIndicator(Modifier.align(Alignment.Center))
                else if (list.isEmpty()) Text("لا توجد تعليقات بعد.", Modifier.align(Alignment.Center))
                else LazyColumn {
                    items(list, key = { it.id }) { c ->
                        Row(Modifier.padding(vertical = 6.dp)) {
                            Avatar(c.profiles?.avatarUrl, c.profiles?.username ?: "؟", 30)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(c.profiles?.username ?: "", fontSize = 13.sp, color = Color(0xFFA392C7))
                                Text(c.body, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
            err?.let { Text(it, color = Color(0xFFFF6B8A), fontSize = 12.sp) }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                OutlinedTextField(text, { text = it }, Modifier.weight(1f), placeholder = { Text("أضف تعليقاً") }, maxLines = 3)
                IconButton(enabled = text.isNotBlank(), onClick = {
                    val body = text.trim()
                    scope.launch {
                        try { Repo.addComment(post.id, body); text = ""; onAdded(); reload() }
                        catch (e: Exception) { err = e.message }
                    }
                }) { Icon(Icons.AutoMirrored.Filled.Send, "إرسال", tint = MaterialTheme.colorScheme.primary) }
            }
        }
    }
}
