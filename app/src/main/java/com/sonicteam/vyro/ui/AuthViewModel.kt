package com.sonicteam.vyro.ui

import com.sonicteam.vyro.data.friendly

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonicteam.vyro.data.supabase
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class AuthViewModel : ViewModel() {
    var loading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var info by mutableStateOf<String?>(null)

    private val usernameRule = Regex("^[a-z0-9._]{3,30}$")

    private fun launchAuth(block: suspend () -> Unit) {
        viewModelScope.launch {
            loading = true; error = null; info = null
            try { block() } catch (e: Exception) { error = e.friendly() ?: "حدث خطأ غير متوقع" }
            loading = false
        }
    }

    fun signIn(mail: String, pass: String) = launchAuth {
        supabase.auth.signInWith(Email) { email = mail.trim(); password = pass }
    }

    fun signUp(mail: String, pass: String, user: String) {
        val name = user.trim().lowercase()
        if (!usernameRule.matches(name)) { error = "اسم المستخدم: 3 إلى 30 حرفاً إنجليزياً صغيراً أو أرقاماً أو . _"; return }
        if (pass.length < 8) { error = "كلمة المرور 8 أحرف على الأقل"; return }
        launchAuth {
            supabase.auth.signUpWith(Email) {
                email = mail.trim(); password = pass
                data = buildJsonObject { put("username", name) }
            }
            if (supabase.auth.currentSessionOrNull() == null) info = "تم إنشاء الحساب. تحقق من بريدك لتأكيد الحساب ثم سجّل الدخول."
        }
    }

    fun signOut() = launchAuth { supabase.auth.signOut() }
}
