package com.asmrhelper.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.DialogProperties
import com.asmrhelper.ui.theme.FrostedContent
import com.asmrhelper.ui.theme.FrostedSurface

/**
 * 磨砂玻璃版 [AlertDialog]。
 *
 * 与 [AlertDialog] 签名一致，但把标题/图标/正文/按钮统一包进 [FrostedContent]，
 * 开启「透明磨砂质感」后：表面为白色磨砂玻璃（[FrostedSurface]），内容文字自动
 * 切为深色，形成真正的浅色磨砂卡片，而非深色半透明。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FrostedAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    shape: Shape = AlertDialogDefaults.shape,
    containerColor: Color = Color.Unspecified,
    iconContentColor: Color = AlertDialogDefaults.iconContentColor,
    titleContentColor: Color = AlertDialogDefaults.titleContentColor,
    textContentColor: Color = AlertDialogDefaults.textContentColor,
    tonalElevation: Dp = AlertDialogDefaults.TonalElevation,
    properties: DialogProperties = DialogProperties()
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = { FrostedContent { confirmButton() } },
        modifier = modifier,
        dismissButton = dismissButton?.let { db -> { FrostedContent { db() } } },
        icon = icon?.let { ic -> { FrostedContent { ic() } } },
        title = title?.let { t -> { FrostedContent { t() } } },
        text = text?.let { tx -> { FrostedContent { tx() } } },
        shape = shape,
        containerColor = if (containerColor == Color.Unspecified) FrostedSurface else containerColor,
        iconContentColor = iconContentColor,
        titleContentColor = titleContentColor,
        textContentColor = textContentColor,
        tonalElevation = tonalElevation,
        properties = properties
    )
}
