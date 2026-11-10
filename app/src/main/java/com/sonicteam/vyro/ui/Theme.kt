package com.sonicteam.vyro.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Violet = Color(0xFF5B12FF)
val Magenta = Color(0xFFFF2FD0)
val Pink = Color(0xFFFF7AA8)
val Night = Color(0xFF07030F)
val Surface = Color(0xFF120A22)

@Composable
fun VyroTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Magenta, secondary = Violet, tertiary = Pink,
            background = Night, surface = Surface,
            onPrimary = Color.White, onBackground = Color(0xFFF4ECFF), onSurface = Color(0xFFF4ECFF)
        ),
        content = content
    )
}
