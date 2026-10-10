package com.example.ota.ui.logs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ota.data.OtaApi
import com.example.ota.ui.theme.*

/** 操作日志页（与平台网页操作日志一致） */
@Composable
fun LogsScreen() {
    var logs by remember { mutableStateOf(listOf<Map<String, String>>()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        OtaApi.operationLogs { list, _ ->
            logs = list
            loading = false
        }
    }

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        Text("操作日志", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        if (loading) LinearProgressIndicator(
            Modifier.fillMaxWidth().padding(top = 10.dp), color = MiBlue)

        LazyColumn(Modifier.padding(top = 10.dp)) {
            items(logs) { log ->
                Card(shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        Row {
                            Text(log["device"] ?: "-", fontSize = 13.sp,
                                fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                            Text(log["time"] ?: "", fontSize = 11.sp, color = MiTextDim)
                        }
                        Text("${log["action"]} · ${log["result"]}", fontSize = 12.sp,
                            color = if (log["result"] == "success") MiGreen else MiTextDim,
                            modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }
    }
}
