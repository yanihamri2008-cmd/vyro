package com.sonicteam.vyro.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.sonicteam.vyro.data.AppPrefs
import com.sonicteam.vyro.data.Gate
import com.sonicteam.vyro.data.Notifier

@Composable
private fun SwitchRow(title: String, sub: String?, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title)
            if (sub != null) Text(sub, fontSize = 12.sp, color = Color(0xFFA392C7))
        }
        Switch(checked, onChange, enabled = enabled)
    }
}

@Composable
private fun SectionTitle(t: String) {
    Text(t, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 20.dp, bottom = 4.dp))
}

@Composable
fun SettingsScreen(onBack: () -> Unit, onSignOut: () -> Unit) {
    val ctx = LocalContext.current
    var delaySec by remember { mutableIntStateOf(AppPrefs.lockDelaySec(ctx)) }
    var shots by remember { mutableStateOf(AppPrefs.blockShots(ctx)) }
    var notif by remember { mutableStateOf(AppPrefs.notifEnabled(ctx)) }
    var nMsg by remember { mutableStateOf(AppPrefs.notifMessages(ctx)) }
    var nAct by remember { mutableStateOf(AppPrefs.notifActivity(ctx)) }
    var hide by remember { mutableStateOf(AppPrefs.hideContent(ctx)) }
    var pwDialog by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf<String?>(null) }

    val perm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) { notif = true; AppPrefs.setNotifEnabled(ctx, true); Notifier.apply(ctx); note = null }
        else note = "لم تُمنح صلاحية الإشعارات من النظام. فعّلها من إعدادات الهاتف."
    }

    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
            IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع") }
            Text("الإعدادات", style = MaterialTheme.typography.titleLarge)
        }
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
            SectionTitle("الأمان والقفل")
            Text("متى يُقفل التطبيق ويعود آلة حاسبة بعد الخروج منه؟", fontSize = 13.sp, color = Color(0xFFA392C7))
            listOf(0 to "فوراً عند الخروج (الأكثر أماناً)", 30 to "بعد 30 ثانية", 60 to "بعد دقيقة", 300 to "بعد 5 دقائق").forEach { (v, label) ->
                Row(Modifier.fillMaxWidth().clickable { delaySec = v; AppPrefs.setLockDelaySec(ctx, v) }.padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(delaySec == v, { delaySec = v; AppPrefs.setLockDelaySec(ctx, v) })
                    Text(label)
                }
            }
            SwitchRow("منع لقطات الشاشة", "ويُخفي معاينة التطبيق في قائمة التطبيقات الأخيرة", shots) {
                shots = it; AppPrefs.setBlockShots(ctx, it); Gate.blockShots = it
            }
            OutlinedButton({ pwDialog = true }, Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("تغيير كلمة السر") }

            SectionTitle("الإشعارات")
            SwitchRow("تفعيل الإشعارات", "تُفحص التحديثات كل 15 دقيقة تقريباً، فهي ليست فورية", notif) { on ->
                if (!on) { notif = false; AppPrefs.setNotifEnabled(ctx, false); Notifier.apply(ctx) }
                else if (Build.VERSION.SDK_INT >= 33 &&
                    ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                ) perm.launch(Manifest.permission.POST_NOTIFICATIONS)
                else { notif = true; AppPrefs.setNotifEnabled(ctx, true); Notifier.apply(ctx) }
            }
            SwitchRow("الرسائل", null, nMsg, enabled = notif) { nMsg = it; AppPrefs.setNotifMessages(ctx, it) }
            SwitchRow("الإعجابات والتعليقات والمتابعات", null, nAct, enabled = notif) { nAct = it; AppPrefs.setNotifActivity(ctx, it) }
            SwitchRow("إخفاء محتوى الإشعار", "يظهر نص عام (الآلة الحاسبة: تحديث جديد) بدل اسم التطبيق", hide, enabled = notif) {
                hide = it; AppPrefs.setHideContent(ctx, it)
            }
            note?.let { Text(it, color = Color(0xFFFF6B8A), fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp)) }

            SectionTitle("الحساب")
            OutlinedButton(onSignOut, Modifier.fillMaxWidth()) { Text("تسجيل الخروج") }
        }
    }
    if (pwDialog) ChangePasswordDialog(onDismiss = { pwDialog = false }, onDone = { pwDialog = false })
}
