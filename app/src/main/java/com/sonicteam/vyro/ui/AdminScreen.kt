package com.sonicteam.vyro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonicteam.vyro.data.AdminDaily
import com.sonicteam.vyro.data.AdminReport
import com.sonicteam.vyro.data.AdminStats
import com.sonicteam.vyro.data.AdminUser
import com.sonicteam.vyro.data.AuditRow
import com.sonicteam.vyro.data.OnlineUser
import com.sonicteam.vyro.data.Repo
import com.sonicteam.vyro.data.friendly
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class BanTarget(val id: String, val name: String, val reportId: String?)
private val muted = Color(0xFFA392C7)
private val green = Color(0xFF3DDC84)

@Composable
private fun StatCard(label: String, value: Long, modifier: Modifier = Modifier, accent: Color = Color.Unspecified) {
    Card(modifier) {
        Column(Modifier.padding(14.dp)) {
            Text("$value", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = accent)
            Text(label, fontSize = 12.sp, color = muted)
        }
    }
}

@Composable
private fun MiniBars(title: String, values: List<Long>, labels: List<String>, color: Color) {
    val max = (values.maxOrNull() ?: 0L).coerceAtLeast(1L)
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth().height(100.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom) {
            values.forEachIndexed { i, v ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                    Text("$v", fontSize = 10.sp)
                    Box(Modifier.fillMaxWidth().height(maxOf(2.dp, 60.dp * (v.toFloat() / max))).clip(RoundedCornerShape(4.dp)).background(color))
                    Text(labels[i], fontSize = 9.sp, color = muted)
                }
            }
        }
    }
}

private fun actionAr(a: String) = when (a) {
    "ban" -> "حظر"; "unban" -> "رفع حظر"; "delete_post" -> "حذف منشور"; "delete_comment" -> "حذف تعليق"; "delete_reel" -> "حذف ريل"; else -> a
}

