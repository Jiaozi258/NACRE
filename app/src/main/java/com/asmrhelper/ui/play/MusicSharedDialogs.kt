package com.asmrhelper.ui.play

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.asmrhelper.R
import com.asmrhelper.domain.model.AmbientSource
import com.asmrhelper.player.BinauralPreset
import com.asmrhelper.player.NoiseType
import com.asmrhelper.ui.components.FrostedAlertDialog
import com.asmrhelper.ui.theme.DarkSurfaceVariant
import com.asmrhelper.ui.theme.ErrorRed
import com.asmrhelper.ui.theme.FrostedSurface
import com.asmrhelper.ui.theme.LocalAccentColor
import com.asmrhelper.ui.theme.TextHint
import com.asmrhelper.ui.theme.TextPrimary
import com.asmrhelper.ui.theme.TextSecondary
import com.asmrhelper.ui.theme.appleClickable

/**
 * 沉浸模式与正常模式共用的播放页对话框（工具箱 / 音效器 / 双耳节拍 / 噪音 / 环境音）。
 * 从 MusicPlayScreen 抽出，保证两种播放模式菜单里的功能行为完全一致。
 */

/** 工具箱：环境音 / 双耳节拍 / 噪音 / 空间音频 / 触觉反馈 / 淡出 / 完全停止。 */
@Composable
fun MusicToolboxDialog(
    viewModel: PlayViewModel,
    state: PlayUiState,
    onOpenAmbient: () -> Unit,
    onOpenBinaural: () -> Unit,
    onOpenNoise: () -> Unit,
    onDismiss: () -> Unit
) {
    FrostedAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.play_toolbox_title), color = TextPrimary) },
        text = {
            Column(
                modifier = Modifier
                    .height(360.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                val noiseLabel = if (state.noiseActive) {
                    when (state.noiseType) {
                        "PINK" -> stringResource(R.string.play_noise_pink)
                        "BROWN" -> stringResource(R.string.play_noise_brown)
                        else -> stringResource(R.string.play_noise_white)
                    }
                } else stringResource(R.string.play_noise_generate)
                val spatialLabel = when (state.spatialMode) {
                    "D3" -> stringResource(R.string.play_spatial_3d)
                    "SWEEP" -> stringResource(R.string.play_spatial_sweep)
                    "CIRCLE" -> stringResource(R.string.play_spatial_circle)
                    "WIDE" -> stringResource(R.string.play_spatial_wide)
                    else -> stringResource(R.string.play_spatial_3d_off)
                }
                val binauralLabel = state.binauralPreset?.nameRes
                    ?.let { stringResource(R.string.play_binaural_preset, stringResource(it)) }
                    ?: stringResource(R.string.play_binaural_full)

                val tools = listOf(
                    Triple(stringResource(R.string.play_ambient), state.playerState.isBackgroundPlaying) { onOpenAmbient() },
                    Triple(binauralLabel, state.binauralActive) { onOpenBinaural() },
                    Triple(noiseLabel, state.noiseActive) { onOpenNoise() },
                    Triple(spatialLabel, state.spatialMode != "OFF") { viewModel.cycleSpatialMode() },
                    Triple(stringResource(R.string.play_haptic_feedback), state.hapticEnabled) { viewModel.toggleHaptic() },
                    Triple(stringResource(R.string.play_fade_out), false) { onDismiss(); viewModel.fadeOut(5000L) },
                    Triple(stringResource(R.string.play_stop_fully), false) { onDismiss(); viewModel.stopPlayback() }
                )
                tools.forEach { (label, active, action) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .appleClickable { action() }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = label,
                            color = if (active) LocalAccentColor.current else TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.weight(1f)
                        )
                        if (active) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = LocalAccentColor.current, modifier = Modifier.size(18.dp))
                        }
                    }
                    HorizontalDivider(color = DarkSurfaceVariant.copy(alpha = 0.3f))
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

