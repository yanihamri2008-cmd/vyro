package com.sonicteam.vyro.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sonicteam.vyro.R

@Composable
fun AuthScreen(vm: AuthViewModel = viewModel()) {
    var signup by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    Column(
        Modifier.fillMaxSize().padding(24.dp).systemBarsPadding(),
        verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(painterResource(R.drawable.vyro_logo), null, Modifier.size(96.dp).clip(RoundedCornerShape(24.dp)))
        Text("VYRO", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), label = { Text("البريد الإلكتروني") },
            singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
        if (signup) {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(user, { user = it }, Modifier.fillMaxWidth(), label = { Text("اسم المستخدم") }, singleLine = true)
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(pass, { pass = it }, Modifier.fillMaxWidth(), label = { Text("كلمة المرور") },
            singleLine = true, visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
        vm.error?.let { Spacer(Modifier.height(8.dp)); Text(it, color = Color(0xFFFF6B8A), fontSize = 13.sp) }
        vm.info?.let { Spacer(Modifier.height(8.dp)); Text(it, color = MaterialTheme.colorScheme.tertiary, fontSize = 13.sp) }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { if (signup) vm.signUp(email, pass, user) else vm.signIn(email, pass) },
            enabled = !vm.loading && email.isNotBlank() && pass.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            if (vm.loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            else Text(if (signup) "إنشاء حساب" else "تسجيل الدخول")
        }
        TextButton({ signup = !signup }) { Text(if (signup) "لديك حساب؟ سجّل الدخول" else "ليس لديك حساب؟ أنشئ حساباً") }
        Spacer(Modifier.height(24.dp))
        Text("من تطوير فريق sonicteam", fontSize = 12.sp, color = Color(0xFFA392C7))
    }
}
