package com.sonicteam.vyro.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonicteam.vyro.data.InboxItem
import com.sonicteam.vyro.data.Repo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun InboxScreen(onOpen: (InboxItem) -> Unit, onNew: () -> Unit, onNewGroup: () -> Unit, onBack: (() -> Unit)? = null) {
    var items by remember { mutableStateOf<List<InboxItem>>(emptyList()) }
    var err by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    suspend fun load() {
        try { items = Repo.inbox(); err = null }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { err = e.message }
        loading = false
    }
    LaunchedEffect(Unit) { load() }
    OnNewMessage(Unit) { scope.launch { load() } }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع") }
            Text("الرسائل", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            TextButton(onNew) { Text("محادثة") }
            TextButton(onNewGroup) { Text("قروب") }
        }
        err?.let { Text(it, Modifier.padding(16.dp), color = Color(0xFFFF6B8A), fontSize = 13.sp) }
        if (loading) Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
        else if (items.isEmpty()) Box(Modifier.fillMaxSize().padding(24.dp), Alignment.Center) {
            Text("لا توجد محادثات بعد. ابدأ محادثة أو أنشئ قروباً.")
        } else LazyColumn(Modifier.fillMaxSize()) {
            items(items, key = { it.id }) { c ->
                val name = c.title ?: c.peerUsername ?: "محادثة"
                Row(Modifier.fillMaxWidth().clickable { onOpen(c) }.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Avatar(c.avatarUrl ?: c.peerAvatar, name, 48)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(name, fontWeight = if (c.unread > 0) FontWeight.Bold else FontWeight.Normal)
                        Text(c.lastBody ?: "", color = Color(0xFFA392C7), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    if (c.unread > 0) Badge { Text("${c.unread}") }
                }
            }
        }
    }
}
