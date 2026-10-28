package com.asmrhelper.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * 单行标题：文本超过可用宽度时横向滚动（跑马灯）展示完整内容，
 * 未超宽时保持静止。仅用于 ASMR 播放页标题等需要完整展示的场景。
 */
@Composable
fun MarqueeText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = LocalTextStyle.current,
    endPauseMillis: Int = 1600,
) {
    if (text.isEmpty()) return

    val textMeasurer = rememberTextMeasurer()
    val measured = remember(text, style) {
        textMeasurer.measure(
            text = AnnotatedString(text),
            style = style,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip
        )
    }
    val textWidth = measured.size.width

    var containerWidth by remember { mutableIntStateOf(0) }
    val overflowPx = (textWidth - containerWidth).coerceAtLeast(0)

    Box(
        modifier = modifier
            .clipToBounds()
            .onSizeChanged { containerWidth = it.width }
    ) {
        if (overflowPx > 0) {
            ScrollingTitle(text = text, color = color, style = style, overflowPx = overflowPx, endPauseMillis = endPauseMillis)
        } else {
            Text(
                text = text,
                style = style,
                color = color,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip
            )
        }
    }
}

@Composable
private fun ScrollingTitle(
    text: String,
    color: Color,
    style: TextStyle,
    overflowPx: Int,
    endPauseMillis: Int,
) {
    val transition = rememberInfiniteTransition(label = "marquee")
    val offset by transition.animateFloat(
        initialValue = 0f,
        targetValue = -overflowPx.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (overflowPx * 30).coerceAtLeast(1),
                delayMillis = endPauseMillis,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "marqueeOffset"
    )
    Text(
        text = text,
        style = style,
        color = color,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
        modifier = Modifier.offset { IntOffset(offset.roundToInt(), 0) }
    )
}
