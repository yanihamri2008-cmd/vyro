package com.sonicteam.vyro.ui

import com.sonicteam.vyro.data.friendly

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

@Composable
fun ExploreScreen(onOpen: (String) -> Unit) {
    var q by remember { mutableStateOf("") }
    var users by remember { mutableStateOf<List<MiniUser>>(emptyList()) }
    var err by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(q) {
        delay(300)
        try { users = Repo.searchProfiles(q); err = null }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { err = e.friendly() }
    }

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        OutlinedTextField(q, { q = it }, Modifier.fillMaxWidth(), placeholder = { Text("ابحث عن مستخدم") }, singleLine = true)
        err?.let { Text(it, color = Color(0xFFFF6B8A), fontSize = 12.sp) }
        Spacer(Modifier.height(8.dp))
        if (users.isEmpty()) Text("لا نتائج.", Modifier.padding(16.dp), color = Color(0xFFA392C7))
        LazyColumn {
            items(users, key = { it.id }) { u ->
                Row(Modifier.fillMaxWidth().clickable { onOpen(u.id) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Avatar(u.avatarUrl, u.username, 44)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(u.username)
                        if (!u.displayName.isNullOrBlank()) Text(u.displayName, fontSize = 13.sp, color = Color(0xFFA392C7))
                    }
                }
            }
        }
    }
}
