package com.example.ota.ui.provision

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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.espressif.provisioning.ESPConstants
import com.espressif.provisioning.ESPProvisionManager
import com.espressif.provisioning.WiFiAccessPoint
import com.espressif.provisioning.listeners.ProvisionListener
import com.espressif.provisioning.listeners.ResponseListener
import com.espressif.provisioning.listeners.WiFiScanListener
import com.example.ota.ui.scan.EspDeviceStore
import com.example.ota.ui.theme.*

/**
 * 配网页（米家风格）：三步向导 —— 连接 → 选 WiFi → 配网进度
 */
@Composable
fun ProvisionScreen(
    deviceName: String,
    transport: String,
    security: Int,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val manager = ESPProvisionManager.getInstance(context)

    var pop by remember { mutableStateOf("") }
    var connecting by remember { mutableStateOf(true) }
    var connected by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }
    var step by remember { mutableStateOf("正在连接设备…") }

    var networks by remember { mutableStateOf(listOf<WiFiAccessPoint>()) }
    var selectedSsid by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var provisioning by remember { mutableStateOf(false) }
    var provisioned by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val secType = if (security == 0) ESPConstants.SecurityType.SECURITY_0
                      else ESPConstants.SecurityType.SECURITY_2
        val dev = if (transport == "ble") {
            val btDevice = EspDeviceStore.getBleDevice(deviceName)
            if (btDevice == null) {
                errorMsg = "蓝牙设备信息丢失，请返回重新扫描"
                connecting = false
                return@LaunchedEffect
            }
            val uuid = EspDeviceStore.getServiceUuid(deviceName)
                ?: "0000ffff-0000-1000-8000-00805f9b34fb"
            val d = manager.createESPDevice(ESPConstants.TransportType.TRANSPORT_BLE, secType)
            d.connectBLEDevice(btDevice, uuid)
            d
        } else {
            val d = manager.createESPDevice(ESPConstants.TransportType.TRANSPORT_SOFTAP, secType)
            d.connectWiFiDevice()
            d
        }

        if (security != 0) {
            try { dev.setProofOfPossession(pop.ifEmpty { "abcd1234" }) } catch (e: Exception) {}
        }

        step = "正在建立安全会话…"
        dev.initSession(object : ResponseListener {
            override fun onSuccess(data: ByteArray?) {
                connected = true
                connecting = false
                step = "连接成功，正在扫描周边 WiFi…"
                dev.scanNetworks(object : WiFiScanListener {
                    override fun onWifiListReceived(list: ArrayList<WiFiAccessPoint>) {
                        networks = list ?: emptyList()
                        step = "选择要绑定的 WiFi"
                    }
                    override fun onWiFiScanFailed(e: Exception?) {
                        errorMsg = "WiFi 扫描失败: ${e?.message}"
                    }
                })
            }
            override fun onFailure(e: Exception?) {
                connecting = false
                errorMsg = "会话建立失败: ${e?.message}（确认设备在配网模式、配网密钥正确）"
            }
        })
    }

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(20.dp))
        // ── 顶部设备信息 ──
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(52.dp).background(Color(0xFFE8F0FE), CircleShape),
                contentAlignment = Alignment.Center) {
                Text("📡", fontSize = 24.sp)
            }
            Column(Modifier.padding(start = 14.dp)) {
                Text(deviceName, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(step, fontSize = 12.sp, color = if (errorMsg.isEmpty()) MiBlue else MiRed)
            }
        }

        Spacer(Modifier.height(20.dp))

        // ── 步骤指示器 ──
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            StepBar("连接", connected || connecting, 1f)
            StepBar("选网", networks.isNotEmpty(), 1f)
            StepBar("配网", provisioned, 1f)
        }

        if (errorMsg.isNotEmpty()) {
            Card(shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF0F0)),
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp)) {
                Text(errorMsg, fontSize = 13.sp, color = MiRed, modifier = Modifier.padding(12.dp))
            }
        }

        if (connecting) {
            LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 16.dp))
            Text("请确保设备已进入配网模式（蓝灯快闪）", fontSize = 12.sp,
                color = MiTextDim, modifier = Modifier.padding(top = 10.dp))
            // POP 输入
            Card(shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("配网密钥（POP）", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    OutlinedTextField(pop, { pop = it },
                        label = { Text("默认 abcd1234") }, singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                }
            }
            return@Column
        }

        if (!connected) return@Column

        // ── WiFi 列表 ──
        Card(shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp)) {
            Column(Modifier.padding(14.dp)) {
                Text("选择 WiFi", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                if (networks.isEmpty()) {
                    Text("暂未扫描到网络", fontSize = 13.sp, color = MiTextDim,
                        modifier = Modifier.padding(top = 8.dp))
                }
            }
        }

        LazyColumn(Modifier.weight(1f).padding(top = 8.dp)) {
            items(networks.sortedByDescending { it.rssi }) { ap ->
                val selected = ap.wifiName == selectedSsid
                Card(shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selected) Color(0xFFE8F0FE)
                                         else MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        .clickable { selectedSsid = ap.wifiName }) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(if (ap.security == 0) "🔓" else "🔒", fontSize = 16.sp)
                        Text(ap.wifiName, modifier = Modifier
                            .padding(start = 10.dp).weight(1f), fontSize = 15.sp)
                        Text("${ap.rssi}", fontSize = 11.sp, color = MiTextDim)
                        if (selected) Text(" ✓", fontSize = 15.sp, color = MiBlue,
                            fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (selectedSsid.isNotEmpty()) {
            Card(shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("WiFi 密码 · $selectedSsid", fontSize = 13.sp, color = MiTextDim)
                    OutlinedTextField(password, { password = it },
                        label = { Text("输入密码") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true, shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                    Button(
                        onClick = {
                            provisioning = true
                            step = "正在配网…"
                            manager.espDevice.provision(selectedSsid, password,
                                object : ProvisionListener {
                                    override fun wifiConfigSent() { step = "凭据已发送" }
                                    override fun wifiConfigFailed(e: Exception?) {
                                        provisioning = false
                                        step = "凭据发送失败: ${e?.message}"
                                    }
                                    override fun wifiConfigApplied() { step = "设备正在连接 WiFi…" }
                                    override fun wifiConfigApplyFailed(e: Exception?) {
                                        provisioning = false
                                        step = "WiFi 连接失败: ${e?.message}"
                                    }
                                    override fun provisioningFailedFromDevice(
                                        f: ESPConstants.ProvisionFailureReason?) {
                                        provisioning = false
                                        step = "设备配网失败: $f"
                                    }
                                    override fun deviceProvisioningSuccess() {
                                        provisioning = false
                                        provisioned = true
                                        step = "配网成功"
                                        android.os.Handler(context.mainLooper).postDelayed({ onDone() }, 1500)
                                    }
                                    override fun onProvisioningFailed(e: Exception?) {
                                        provisioning = false
                                        step = "配网失败: ${e?.message}"
                                    }
                                    override fun createSessionFailed(e: Exception?) {
                                        provisioning = false
                                        step = "会话创建失败: ${e?.message}"
                                    }
                                })
                        },
                        enabled = !provisioning,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(48.dp)
                    ) { Text(if (provisioning) "配网中…" else "开始配网", fontSize = 15.sp) }
                }
            }
        }

        if (provisioned) {
            Card(shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8FBEF)),
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("✅", fontSize = 22.sp)
                    Column(Modifier.padding(start = 10.dp)) {
                        Text("设备配网成功", fontSize = 15.sp, fontWeight = FontWeight.Medium,
                            color = MiGreen)
                        Text("即将进入设备列表…", fontSize = 12.sp, color = MiTextDim)
                    }
                }
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

/** 步骤指示条 */
@Composable
fun androidx.compose.foundation.layout.RowScope.StepBar(label: String, active: Boolean, w: Float) {
    Column(Modifier.weight(w)) {
        Box(Modifier.fillMaxWidth().height(4.dp).background(
            if (active) MiBlue else MaterialTheme.colorScheme.surfaceVariant,
            RoundedCornerShape(2.dp)))
        Text(label, fontSize = 11.sp,
            color = if (active) MiBlue else MiTextDim,
            modifier = Modifier.padding(top = 4.dp))
    }
}
