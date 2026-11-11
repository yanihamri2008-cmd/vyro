package com.sonicteam.vyro.ui

import com.sonicteam.vyro.data.friendly

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import com.sonicteam.vyro.data.Gate
import com.sonicteam.vyro.data.ProfileInfo
import com.sonicteam.vyro.data.ProfileUpdate
import com.sonicteam.vyro.data.Repo
import com.sonicteam.vyro.data.compressImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val usernameRule = Regex("^[a-z0-9._]{3,30}$")

@Composable
fun EditProfileScreen(onDone: () -> Unit, onCancel: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var info by remember { mutableStateOf<ProfileInfo?>(null) }
    var name by remember { mutableStateOf("") }
    var user by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var priv by remember { mutableStateOf(false) }
    var newAvatar by remember { mutableStateOf<Uri?>(null) }
    var busy by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { Gate.pickerOpen = false; if (it != null) newAvatar = it }

    LaunchedEffect(Unit) {
        try {
            val p = Repo.profile(Repo.uid ?: return@LaunchedEffect)
            info = p; name = p.displayName ?: ""; user = p.username; bio = p.bio ?: ""; priv = p.isPrivate
        } catch (e: Exception) { err = e.friendly() }
    }
    val p = info
    if (p == null) { Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }; return }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("تعديل الملف الشخصي", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            val pick = { Gate.pickerOpen = true; picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
            if (newAvatar != null) AsyncImage(newAvatar, null, Modifier.size(96.dp).clip(CircleShape).clickable { pick() }, contentScale = ContentScale.Crop)
            else Box(Modifier.clickable { pick() }) { Avatar(p.avatarUrl, p.username, 96) }
            TextButton(pick) { Text("تغيير الصورة") }
        }
        OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("الاسم") }, singleLine = true)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(user, { user = it.lowercase() }, Modifier.fillMaxWidth(), label = { Text("اسم المستخدم") }, singleLine = true)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(bio, { if (it.length <= 150) bio = it }, Modifier.fillMaxWidth(), label = { Text("النبذة (150 حرفاً)") }, minLines = 3)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("حساب خاص")
                Text("لا يرى منشوراتك إلا من توافق على متابعته", color = Color(0xFFA392C7), style = MaterialTheme.typography.bodySmall)
            }
            Switch(priv, { priv = it })
        }
        err?.let { Spacer(Modifier.height(8.dp)); Text(it, color = Color(0xFFFF6B8A)) }
        Spacer(Modifier.height(16.dp))
        Button(
            enabled = !busy, modifier = Modifier.fillMaxWidth().height(48.dp),
            onClick = {
                if (!usernameRule.matches(user)) { err = "اسم المستخدم: 3 إلى 30 حرفاً إنجليزياً صغيراً أو أرقاماً أو . _"; return@Button }
                scope.launch {
                    busy = true; err = null
                    try {
                        newAvatar?.let { u -> Repo.uploadAvatar(withContext(Dispatchers.IO) { compressImage(ctx, u) }) }
                        Repo.updateProfile(ProfileUpdate(name.trim(), bio.trim(), user, priv))
                        onDone()
                    } catch (e: Exception) {
                        val m = e.friendly() ?: ""
                        err = if (m.contains("duplicate", true) || m.contains("unique", true)) "اسم المستخدم مستخدم، اختر غيره" else m.ifBlank { "فشل الحفظ" }
                    }
                    busy = false
                }
            }
        ) { if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Text("حفظ") }
        TextButton(onCancel, Modifier.fillMaxWidth()) { Text("إلغاء") }
    }
}
