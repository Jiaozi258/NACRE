package com.asmrhelper.ui.play

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.asmrhelper.R
import com.asmrhelper.ui.components.FrostedAlertDialog
import com.asmrhelper.ui.theme.DarkSurfaceVariant
import com.asmrhelper.ui.theme.ErrorRed
import com.asmrhelper.ui.theme.FrostedSurface
import com.asmrhelper.ui.theme.LocalAccentColor
import com.asmrhelper.ui.theme.TextHint
import com.asmrhelper.ui.theme.TextPrimary
import com.asmrhelper.ui.theme.TextSecondary

/** MUSIC 正常模式默认页右上角的「一起听」入口（进入房间后高亮）。 */
@Composable
fun ListenTogetherButton(
    inRoom: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = LocalAccentColor.current
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(if (inRoom) accent.copy(alpha = 0.22f) else DarkSurfaceVariant.copy(alpha = 0.7f))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Group,
            contentDescription = stringResource(R.string.listen_together_title),
            tint = if (inRoom) accent else TextSecondary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.size(5.dp))
        Text(
            text = stringResource(R.string.listen_together_title),
            color = if (inRoom) accent else TextSecondary,
            fontSize = 12.sp
        )
    }
}

/** 「一起听」房间会话对话框：创建 / 加入 / 复制邀请 / 分享 / 离开。 */
@Composable
fun ListenTogetherDialog(
    viewModel: ListenTogetherViewModel,
    onDismiss: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val accent = LocalAccentColor.current

    var nickname by remember(state) { mutableStateOf(state.nickname) }
    var joinError by remember { mutableStateOf(false) }
    var manualMode by remember { mutableStateOf(false) }
    var manualCode by remember { mutableStateOf("") }

    val effectiveNickname: () -> String = {
        nickname.ifBlank { context.getString(R.string.listen_together_default_nickname) }
    }

    fun createRoom() {
        viewModel.setNickname(effectiveNickname())
        viewModel.createRoom()
    }

    fun joinFromClipboard() {
        viewModel.setNickname(effectiveNickname())
        val clip = clipboard.getText()?.text
        val code = clip?.let { viewModel.parseRoomId(it) }
        if (code != null) {
            viewModel.joinRoom(code)
        } else {
            joinError = true
            manualMode = true
        }
    }

    fun joinManual() {
        viewModel.setNickname(effectiveNickname())
        viewModel.joinRoom(manualCode) { ok -> if (!ok) joinError = true }
    }

    fun copyInvite() {
        val text = viewModel.inviteText()
        if (text.isNotBlank()) {
            clipboard.setText(AnnotatedString(text))
            Toast.makeText(context, R.string.listen_together_invite_copied, Toast.LENGTH_SHORT).show()
        }
    }

    fun shareInvite() {
        val text = viewModel.inviteText()
        if (text.isBlank()) return
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(
            Intent.createChooser(intent, context.getString(R.string.listen_together_share_invite))
        )
    }

    FrostedAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.listen_together_title), color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!state.inRoom) {
                    // ── 未加入房间：昵称 + 创建 / 加入 ──
                    OutlinedTextField(
                        value = nickname,
                        onValueChange = { nickname = it },
                        singleLine = true,
                        placeholder = { Text(stringResource(R.string.listen_together_nickname_hint), color = TextHint) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = accent,
                            unfocusedBorderColor = DarkSurfaceVariant,
                            cursorColor = accent
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            onClick = { createRoom() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.listen_together_create_room), color = accent, fontSize = 14.sp)
                        }
                        TextButton(
                            onClick = { joinFromClipboard() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.listen_together_join_room), color = accent, fontSize = 14.sp)
                        }
                    }
                    Text(
                        text = stringResource(R.string.listen_together_join_hint),
                        color = TextHint,
                        fontSize = 11.sp
                    )
                    if (joinError) {
                        Text(
                            text = stringResource(R.string.listen_together_join_failed),
                            color = ErrorRed,
                            fontSize = 12.sp
                        )
                    }
                    if (manualMode) {
                        OutlinedTextField(
                            value = manualCode,
                            onValueChange = { manualCode = it.uppercase() },
                            singleLine = true,
                            placeholder = { Text(stringResource(R.string.listen_together_enter_code), color = TextHint) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = accent,
                                unfocusedBorderColor = DarkSurfaceVariant,
                                cursorColor = accent
                            )
                        )
                        TextButton(onClick = { joinManual() }, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.listen_together_join_room), color = accent, fontSize = 14.sp)
                        }
                    }
                } else {
                    // ── 已在房间：房间号 + 身份 + 复制 / 分享 / 离开 ──
                    Text(
                        text = stringResource(R.string.listen_together_room_id),
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Text(
                        text = state.roomId ?: "",
                        color = accent,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 4.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                    val role = if (state.isHost) {
                        stringResource(R.string.listen_together_role_host)
                    } else {
                        stringResource(R.string.listen_together_role_member)
                    }
                    Text(
                        text = "${state.nickname} · $role",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    Text(
                        text = stringResource(R.string.listen_together_room_hint),
                        color = TextHint,
                        fontSize = 11.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    TextButton(onClick = { copyInvite() }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.listen_together_copy_invite), color = accent, fontSize = 14.sp)
                    }
                    TextButton(onClick = { shareInvite() }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.listen_together_share_invite), color = accent, fontSize = 14.sp)
                    }
                    TextButton(onClick = { viewModel.leaveRoom() }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.listen_together_leave_room), color = ErrorRed, fontSize = 14.sp)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_close), color = TextSecondary) }
        },
        containerColor = FrostedSurface,
        shape = RoundedCornerShape(16.dp)
    )
}
