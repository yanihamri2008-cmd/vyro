package com.sonicteam.vyro.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.sonicteam.vyro.data.Gate
import com.sonicteam.vyro.data.Repo
import com.sonicteam.vyro.data.compressImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun CreatePostScreen(onDone: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var images by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var caption by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(10)) { Gate.pickerOpen = false; images = it }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("منشور جديد", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        OutlinedButton({ Gate.pickerOpen = true; picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
            Text(if (images.isEmpty()) "اختر صوراً (حتى 10)" else "تغيير الصور (${images.size})")
        }
        if (images.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(images) { AsyncImage(it, null, Modifier.size(96.dp), contentScale = ContentScale.Crop) }
            }
        }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(caption, { caption = it }, Modifier.fillMaxWidth(), label = { Text("اكتب وصفاً") }, minLines = 3)
        err?.let { Spacer(Modifier.height(8.dp)); Text(it, color = Color(0xFFFF6B8A)) }
        Spacer(Modifier.height(16.dp))
        Button(
            enabled = !busy && images.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().height(48.dp),
            onClick = {
                scope.launch {
                    busy = true; err = null
                    try {
                        val bytes = withContext(Dispatchers.IO) { images.map { compressImage(ctx, it) } }
                        Repo.createPost(caption.trim(), bytes)
                        images = emptyList(); caption = ""
                        onDone()
                    } catch (e: Exception) { err = e.message ?: "فشل النشر" }
                    busy = false
                }
            }
        ) { if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Text("نشر") }
    }
}
