package com.example.ota

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ota.data.OtaApi
import com.example.ota.ui.dashboard.DashboardScreen
import com.example.ota.ui.device.DeviceDetailScreen
import com.example.ota.ui.devices.DevicesScreen
import com.example.ota.ui.firmware.FirmwareScreen
import com.example.ota.ui.login.LoginScreen
import com.example.ota.ui.logs.LogsScreen
import com.example.ota.ui.provision.ProvisionScreen
import com.example.ota.ui.scan.ScanScreen
import com.example.ota.ui.settings.SettingsScreen
import com.example.ota.ui.theme.*

/** 根路由：未登录 → 登录页；已登录 → 主框架（顶栏扫码 + 底部导航含 ➕） */
@Composable
fun AppRoot() {
    var loggedIn by remember { mutableStateOf(OtaApi.loggedIn) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        OtaApi.meJson { ok -> if (ok) loggedIn = true }
    }

    if (!loggedIn) {
        LoginScreen(onLoginSuccess = { loggedIn = true })
        return
    }

    val nav = rememberNavController()
    val navBackEntry = nav.currentBackStackEntryAsState()
    val currentRoute = navBackEntry.value?.destination?.route

    Scaffold(
        topBar = {
            Row(
                Modifier.fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左上角扫码图标（配网入口）
                Box(
                    Modifier.size(38.dp).background(Color(0xFFE8F0FE), CircleShape)
                        .clickable { nav.navigate("scan") },
                    contentAlignment = Alignment.Center
                ) {
                    Text("⌗", fontSize = 19.sp, color = MiBlue, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(12.dp))
                Text("OTA Hub", fontSize = 19.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground)
                Spacer(Modifier.weight(1f))
                Text(OtaApi.displayName.ifEmpty { "已登录" }, fontSize = 13.sp, color = MiTextDim)
            }
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp) {
                NavItem(Icons.Filled.Home, Icons.Outlined.Home, "首页", "首页", nav)
                NavItem(Icons.Filled.List, Icons.Outlined.List, "设备", "设备", nav)
                // ➕ 中央凸起按钮（添加设备）
                NavigationBarItem(
                    selected = false,
                    onClick = { nav.navigate("scan") },
                    icon = {
                        Box(Modifier.size(46.dp).background(MiBlue, CircleShape),
                            contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Add, contentDescription = "添加设备",
                                tint = Color.White, modifier = Modifier.size(26.dp))
                        }
                    },
                    label = { Text("添加", fontSize = 11.sp, color = MiBlue) })
                NavItem(Icons.Filled.ReceiptLong, Icons.Outlined.ReceiptLong, "日志", "日志", nav)
                NavItem(Icons.Filled.Person, Icons.Outlined.Person, "我的", "我的", nav)
            }
        }
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = "首页",
            modifier = Modifier.padding(padding)
        ) {
            composable("首页") { DashboardScreen() }
            composable("设备") { DevicesScreen(onOpenDevice = { id, name ->
                nav.navigate("device/$id/$name")
            }) }
            composable("日志") { LogsScreen() }
            composable("我的") { SettingsScreen(onLogout = { loggedIn = false }) }
            composable("scan") { ScanScreen(onDeviceProvisioned = { nav.navigate("设备") }) }
            composable("firmware") { FirmwareScreen() }
            composable("device/{deviceId}/{name}") { entry ->
                val id = entry.arguments?.getString("deviceId") ?: ""
                val name = entry.arguments?.getString("name") ?: ""
                DeviceDetailScreen(deviceId = id, deviceName = name,
                    onBack = { nav.popBackStack() })
            }
        }
    }
}

@Composable
private fun RowScope.NavItem(filled: ImageVector, outlined: ImageVector, label: String,
                             route: String, nav: androidx.navigation.NavHostController) {
    val currentRoute = nav.currentBackStackEntryAsState().value?.destination?.route
    val selected = currentRoute == route
    NavigationBarItem(
        selected = selected,
        onClick = {
            nav.navigate(route) {
                popUpTo(nav.graph.startDestinationId) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        },
        icon = {
            Icon(if (selected) filled else outlined, contentDescription = label,
                modifier = Modifier.size(24.dp))
        },
        label = { Text(label, fontSize = 11.sp) },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = MiBlue,
            selectedTextColor = MiBlue,
            unselectedIconColor = Color(0xFF9AA1AB),
            unselectedTextColor = Color(0xFF9AA1AB),
            indicatorColor = Color(0xFFE8F0FE)
        )
    )
}
