package com.theapache64.stackzy.ui.common

import androidx.compose.animation.core.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

@Composable
fun LoadingText(
    message: String,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition()

    val animatedAlpha by transition.animateFloat(
        1f, 0.2f,
        infiniteRepeatable(tween(300), repeatMode = RepeatMode.Reverse)
    )

    Text(
        text = message,
        modifier = modifier.graphicsLayer { alpha = animatedAlpha }
    )
}