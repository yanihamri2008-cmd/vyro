package com.sonicteam.vyro.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import com.sonicteam.vyro.data.supabase
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/** Calls [onEvent] whenever a message row is inserted that this user can see (RLS applies). */
@Composable
fun OnNewMessage(key: Any, onEvent: () -> Unit) {
    val cb = rememberUpdatedState(onEvent)
    LaunchedEffect(key) {
        val ch = supabase.channel("msg-${System.nanoTime()}")
        val flow = ch.postgresChangeFlow<PostgresAction.Insert>(schema = "public") { table = "messages" }
        try {
            ch.subscribe()
            flow.collect { cb.value() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // realtime is optional: screens also reload after sending
        } finally {
            withContext(NonCancellable) { try { supabase.realtime.removeChannel(ch) } catch (_: Exception) {} }
        }
    }
}