/** 音效器：场景效果 + 音量阈值 + 播放速度/音调 + 声道平衡 + 三频段均衡器。 */
@Composable
fun MusicEffectsDialog(
    viewModel: PlayViewModel,
    onDismiss: () -> Unit
) {
    val eqLevels by viewModel.eqBandLevels.collectAsStateWithLifecycle()
    val currentScene by viewModel.currentSceneEffect.collectAsStateWithLifecycle()
    val loudness by viewModel.loudnessGain.collectAsStateWithLifecycle()
    val balance by viewModel.stereoBalance.collectAsStateWithLifecycle()
    val playbackSpeed by viewModel.playbackSpeed.collectAsStateWithLifecycle()
    val playbackPitch by viewModel.playbackPitch.collectAsStateWithLifecycle()
    val thresholdMode by viewModel.volumeThresholdMode.collectAsStateWithLifecycle()
    val minThresholdDb by viewModel.minThresholdDb.collectAsStateWithLifecycle()
    val maxThresholdDb by viewModel.maxThresholdDb.collectAsStateWithLifecycle()

    FrostedAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.play_effects_title), color = TextPrimary) },
        text = {
            Column(
                modifier = Modifier
                    .height(420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(stringResource(R.string.play_scene_effects), color = TextSecondary, fontSize = 13.sp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    com.asmrhelper.player.SceneEffect.entries.forEach { scene ->
                        val selected = currentScene == scene
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) LocalAccentColor.current.copy(alpha = 0.2f) else DarkSurfaceVariant)
                                .appleClickable { viewModel.applySceneEffect(scene) }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(stringResource(scene.labelRes), color = if (selected) LocalAccentColor.current else TextSecondary, fontSize = 12.sp)
                        }
                    }
                }

                HorizontalDivider(color = DarkSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 8.dp))
                Text(stringResource(R.string.play_volume_threshold), color = TextSecondary, fontSize = 13.sp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(stringResource(R.string.play_loudness_mode), stringResource(R.string.play_threshold_mode)).forEachIndexed { i, label ->
                        val selected = thresholdMode == i
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) LocalAccentColor.current.copy(alpha = 0.2f) else DarkSurfaceVariant)
                                .appleClickable { viewModel.setVolumeThresholdMode(i) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, color = if (selected) LocalAccentColor.current else TextSecondary, fontSize = 13.sp)
                        }
                    }
                }
                if (thresholdMode == 0) {
                    Text(stringResource(R.string.play_loudness_value, if (loudness == 0) stringResource(R.string.play_off) else "+${loudness / 100}.${(loudness % 100) / 10}dB"), color = TextSecondary, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
                    Slider(
                        value = loudness.toFloat(),
                        onValueChange = { viewModel.setLoudnessGain(it.toInt()) },
                        valueRange = 0f..1000f,
                        steps = 9,
                        colors = SliderDefaults.colors(thumbColor = LocalAccentColor.current, activeTrackColor = LocalAccentColor.current)
                    )
                } else {
                    Text(stringResource(R.string.play_max_threshold, maxThresholdDb), color = TextSecondary, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
                    Slider(
                        value = maxThresholdDb.toFloat(),
                        onValueChange = { viewModel.setMaxThresholdDb(it.toInt()) },
                        valueRange = -40f..0f,
                        steps = 39,
                        colors = SliderDefaults.colors(thumbColor = LocalAccentColor.current, activeTrackColor = LocalAccentColor.current)
                    )
                    Text(stringResource(R.string.play_min_threshold, minThresholdDb), color = TextSecondary, fontSize = 13.sp)
                    Slider(
                        value = minThresholdDb.toFloat(),
                        onValueChange = { viewModel.setMinThresholdDb(it.toInt()) },
                        valueRange = -60f..0f,
                        steps = 59,
                        colors = SliderDefaults.colors(thumbColor = LocalAccentColor.current, activeTrackColor = LocalAccentColor.current)
                    )
                }

                HorizontalDivider(color = DarkSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 8.dp))
                Text(stringResource(R.string.play_speed, "%.2f".format(playbackSpeed)), color = TextSecondary, fontSize = 13.sp)
                Slider(
                    value = playbackSpeed,
                    onValueChange = { viewModel.setPlaybackSpeed(it) },
                    valueRange = 0.5f..2f,
                    colors = SliderDefaults.colors(thumbColor = LocalAccentColor.current, activeTrackColor = LocalAccentColor.current)
                )
                Text(stringResource(R.string.play_pitch, "%.2f".format(playbackPitch)), color = TextSecondary, fontSize = 13.sp)
                Slider(
                    value = playbackPitch,
                    onValueChange = { viewModel.setPlaybackPitch(it) },
                    valueRange = 0.5f..2f,
                    colors = SliderDefaults.colors(thumbColor = LocalAccentColor.current, activeTrackColor = LocalAccentColor.current)
                )

                HorizontalDivider(color = DarkSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 8.dp))
                Text(stringResource(R.string.play_balance, when { balance < -0.05f -> stringResource(R.string.play_balance_left); balance > 0.05f -> stringResource(R.string.play_balance_right); else -> stringResource(R.string.play_balance_center) }), color = TextSecondary, fontSize = 13.sp)
                Slider(
                    value = balance,
                    onValueChange = { viewModel.setStereoBalance(it) },
                    valueRange = -1f..1f,
                    colors = SliderDefaults.colors(thumbColor = LocalAccentColor.current, activeTrackColor = LocalAccentColor.current)
                )

                HorizontalDivider(color = DarkSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 8.dp))
                Text(stringResource(R.string.play_eq_hint), color = TextHint, fontSize = 12.sp)
                val bandLabels = listOf(stringResource(R.string.play_eq_low), stringResource(R.string.play_eq_mid), stringResource(R.string.play_eq_high))
                eqLevels.take(3).forEachIndexed { i, level ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                        Text(bandLabels.getOrElse(i) { stringResource(R.string.play_band, i) }, color = TextSecondary, fontSize = 12.sp, modifier = Modifier.width(44.dp))
                        Slider(
                            value = level,
                            onValueChange = { viewModel.setEqBand(i, it) },
                            valueRange = -10f..10f,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(thumbColor = LocalAccentColor.current, activeTrackColor = LocalAccentColor.current)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_done), color = LocalAccentColor.current) }
        },
        dismissButton = {},
        containerColor = FrostedSurface,
        shape = RoundedCornerShape(16.dp)
    )
}

