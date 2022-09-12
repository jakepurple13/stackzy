package com.theapache64.stackzy.ui.common.loading

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import com.theapache64.stackzy.data.remote.FunFact
import com.theapache64.stackzy.ui.common.LoadingText
import com.theapache64.stackzy.ui.common.loading.funfact.FunFact

/**
 * To show a rotating icon at the center and blinking text at the bottom of the screen
 */
@Composable
fun LoadingAnimation(
    message: String,
    funFacts: Set<FunFact>?
) {

    var isRotated by remember { mutableStateOf(false) }

    val animatedRotation by animateFloatAsState(
        targetValue = if (isRotated) 0f else 90f,
        animationSpec = infiniteRepeatable(tween(200), repeatMode = RepeatMode.Reverse),
    )

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
        ) {
            Icon(
                Icons.Default.Autorenew,
                modifier = Modifier
                    .rotate(animatedRotation)
                    .align(Alignment.CenterHorizontally)
                    .size(50.dp),
                tint = MaterialTheme.colors.primary,
                contentDescription = ""
            )

            if (funFacts != null) {
                Spacer(modifier = Modifier.height(15.dp))
                FunFact(funFacts)
            }
        }

        LoadingText(
            modifier = Modifier.align(Alignment.BottomCenter),
            message = message
        )
    }

    LaunchedEffect(Unit) {
        // Ignite the animation
        isRotated = !isRotated
    }
}
