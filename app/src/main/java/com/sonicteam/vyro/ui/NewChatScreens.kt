package com.sonicteam.vyro.ui

import com.sonicteam.vyro.data.friendly

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonicteam.vyro.data.MiniUser
import com.sonicteam.vyro.data.Repo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
private fun UserSearch(
    onTap: (MiniUser) -> Unit, selected: Set<String> = emptySet(), modifier: Modifier = Modifier
) {
    var q by remember { mutableStateOf("") }
    var users by remember { mutableStateOf<List<MiniUser>>(emptyList()) }
    var err by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(q) {
        delay(300)
        try { users = Repo.searchProfiles(q); err = null }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { err = e.friendly() }
    }
    Column(modifier) {
        OutlinedTextField(q, { q = it }, Modifier.fillMaxWidth(), placeholder = { Text("ابحث عن مستخدم") }, singleLine = true)
        err?.let { Text(it, color = Color(0xFFFF6B8A), fontSize = 12.sp) }
        LazyColumn {
            items(users, key = { it.id }) { u ->
                Row(Modifier.fillMaxWidth().clickable { onTap(u) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Avatar(u.avatarUrl, u.username, 40)
                    Spacer(Modifier.width(12.dp))
                    Text(u.username, Modifier.weight(1f))
                    if (u.id in selected) Text("✓", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
fun NewChatScreen(onStarted: (id: String, name: String) -> Unit, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var err by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع") }
            Text("محادثة جديدة", style = MaterialTheme.typography.titleLarge)
        }
        err?.let { Text(it, color = Color(0xFFFF6B8A), fontSize = 12.sp) }
        UserSearch(onTap = { u ->
            scope.launch {
                try { onStarted(Repo.startDm(u.id), u.username) } catch (e: Exception) { err = e.friendly() }
            }
        })
    }
}

@Composable
fun NewGroupScreen(onCreated: (id: String, title: String) -> Unit, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf("") }
    var picked by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var busy by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع") }
            Text("قروب جديد", style = MaterialTheme.typography.titleLarge)
        }
        OutlinedTextField(title, { if (it.length <= 60) title = it }, Modifier.fillMaxWidth(), label = { Text("اسم القروب") }, singleLine = true)
        Spacer(Modifier.height(6.dp))
        Text(if (picked.isEmpty()) "اختر الأعضاء من القائمة" else "المختارون: " + picked.values.joinToString("، "),
            color = Color(0xFFA392C7), fontSize = 13.sp)
        UserSearch(
            onTap = { u -> picked = if (u.id in picked) picked - u.id else picked + (u.id to u.username) },
            selected = picked.keys, modifier = Modifier.weight(1f)
        )
        err?.let { Text(it, color = Color(0xFFFF6B8A), fontSize = 12.sp) }
        Button(
            enabled = !busy && title.isNotBlank() && picked.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().height(48.dp),
            onClick = {
                scope.launch {
                    busy = true; err = null
                    try { onCreated(Repo.createGroup(title.trim(), picked.keys.toList()), title.trim()) }
                    catch (e: Exception) { err = e.friendly() }
                    busy = false
                }
            }
        ) { if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Text("إنشاء القروب") }
    }
}
