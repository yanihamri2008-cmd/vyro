package com.sonicteam.vyro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonicteam.vyro.data.MemberRow
import com.sonicteam.vyro.data.MessageRow
import com.sonicteam.vyro.data.Repo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(id: String, title: String, group: Boolean, onBack: () -> Unit) {
    var msgs by remember { mutableStateOf<List<MessageRow>>(emptyList()) }
    var text by remember { mutableStateOf("") }
    var err by remember { mutableStateOf<String?>(null) }
    var members by remember { mutableStateOf<List<MemberRow>?>(null) }
    var showMembers by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val me = Repo.uid

    suspend fun reload() {
        try { msgs = Repo.messages(id); Repo.markRead(id); err = null }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { err = e.message }
    }
    LaunchedEffect(id) { reload() }
    OnNewMessage(id) { scope.launch { reload() } }
    LaunchedEffect(showMembers) {
        if (showMembers) try { members = Repo.members(id) } catch (e: Exception) { err = e.message }
    }

    Column(Modifier.fillMaxSize().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع") }
            Text(title, style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f).then(if (group) Modifier.clickable { showMembers = true } else Modifier))
            if (group) TextButton({ showMembers = true }) { Text("الأعضاء") }
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), reverseLayout = true) {
            items(msgs, key = { it.id }) { m ->
                val mine = m.senderId == me
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 3.dp),
                    horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
                    Column(Modifier.widthIn(max = 280.dp).clip(RoundedCornerShape(16.dp))
                        .background(if (mine) Violet else Color(0xFF1B1030))
                        .padding(horizontal = 12.dp, vertical = 8.dp)) {
                        if (group && !mine) Text(m.profiles?.username ?: "", fontSize = 11.sp, color = Pink)
                        Text(if (m.deleted) "تم حذف الرسالة" else m.body, color = Color.White)
                    }
                }
            }
        }
        err?.let { Text(it, Modifier.padding(horizontal = 16.dp), color = Color(0xFFFF6B8A), fontSize = 12.sp) }
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(text, { text = it }, Modifier.weight(1f), placeholder = { Text("اكتب رسالة") }, maxLines = 4)
            IconButton(enabled = text.isNotBlank(), onClick = {
                val body = text.trim()
                scope.launch {
                    try { Repo.sendMessage(id, body); text = ""; reload() }
                    catch (e: Exception) { err = e.message }
                }
            }) { Icon(Icons.AutoMirrored.Filled.Send, "إرسال", tint = MaterialTheme.colorScheme.primary) }
        }
    }

    if (showMembers) AlertDialog(
        onDismissRequest = { showMembers = false },
        title = { Text("أعضاء القروب") },
        text = {
            Column {
                (members ?: emptyList()).forEach { m ->
                    Text("${m.profiles?.username ?: "?"}${if (m.role != "member") "  (${if (m.role == "owner") "المالك" else "مشرف"})" else ""}",
                        Modifier.padding(vertical = 4.dp))
                }
                if (members == null) CircularProgressIndicator()
            }
        },
        confirmButton = { TextButton({ showMembers = false }) { Text("إغلاق") } },
        dismissButton = {
            TextButton({
                scope.launch {
                    try { Repo.leaveConversation(id); showMembers = false; onBack() }
                    catch (e: Exception) { err = e.message }
                }
            }) { Text("مغادرة القروب", color = Color(0xFFFF6B8A)) }
        }
    )
}
