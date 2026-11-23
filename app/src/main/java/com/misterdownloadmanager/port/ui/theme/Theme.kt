package com.misterdownloadmanager.port.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

val MdmBlue = Color(0xFF2A78D6)
val MdmBlueDark = Color(0xFF1E5FA8)
val MdmGreen = Color(0xFF2E9E5B)
val MdmRed = Color(0xFFD64545)
val MdmAmber = Color(0xFFE8A13A)

private val LightColors = lightColorScheme(
    primary = MdmBlue,
    secondary = MdmBlueDark,
    tertiary = MdmGreen,
    error = MdmRed
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7FB3EC),
    secondary = Color(0xFF9CC6F0),
    tertiary = Color(0xFF6CC48F),
    error = Color(0xFFF08080)
)

@Composable
fun MisterDownloadManagerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
