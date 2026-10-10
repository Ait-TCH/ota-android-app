package com.example.ota.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ota.data.OtaApi
import com.example.ota.ui.theme.*

/** 我的页：账号信息 + 固件库入口 + 退出登录 */
@Composable
fun SettingsScreen(onLogout: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("我的", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(16.dp))

        // ── 账号卡 ──
        Card(shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MiBlue),
            modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(54.dp).background(Color.White.copy(alpha = 0.25f), CircleShape),
                    contentAlignment = Alignment.Center) {
                    Text(OtaApi.displayName.take(1).ifEmpty { "?" }, fontSize = 22.sp,
                        color = Color.White, fontWeight = FontWeight.Bold)
                }
                Column(Modifier.padding(start = 14.dp)) {
                    Text(OtaApi.displayName.ifEmpty { "已登录" }, fontSize = 17.sp,
                        fontWeight = FontWeight.Medium, color = Color.White)
                    Text("OTA 平台账号", fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.75f))
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // ── 功能列表 ──
        Card(shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column {
                SettingRow("📦", "固件库", "查看固件版本")
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                SettingRow("🌐", "OTA 平台网页", "ota.aittch.xyz")
            }
        }

        Spacer(Modifier.weight(1f))

        Button(onClick = {
            OtaApi.logout { onLogout() }
        }, shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White,
                contentColor = MiRed),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) { Text("退出登录", fontSize = 15.sp) }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
fun SettingRow(icon: String, title: String, sub: String) {
    Row(Modifier.fillMaxWidth().clickable { }
        .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text(icon, fontSize = 18.sp)
        Column(Modifier.padding(start = 12.dp).weight(1f)) {
            Text(title, fontSize = 15.sp)
            Text(sub, fontSize = 11.sp, color = MiTextDim)
        }
        Text("›", fontSize = 18.sp, color = MiTextDim)
    }
}