/** 双耳节拍选择。 */
@Composable
fun MusicBinauralDialog(
    viewModel: PlayViewModel,
    state: PlayUiState,
    onDismiss: () -> Unit
) {
    FrostedAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.play_binaural_title), color = TextPrimary) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                BinauralPreset.PRESETS.groupBy { it.category }.forEach { (category, presets) ->
                    Text(category, color = TextHint, fontSize = 12.sp, modifier = Modifier.padding(vertical = 4.dp))
                    presets.forEach { preset ->
                        val isActive = state.binauralActive && state.binauralPreset == preset
                        TextButton(
                            onClick = { viewModel.toggleBinaural(preset) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (isActive) "▶ ${stringResource(preset.nameRes)}" else "${stringResource(preset.nameRes)}  (${stringResource(preset.descriptionRes)})",
                                color = if (isActive) LocalAccentColor.current else TextSecondary,
                                fontSize = 14.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                if (state.binauralActive) {
                    HorizontalDivider(color = DarkSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.common_volume), color = TextSecondary, fontSize = 12.sp)
                        Slider(
                            value = state.binauralVolume,
                            onValueChange = { viewModel.setBinauralVolume(it) },
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(thumbColor = LocalAccentColor.current, activeTrackColor = LocalAccentColor.current)
                        )
                        Text("${(state.binauralVolume * 100).toInt()}%", color = TextHint, fontSize = 11.sp)
                    }
                    TextButton(
                        onClick = { viewModel.stopBinaural(); onDismiss() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.play_stop_binaural), color = ErrorRed, fontSize = 14.sp)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_close), color = TextSecondary) } },
        containerColor = FrostedSurface,
        shape = RoundedCornerShape(16.dp)
    )
}

