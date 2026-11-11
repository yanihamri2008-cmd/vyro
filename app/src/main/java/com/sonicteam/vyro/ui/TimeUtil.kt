package com.sonicteam.vyro.ui

import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime

fun agoText(iso: String?): String {
    if (iso == null) return "لم يتصل بعد"
    return try {
        val m = Duration.between(OffsetDateTime.parse(iso).toInstant(), Instant.now()).toMinutes()
        when {
            m < 2 -> "متصل الآن"
            m < 60 -> "قبل $m دقيقة"
            m < 1440 -> "قبل ${m / 60} ساعة"
            else -> "قبل ${m / 1440} يوم"
        }
    } catch (e: Exception) { iso.take(10) }
}
