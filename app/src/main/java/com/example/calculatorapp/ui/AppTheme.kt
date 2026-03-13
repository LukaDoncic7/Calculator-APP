package com.example.calculatorapp.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

@Composable
fun AppTheme(themePreset: ThemePreset, content: @Composable () -> Unit) {
    val colors = when (themePreset) {
        ThemePreset.SYSTEM -> lightColorScheme()
        ThemePreset.LIGHT -> lightColorScheme()
        ThemePreset.DARK -> darkColorScheme()
        ThemePreset.METALLIC -> darkColorScheme(
            primary = androidx.compose.ui.graphics.Color(0xFFB0BEC5),
            secondary = androidx.compose.ui.graphics.Color(0xFF78909C),
            background = androidx.compose.ui.graphics.Color(0xFF263238)
        )
        ThemePreset.OCEAN -> lightColorScheme(
            primary = androidx.compose.ui.graphics.Color(0xFF006994),
            secondary = androidx.compose.ui.graphics.Color(0xFF00A8CC),
            background = androidx.compose.ui.graphics.Color(0xFFE6F7FF)
        )
    }
    MaterialTheme(colorScheme = colors, content = content)
}
