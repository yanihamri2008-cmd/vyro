package com.sonicteam.vyro.data

import android.content.Context
import android.util.Base64
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** In-memory lock state. The app is locked again whenever it leaves the foreground. */
object Gate {
    var unlocked by mutableStateOf(false)
    var asking by mutableStateOf(false)
    /** true while a system picker (gallery) is open, so leaving the app does not lock it */
    var pickerOpen = false

    fun lock() { unlocked = false; asking = false }
}

/** Access password for the real app. Stored as a salted PBKDF2 hash, never in clear text. */
object Security {
    private const val FILE = "calc_prefs"
    private fun p(ctx: Context) = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    private fun hash(pw: String, salt: ByteArray): ByteArray =
        SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(PBEKeySpec(pw.toCharArray(), salt, 120_000, 256)).encoded

    fun hasPassword(ctx: Context) = p(ctx).contains("hash")

    fun setPassword(ctx: Context, pw: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        p(ctx).edit()
            .putString("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString("hash", Base64.encodeToString(hash(pw, salt), Base64.NO_WRAP))
            .putInt("fails", 0).putLong("until", 0L).apply()
    }

    fun verify(ctx: Context, pw: String): Boolean {
        val s = p(ctx)
        val salt = Base64.decode(s.getString("salt", null) ?: return false, Base64.NO_WRAP)
        val want = Base64.decode(s.getString("hash", null) ?: return false, Base64.NO_WRAP)
        return MessageDigest.isEqual(want, hash(pw, salt))
    }

    fun lockedUntil(ctx: Context): Long = p(ctx).getLong("until", 0L)

    /** After 5 wrong attempts the lock time doubles each time (30s, 60s, ... max 15 min). */
    fun registerFail(ctx: Context) {
        val s = p(ctx)
        val fails = s.getInt("fails", 0) + 1
        val e = s.edit().putInt("fails", fails)
        if (fails >= 5) {
            val secs = minOf(30L shl (fails - 5).coerceAtMost(5), 900L)
            e.putLong("until", System.currentTimeMillis() + secs * 1000)
        }
        e.apply()
    }

    fun resetFails(ctx: Context) { p(ctx).edit().putInt("fails", 0).putLong("until", 0L).apply() }
}
