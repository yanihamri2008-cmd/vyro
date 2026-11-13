package com.sonicteam.vyro

import android.os.Bundle
import android.os.SystemClock
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonicteam.vyro.data.AppPrefs
import com.sonicteam.vyro.data.Gate
import com.sonicteam.vyro.data.Notifier
import com.sonicteam.vyro.data.supabase
import com.sonicteam.vyro.ui.AuthScreen
import com.sonicteam.vyro.ui.CalculatorScreen
import com.sonicteam.vyro.ui.MainScaffold
import com.sonicteam.vyro.ui.PasswordScreen
import com.sonicteam.vyro.ui.VyroTheme
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Gate.blockShots = AppPrefs.blockShots(this)
        Notifier.apply(this)
        setContent { VyroTheme { Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { Root() } } }
    }

    override fun onStart() {
        super.onStart()
        if (Gate.leftAt > 0L) {
            val away = SystemClock.elapsedRealtime() - Gate.leftAt
            Gate.leftAt = 0L
            if (Gate.unlocked && away > AppPrefs.lockDelaySec(this) * 1000L) Gate.lock()
        }
    }

    /** Leaving the app locks it (immediately, or after the delay chosen in Settings). */
    override fun onStop() {
        super.onStop()
        if (Gate.pickerOpen || isChangingConfigurations) return
        if (AppPrefs.lockDelaySec(this) <= 0) Gate.lock()
        else if (Gate.unlocked) Gate.leftAt = SystemClock.elapsedRealtime()
    }
}

@Composable
fun Root() {
    val act = LocalContext.current as ComponentActivity
    LaunchedEffect(Gate.unlocked, Gate.blockShots) {
        if (Gate.unlocked && Gate.blockShots) act.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        else act.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }
    when {
        Gate.unlocked -> VyroApp()
        Gate.asking -> PasswordScreen(onOk = { Gate.unlocked = true; Gate.asking = false }, onCancel = { Gate.asking = false })
        else -> CalculatorScreen(onSecret = { Gate.asking = true })
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
