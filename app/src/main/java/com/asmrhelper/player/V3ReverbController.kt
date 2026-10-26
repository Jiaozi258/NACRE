package com.asmrhelper.player

import android.media.audiofx.PresetReverb
import androidx.annotation.StringRes
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.asmrhelper.R
import com.asmrhelper.di.MainPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 混响预设（正常模式均衡器用，独立于沉浸模式场景效果）。
 */
enum class V3ReverbPreset(@StringRes val labelRes: Int) {
    NONE(R.string.v3_reverb_none),
    STUDIO(R.string.v3_reverb_studio),       // 录音室：紧凑短促
    CHAMBER(R.string.v3_reverb_chamber),     // 室内：亲密小房间
    HALL(R.string.v3_reverb_hall),           // 音乐厅：中型大厅
    CATHEDRAL(R.string.v3_reverb_cathedral), // 大教堂：超长宏大
    PLATE(R.string.v3_reverb_plate),         // 板式：明亮稠密
}

/**
 * 混响控制器。改用 [PresetReverb]（与沉浸模式场景效果同源）：此前用
 * [android.media.audiofx.EnvironmentalReverb] 是「干声原样通过 + 湿声叠加」的
 * 加法式全局效果，只要开启就会抬高整体响度、切换预设瞬间突然大声；其湿声电平参数
 * 无法抵消干声叠加。PresetReverb 预置由音频 HAL 调校为干湿均衡、开启不炸响度，
 * 用户选中预设不会再有「音量突然被放大」的问题。
 *
 * 挂载时机与 [SceneEffectsController] 一致：监听 `onAudioSessionIdChanged`，
 * sessionId 真正可用时重建混响对象并重放当前预设。
 */
@Singleton
class V3ReverbController @Inject constructor(
    @MainPlayer private val mainPlayer: ExoPlayer
) {
    private var reverb: PresetReverb? = null

    private val _currentPreset = MutableStateFlow(V3ReverbPreset.NONE)
    val currentPreset: StateFlow<V3ReverbPreset> = _currentPreset.asStateFlow()

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    init {
        mainPlayer.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                if (audioSessionId > 0) attach() else release()
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY && !_isReady.value) {
                    attach()
                }
            }
        })
        if (mainPlayer.audioSessionId > 0) attach()
    }

    private fun attach() {
        val sessionId = mainPlayer.audioSessionId
        if (sessionId <= 0) return
        release()
        try {
            reverb = PresetReverb(0, sessionId).apply {
                enabled = false
                preset = PresetReverb.PRESET_NONE
            }
            _isReady.value = true
        } catch (_: Exception) {
            reverb = null
            _isReady.value = false
        }
        apply(_currentPreset.value)
    }

    /** 应用混响预设（NONE 关闭混响）。 */
    fun apply(preset: V3ReverbPreset) {
        _currentPreset.value = preset
        val r = reverb ?: return
        try {
            // 先关闭、再设预置、最后开启：避免开启后预置跳变产生爆音/电流声。
            r.enabled = false
            if (preset == V3ReverbPreset.NONE) {
                r.preset = PresetReverb.PRESET_NONE
            } else {
                r.preset = presetOf(preset)
                r.enabled = true
            }
        } catch (_: Exception) {
            r.enabled = false
        }
    }

    private fun presetOf(preset: V3ReverbPreset): Short = when (preset) {
        V3ReverbPreset.NONE -> PresetReverb.PRESET_NONE
        V3ReverbPreset.STUDIO -> PresetReverb.PRESET_SMALLROOM
        V3ReverbPreset.CHAMBER -> PresetReverb.PRESET_MEDIUMROOM
        V3ReverbPreset.HALL -> PresetReverb.PRESET_MEDIUMHALL
        V3ReverbPreset.CATHEDRAL -> PresetReverb.PRESET_LARGEHALL
        V3ReverbPreset.PLATE -> PresetReverb.PRESET_PLATE
    }

    private fun release() {
        try { reverb?.release() } catch (_: Exception) { }
        reverb = null
        _isReady.value = false
    }
}
