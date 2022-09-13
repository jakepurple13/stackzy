package com.theapache64.stackzy.ui.common.loading

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
    val transition = rememberInfiniteTransition()

    val rotate by transition.animateFloat(
        0f, 90f,
        infiniteRepeatable(tween(200), repeatMode = RepeatMode.Reverse),
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
                    .rotate(rotate)
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
}

@Composable
fun LoadingAnimation(
    message: String,
    progress: Double
) {
    val transition = rememberInfiniteTransition()

    val rotate by transition.animateFloat(
        0f, 90f,
        infiniteRepeatable(tween(200), repeatMode = RepeatMode.Reverse),
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
                    .rotate(rotate)
                    .align(Alignment.CenterHorizontally)
                    .size(50.dp),
                tint = MaterialTheme.colors.primary,
                contentDescription = ""
            )

            LinearProgressIndicator(
                progress.toFloat(),
                modifier = Modifier.height(15.dp)
            )
        }

        LoadingText(
            modifier = Modifier.align(Alignment.BottomCenter),
            message = message
        )
    }
}
