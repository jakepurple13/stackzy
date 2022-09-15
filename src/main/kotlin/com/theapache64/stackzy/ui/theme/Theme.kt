package com.theapache64.stackzy.ui.theme

import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Color set
val LightTheme = lightColors() // TODO :
val DarkTheme = darkColors(
    primary = R.color.TelegramBlue,
    onPrimary = Color.White,
    secondary = R.color.Elephant,
    onSecondary = Color.White,
    surface = R.color.BigStone,
    error = R.color.WildWatermelon
)
val LightColorTheme = lightColorScheme() // TODO :
val DarkColorTheme = darkColorScheme(
    primary = R.color.TelegramBlue,
    onPrimary = Color.White,
    secondary = R.color.Elephant,
    onSecondary = Color.White,
    surface = R.color.BigStone,
    error = R.color.WildWatermelon
)

@Composable
fun StackzyTheme(
    isDark: Boolean = true,
    content: @Composable () -> Unit
) {
    androidx.compose.material3.MaterialTheme(
        colorScheme = if (isDark) DarkColorTheme else LightColorTheme
    ) {
        MaterialTheme(
            colors = if (isDark) DarkTheme else LightTheme,
            typography = StackzyTypography,
            content = content
        )
    }
}