/** 噪音生成选择。 */
@Composable
fun MusicNoiseDialog(
    viewModel: PlayViewModel,
    state: PlayUiState,
    onDismiss: () -> Unit
) {
    FrostedAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.play_noise_title), color = TextPrimary) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                listOf(
                    "WHITE" to R.string.play_noise_white_desc,
                    "PINK" to R.string.play_noise_pink_desc,
                    "BROWN" to R.string.play_noise_brown_desc
                ).forEach { (type, descRes) ->
                    val desc = stringResource(descRes)
                    val isActive = state.noiseActive && state.noiseType == type
                    TextButton(
                        onClick = { viewModel.toggleNoise(NoiseType.valueOf(type)) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isActive) "▶ $desc" else desc,
                            color = if (isActive) LocalAccentColor.current else TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
                if (state.noiseActive) {
                    HorizontalDivider(color = DarkSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.common_volume), color = TextSecondary, fontSize = 12.sp)
                        Slider(
                            value = state.noiseVolume,
                            onValueChange = { viewModel.setNoiseVolume(it) },
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(thumbColor = LocalAccentColor.current, activeTrackColor = LocalAccentColor.current)
                        )
                        Text("${(state.noiseVolume * 100).toInt()}%", color = TextHint, fontSize = 11.sp)
                    }
                    TextButton(
                        onClick = { viewModel.stopNoise(); onDismiss() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.play_stop_noise), color = ErrorRed, fontSize = 14.sp)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_close), color = TextSecondary) } },
        containerColor = FrostedSurface,
        shape = RoundedCornerShape(16.dp)
    )
}

/** 环境音选择。 */
@Composable
fun MusicAmbientDialog(
    viewModel: PlayViewModel,
    state: PlayUiState,
    onDismiss: () -> Unit
) {
    FrostedAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.play_ambient_title), color = TextPrimary) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(stringResource(R.string.play_builtin_sounds), color = TextHint, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 4.dp))
                AmbientSource.BUILT_IN.forEach { source ->
                    val isBuiltInSelected = state.selectedAmbientPath == source.sourcePath
                    val isBuiltInPlaying = state.playerState.isBackgroundPlaying && isBuiltInSelected
                    TextButton(
                        onClick = { viewModel.playBuiltInAmbient(source); onDismiss() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(source.labelRes), color = if (isBuiltInSelected) LocalAccentColor.current else TextSecondary, fontSize = 14.sp)
                            Text(
                                text = when { isBuiltInPlaying -> stringResource(R.string.play_playing); isBuiltInSelected -> stringResource(R.string.play_selected); else -> "" },
                                color = if (isBuiltInPlaying) LocalAccentColor.current else TextHint,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                HorizontalDivider(color = DarkSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 8.dp))

                Text(stringResource(R.string.play_imported_ambient), color = TextHint, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 4.dp))

                if (state.ambientAudios.isEmpty()) {
                    Text(stringResource(R.string.play_no_ambient), color = TextHint, fontSize = 14.sp, modifier = Modifier.padding(vertical = 8.dp))
                }
                state.ambientAudios.forEach { path ->
                    val name = path.substringAfterLast('/')
                    val isSelected = state.selectedAmbientPath == path
                    val isPlaying = state.playerState.isBackgroundPlaying && isSelected
                    TextButton(
                        onClick = {
                            if (isSelected && isPlaying) {
                                viewModel.toggleBackground()
                            } else {
                                viewModel.selectAmbientAudio(path)
                            }
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(name, color = if (isSelected) LocalAccentColor.current else TextSecondary, fontSize = 14.sp)
                            Text(
                                text = when { isPlaying -> stringResource(R.string.play_playing); isSelected -> stringResource(R.string.play_selected); else -> "" },
                                color = if (isPlaying) LocalAccentColor.current else TextHint,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                HorizontalDivider(color = DarkSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.play_ambient_loop), color = TextSecondary, fontSize = 14.sp)
                        Text(stringResource(R.string.play_ambient_loop_desc), color = TextHint, fontSize = 11.sp)
                    }
                    Switch(
                        checked = state.playerState.ambientLoopEnabled,
                        onCheckedChange = { viewModel.toggleAmbientLoop() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = LocalAccentColor.current,
                            checkedTrackColor = LocalAccentColor.current.copy(alpha = 0.4f)
                        )
                    )
                }

                if (state.playerState.isBackgroundPlaying) {
                    HorizontalDivider(color = DarkSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 8.dp))
                    TextButton(
                        onClick = { viewModel.toggleBackground(); onDismiss() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.play_stop_ambient), color = ErrorRed, fontSize = 14.sp)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_close), color = TextSecondary) } },
        containerColor = FrostedSurface,
        shape = RoundedCornerShape(16.dp)
    )
}
