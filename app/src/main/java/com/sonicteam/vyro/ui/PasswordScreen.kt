package com.sonicteam.vyro.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonicteam.vyro.R
import com.sonicteam.vyro.data.Security
import kotlinx.coroutines.launch

@Composable
fun PasswordScreen(onOk: () -> Unit, onCancel: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val first = remember { !Security.hasPassword(ctx) }
    var pw by remember { mutableStateOf("") }
    var pw2 by remember { mutableStateOf("") }
    var err by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    BackHandler { onCancel() }

    Column(Modifier.fillMaxSize().padding(28.dp).systemBarsPadding(), Arrangement.Center, Alignment.CenterHorizontally) {
        Image(painterResource(R.drawable.vyro_logo), null, Modifier.size(80.dp).clip(RoundedCornerShape(20.dp)))
        Spacer(Modifier.height(16.dp))
        Text(if (first) "اختر كلمة السر" else "أدخل كلمة السر", style = MaterialTheme.typography.titleLarge)
        if (first) Text("6 أحرف على الأقل. لا توجد طريقة لاستعادتها إن نسيتها.", color = Color(0xFFA392C7), fontSize = 12.sp)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(pw, { pw = it }, Modifier.fillMaxWidth(), label = { Text("كلمة السر") },
            singleLine = true, visualTransformation = PasswordVisualTransformation(), enabled = !busy)
        if (first) {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(pw2, { pw2 = it }, Modifier.fillMaxWidth(), label = { Text("أعد كتابتها") },
                singleLine = true, visualTransformation = PasswordVisualTransformation(), enabled = !busy)
        }
        err?.let { Spacer(Modifier.height(8.dp)); Text(it, color = Color(0xFFFF6B8A), fontSize = 13.sp) }
        Spacer(Modifier.height(16.dp))
        Button(
            enabled = pw.isNotEmpty() && !busy, modifier = Modifier.fillMaxWidth().height(48.dp),
            onClick = {
                scope.launch {
                    busy = true; err = null
                    if (first) {
                        if (pw.length < 6) err = "كلمة السر قصيرة (6 أحرف على الأقل)"
                        else if (pw != pw2) err = "كلمتا السر غير متطابقتين"
                        else { Security.setPassword(ctx, pw); onOk() }
                    } else {
                        val wait = Security.lockedUntil(ctx) - System.currentTimeMillis()
                        if (wait > 0) err = "محاولات كثيرة. حاول بعد ${wait / 1000 + 1} ثانية"
                        else if (Security.verify(ctx, pw)) { Security.resetFails(ctx); onOk() }
                        else { Security.registerFail(ctx); err = "كلمة السر غير صحيحة"; pw = "" }
                    }
                    busy = false
                }
            }
        ) { if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Text(if (first) "حفظ ودخول" else "دخول") }
        TextButton(onCancel, enabled = !busy) { Text("رجوع") }
    }
}

@Composable
fun ChangePasswordDialog(onDismiss: () -> Unit, onDone: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var old by remember { mutableStateOf("") }
    var n1 by remember { mutableStateOf("") }
    var n2 by remember { mutableStateOf("") }
    var err by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("تغيير كلمة السر") },
        text = {
            Column {
                OutlinedTextField(old, { old = it }, label = { Text("الحالية") }, singleLine = true, visualTransformation = PasswordVisualTransformation())
                OutlinedTextField(n1, { n1 = it }, label = { Text("الجديدة") }, singleLine = true, visualTransformation = PasswordVisualTransformation())
                OutlinedTextField(n2, { n2 = it }, label = { Text("أعد الجديدة") }, singleLine = true, visualTransformation = PasswordVisualTransformation())
                err?.let { Text(it, color = Color(0xFFFF6B8A), fontSize = 12.sp) }
            }
        },
        confirmButton = {
            TextButton(enabled = !busy, onClick = {
                scope.launch {
                    busy = true
                    if (!Security.verify(ctx, old)) err = "كلمة السر الحالية غير صحيحة"
                    else if (n1.length < 6) err = "الجديدة قصيرة (6 أحرف على الأقل)"
                    else if (n1 != n2) err = "كلمتا السر غير متطابقتين"
                    else { Security.setPassword(ctx, n1); onDone() }
                    busy = false
                }
            }) { if (busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text("حفظ") }
        },
        dismissButton = { TextButton(onDismiss, enabled = !busy) { Text("إلغاء") } }
    )
}
