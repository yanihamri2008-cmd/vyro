package com.sonicteam.vyro.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
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
    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.values().forEach { t ->
                    NavigationBarItem(selected = tab == t, onClick = { tab = t },
                        icon = { Icon(t.icon, t.label) }, label = { Text(t.label) })
                }
            }
        }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when (tab) {
                Tab.Home -> FeedScreen(feedVm)
                Tab.Create -> CreatePostScreen(onDone = { feedVm.load(); tab = Tab.Home })
                Tab.Me -> Column(Modifier.fillMaxSize().padding(24.dp), Arrangement.Center, Alignment.CenterHorizontally) {
                    Text(Repo.email ?: "", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton({ authVm.signOut() }) { Text("تسجيل الخروج") }
                }
                else -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text("قريباً في الدفعة التالية") }
            }
        }
    }
}
