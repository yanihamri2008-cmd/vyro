package com.sonicteam.vyro.ui

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
import com.sonicteam.vyro.data.AdminReport
import com.sonicteam.vyro.data.AdminUser
import com.sonicteam.vyro.data.Repo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class BanTarget(val id: String, val name: String, val reportId: String?)

@Composable
fun AdminScreen(onBack: () -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    var reports by remember { mutableStateOf<List<AdminReport>>(emptyList()) }
    var users by remember { mutableStateOf<List<AdminUser>>(emptyList()) }
    var q by remember { mutableStateOf("") }
    var err by remember { mutableStateOf<String?>(null) }
    var tick by remember { mutableIntStateOf(0) }
    var ban by remember { mutableStateOf<BanTarget?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(tab, tick) {
        if (tab == 0) try { reports = Repo.adminReports(); err = null }
        catch (e: CancellationException) { throw e } catch (e: Exception) { err = e.message }
    }
    LaunchedEffect(tab, q, tick) {
        if (tab == 1) {
            delay(300)
            try { users = Repo.adminUsers(q); err = null }
            catch (e: CancellationException) { throw e } catch (e: Exception) { err = e.message }
        }
    }
    fun act(block: suspend () -> Unit) {
        scope.launch { try { block(); tick++ } catch (e: Exception) { err = e.message } }
    }

    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
            IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع") }
            Text("لوحة الإدارة", style = MaterialTheme.typography.titleLarge)
        }
        TabRow(selectedTabIndex = tab) {
            Tab(tab == 0, { tab = 0 }, text = { Text("البلاغات") })
            Tab(tab == 1, { tab = 1 }, text = { Text("المستخدمون") })
        }
        err?.let { Text(it, Modifier.padding(12.dp), color = Color(0xFFFF6B8A), fontSize = 12.sp) }
        if (tab == 0) {
            if (reports.isEmpty()) Text("لا بلاغات مفتوحة.", Modifier.padding(24.dp), color = Color(0xFFA392C7))
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(reports, key = { it.id }) { r ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            val kind = when (r.targetType) { "post" -> "منشور"; "comment" -> "تعليق"; else -> "حساب" }
                            Text("$kind • السبب: ${r.reason ?: "-"}", style = MaterialTheme.typography.titleSmall)
                            Text("المبلّغ: @${r.reporter ?: "?"}   صاحب المحتوى: @${r.targetUsername ?: "?"}", fontSize = 12.sp, color = Color(0xFFA392C7))
                            if (!r.preview.isNullOrBlank()) Text(r.preview, Modifier.padding(top = 6.dp), fontSize = 14.sp)
                            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (r.targetType != "user") TextButton({
                                    act { Repo.adminDeleteContent(r.targetType, r.targetId); Repo.adminResolve(r.id, "resolved") }
                                }) { Text("حذف المحتوى") }
                                if (r.targetUser != null) TextButton({ ban = BanTarget(r.targetUser, r.targetUsername ?: "?", r.id) }) { Text("حظر") }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton({ act { Repo.adminResolve(r.id, "dismissed") } }) { Text("تجاهل") }
                                TextButton({ act { Repo.adminResolve(r.id, "resolved") } }) { Text("تم الحل") }
                            }
                        }
                    }
                }
            }
        } else {
            Column(Modifier.fillMaxSize().padding(12.dp)) {
                OutlinedTextField(q, { q = it }, Modifier.fillMaxWidth(), placeholder = { Text("ابحث بالاسم") }, singleLine = true)
                LazyColumn {
                    items(users, key = { it.id }) { u ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("@${u.username}")
                                if (u.banned) Text("محظور", color = Color(0xFFFF6B8A), fontSize = 12.sp)
                            }
                            if (u.banned) OutlinedButton({ act { Repo.adminUnban(u.id) } }) { Text("رفع الحظر") }
                            else Button({ ban = BanTarget(u.id, u.username, null) }) { Text("حظر") }
                        }
                    }
                }
            }
        }
    }

    ban?.let { t ->
        var reason by remember(t) { mutableStateOf("") }
        var days by remember(t) { mutableStateOf<Int?>(7) }
        AlertDialog(
            onDismissRequest = { ban = null },
            title = { Text("حظر @${t.name}") },
            text = {
                Column {
                    OutlinedTextField(reason, { reason = it }, label = { Text("السبب") })
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf<Pair<Int?, String>>(1 to "يوم", 7 to "7 أيام", 30 to "30 يوماً", null to "دائم").forEach { (d, label) ->
                            FilterChip(days == d, { days = d }, label = { Text(label, fontSize = 12.sp) })
                        }
                    }
                }
            },
            confirmButton = {
                TextButton({
                    act {
                        Repo.adminBan(t.id, reason, days)
                        t.reportId?.let { Repo.adminResolve(it, "resolved") }
                        ban = null
                    }
                }) { Text("تأكيد الحظر", color = Color(0xFFFF6B8A)) }
            },
            dismissButton = { TextButton({ ban = null }) { Text("إلغاء") } }
        )
    }
}
