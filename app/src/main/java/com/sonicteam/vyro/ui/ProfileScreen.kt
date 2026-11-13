package com.sonicteam.vyro.ui

import com.sonicteam.vyro.data.friendly

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.sonicteam.vyro.data.PostCover
import com.sonicteam.vyro.data.ProfileInfo
import com.sonicteam.vyro.data.Repo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    userId: String,
    refreshKey: Int = 0,
    onBack: (() -> Unit)? = null,
    onEdit: () -> Unit = {},
    onSignOut: (() -> Unit)? = null,
    onMessage: ((String, String) -> Unit)? = null,
    onAdmin: (() -> Unit)? = null,
    onSecurity: (() -> Unit)? = null
) {
    var info by remember { mutableStateOf<ProfileInfo?>(null) }
    var posts by remember { mutableStateOf<List<PostCover>>(emptyList()) }
    var err by remember { mutableStateOf<String?>(null) }
    var tick by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val isMe = userId == Repo.uid

    LaunchedEffect(userId, refreshKey, tick) {
        try { info = Repo.profile(userId); posts = Repo.userPosts(userId); err = null }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { err = e.friendly() ?: "تعذّر تحميل الملف" }
    }

    Column(Modifier.fillMaxSize()) {
        if (onBack != null) IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع") }
        val p = info
        if (p == null) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                if (err != null) Text(err ?: "", color = Color(0xFFFF6B8A)) else CircularProgressIndicator()
            }
            return@Column
        }
        val canSee = isMe || !p.isPrivate || p.followStatus == "accepted"
        LazyVerticalGrid(
            GridCells.Fixed(3), Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(2.dp), verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Avatar(p.avatarUrl, p.username, 84)
                        Spacer(Modifier.width(20.dp))
                        Stat(p.postsCount, "منشورات"); Stat(p.followersCount, "متابعون"); Stat(p.followingCount, "يتابع")
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(p.displayName?.takeIf { it.isNotBlank() } ?: p.username, fontWeight = FontWeight.Bold)
                    Text("@${p.username}", color = Color(0xFFA392C7), fontSize = 13.sp)
                    if (!p.bio.isNullOrBlank()) Text(p.bio, Modifier.padding(top = 4.dp), fontSize = 14.sp)
                    Spacer(Modifier.height(12.dp))
                    if (isMe) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onEdit, Modifier.weight(1f)) { Text("تعديل الملف") }
                            if (onSignOut != null) OutlinedButton(onSignOut, Modifier.weight(1f)) { Text("تسجيل الخروج") }
                        }
                        if (onSecurity != null) {
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(onSecurity, Modifier.fillMaxWidth()) { Text("الإعدادات") }
                        }
                        if (onAdmin != null) {
                            Spacer(Modifier.height(8.dp))
                            Button(onAdmin, Modifier.fillMaxWidth()) { Text("لوحة الإدارة") }
                        }
                    } else {
                        val label = when (p.followStatus) { "accepted" -> "إلغاء المتابعة"; "pending" -> "تم إرسال الطلب"; else -> "متابعة" }
                        val filled = p.followStatus == null
                        val act: () -> Unit = {
                            scope.launch {
                                try { if (p.followStatus == null) Repo.follow(p.id) else Repo.unfollow(p.id); tick++ }
                                catch (e: Exception) { err = e.friendly() }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (filled) Button(act, Modifier.weight(1f)) { Text(label) }
                            else OutlinedButton(act, Modifier.weight(1f)) { Text(label) }
                            if (onMessage != null) OutlinedButton({ onMessage(p.id, p.username) }, Modifier.weight(1f)) { Text("رسالة") }
                        }
                    }
                    err?.let { Text(it, color = Color(0xFFFF6B8A), fontSize = 12.sp) }
                }
            }
            if (!canSee) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text("هذا الحساب خاص. تابعه لرؤية منشوراته.", Modifier.padding(24.dp), color = Color(0xFFA392C7))
                }
            } else {
                items(posts, key = { it.id }) { c ->
                    AsyncImage(c.media.minByOrNull { it.position }?.url, null,
                        Modifier.fillMaxWidth().aspectRatio(1f), contentScale = ContentScale.Crop)
                }
            }
        }
    }
}

@Composable
private fun RowScope.Stat(n: Long, label: String) {
    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$n", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(label, fontSize = 12.sp, color = Color(0xFFA392C7))
    }
}
