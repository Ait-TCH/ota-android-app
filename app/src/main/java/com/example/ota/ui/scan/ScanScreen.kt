package com.example.ota.ui.scan

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.espressif.provisioning.ESPProvisionManager
import com.espressif.provisioning.listeners.BleScanListener
import com.example.ota.ui.theme.MiBlue
import com.example.ota.ui.theme.MiGreen
import com.example.ota.ui.theme.MiRed
import com.example.ota.ui.theme.MiTextDim

/** 扫码/扫描配网页（从顶部扫码图标进入） */
@Composable
fun ScanScreen(onDeviceProvisioned: () -> Unit) {
    val context = LocalContext.current
    val provisionManager = ESPProvisionManager.getInstance(context)

    var scanning by remember { mutableStateOf(false) }
    var found by remember { mutableStateOf(setOf<String>()) }
    var errorMsg by remember { mutableStateOf("") }

    val perms = listOf(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.ACCESS_FINE_LOCATION
    )
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()) { }

    LaunchedEffect(Unit) {
        val missing = perms.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) permLauncher.launch(missing.toTypedArray())
    }

    fun startScan() {
        val granted = perms.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (!granted) { permLauncher.launch(perms.toTypedArray()); return }

        val bt = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        if (bt.adapter?.isEnabled != true) { errorMsg = "请先打开手机蓝牙"; return }

        scanning = true; found = emptySet(); errorMsg = ""
        provisionManager.searchBleEspDevices("PROV", object : BleScanListener {
            override fun scanStartFailed() { scanning = false; errorMsg = "扫描启动失败" }
            override fun onPeripheralFound(device: android.bluetooth.BluetoothDevice?,
                                           scanResult: android.bluetooth.le.ScanResult?) {
                device?.name?.let { n ->
                    if (n.startsWith("PROV")) {
                        found = found + n
                        scanResult?.scanRecord?.serviceUuids?.firstOrNull()?.let {
                            EspDeviceStore.setServiceUuid(context, n, it.toString())
                        }
                        EspDeviceStore.setBleDevice(context, n, device)
                    }
                }
            }
            override fun scanCompleted() {
                scanning = false
                if (found.isEmpty()) errorMsg = "未发现配网设备，请确认设备处于配网模式（蓝灯快闪）"
            }
            override fun onFailure(e: Exception) {
                scanning = false
                errorMsg = "扫描失败: ${e.message}"
            }
        })
    }

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(20.dp))
        Text("添加设备", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("设备需处于配网模式（蓝灯快闪），通过 BLE 连接后为其配置 WiFi",
            fontSize = 12.sp, color = MiTextDim, modifier = Modifier.padding(top = 4.dp))

        Spacer(Modifier.height(18.dp))

        Button(onClick = { startScan() }, enabled = !scanning,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) { Text(if (scanning) "扫描中…" else "开始扫描配网设备", fontSize = 15.sp) }

        if (errorMsg.isNotEmpty()) {
            Card(shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF4E5)),
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Text(errorMsg, fontSize = 13.sp, color = Color(0xFF9A6A00),
                    modifier = Modifier.padding(12.dp))
            }
        }

        Text("发现的设备", fontSize = 14.sp, fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 18.dp, bottom = 6.dp))

        LazyColumn {
            items(found.toList()) { name ->
                Card(shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)
                        .clickable { onDeviceProvisioned() }) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(46.dp).background(Color(0xFFE8F0FE), CircleShape),
                            contentAlignment = Alignment.Center) { Text("📡", fontSize = 20.sp) }
                        Column(Modifier.padding(start = 14.dp).weight(1f)) {
                            Text(name, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                            Text("等待配网", fontSize = 12.sp, color = MiTextDim)
                        }
                        Text("›", fontSize = 18.sp, color = MiTextDim)
                    }
                }
            }
        }
    }
}
