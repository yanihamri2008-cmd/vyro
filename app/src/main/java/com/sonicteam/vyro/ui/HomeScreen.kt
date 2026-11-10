package com.sonicteam.vyro.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sonicteam.vyro.R

// Placeholder: the real feed (get_feed) is the next step.
@Composable
fun HomeScreen(vm: AuthViewModel = viewModel()) {
    Column(Modifier.fillMaxSize().systemBarsPadding().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.vyro_logo), null, Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)))
            Spacer(Modifier.width(8.dp))
            Text("VYRO", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.weight(1f))
            TextButton({ vm.signOut() }) { Text("خروج") }
        }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("تم تسجيل الدخول. الخلاصة قيد البناء.")
        }
    }
}
