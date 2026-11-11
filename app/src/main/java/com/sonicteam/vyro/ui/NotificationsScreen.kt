package com.sonicteam.vyro.ui

import com.sonicteam.vyro.data.friendly

import androidx.compose.foundation.background
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
import com.sonicteam.vyro.data.NotifRow
import com.sonicteam.vyro.data.Repo

@Composable
fun NotificationsScreen(onBack: () -> Unit) {
    var items by remember { mutableStateOf<List<NotifRow>>(emptyList()) }
    var err by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        try { items = Repo.notifications(); Repo.markNotificationsRead() } catch (e: Exception) { err = e.friendly() }
        loading = false
    }
    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
            IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع") }
            Text("الإشعارات", style = MaterialTheme.typography.titleLarge)
        }
        err?.let { Text(it, Modifier.padding(16.dp), color = Color(0xFFFF6B8A), fontSize = 13.sp) }
        if (loading) Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
        else if (items.isEmpty()) Box(Modifier.fillMaxSize(), Alignment.Center) { Text("لا إشعارات بعد.") }
        else LazyColumn(Modifier.fillMaxSize()) {
            items(items, key = { it.id }) { n ->
                val who = n.actor?.username ?: "؟"
                val what = when (n.type) {
                    "like" -> "أعجب بمنشورك"
                    "comment" -> "علّق على منشورك"
                    "follow" -> "بدأ بمتابعتك"
                    "follow_request" -> "طلب متابعتك"
                    else -> "نشاط جديد"
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Avatar(n.actor?.avatarUrl, who, 44)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("$who $what")
                        Text(n.createdAt.take(10), fontSize = 11.sp, color = Color(0xFFA392C7))
                    }
                    if (!n.read) Box(Modifier.size(8.dp).background(MaterialTheme.colorScheme.primary, androidx.compose.foundation.shape.CircleShape))
                }
            }
        }
    }
}
