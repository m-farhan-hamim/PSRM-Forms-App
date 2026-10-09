package com.psrm.forms.psbdx.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PsrmBlue = Color(0xFF2271B1) // matches the PC editor's --psrm-accent
private val PsrmBlueDark = Color(0xFF6BB1E8)

private val LightColors = lightColorScheme(
    primary = PsrmBlue,
    secondary = Color(0xFF7C3AED)
)

private val DarkColors = darkColorScheme(
    primary = PsrmBlueDark,
    secondary = Color(0xFFA78BFA)
)

@Composable
fun PsrmFormsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}
