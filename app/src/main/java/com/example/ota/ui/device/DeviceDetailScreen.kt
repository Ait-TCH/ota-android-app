package com.example.ota.ui.device

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ota.data.Entity
import com.example.ota.data.OtaApi
import com.example.ota.ui.theme.*

/**
 * 设备详情页（米家风格）：
 * - 实体控制卡（开关/亮度滑条/RGB 色盘/色温，按能力渲染）
 * - 按钮映射列表
 * 实体状态变化即时发 /api/entity/control。
 */
@Composable
fun DeviceDetailScreen(deviceId: String, deviceName: String, onBack: () -> Unit) {
    var entities by remember { mutableStateOf(listOf<Entity>()) }
    var mappings by remember { mutableStateOf(listOf<com.example.ota.data.ButtonMapping>()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var busyEntity by remember { mutableStateOf<String?>(null) }
    var localBrightness by remember { mutableStateOf(mapOf<String, Int>()) }
    var localColor by remember { mutableStateOf(mapOf<String, Color>()) }

    fun load() {
        loading = true
        OtaApi.deviceEntities(deviceId) { ents, maps, err ->
            entities = ents
            mappings = maps
            error = err ?: ""
            loading = false
        }
    }
    LaunchedEffect(deviceId) { load() }

    fun control(entityId: String, action: String, value: String? = null,
                onDone: () -> Unit = {}) {
        busyEntity = entityId
        OtaApi.controlEntity(entityId, action, value) { _, _ ->
            busyEntity = null
            onDone()
        }
    }

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(36.dp).background(MaterialTheme.colorScheme.surface, CircleShape)
                .clickable { onBack() }, contentAlignment = Alignment.Center) {
                Text("←", fontSize = 18.sp)
            }
            Text(deviceName, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 12.dp), maxLines = 1)
        }
        Spacer(Modifier.height(14.dp))

        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth(), color = MiBlue)
        if (error.isNotEmpty()) Text(error, fontSize = 12.sp, color = MiRed)

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Text("设备控制", fontSize = 15.sp, fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 6.dp, bottom = 8.dp))

            entities.forEach { e ->
                val isOn = e.state == "on" || e.state == "ON"
                EntityCard(
                    entity = e,
                    isOn = isOn,
                    busy = busyEntity == e.entityId,
                    brightness = localBrightness[e.entityId]
                        ?: (e.brightness ?: if (isOn) 255 else 0),
                    color = localColor[e.entityId]
                        ?: e.rgbColor?.let { Color(it[0], it[1], it[2]) }
                        ?: Color.White,
                    onToggle = { newOn ->
                        // 本地即时反馈
                        entities = entities.map {
                            if (it.entityId == e.entityId)
                                it.copy(state = if (newOn) "on" else "off") else it
                        }
                        control(e.entityId, if (newOn) "turn_on" else "turn_off")
                    },
                    onBrightness = { v ->
                        // 拖动中只更新本地显示，松手（onValueChangeFinished）才发指令
                        localBrightness = localBrightness + (e.entityId to v)
                    },
                    onBrightnessCommit = { v ->
                        control(e.entityId, "set_brightness", v.toString()) {
                            // 完成后清本地覆盖并刷新真实状态
                            localBrightness = localBrightness - e.entityId
                            load()
                        }
                    },
                    onColor = { c ->
                        localColor = localColor + (e.entityId to c)
                        val hex = String.format("#%02X%02X%02X",
                            (c.red * 255).toInt(), (c.green * 255).toInt(),
                            (c.blue * 255).toInt())
                        control(e.entityId, "set_color", hex) { load() }
                    }
                )
            }

            if (entities.isEmpty() && !loading) {
                Card(shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("💡", fontSize = 34.sp)
                        Text("该设备暂无可控实体", fontSize = 14.sp, fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 8.dp))
                        Text("确认设备已接入 Home Assistant", fontSize = 12.sp,
                            color = MiTextDim, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }

            // ── 按钮映射 ──
            if (mappings.isNotEmpty()) {
                Text("按钮映射", fontSize = 15.sp, fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                Card(shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(14.dp)) {
                        mappings.forEach { m ->
                            Row(Modifier.padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(32.dp).background(
                                    Color(0xFFE8F0FE), CircleShape),
                                    contentAlignment = Alignment.Center) {
                                    Text("BTN${m.btnIndex}", fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold, color = MiBlue)
                                }
                                Text("GPIO ${m.gpio}",
                                    fontSize = 13.sp, modifier = Modifier.padding(start = 10.dp))
                                Spacer(Modifier.weight(1f))
                                Text(m.devices.ifEmpty { "未绑定" }, fontSize = 12.sp,
                                    color = MiTextDim, maxLines = 1)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

/** 实体控制卡片 */
@Composable
fun EntityCard(
    entity: Entity,
    isOn: Boolean,
    busy: Boolean,
    brightness: Int,
    color: Color,
    onToggle: (Boolean) -> Unit,
    onBrightness: (Int) -> Unit,
    onBrightnessCommit: (Int) -> Unit,
    onColor: (Color) -> Unit
) {
    Card(shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isOn) Color(0xFFE8F0FE) else MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).background(
                    if (isOn) color.copy(alpha = 0.9f) else Color(0xFFF2F3F7),
                    CircleShape), contentAlignment = Alignment.Center) {
                    Text(if (isOn) "💡" else "🌙", fontSize = 18.sp)
                }
                Column(Modifier.padding(start = 12.dp).weight(1f)) {
                    Text(entity.friendlyName, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Text(if (isOn) "已开启" else "已关闭", fontSize = 12.sp,
                        color = if (isOn) MiGreen else MiTextDim)
                }
                // iOS 风格开关
                Box(
                    Modifier.size(width = 50.dp, height = 30.dp)
                        .background(
                            if (isOn) MiGreen else Color(0xFFE4E7EC),
                            RoundedCornerShape(15.dp))
                        .clickable { onToggle(!isOn) },
                    contentAlignment = if (isOn) Alignment.CenterEnd else Alignment.CenterStart
                ) {
                    Box(Modifier.padding(horizontal = 2.dp).size(26.dp)
                        .background(Color.White, CircleShape))
                }
            }

            // 亮度滑条
            if (isOn && entity.supportedBrightness) {
                Text("亮度 ${brightness * 100 / 255}%", fontSize = 12.sp, color = MiTextDim,
                    modifier = Modifier.padding(top = 10.dp))
                Slider(
                    value = brightness.toFloat(),
                    onValueChange = { onBrightness(it.toInt()) },
                    onValueChangeFinished = {
                        // 松手才发指令 + 刷新真实状态（拖动中发会被连续 load 打断回弹）
                        onBrightnessCommit(brightness)
                    },
                    valueRange = 3f..255f,
                    colors = SliderDefaults.colors(
                        thumbColor = MiBlue, activeTrackColor = MiBlue),
                    modifier = Modifier.fillMaxWidth())
            }

            // 颜色选择
            if (isOn && entity.supportedColor) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 6.dp)) {
                    Text("颜色", fontSize = 12.sp, color = MiTextDim)
                    Spacer(Modifier.width(10.dp))
                    listOf(
                        Color(0xFFFFFFFF), Color(0xFFFF3B30), Color(0xFFFF9F0A),
                        Color(0xFF34C759), Color(0xFF3478F6), Color(0xFFAF52DE)
                    ).forEach { c ->
                        Box(Modifier.size(30.dp).background(c, CircleShape)
                            .clickable { onColor(c) }
                            .padding(2.dp)
                            .background(Color.Transparent)) {
                            Box(Modifier.fillMaxSize().background(c, CircleShape)
                                .clickable { onColor(c) })
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                }
            }
        }
    }
}
