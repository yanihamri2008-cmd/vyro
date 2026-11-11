package com.sonicteam.vyro.data

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import java.io.ByteArrayOutputStream

const val MAX_REEL_BYTES = 45L * 1024 * 1024
const val MAX_REEL_MS = 90_000L

data class VideoInfo(val sizeBytes: Long, val durationMs: Long)

fun videoInfo(ctx: Context, uri: Uri): VideoInfo {
    var size = 0L
    ctx.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { c ->
        if (c.moveToFirst()) size = c.getLong(0)
    }
    val r = MediaMetadataRetriever()
    val dur = try {
        r.setDataSource(ctx, uri)
        r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
    } finally { r.release() }
    return VideoInfo(size, dur)
}

fun videoThumb(ctx: Context, uri: Uri): ByteArray? {
    val r = MediaMetadataRetriever()
    return try {
        r.setDataSource(ctx, uri)
        val f = r.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        if (f == null) null else {
            val m = 720f / maxOf(f.width, f.height)
            val b = if (m < 1f) Bitmap.createScaledBitmap(f, (f.width * m).toInt(), (f.height * m).toInt(), true) else f
            val o = ByteArrayOutputStream()
            b.compress(Bitmap.CompressFormat.JPEG, 80, o)
            o.toByteArray()
        }
    } catch (e: Exception) { null } finally { r.release() }
}

fun readVideoBytes(ctx: Context, uri: Uri): ByteArray =
    ctx.contentResolver.openInputStream(uri)!!.use { it.readBytes() }
