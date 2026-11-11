package com.sonicteam.vyro.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonicteam.vyro.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun CreateReelScreen(onDone: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var uri by remember { mutableStateOf<Uri?>(null) }
    var info by remember { mutableStateOf<VideoInfo?>(null) }
    var caption by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { u ->
        Gate.pickerOpen = false
        if (u != null) {
            uri = u; info = null; err = null
            scope.launch {
                try {
                    val i = withContext(Dispatchers.IO) { videoInfo(ctx, u) }
                    info = i
                    err = when {
                        i.sizeBytes > MAX_REEL_BYTES -> "الفيديو كبير (${i.sizeBytes / 1024 / 1024} ميغابايت). الحد الأقصى ${MAX_REEL_BYTES / 1024 / 1024}."
                        i.durationMs > MAX_REEL_MS -> "الفيديو طويل (${i.durationMs / 1000} ثانية). الحد الأقصى ${MAX_REEL_MS / 1000} ثانية."
                        i.durationMs <= 0L -> "تعذّر قراءة الفيديو"
                        else -> null
                    }
                } catch (e: Exception) { err = e.friendly() }
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("ريل جديد", style = MaterialTheme.typography.titleLarge)
        Text("الحد الأقصى ${MAX_REEL_BYTES / 1024 / 1024} ميغابايت و${MAX_REEL_MS / 1000} ثانية", color = Color(0xFFA392C7), fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))
        OutlinedButton({ Gate.pickerOpen = true; picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)) }) {
            Text(if (uri == null) "اختر فيديو" else "تغيير الفيديو")
        }
        info?.let { Text("المدة: ${it.durationMs / 1000} ثانية • الحجم: ${it.sizeBytes / 1024 / 1024} ميغابايت", Modifier.padding(top = 8.dp), fontSize = 13.sp) }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(caption, { if (it.length <= 2200) caption = it }, Modifier.fillMaxWidth(), label = { Text("اكتب وصفاً") }, minLines = 2)
        err?.let { Spacer(Modifier.height(8.dp)); Text(it, color = Color(0xFFFF6B8A)) }
        Spacer(Modifier.height(16.dp))
        Button(
            enabled = !busy && uri != null && info != null && err == null,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            onClick = {
                val u = uri ?: return@Button
                val i = info ?: return@Button
                scope.launch {
                    busy = true; err = null
                    try {
                        val video = withContext(Dispatchers.IO) { readVideoBytes(ctx, u) }
                        val thumb = withContext(Dispatchers.IO) { videoThumb(ctx, u) }
                        Repo.createReel(video, thumb, caption.trim(), i.durationMs)
                        onDone()
                    } catch (e: Exception) { err = e.friendly() }
                    busy = false
                }
            }
        ) {
            if (busy) { CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp); Spacer(Modifier.width(10.dp)); Text("جارٍ الرفع... لا تغلق الشاشة") }
            else Text("نشر الريل")
        }
    }
}
