package com.example.ota.ui.firmware

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
import com.example.ota.data.Firmware
import com.example.ota.data.OtaApi
import com.example.ota.ui.theme.*

/** 固件库（米家风格） */
@Composable
fun FirmwareScreen() {
    var firmwares by remember { mutableStateOf(listOf<Firmware>()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        OtaApi.firmwares { list, err ->
            firmwares = list
            error = err ?: ""
            loading = false
        }
    }

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("固件库", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = {
                loading = true
                OtaApi.firmwares { list, err -> firmwares = list; error = err ?: ""; loading = false }
            }) { Text("刷新", fontSize = 13.sp, color = MiBlue) }
        }

        Spacer(Modifier.height(14.dp))

        Card(shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MiBlue),
            modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp)) {
                Text("共 ${firmwares.size} 个固件", fontSize = 22.sp,
                    fontWeight = FontWeight.Bold, color = Color.White)
                Text("固件上传请使用 OTA 平台网页端", fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.75f), modifier = Modifier.padding(top = 4.dp))
            }
        }

        if (loading) LinearProgressIndicator(
            Modifier.fillMaxWidth().padding(top = 10.dp), color = MiBlue)
        if (error.isNotEmpty()) Text(error, fontSize = 12.sp, color = MiRed,
            modifier = Modifier.padding(top = 8.dp))

        LazyColumn(Modifier.padding(top = 10.dp)) {
            items(firmwares) { fw ->
                Card(shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(44.dp).background(Color(0xFFE8F0FE), CircleShape),
                            contentAlignment = Alignment.Center) {
                            Text("📦", fontSize = 19.sp)
                        }
                        Column(Modifier.padding(start = 14.dp).weight(1f)) {
                            Text("v${fw.version}", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            Text(fw.name, fontSize = 12.sp, color = MiTextDim,
                                modifier = Modifier.padding(top = 2.dp))
                        }
                        Text("${fw.size / 1024}KB", fontSize = 12.sp, color = MiTextDim)
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}
