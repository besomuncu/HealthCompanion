package com.besomuncu.healthcompanion.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.delay

@Composable
fun AutoScrollingText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start,
    color: Color = Color.Unspecified
) {
    val scrollState = rememberScrollState()
    
    LaunchedEffect(text) {
        while (true) {
            scrollState.scrollTo(0)
            delay(2000)
            if (scrollState.maxValue > 0) {
                scrollState.animateScrollTo(
                    scrollState.maxValue,
                    animationSpec = tween(
                        durationMillis = (scrollState.maxValue * 20).coerceIn(1000, 10000),
                        easing = LinearEasing
                    )
                )
                delay(2000)
            } else {
                delay(1000)
            }
        }
    }

    Text(
        text = text,
        style = style,
        textAlign = textAlign,
        maxLines = 1,
        softWrap = false,
        color = color,
        modifier = modifier
            .horizontalScroll(scrollState, enabled = false)
    )
}
