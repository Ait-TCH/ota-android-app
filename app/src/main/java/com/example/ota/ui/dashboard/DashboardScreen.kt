package com.example.ota.ui.dashboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.filled.Countertops
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Weekend
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ota.data.Device
import com.example.ota.data.OtaApi
import com.example.ota.ui.theme.*

/**
 * 首页 v4：
 * - 在线设备：绿色"在线"标签醒目显示
 * - 开灯背景 = 淡蓝色（不再是黄色）
 * - 能力感知控件：RGB 灯（开关+色盘+亮度）/ 单色灯（开关+亮度）/ 继电器（开关）
 */
@Composable
fun DashboardScreen() {
    var devices by remember { mutableStateOf(listOf<Device>()) }
    var loading by remember { mutableStateOf(true) }
    var rooms by remember { mutableStateOf(listOf("全屋")) }
    var currentRoom by remember { mutableStateOf("全屋") }
    var switchState by remember { mutableStateOf(mapOf<String, Boolean>()) }
    var controlling by remember { mutableStateOf(setOf<String>()) }

    // 设备能力缓存：deviceId -> Capabilities
    var caps by remember { mutableStateOf(mapOf<String, DevCaps>()) }

    fun loadCaps(d: Device) {
        if (!d.online) return
        OtaApi.deviceEntities(d.id) { ents, _, _ ->
            if (ents.isNotEmpty()) {
                caps = caps + (d.id to DevCaps(
                    hasColor = ents.any { it.supportedColor },
                    hasBrightness = ents.any { it.supportedBrightness },
                    hasColorTemp = ents.any { it.supportedColorTemp },
                    // 主灯实体（第一个 light）
                    mainEntityId = ents.firstOrNull()?.entityId ?: ""
                ))
            }
        }
    }

    fun load() {
        loading = true
        OtaApi.devices { list, _ ->
            devices = list
            val roomsSet = mutableSetOf("全屋")
            list.forEach { d ->
                val n = d.name
                if (n.contains("卧室")) roomsSet.add("卧室")
                if (n.contains("客厅")) roomsSet.add("客厅")
                if (n.contains("厨")) roomsSet.add("厨房")
                if (n.contains("卫生间") || n.contains("浴室")) roomsSet.add("卫生间")
                if (n.contains("书房")) roomsSet.add("书房")
            }
            rooms = roomsSet.toList()
            // 异步拉取每个在线设备的能力
            list.filter { it.status == "online" }.forEach { loadCaps(it) }
            loading = false
        }
    }
    LaunchedEffect(Unit) { load() }

    val filtered = if (currentRoom == "全屋") devices
                   else devices.filter { d -> d.name.contains(currentRoom) }

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(12.dp))

        // ── 房间标签栏 ──
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                rooms.take(4).forEach { room ->
                    val active = room == currentRoom
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(
                                if (active) Color(0xFFE8F0FE) else Color.Transparent,
                                RoundedCornerShape(20.dp))
                            .clickable { currentRoom = room }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(roomIcon(room), contentDescription = room,
                            tint = if (active) MiBlue else MiTextDim,
                            modifier = Modifier.size(16.dp))
                        Text(room, fontSize = if (active) 15.sp else 14.sp,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                            color = if (active) MiBlue else MiTextDim,
                            modifier = Modifier.padding(start = 4.dp))
                    }
                }
            }
            Icon(Icons.Default.Settings, contentDescription = "设置",
                tint = MiTextDim, modifier = Modifier.size(22.dp)
                    .clickable { currentRoom = "全屋" })
        }

        Spacer(Modifier.height(10.dp))

        if (loading) {
            LinearProgressIndicator(Modifier.fillMaxWidth(), color = MiBlue)
        } else if (filtered.isEmpty()) {
            EmptyCard()
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f).padding(top = 4.dp)
        ) {
            items(filtered, key = { it.id }) { d ->
                val on = switchState[d.id] ?: false
                val devCaps = caps[d.id] ?: DevCaps()
                DeviceTile(
                    name = d.name.ifEmpty { d.id.take(8).uppercase() },
                    sub = if (d.status == "online") "在线 · v${d.firmware}" else "离线",
                    isOn = on && d.status == "online",
                    online = d.status == "online",
                    busy = controlling.contains(d.id),
                    icon = deviceIcon(d),
                    caps = devCaps,
                    onToggle = {
                        val newOn = !on
                        switchState = switchState + (d.id to newOn)
                        controlling = controlling + d.id
                        val mainEntity = devCaps.mainEntityId
                        if (mainEntity.isNotEmpty()) {
                            OtaApi.controlEntity(
                                mainEntity,
                                if (newOn) "turn_on" else "turn_off"
                            ) { _, _ -> controlling = controlling - d.id }
                        } else {
                            controlling = controlling - d.id
                        }
                    },
                    onColor = { c ->
                        val r = (c.red * 255).toInt()
                        val g = (c.green * 255).toInt()
                        val b = (c.blue * 255).toInt()
                        val mainEntity = devCaps.mainEntityId
                        if (mainEntity.isNotEmpty()) {
                            controlling = controlling + d.id
                            OtaApi.controlEntity(
                                mainEntity, "set_color",
                                "{\"r\":$r,\"g\":$g,\"b\":$b}"
                            ) { _, _ -> controlling = controlling - d.id }
                        }
                    },
                    onOpen = { }
                )
            }
        }
    }
}

/** 设备能力（聚合自 entities） */
data class DevCaps(
    val hasColor: Boolean = false,
    val hasBrightness: Boolean = false,
    val hasColorTemp: Boolean = false,
    val mainEntityId: String = ""
)

