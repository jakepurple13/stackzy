package com.theapache64.stackzy.ui.common

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha

@Composable
fun LoadingText(
    message: String,
    modifier: Modifier = Modifier
) {
    var enabled by remember { mutableStateOf(true) }

    val animatedAlpha by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.2f,
        animationSpec = infiniteRepeatable(tween(200), repeatMode = RepeatMode.Reverse),
    )

    Text(
        text = message,
        modifier = modifier.alpha(animatedAlpha)
    )

    LaunchedEffect(Unit) {
        enabled = !enabled
    }
}