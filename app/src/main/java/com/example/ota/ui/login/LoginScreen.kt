package com.example.ota

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ota.data.OtaApi
import com.example.ota.ui.theme.*

/** 登录页（米家风格：居中卡片，未登录不可进主界面） */
@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(90.dp))
        Text("OTA Hub", fontSize = 34.sp, fontWeight = FontWeight.Bold, color = MiBlue)
        Text("智能家居设备管理平台", fontSize = 14.sp, color = MiTextDim,
            modifier = Modifier.padding(top = 6.dp))
        Spacer(Modifier.height(48.dp))

        Card(shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp)) {
                Text("账号登录", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                OutlinedTextField(phone, { phone = it }, label = { Text("手机号") },
                    singleLine = true, shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 14.dp))
                OutlinedTextField(password, { password = it }, label = { Text("密码") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true, shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
                Button(onClick = {
                    if (phone.isEmpty() || password.isEmpty()) { msg = "请输入手机号和密码"; return@Button }
                    busy = true
                    msg = "登录中…"
                    OtaApi.login(phone, password) { ok, m ->
                        msg = m
                        busy = false
                        if (ok) onLoginSuccess()
                    }
                }, enabled = !busy,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp).height(48.dp)
                ) { Text(if (busy) "登录中…" else "登 录", fontSize = 15.sp) }
                if (msg.isNotEmpty() && !busy) Text(msg, fontSize = 12.sp,
                    color = if (msg.contains("成功")) MiGreen else MiRed,
                    modifier = Modifier.padding(top = 8.dp))
            }
        }
        Spacer(Modifier.weight(1f))
        Text("OTA Hub · 智能家居设备管理", fontSize = 11.sp, color = MiTextDim,
            modifier = Modifier.padding(bottom = 20.dp))
    }
}
