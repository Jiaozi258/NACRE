package com.asmrhelper.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.sin

/**
 * 收藏歌曲时闪过的「心率图」心脏跳动指示符：一条直线，中间一上一下（QRS 尖峰）再回到直线。
 * 受「播放界面特效」开关控制；每次 trigger 变化播放一次（约 1.4s 渐入渐出）。
 */
@Composable
fun HeartbeatEcgOverlay(
    trigger: Long,
    enabled: Boolean,
    color: Color,
    modifier: Modifier = Modifier
) {
    val progress = remember { Animatable(0f) }

    LaunchedEffect(trigger) {
        if (trigger > 0L && enabled) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(durationMillis = 1400, easing = FastOutSlowInEasing))
            progress.snapTo(0f)
        }
    }

    val p = progress.value
    // 单一脉冲：0 → 峰值 → 0，同时承担渐入渐出
    val alpha = (sin(p * PI.toFloat()).coerceIn(0f, 1f)).let { if (enabled) it else 0f }
    if (alpha <= 0.01f) return

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val baselineY = h * 0.5f
        val spikeX = w * 0.62f
        val peakAmp = h * 0.16f * alpha
        val valleyAmp = h * 0.10f * alpha
        val stroke = h * 0.006f

        val path = Path().apply {
            moveTo(0f, baselineY)
            lineTo(spikeX - w * 0.07f, baselineY)
            // 一上一下，再回到直线
            lineTo(spikeX, baselineY - peakAmp)
            lineTo(spikeX + w * 0.05f, baselineY + valleyAmp)
            lineTo(spikeX + w * 0.10f, baselineY)
            lineTo(w, baselineY)
        }

        drawPath(
            path = path,
            color = color.copy(alpha = alpha),
            style = Stroke(width = stroke)
        )
    }
}
