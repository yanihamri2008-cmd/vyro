package com.sonicteam.vyro.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sonicteam.vyro.data.BanInfo

@Composable
fun BannedScreen(ban: BanInfo, onSignOut: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(32.dp), Arrangement.Center, Alignment.CenterHorizontally) {
        Text("تم تقييد حسابك", style = MaterialTheme.typography.headlineSmall, color = Color(0xFFFF6B8A))
        Spacer(Modifier.height(12.dp))
        if (!ban.reason.isNullOrBlank()) Text("السبب: ${ban.reason}")
        Text(if (ban.expiresAt == null) "الحظر دائم." else "ينتهي الحظر في: ${ban.expiresAt.take(10)}", Modifier.padding(top = 6.dp))
        Spacer(Modifier.height(20.dp))
        OutlinedButton(onSignOut) { Text("تسجيل الخروج") }
    }
}
