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
import com.sonicteam.vyro.data.Repo

private enum class Tab(val label: String, val icon: ImageVector) {
    Home("الرئيسية", Icons.Filled.Home),
    Explore("استكشف", Icons.Filled.Search),
    Create("نشر", Icons.Filled.AddCircle),
    Alerts("الإشعارات", Icons.Filled.Favorite),
    Me("حسابي", Icons.Filled.Person)
}

@Composable
fun MainScaffold(feedVm: FeedViewModel = viewModel(), authVm: AuthViewModel = viewModel()) {
    var tab by remember { mutableStateOf(Tab.Home) }
    var openProfile by remember { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf(false) }
    var profileKey by remember { mutableIntStateOf(0) }

    BackHandler(enabled = editing || openProfile != null) {
        if (editing) editing = false else openProfile = null
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.values().forEach { t ->
                    NavigationBarItem(selected = tab == t && !editing && openProfile == null,
                        onClick = { tab = t; openProfile = null; editing = false },
                        icon = { Icon(t.icon, t.label) }, label = { Text(t.label) })
                }
            }
        }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            val me = Repo.uid
            when {
                editing -> EditProfileScreen(
                    onDone = { editing = false; profileKey++; feedVm.load() },
                    onCancel = { editing = false })
                openProfile != null -> ProfileScreen(
                    userId = openProfile!!, refreshKey = profileKey,
                    onBack = { openProfile = null },
                    onEdit = { editing = true }, onSignOut = { authVm.signOut() })
                tab == Tab.Home -> FeedScreen(feedVm, onProfile = { openProfile = it })
                tab == Tab.Explore -> ExploreScreen(onOpen = { openProfile = it })
                tab == Tab.Create -> CreatePostScreen(onDone = { feedVm.load(); profileKey++; tab = Tab.Home })
                tab == Tab.Me && me != null -> ProfileScreen(
                    userId = me, refreshKey = profileKey,
                    onEdit = { editing = true }, onSignOut = { authVm.signOut() })
                else -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text("قريباً في الدفعة التالية") }
            }
        }
    }
}
