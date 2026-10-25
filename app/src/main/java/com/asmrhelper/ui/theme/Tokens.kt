package com.asmrhelper.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.unit.dp

/**
 * 设计令牌 —— 视觉与动效的唯一事实来源。
 * 各界面引用这里的值，而非散落的 magic number，保证全局统一质感。
 */

/** 圆角层级：卡片 / 输入框 / 面板 / 全屏弹层。 */
object Radius {
    val sm = 10.dp
    val md = 14.dp
    val lg = 20.dp
    val xl = 28.dp
    val chip = 8.dp
}

/** 留白节奏：4 / 8 / 12 / 16 / 24 dp。 */
object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
}

/** 动效时长（毫秒）：微交互 / 常规 / 页面级。 */
object Motion {
    const val quick = 160
    const val standard = 240
    const val slow = 360
}

/** 卡片标准圆角（= Radius.md）。 */
val CardShape = RoundedCornerShape(Radius.md)

/** chip / 小型选中框圆角（= Radius.chip）。 */
val ChipShape = RoundedCornerShape(Radius.chip)

/**
 * 深色卡片的白描边（磨砂感）。深色 UI 阴影易显脏，改用 1dp 极淡描边 + 层次。
 * 用法：`Card(border = HairlineBorder)`。
 */
val HairlineBorder: BorderStroke
    @Composable @ReadOnlyComposable
    get() = BorderStroke(1.dp, TextPrimary.copy(alpha = 0.08f))
