package com.sonicteam.vyro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonicteam.vyro.data.supabase
import com.sonicteam.vyro.ui.AuthScreen
import com.sonicteam.vyro.ui.MainScaffold
import com.sonicteam.vyro.ui.VyroTheme
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { VyroTheme { Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { VyroApp() } } }
    }
}

@Composable
fun VyroApp() {
    val status by supabase.auth.sessionStatus.collectAsStateWithLifecycle()
    when (status) {
        is SessionStatus.Initializing -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
        is SessionStatus.Authenticated -> MainScaffold()
        else -> AuthScreen()
    }
}
