package com.sonicteam.vyro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.math.BigDecimal
import java.math.RoundingMode

private const val OPS = "+−×÷"

private fun evaluate(s: String): Double? {
    var i = 0
    fun peek() = if (i < s.length) s[i] else '\u0000'
    fun number(): Double? {
        val st = i
        while (i < s.length && (s[i].isDigit() || s[i] == '.')) i++
        return if (st == i) null else s.substring(st, i).toDoubleOrNull()
    }
    fun unary(): Double? {
        if (peek() == '−') { i++; return unary()?.let { -it } }
        if (peek() == '+') { i++; return unary() }
        var v = number() ?: return null
        while (peek() == '%') { i++; v /= 100.0 }
        return v
    }
    fun term(): Double? {
        var v = unary() ?: return null
        while (peek() == '×' || peek() == '÷') {
            val op = peek(); i++
            val r = unary() ?: return null
            if (op == '×') v *= r else { if (r == 0.0) return null; v /= r }
        }
        return v
    }
    fun sum(): Double? {
        var v = term() ?: return null
        while (peek() == '+' || peek() == '−') {
            val op = peek(); i++
            val r = term() ?: return null
            v = if (op == '+') v + r else v - r
        }
        return v
    }
    val r = sum()
    return if (i == s.length) r else null
}

private fun fmt(d: Double): String {
    if (d.isNaN() || d.isInfinite() || kotlin.math.abs(d) > 1e15) return "خطأ"
    val t = BigDecimal(d).setScale(8, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
    return (if (t == "-0") "0" else t).replace('-', '−')
}

@Composable
fun CalculatorScreen(onSecret: () -> Unit) {
    var expr by remember { mutableStateOf("") }
    var shown by remember { mutableStateOf("0") }
    var done by remember { mutableStateOf(false) }
    var lastExpr by remember { mutableStateOf("") }

    fun clear() { expr = ""; shown = "0"; done = false; lastExpr = "" }

    fun press(k: String) {
        if (k == "C") { clear(); return }
        if (k == "⌫") {
            if (!done && expr.isNotEmpty()) { expr = expr.dropLast(1); shown = expr.ifEmpty { "0" } }
            return
        }
        if (k == "=") {
            if (done) { if (lastExpr == "202+202") { clear(); onSecret() }; return }
            if (expr.isEmpty()) return
            val r = evaluate(expr)
            lastExpr = expr
            shown = r?.let { fmt(it) } ?: "خطأ"
            done = true
            return
        }
        if (k == "±") {
            if (done) { if (shown != "خطأ") { shown = if (shown.startsWith("−")) shown.drop(1) else "−$shown"; expr = shown } }
            else if (expr.startsWith("−") && expr.drop(1).none { it in OPS }) { expr = expr.drop(1); shown = expr.ifEmpty { "0" } }
            else if (expr.isNotEmpty() && expr.none { it in OPS }) { expr = "−$expr"; shown = expr }
            return
        }
        if (k.length == 1 && k[0] in OPS) {
            if (done) { expr = if (shown == "خطأ") "" else shown; done = false }
            if (expr.isEmpty()) { if (k == "−") { expr = "−"; shown = expr }; return }
            if (expr == "−") return
            expr = if (expr.last() in OPS) expr.dropLast(1) + k else expr + k
            shown = expr
            return
        }
        if (k == "%") {
            if (done) { expr = if (shown == "خطأ") "" else shown; done = false }
            if (expr.isNotEmpty() && expr.last().isDigit()) { expr += "%"; shown = expr }
            return
        }
        // digit or dot
        if (done) { expr = ""; done = false }
        if (k == ".") {
            val n = expr.takeLastWhile { it.isDigit() || it == '.' }
            if (n.contains('.')) return
            if (n.isEmpty()) expr += "0"
        }
        if (expr.length < 30) expr += k
        shown = expr
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(Modifier.fillMaxSize().background(Color.Black).systemBarsPadding().padding(12.dp)) {
            Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp), contentAlignment = Alignment.BottomEnd) {
                Text(shown, color = Color.White, maxLines = 1, fontWeight = FontWeight.Light,
                    fontSize = when { shown.length > 14 -> 32.sp; shown.length > 9 -> 48.sp; else -> 76.sp })
            }
            Spacer(Modifier.height(12.dp))
            val rows = listOf(
                listOf("C", "⌫", "%", "÷"),
                listOf("7", "8", "9", "×"),
                listOf("4", "5", "6", "−"),
                listOf("1", "2", "3", "+"),
                listOf("±", "0", ".", "=")
            )
            rows.forEach { row ->
                Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { k ->
                        val top = k == "C" || k == "⌫" || k == "%"
                        val op = k.length == 1 && (k[0] in OPS || k == "=")
                        Box(
                            Modifier.weight(1f).aspectRatio(1f).clip(CircleShape)
                                .background(when { op -> Color(0xFFFF9F0A); top -> Color(0xFFA5A5A5); else -> Color(0xFF333333) })
                                .clickable { press(k) },
                            contentAlignment = Alignment.Center
                        ) { Text(k, fontSize = 30.sp, color = if (top) Color.Black else Color.White) }
                    }
                }
            }
        }
    }
}
