package com.example.ota.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── 米家风格色板 ──
val MiBlue = Color(0xFF3478F6)        // 主色（米家蓝）
val MiBlueDark = Color(0xFF2A63D0)
val MiBg = Color(0xFFF5F6F8)          // 页面背景
val MiCard = Color(0xFFFFFFFF)        // 卡片
val MiText = Color(0xFF1A1D21)        // 主文字
val MiTextDim = Color(0xFF8C9299)     // 次要文字
val MiGreen = Color(0xFF34C759)       // 在线
val MiRed = Color(0xFFFA5151)         // 离线/错误
val MiOrange = Color(0xFFFF9F0A)      // 升级中
val MiDivider = Color(0xFFEDEEF2)

private val MiLightColors = lightColorScheme(
    primary = MiBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8F0FE),
    onPrimaryContainer = MiBlue,
    secondary = MiGreen,
    background = MiBg,
    onBackground = MiText,
    surface = MiCard,
    onSurface = MiText,
    surfaceVariant = Color(0xFFF2F3F7),
    onSurfaceVariant = MiTextDim,
    error = MiRed,
    outline = MiDivider
)

private val MiDarkColors = darkColorScheme(
    primary = Color(0xFF5B93FF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1E2A44),
    onPrimaryContainer = Color(0xFFA9C6FF),
    background = Color(0xFF121417),
    onBackground = Color(0xFFE8EAED),
    surface = Color(0xFF1C1F24),
    onSurface = Color(0xFFE8EAED),
    surfaceVariant = Color(0xFF24272D),
    onSurfaceVariant = Color(0xFF9AA1AB),
    error = Color(0xFFFF6B6B),
    outline = Color(0xFF2C3038)
)

@Composable
fun OTATheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) MiDarkColors else MiLightColors,
        content = content
    )
}
