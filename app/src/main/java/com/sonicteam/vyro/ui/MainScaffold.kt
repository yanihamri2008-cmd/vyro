package com.sonicteam.vyro.ui

import com.sonicteam.vyro.data.friendly

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sonicteam.vyro.R
import com.sonicteam.vyro.data.BanInfo
import com.sonicteam.vyro.data.Repo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Same order as Instagram: Home, Reels, Messages, Search, Profile (mirrors automatically in RTL).
private enum class NavTab(val icon: ImageVector?) {
    Home(Icons.Filled.Home),
    Reels(Icons.Outlined.PlayCircleOutline),
    Messages(Icons.AutoMirrored.Filled.Send),
    Search(Icons.Filled.Search),
    Me(null)
}

private sealed interface Screen {
    data object None : Screen
    data object NewChat : Screen
    data object NewGroup : Screen
    data object Admin : Screen
    data object Create : Screen
    data object Alerts : Screen
    data class Chat(val id: String, val title: String, val group: Boolean) : Screen
}

@Composable
private fun HomeTopBar(onAdd: () -> Unit, onAlerts: () -> Unit) {
    Box(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
        IconButton(onAdd, Modifier.align(Alignment.CenterStart)) { Icon(Icons.Filled.Add, "نشر", Modifier.size(30.dp)) }
        Row(Modifier.align(Alignment.Center), verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.vyro_logo), null, Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)))
            Spacer(Modifier.width(8.dp))
            Text("VYRO", style = TextStyle(
                brush = Brush.horizontalGradient(listOf(Violet, Magenta, Pink)),
                fontSize = 26.sp, fontWeight = FontWeight.ExtraBold))
        }
        IconButton(onAlerts, Modifier.align(Alignment.CenterEnd)) { Icon(Icons.Outlined.FavoriteBorder, "الإشعارات", Modifier.size(28.dp)) }
    }
}

