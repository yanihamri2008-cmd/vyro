package com.sonicteam.vyro.data

/** Short, safe error text for the screen: never shows request URLs or headers (they can contain tokens). */
fun Throwable.friendly(): String {
    val m = message ?: return "حدث خطأ غير متوقع"
    return m.substringBefore("URL:").substringBefore("Headers:").trim().take(300)
        .ifBlank { "حدث خطأ غير متوقع" }
}
