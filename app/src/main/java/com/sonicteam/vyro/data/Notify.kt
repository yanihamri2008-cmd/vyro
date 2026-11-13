package com.sonicteam.vyro.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.sonicteam.vyro.MainActivity
import com.sonicteam.vyro.R
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

object Notifier {
    private const val WORK = "vyro-notify"
    const val CHANNEL = "updates"

    /** Starts or stops the background check according to the user's setting. */
    fun apply(ctx: Context) {
        val wm = WorkManager.getInstance(ctx)
        if (AppPrefs.notifEnabled(ctx)) {
            val req = PeriodicWorkRequestBuilder<NotifyWorker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            wm.enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.UPDATE, req)
        } else wm.cancelUniqueWork(WORK)
    }
}

/** Checks for new messages / activity every ~15 minutes (Android's minimum) and shows a local notification. */
class NotifyWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val ctx = applicationContext
        if (!AppPrefs.notifEnabled(ctx)) return Result.success()
        return try {
            val st = supabase.auth.sessionStatus.first { it !is SessionStatus.Initializing }
            if (st !is SessionStatus.Authenticated) return Result.success()
            var msgs = 0L
            var act = 0L
            if (AppPrefs.notifMessages(ctx)) msgs = Repo.inbox().sumOf { it.unread }
            if (AppPrefs.notifActivity(ctx)) act = Repo.notifications().count { !it.read }.toLong()
            val total = msgs + act
            if (total > 0 && total > AppPrefs.lastNotified(ctx)) show(ctx, total)
            AppPrefs.setLastNotified(ctx, total)
            Result.success()
        } catch (e: Exception) {
            Result.success()
        }
    }

    private fun show(ctx: Context, total: Long) {
        val nm = NotificationManagerCompat.from(ctx)
        if (!nm.areNotificationsEnabled()) return
        if (Build.VERSION.SDK_INT >= 26) {
            val ch = NotificationChannel(Notifier.CHANNEL, "التحديثات", NotificationManager.IMPORTANCE_DEFAULT)
            ctx.getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
        val hide = AppPrefs.hideContent(ctx)
        val intent = Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pi = PendingIntent.getActivity(ctx, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val n = NotificationCompat.Builder(ctx, Notifier.CHANNEL)
            .setSmallIcon(R.drawable.ic_stat)
            .setContentTitle(if (hide) "الآلة الحاسبة" else "VYRO")
            .setContentText(if (hide) "تحديث جديد" else "لديك $total تحديثاً جديداً")
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        try { nm.notify(1001, n) } catch (e: SecurityException) { }
    }
}
