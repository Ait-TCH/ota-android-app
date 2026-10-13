package com.example.ota.ui.login

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

/** 登录/注册页：密码 + 短信验证码两种方式 */
@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    var mode by remember { mutableStateOf("login") }        // login | register
    var loginType by remember { mutableStateOf("password") } // password | sms
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var password2 by remember { mutableStateOf("") }
    var smsCode by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf("") }
    var msgOk by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var countdown by remember { mutableStateOf(0) }

    LaunchedEffect(countdown) {
        if (countdown > 0) {
            kotlinx.coroutines.delay(1000)
            countdown -= 1
        }
    }

    fun sendSms() {
        if (phone.isEmpty()) { msg = "请先输入手机号"; msgOk = false; return }
        busy = true
        OtaApi.sendSms(phone) { ok, m ->
            busy = false
            msg = m; msgOk = ok
            if (ok) countdown = 60
        }
    }

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(70.dp))
        Text("OTA Hub", fontSize = 34.sp, fontWeight = FontWeight.Bold, color = MiBlue)
        Text("智能家居设备管理平台", fontSize = 14.sp, color = MiTextDim,
            modifier = Modifier.padding(top = 6.dp))
        Spacer(Modifier.height(36.dp))

        Card(shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp)) {
                // 登录/注册切换
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    TabText("登录", mode == "login") { mode = "login"; msg = "" }
                    TabText("注册", mode == "register") { mode = "register"; msg = "" }
                }
                Spacer(Modifier.height(12.dp))

                // 登录方式切换（仅登录模式）
                if (mode == "login") {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Text("密码登录",
                            fontSize = 13.sp,
                            color = if (loginType == "password") MiBlue else MiTextDim,
                            fontWeight = if (loginType == "password") FontWeight.Medium else FontWeight.Normal,
                            modifier = Modifier.clickable { loginType = "password" })
                        Text("短信登录",
                            fontSize = 13.sp,
                            color = if (loginType == "sms") MiBlue else MiTextDim,
                            fontWeight = if (loginType == "sms") FontWeight.Medium else FontWeight.Normal,
                            modifier = Modifier.clickable { loginType = "sms" })
                    }
                    Spacer(Modifier.height(8.dp))
                }

                OutlinedTextField(phone, { phone = it }, label = { Text("手机号") },
                    singleLine = true, shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth())

                if (mode == "login" && loginType == "password") {
                    OutlinedTextField(password, { password = it }, label = { Text("密码") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true, shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
                }

                // 短信验证码（短信登录 / 注册时显示）
                if ((mode == "login" && loginType == "sms") || mode == "register") {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
                        OutlinedTextField(smsCode, { smsCode = it },
                            label = { Text("短信验证码") },
                            singleLine = true, shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f))
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = { sendSms() },
                            enabled = countdown == 0 && phone.isNotEmpty() && !busy,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MiBlue)
                        ) {
                            Text(if (countdown > 0) "${countdown}s" else "发送验证码",
                                fontSize = 12.sp)
                        }
                    }
                }

                if (mode == "register") {
                    OutlinedTextField(password, { password = it },
                        label = { Text("设置密码（至少 6 位）") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true, shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
                    OutlinedTextField(password2, { password2 = it },
                        label = { Text("确认密码") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true, shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
                    OutlinedTextField(nickname, { nickname = it },
                        label = { Text("昵称（可选）") },
                        singleLine = true, shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
                }

                Button(onClick = {
                    if (mode == "login") {
                        if (phone.isEmpty()) { msg = "请输入手机号"; msgOk = false; return@Button }
                        if (loginType == "password" && password.isEmpty()) {
                            msg = "请输入密码"; msgOk = false; return@Button
                        }
                        if (loginType == "sms" && smsCode.isEmpty()) {
                            msg = "请输入短信验证码"; msgOk = false; return@Button
                        }
                        busy = true; msg = "登录中…"
                        if (loginType == "sms") {
                            OtaApi.loginSms(phone, smsCode) { ok, m ->
                                busy = false; msg = m; msgOk = ok
                                if (ok) onLoginSuccess()
                            }
                        } else {
                            OtaApi.login(phone, password) { ok, m ->
                                busy = false; msg = m; msgOk = ok
                                if (ok) onLoginSuccess()
                            }
                        }
                    } else {
                        if (phone.isEmpty() || password.length < 6) {
                            msg = "手机号必填，密码至少 6 位"; msgOk = false; return@Button
                        }
                        if (password != password2) {
                            msg = "两次密码不一致"; msgOk = false; return@Button
                        }
                        if (smsCode.isEmpty()) {
                            msg = "请输入短信验证码"; msgOk = false; return@Button
                        }
                        busy = true; msg = "注册中…"
                        OtaApi.registerSms(phone, smsCode, password, nickname) { ok, m ->
                            busy = false; msg = m; msgOk = ok
                            if (ok) mode = "login"
                        }
                    }
                }, enabled = !busy,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp).height(48.dp)
                ) {
                    Text(
                        when {
                            busy && mode == "login" -> "登录中…"
                            busy -> "注册中…"
                            mode == "login" -> "登 录"
                            else -> "注 册"
                        }, fontSize = 15.sp)
                }
                if (msg.isNotEmpty() && !busy) Text(msg, fontSize = 12.sp,
                    color = if (msgOk) MiGreen else MiRed,
                    modifier = Modifier.padding(top = 8.dp))
            }
        }
        Spacer(Modifier.weight(1f))
        Text("注册后需管理员审核通过才能使用", fontSize = 11.sp, color = MiTextDim,
            modifier = Modifier.padding(bottom = 20.dp))
    }
}

@Composable
fun TabText(text: String, active: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }) {
        Text(text, fontSize = 16.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
            color = if (active) MiBlue else MiTextDim)
        Box(Modifier.padding(top = 4.dp).width(32.dp).height(3.dp)
            .background(if (active) MiBlue else Color.Transparent,
                RoundedCornerShape(2.dp)))
    }
}