@Composable
fun AdminScreen(onBack: () -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    var stats by remember { mutableStateOf<AdminStats?>(null) }
    var daily by remember { mutableStateOf<List<AdminDaily>>(emptyList()) }
    var online by remember { mutableStateOf<List<OnlineUser>>(emptyList()) }
    var reports by remember { mutableStateOf<List<AdminReport>>(emptyList()) }
    var users by remember { mutableStateOf<List<AdminUser>>(emptyList()) }
    var audit by remember { mutableStateOf<List<AuditRow>>(emptyList()) }
    var q by remember { mutableStateOf("") }
    var err by remember { mutableStateOf<String?>(null) }
    var tick by remember { mutableIntStateOf(0) }
    var ban by remember { mutableStateOf<BanTarget?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(tab, tick) {
        when (tab) {
            0 -> while (true) {
                try { stats = Repo.adminStats(); daily = Repo.adminDaily(); online = Repo.adminOnline(); err = null }
                catch (e: CancellationException) { throw e } catch (e: Exception) { err = e.friendly() }
                delay(30_000)
            }
            1 -> try { reports = Repo.adminReports(); err = null }
                catch (e: CancellationException) { throw e } catch (e: Exception) { err = e.friendly() }
            3 -> try { audit = Repo.adminAudit(); err = null }
                catch (e: CancellationException) { throw e } catch (e: Exception) { err = e.friendly() }
            else -> {}
        }
    }
    LaunchedEffect(tab, q, tick) {
        if (tab == 2) {
            delay(300)
            try { users = Repo.adminUsers(q); err = null }
            catch (e: CancellationException) { throw e } catch (e: Exception) { err = e.friendly() }
        }
    }
    fun act(block: suspend () -> Unit) {
        scope.launch { try { block(); tick++ } catch (e: Exception) { err = e.friendly() } }
    }

    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
            IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع") }
            Text("لوحة الإدارة", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            TextButton({ tick++ }) { Text("تحديث") }
        }
        ScrollableTabRow(selectedTabIndex = tab, edgePadding = 8.dp) {
            listOf("الإحصائيات", "البلاغات", "المستخدمون", "السجل").forEachIndexed { i, t ->
                Tab(tab == i, { tab = i }, text = { Text(t) })
            }
        }
        err?.let { Text(it, Modifier.padding(12.dp), color = Color(0xFFFF6B8A), fontSize = 12.sp) }

        when (tab) {
            0 -> {
                val s = stats
                if (s == null) Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                else LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item {
                        Card(Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(12.dp).clip(CircleShape).background(green))
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("المتصلون الآن", fontSize = 13.sp, color = muted)
                                    Text("${s.onlineNow}", fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = green)
                                }
                                Text("يتحدث كل 30 ثانية", fontSize = 11.sp, color = muted)
                            }
                        }
                    }
                    val cards = listOf(
                        "المستخدمون" to s.usersTotal, "نشطون خلال 24 ساعة" to s.active24h,
                        "جدد اليوم" to s.new24h, "جدد هذا الأسبوع" to s.new7d,
                        "المنشورات" to s.postsTotal, "منشورات اليوم" to s.posts24h,
                        "التعليقات" to s.commentsTotal, "رسائل اليوم" to s.messages24h,
                        "القروبات" to s.groupsTotal, "حسابات خاصة" to s.privateAccounts,
                        "محظورون" to s.bannedNow, "بلاغات مفتوحة" to s.openReports
                    )
                    items(cards.chunked(2)) { pair ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            pair.forEach { (label, v) ->
                                val warn = (label == "بلاغات مفتوحة" || label == "محظورون") && v > 0
                                StatCard(label, v, Modifier.weight(1f), if (warn) Color(0xFFFF6B8A) else Color.Unspecified)
                            }
                        }
                    }
                    item {
                        val labels = daily.map { it.day.takeLast(5) }
                        MiniBars("تسجيلات جديدة (آخر 7 أيام)", daily.map { it.signups }, labels, Color(0xFF5B12FF))
                        MiniBars("منشورات (آخر 7 أيام)", daily.map { it.posts }, labels, Color(0xFFFF2FD0))
                        MiniBars("مستخدمون نشطون (آخر 7 أيام)", daily.map { it.active }, labels, green)
                    }
                    item { Text("المتصلون الآن (${online.size})", style = MaterialTheme.typography.titleSmall) }
                    if (online.isEmpty()) item { Text("لا أحد متصل حالياً.", color = muted, fontSize = 13.sp) }
                    items(online, key = { it.id }) { u ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(8.dp).clip(CircleShape).background(green))
                            Spacer(Modifier.width(8.dp))
                            Text("@${u.username}", Modifier.weight(1f))
                            Text(agoText(u.lastSeen), fontSize = 12.sp, color = muted)
                        }
                    }
                }
            }
            1 -> {
                if (reports.isEmpty()) Text("لا بلاغات مفتوحة.", Modifier.padding(24.dp), color = muted)
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(reports, key = { it.id }) { r ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp)) {
                                val kind = when (r.targetType) { "post" -> "منشور"; "comment" -> "تعليق"; "reel" -> "ريل"; else -> "حساب" }
                                Text("$kind • السبب: ${r.reason ?: "-"}", style = MaterialTheme.typography.titleSmall)
                                Text("المبلّغ: @${r.reporter ?: "?"}   صاحب المحتوى: @${r.targetUsername ?: "?"}", fontSize = 12.sp, color = muted)
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
            }
            2 -> Column(Modifier.fillMaxSize().padding(12.dp)) {
                OutlinedTextField(q, { q = it }, Modifier.fillMaxWidth(), placeholder = { Text("ابحث بالاسم") }, singleLine = true)
                LazyColumn {
                    items(users, key = { it.id }) { u ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("@${u.username}")
                                Text("${agoText(u.lastSeen)} • انضم ${u.createdAt?.take(10) ?: "?"}", fontSize = 11.sp, color = muted)
                                if (u.banned) Text("محظور", color = Color(0xFFFF6B8A), fontSize = 12.sp)
                            }
                            if (u.banned) OutlinedButton({ act { Repo.adminUnban(u.id) } }) { Text("رفع الحظر") }
                            else Button({ ban = BanTarget(u.id, u.username, null) }) { Text("حظر") }
                        }
                    }
                }
            }
            else -> {
                if (audit.isEmpty()) Text("لا إجراءات مسجلة بعد.", Modifier.padding(24.dp), color = muted)
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(audit) { a ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp)) {
                                Text("${actionAr(a.action)} ${if (a.targetName != null) "@" + a.targetName else ""}", fontWeight = FontWeight.SemiBold)
                                Text("بواسطة @${a.adminName ?: "?"} • ${a.createdAt.take(16).replace('T', ' ')}", fontSize = 11.sp, color = muted)
                                if (!a.note.isNullOrBlank()) Text(a.note, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
                            }
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
