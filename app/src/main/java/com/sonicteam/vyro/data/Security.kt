package com.sonicteam.vyro.data

import android.content.Context
import android.util.Base64
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** In-memory lock state. */
object Gate {
    var unlocked by mutableStateOf(false)
    var asking by mutableStateOf(false)
    var blockShots by mutableStateOf(true)
    /** true while a system picker (gallery) is open, so leaving the app does not lock it */
    var pickerOpen = false
    /** elapsedRealtime when the app went to background (used for the timed auto-lock) */
    var leftAt = 0L

    fun lock() { unlocked = false; asking = false; leftAt = 0L }
}

/** Access password. Stored as a salted PBKDF2 hash. Hashing runs off the main thread (slow phones). */
object Security {
    private const val FILE = "calc_prefs"
    private const val ITER_NEW = 60_000
    private const val ITER_OLD = 120_000   // hashes created by earlier versions
    private fun p(ctx: Context) = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    private fun hash(pw: String, salt: ByteArray, iter: Int): ByteArray =
        SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(PBEKeySpec(pw.toCharArray(), salt, iter, 256)).encoded

    fun hasPassword(ctx: Context) = p(ctx).contains("hash")

    suspend fun setPassword(ctx: Context, pw: String) = withContext(Dispatchers.Default) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val h = hash(pw, salt, ITER_NEW)
        p(ctx).edit()
            .putString("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString("hash", Base64.encodeToString(h, Base64.NO_WRAP))
            .putInt("iter", ITER_NEW)
            .putInt("fails", 0).putLong("until", 0L).apply()
    }

    suspend fun verify(ctx: Context, pw: String): Boolean = withContext(Dispatchers.Default) {
        val s = p(ctx)
        val saltS = s.getString("salt", null)
        val hashS = s.getString("hash", null)
        if (saltS == null || hashS == null) false
        else {
            val salt = Base64.decode(saltS, Base64.NO_WRAP)
            val want = Base64.decode(hashS, Base64.NO_WRAP)
            MessageDigest.isEqual(want, hash(pw, salt, s.getInt("iter", ITER_OLD)))
        }
    }

    fun lockedUntil(ctx: Context): Long = p(ctx).getLong("until", 0L)

    /** After 5 wrong attempts the lock time doubles each time (30s ... max 15 min). */
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
