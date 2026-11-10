package com.sonicteam.vyro.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream

/** Downscales to max 1440px and re-encodes as JPEG. (EXIF rotation is handled in a later step.) */
fun compressImage(ctx: Context, uri: Uri): ByteArray {
    val cr = ctx.contentResolver
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    cr.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it, null, bounds) }
    var sample = 1
    while (bounds.outWidth / sample > 2880 || bounds.outHeight / sample > 2880) sample *= 2
    val opts = BitmapFactory.Options().apply { inSampleSize = sample }
    val bmp = cr.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it, null, opts) }
        ?: error("تعذّر قراءة الصورة")
    val m = 1440f / maxOf(bmp.width, bmp.height)
    val out = if (m < 1f) Bitmap.createScaledBitmap(bmp, (bmp.width * m).toInt(), (bmp.height * m).toInt(), true) else bmp
    val bos = ByteArrayOutputStream()
    out.compress(Bitmap.CompressFormat.JPEG, 85, bos)
    return bos.toByteArray()
}
