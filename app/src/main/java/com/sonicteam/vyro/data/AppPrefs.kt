package com.sonicteam.vyro.data

import android.content.Context

/** User settings stored on the device. */
object AppPrefs {
    private fun p(c: Context) = c.getSharedPreferences("vyro_settings", Context.MODE_PRIVATE)

    fun lockDelaySec(c: Context) = p(c).getInt("lock_delay", 0)
    fun setLockDelaySec(c: Context, v: Int) = p(c).edit().putInt("lock_delay", v).apply()

    fun blockShots(c: Context) = p(c).getBoolean("block_shots", true)
    fun setBlockShots(c: Context, v: Boolean) = p(c).edit().putBoolean("block_shots", v).apply()

    fun notifEnabled(c: Context) = p(c).getBoolean("notif_on", false)
    fun setNotifEnabled(c: Context, v: Boolean) = p(c).edit().putBoolean("notif_on", v).apply()

    fun notifMessages(c: Context) = p(c).getBoolean("notif_msg", true)
    fun setNotifMessages(c: Context, v: Boolean) = p(c).edit().putBoolean("notif_msg", v).apply()

    fun notifActivity(c: Context) = p(c).getBoolean("notif_act", true)
    fun setNotifActivity(c: Context, v: Boolean) = p(c).edit().putBoolean("notif_act", v).apply()

    fun hideContent(c: Context) = p(c).getBoolean("notif_hide", true)
    fun setHideContent(c: Context, v: Boolean) = p(c).edit().putBoolean("notif_hide", v).apply()

    fun lastNotified(c: Context) = p(c).getLong("last_notified", 0L)
    fun setLastNotified(c: Context, v: Long) = p(c).edit().putLong("last_notified", v).apply()
}
