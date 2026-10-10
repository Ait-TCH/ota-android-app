package com.example.ota.ui.devices

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.ota.data.Device
import com.example.ota.data.Firmware
import com.example.ota.data.OtaApi
import com.example.ota.ui.theme.*

/** 设备管理页（米家风格）：状态卡片 + 设备列表 + OTA 下发 */
@Composable
fun DevicesScreen(onOpenDevice: (String, String) -> Unit = { _, _ -> }) {
    var devices by remember { mutableStateOf(listOf<Device>()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<String?>(null) }
    var firmwares by remember { mutableStateOf(listOf<Firmware>()) }
    var otaMsg by remember { mutableStateOf("") }
    var otaBusy by remember { mutableStateOf(false) }

    fun load() {
        loading = true
        OtaApi.devices { list, err ->
            devices = list
            error = err ?: ""
            loading = false
        }
    }

    LaunchedEffect(Unit) { load() }
    LaunchedEffect(selected) {
        if (selected != null) OtaApi.firmwares { list, _ -> firmwares = list }
    }

    val online = devices.count { it.status == "online" }

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(20.dp))
        // ── 顶栏 ──
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("我的设备", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { load() }) { Text("刷新", fontSize = 13.sp, color = MiBlue) }
        }

        Spacer(Modifier.height(16.dp))

        // ── 概览卡 ──
        Card(shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MiBlue),
            modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(20.dp)) {
                Stat("设备总数", devices.size.toString(), Modifier.weight(1f))
                Stat("在线", online.toString(), Modifier.weight(1f), MiGreen)
                Stat("离线", (devices.size - online).toString(), Modifier.weight(1f), Color.White.copy(alpha = 0.6f))
            }
        }

        if (error.isNotEmpty()) Text(error, fontSize = 12.sp, color = MiRed,
            modifier = Modifier.padding(top = 8.dp))
        if (loading) LinearProgressIndicator(
            Modifier.fillMaxWidth().padding(top = 8.dp), color = MiBlue)

        // ── 设备列表 ──
        LazyColumn(Modifier.weight(1f).padding(top = 10.dp)) {
            items(devices) { d ->
                val onlineDev = d.status == "online"
                Card(shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)
                        .clickable { onOpenDevice(d.id, d.name.ifEmpty { d.id }) }) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // 设备图标
                            Box(Modifier.size(48.dp).background(
                                if (onlineDev) Color(0xFFE8F0FE) else Color(0xFFF2F3F7),
                                CircleShape), contentAlignment = Alignment.Center) {
                                Text("💡", fontSize = 20.sp)
                            }
                            Column(Modifier.padding(start = 14.dp).weight(1f)) {
                                Text(d.name.ifEmpty { d.id.take(8).uppercase() },
                                    fontSize = 16.sp, fontWeight = FontWeight.Medium)
                                Text("v${d.firmware} · ${d.ip}",
                                    fontSize = 12.sp, color = MiTextDim,
                                    modifier = Modifier.padding(top = 2.dp))
                            }
                            // 状态点
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(8.dp).background(
                                    if (onlineDev) MiGreen else Color(0xFFC6CAD0), CircleShape))
                                Text(if (onlineDev) "在线" else "离线", fontSize = 12.sp,
                                    color = MiTextDim, modifier = Modifier.padding(start = 5.dp))
                            }
                        }

                        // OTA 进度
                        if (d.otaStatus == "updating" || d.otaStatus == "pending") {
                            LinearProgressIndicator(
                                progress = { d.otaProgress / 100f },
                                color = MiOrange,
                                trackColor = Color(0xFFF2F3F7),
                                modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
                            Text("OTA 升级中 ${d.otaProgress}%", fontSize = 11.sp,
                                color = MiOrange, modifier = Modifier.padding(top = 4.dp))
                        }

                        // OTA 下发区
                        if (selected == d.id) {
                            HorizontalDivider(
                                Modifier.padding(vertical = 10.dp),
                                color = MaterialTheme.colorScheme.outline)
                            Text("选择固件发起升级", fontSize = 13.sp, color = MiTextDim)
                            firmwares.forEach { fw ->
                                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(34.dp).background(
                                        Color(0xFFE8F0FE), RoundedCornerShape(10.dp)),
                                        contentAlignment = Alignment.Center) {
                                        Text("📦", fontSize = 15.sp)
                                    }
                                    Column(Modifier.padding(start = 10.dp).weight(1f)) {
                                        Text("v${fw.version}", fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium)
                                        Text(fw.name, fontSize = 11.sp, color = MiTextDim)
                                    }
                                    Button(
                                        enabled = !otaBusy && onlineDev,
                                        onClick = {
                                            otaBusy = true
                                            otaMsg = "下发中…"
                                            OtaApi.startOta(d.id, fw.id) { ok, msg ->
                                                otaMsg = msg
                                                otaBusy = false
                                            }
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MiBlue)
                                    ) { Text("升级", fontSize = 12.sp) }
                                }
                            }
                            if (firmwares.isEmpty()) Text("固件库为空（先到平台网页上传固件）",
                                fontSize = 12.sp, color = MiTextDim)
                            if (otaMsg.isNotEmpty()) Text(otaMsg, fontSize = 12.sp, color = MiBlue,
                                modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun Stat(label: String, value: String, modifier: Modifier = Modifier, color: Color = Color.White) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = color)
        Text(label, fontSize = 12.sp, color = Color.White.copy(alpha = 0.75f),
            modifier = Modifier.padding(top = 2.dp))
    }
}