@Composable
private fun StoryTray(avatar: String?, name: String, onTap: () -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 8.dp)) {
        Column(Modifier.clickable { onTap() }, horizontalAlignment = Alignment.CenterHorizontally) {
            Box {
                Avatar(avatar, name, 70)
                Box(Modifier.align(Alignment.BottomEnd).size(22.dp).clip(CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.background, CircleShape)
                    .background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Add, null, Modifier.size(14.dp), tint = Color.White)
                }
            }
            Text("قصتك", fontSize = 12.sp, color = Color(0xFFA392C7), modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
fun MainScaffold(feedVm: FeedViewModel = viewModel(), authVm: AuthViewModel = viewModel()) {
    var tab by remember { mutableStateOf(NavTab.Home) }
    var openProfile by remember { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf(false) }
    var screen by remember { mutableStateOf<Screen>(Screen.None) }
    var profileKey by remember { mutableIntStateOf(0) }
    var ban by remember { mutableStateOf<BanInfo?>(null) }
    var isAdmin by remember { mutableStateOf(false) }
    var myAvatar by remember { mutableStateOf<String?>(null) }
    var myName by remember { mutableStateOf("?") }
    var unread by remember { mutableLongStateOf(0L) }
    var showPw by remember { mutableStateOf(false) }
    val snack = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val notice: (String) -> Unit = { m -> scope.launch { snack.showSnackbar(m) } }

    suspend fun refreshUnread() {
        try { unread = Repo.inbox().sumOf { it.unread } } catch (e: CancellationException) { throw e } catch (e: Exception) { }
    }
    LaunchedEffect(Unit) {
        try { ban = Repo.myBan(); isAdmin = Repo.amIAdmin() } catch (e: Exception) { }
    }
    LaunchedEffect(profileKey) {
        try { Repo.uid?.let { val p = Repo.profile(it); myAvatar = p.avatarUrl; myName = p.username } }
        catch (e: CancellationException) { throw e } catch (e: Exception) { }
    }
    LaunchedEffect(tab, screen) { refreshUnread() }
    OnNewMessage(Unit) { scope.launch { refreshUnread() } }
    LaunchedEffect(tab) { if (tab == NavTab.Home) feedVm.load() }
    // presence: counts as "online" only while the real app is open
    LaunchedEffect(Unit) {
        while (true) {
            try { Repo.heartbeat() } catch (e: CancellationException) { throw e } catch (e: Exception) { }
            delay(60_000)
        }
    }

    BackHandler(enabled = editing || screen != Screen.None || openProfile != null || tab != NavTab.Home) {
        when {
            editing -> editing = false
            screen != Screen.None -> screen = Screen.None
            openProfile != null -> openProfile = null
            else -> tab = NavTab.Home
        }
    }

    val b = ban
    if (b != null) { BannedScreen(b) { authVm.signOut() }; return }

    Scaffold(
        snackbarHost = { SnackbarHost(snack) },
        bottomBar = {
            NavigationBar {
                NavTab.values().forEach { t ->
                    NavigationBarItem(
                        selected = tab == t && !editing && openProfile == null && screen == Screen.None,
                        onClick = { tab = t; openProfile = null; editing = false; screen = Screen.None },
                        icon = {
                            when (t) {
                                NavTab.Me -> Avatar(myAvatar, myName, 28)
                                NavTab.Messages -> BadgedBox(badge = { if (unread > 0) Badge { Text("$unread") } }) { Icon(t.icon!!, "الرسائل") }
                                else -> Icon(t.icon!!, null)
                            }
                        },
                        alwaysShowLabel = false)
                }
            }
        }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            val me = Repo.uid
            val s = screen
            val adminAction: (() -> Unit)? = if (isAdmin) ({ screen = Screen.Admin }) else null
            when {
                editing -> EditProfileScreen(
                    onDone = { editing = false; profileKey++; feedVm.load() },
                    onCancel = { editing = false })
                s is Screen.Chat -> ChatScreen(s.id, s.title, s.group, onBack = { screen = Screen.None })
                s is Screen.NewChat -> NewChatScreen(
                    onStarted = { id, name -> screen = Screen.Chat(id, name, false) }, onBack = { screen = Screen.None })
                s is Screen.NewGroup -> NewGroupScreen(
                    onCreated = { id, title -> screen = Screen.Chat(id, title, true) }, onBack = { screen = Screen.None })
                s is Screen.Admin -> AdminScreen(onBack = { screen = Screen.None })
                s is Screen.Create -> CreatePostScreen(onDone = { feedVm.load(); profileKey++; screen = Screen.None; tab = NavTab.Home })
                s is Screen.Alerts -> NotificationsScreen(onBack = { screen = Screen.None })
                openProfile != null -> ProfileScreen(
                    userId = openProfile!!, refreshKey = profileKey,
                    onBack = { openProfile = null },
                    onEdit = { editing = true }, onSignOut = { authVm.signOut() },
                    onMessage = { id, name ->
                        scope.launch {
                            try { val cid = Repo.startDm(id); openProfile = null; screen = Screen.Chat(cid, name, false) }
                            catch (e: Exception) { notice(e.friendly() ?: "تعذّر بدء المحادثة") }
                        }
                    },
                    onAdmin = adminAction)
                tab == NavTab.Home -> Column(Modifier.fillMaxSize()) {
                    HomeTopBar(onAdd = { screen = Screen.Create }, onAlerts = { screen = Screen.Alerts })
                    FeedScreen(feedVm, onProfile = { openProfile = it }, onNotice = notice,
                        header = { StoryTray(myAvatar, myName) { notice("القصص قريباً") } })
                }
                tab == NavTab.Reels -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text("الريلز قريباً") }
                tab == NavTab.Messages -> InboxScreen(
                    onOpen = { c -> screen = Screen.Chat(c.id, c.title ?: c.peerUsername ?: "محادثة", c.kind == "group") },
                    onNew = { screen = Screen.NewChat }, onNewGroup = { screen = Screen.NewGroup })
                tab == NavTab.Search -> ExploreScreen(onOpen = { openProfile = it })
                tab == NavTab.Me && me != null -> ProfileScreen(
                    userId = me, refreshKey = profileKey,
                    onEdit = { editing = true }, onSignOut = { authVm.signOut() },
                    onAdmin = adminAction, onSecurity = { showPw = true })
            }
        }
    }
    if (showPw) ChangePasswordDialog(onDismiss = { showPw = false }, onDone = { showPw = false; notice("تم تغيير كلمة السر") })
}