@Composable
fun DeviceTile(
    name: String, sub: String, isOn: Boolean, online: Boolean,
    busy: Boolean, icon: ImageVector, caps: DevCaps,
    onToggle: () -> Unit, onColor: (Color) -> Unit, onOpen: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "scale")

    // 开灯 = 淡蓝色背景（不再黄色）
    val bg by animateColorAsState(
        targetValue = if (isOn && online) Color(0xFFEAF2FF) else MaterialTheme.colorScheme.surface,
        animationSpec = spring(stiffness = Spring.StiffnessLow), label = "bg")

    val iconTint by animateColorAsState(
        targetValue = when {
            !online -> Color(0xFFD0D5DC)
            isOn -> MiBlue
            else -> Color(0xFFB9C0CA)
        }, animationSpec = spring(stiffness = Spring.StiffnessLow), label = "icon")

    val powerBg by animateColorAsState(
        targetValue = when {
            busy -> Color(0xFFE4E7EC)
            isOn && online -> MiBlue
            else -> Color(0xFFF2F3F7)
        }, animationSpec = spring(stiffness = Spring.StiffnessLow), label = "power")

    Card(shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = bg),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isOn && online) 6.dp else 1.dp),
        modifier = Modifier.fillMaxWidth()
            .scale(scale)
            .clickable(interactionSource = interaction, indication = null) { onOpen() }) {

        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(contentAlignment = Alignment.Center) {
                    if (isOn && online) {
                        Box(Modifier.size(50.dp).background(
                            Brush.radialGradient(listOf(
                                Color(0x4489B8FF), Color.Transparent)),
                            CircleShape))
                    }
                    Icon(icon, contentDescription = null,
                        tint = iconTint, modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier.size(38.dp)
                        .background(powerBg, CircleShape)
                        .clickable(enabled = !busy && online) { onToggle() },
                    contentAlignment = Alignment.Center
                ) {
                    if (busy) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Filled.PowerSettingsNew, contentDescription = "开关",
                            tint = if (isOn && online) Color.White else Color(0xFFB9C0CA),
                            modifier = Modifier.size(19.dp))
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            Text(name, fontSize = 15.sp, fontWeight = FontWeight.Medium,
                color = if (isOn && online) Color(0xFF1A4FA8) else MaterialTheme.colorScheme.onSurface,
                maxLines = 1, overflow = TextOverflow.Ellipsis)

            // 状态行：在线绿色标签 / 离线
            Row(verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 3.dp)) {
                if (online) {
                    Box(Modifier.background(Color(0xFFE1F7EA), RoundedCornerShape(4.dp))
                        .padding(horizontal = 5.dp, vertical = 1.dp)) {
                        Text("在线", fontSize = 10.sp, color = Color(0xFF1FA05C),
                            fontWeight = FontWeight.Medium)
                    }
                    Spacer(Modifier.width(6.dp))
                    Text("v" + sub.substringAfter("v"), fontSize = 11.sp, color = MiTextDim)
                } else {
                    Text("离线", fontSize = 11.sp, color = MiRed)
                }
            }

            // ── 能力感知控件 ──
            if (isOn && online && (caps.hasColor || caps.hasBrightness)) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 8.dp)) {
                    // RGB 色盘（快速色点）
                    if (caps.hasColor) {
                        Icon(Icons.Default.Palette, contentDescription = "颜色",
                            tint = MiTextDim, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        listOf(
                            Color(0xFFFFFFFF), Color(0xFFFF5252), Color(0xFF448AFF),
                            Color(0xFF66BB6A), Color(0xFFFFEB3B), Color(0xFFAB47BC)
                        ).forEach { c ->
                            Box(Modifier.size(20.dp).background(c, CircleShape)
                                .clickable { onColor(c) }
                                .border(1.dp, Color(0xFFE4E7EC), CircleShape))
                            Spacer(Modifier.width(5.dp))
                        }
                    }
                }
                // 亮度滑条（压缩版）
                if (caps.hasBrightness) {
                    var sliderVal by remember { mutableStateOf(128f) }
                    Slider(
                        value = sliderVal,
                        onValueChange = { sliderVal = it },
                        onValueChangeFinished = {
                            onBrightnessLocal(sliderVal.toInt())
                        },
                        valueRange = 3f..255f,
                        colors = SliderDefaults.colors(
                            thumbColor = MiBlue, activeTrackColor = MiBlue),
                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp))
                }
            }
        }
    }
}

/** 亮度滑条松手回调（由 DeviceTile 持有的 caps 提供 mainEntityId，
 *  这里通过外部闭包注入 —— 见调用处参数）
 */
var onBrightnessLocal: (Int) -> Unit = {}

/** 按设备名推断设备图标 */
private fun deviceIcon(d: Device): ImageVector = when {
    d.name.contains("switch", true) || d.name.contains("开关", true) ->
        Icons.Filled.PowerSettingsNew
    else -> Icons.Filled.Lightbulb
}

/** 按房间名返回图标 */
private fun roomIcon(room: String): ImageVector = when {
    room.contains("卧室") -> Icons.Filled.Bed
    room.contains("客厅") -> Icons.Filled.Weekend
    room.contains("厨房") -> Icons.Filled.Countertops
    else -> Icons.Filled.Home
}

@Composable
fun EmptyCard() {
    Card(shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(36.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.WifiOff, contentDescription = null,
                tint = MiTextDim, modifier = Modifier.size(44.dp))
            Text("暂无设备", fontSize = 16.sp, fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 10.dp))
            Text("通过顶部扫码添加新设备", fontSize = 12.sp, color = MiTextDim,
                modifier = Modifier.padding(top = 4.dp))
        }
    }
}
