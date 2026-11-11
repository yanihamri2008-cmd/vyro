package com.sonicteam.vyro.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sonicteam.vyro.data.BanInfo
import com.sonicteam.vyro.data.Repo
import kotlinx.coroutines.launch

private enum class Tab(val label: String, val icon: ImageVector) {
    Home("الرئيسية", Icons.Filled.Home),
    Explore("استكشف", Icons.Filled.Search),
    Create("نشر", Icons.Filled.AddCircle),
    Alerts("الإشعارات", Icons.Filled.Favorite),
    Me("حسابي", Icons.Filled.Person)
}

private sealed interface Screen {
    data object None : Screen
    data object Inbox : Screen
    data object NewChat : Screen
    data object NewGroup : Screen
    data object Admin : Screen
    data class Chat(val id: String, val title: String, val group: Boolean) : Screen
}

@Composable
fun MainScaffold(feedVm: FeedViewModel = viewModel(), authVm: AuthViewModel = viewModel()) {
    var tab by remember { mutableStateOf(Tab.Home) }
    var openProfile by remember { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf(false) }
    var screen by remember { mutableStateOf<Screen>(Screen.None) }
    var profileKey by remember { mutableIntStateOf(0) }
    var ban by remember { mutableStateOf<BanInfo?>(null) }
    var isAdmin by remember { mutableStateOf(false) }
    val snack = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        try { ban = Repo.myBan(); isAdmin = Repo.amIAdmin() } catch (e: Exception) { }
    }
    val notice: (String) -> Unit = { m -> scope.launch { snack.showSnackbar(m) } }

    BackHandler(enabled = editing || screen != Screen.None || openProfile != null) {
        val s = screen
        when {
            editing -> editing = false
            s is Screen.Chat || s == Screen.NewChat || s == Screen.NewGroup -> screen = Screen.Inbox
            s != Screen.None -> screen = Screen.None
            else -> openProfile = null
        }
    }

    val b = ban
    if (b != null) { BannedScreen(b) { authVm.signOut() }; return }

    Scaffold(
        snackbarHost = { SnackbarHost(snack) },
        bottomBar = {
            NavigationBar {
                Tab.values().forEach { t ->
                    NavigationBarItem(
                        selected = tab == t && !editing && openProfile == null && screen == Screen.None,
                        onClick = { tab = t; openProfile = null; editing = false; screen = Screen.None },
                        icon = { Icon(t.icon, t.label) }, label = { Text(t.label) })
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
                s is Screen.Inbox -> InboxScreen(
                    onOpen = { c -> screen = Screen.Chat(c.id, c.title ?: c.peerUsername ?: "محادثة", c.kind == "group") },
                    onNew = { screen = Screen.NewChat }, onNewGroup = { screen = Screen.NewGroup },
                    onBack = { screen = Screen.None })
                s is Screen.Chat -> ChatScreen(s.id, s.title, s.group, onBack = { screen = Screen.Inbox })
                s is Screen.NewChat -> NewChatScreen(
                    onStarted = { id, name -> screen = Screen.Chat(id, name, false) }, onBack = { screen = Screen.Inbox })
                s is Screen.NewGroup -> NewGroupScreen(
                    onCreated = { id, title -> screen = Screen.Chat(id, title, true) }, onBack = { screen = Screen.Inbox })
                s is Screen.Admin -> AdminScreen(onBack = { screen = Screen.None })
                openProfile != null -> ProfileScreen(
                    userId = openProfile!!, refreshKey = profileKey,
                    onBack = { openProfile = null },
                    onEdit = { editing = true }, onSignOut = { authVm.signOut() },
                    onMessage = { id, name ->
                        scope.launch {
                            try { val cid = Repo.startDm(id); openProfile = null; screen = Screen.Chat(cid, name, false) }
                            catch (e: Exception) { notice(e.message ?: "تعذّر بدء المحادثة") }
                        }
                    },
                    onAdmin = adminAction)
                tab == Tab.Home -> FeedScreen(feedVm, onProfile = { openProfile = it },
                    onInbox = { screen = Screen.Inbox }, onNotice = notice)
                tab == Tab.Explore -> ExploreScreen(onOpen = { openProfile = it })
                tab == Tab.Create -> CreatePostScreen(onDone = { feedVm.load(); profileKey++; tab = Tab.Home })
                tab == Tab.Me && me != null -> ProfileScreen(
                    userId = me, refreshKey = profileKey,
                    onEdit = { editing = true }, onSignOut = { authVm.signOut() }, onAdmin = adminAction)
                else -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text("قريباً في الدفعة التالية") }
            }
        }
    }
}
