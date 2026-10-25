package com.asmrhelper.player

import android.media.audiofx.EnvironmentalReverb
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
 * V3 混响预设：基于 [EnvironmentalReverb]（环境混响，独立于场景效果的 [android.media.audiofx.PresetReverb]），
 * 通过房间体积/衰减/早期反射/扩散等物理参数合成更真实、更立体的空间感。
 */
enum class V3ReverbPreset(@StringRes val labelRes: Int) {
    NONE(R.string.v3_reverb_none),
    STUDIO(R.string.v3_reverb_studio),       // 录音室：紧凑短促
    CHAMBER(R.string.v3_reverb_chamber),     // 室内：亲密小房间
    HALL(R.string.v3_reverb_hall),           // 音乐厅：中型大厅
    CATHEDRAL(R.string.v3_reverb_cathedral), // 大教堂：超长宏大
    PLATE(R.string.v3_reverb_plate),         // 板式：明亮稠密
}

/** EnvironmentalReverb 物理参数。mB = dB×100，per mille = 千分比。 */
private data class V3Params(
    val roomLevel: Int,
    val roomHFLevel: Int,
    val decayTime: Int,
    val decayHFRatio: Int,
    val reflectionsLevel: Int,
    val reflectionsDelay: Int,
    val reverbLevel: Int,
    val reverbDelay: Int,
    val diffusion: Int,
    val density: Int,
)

/**
 * V3 混响控制器。挂载时机与 [SceneEffectsController] 一致：监听
 * `onAudioSessionIdChanged`，sessionId 真正可用时重建混响对象并重放当前预设。
 */
@Singleton
class V3ReverbController @Inject constructor(
    @MainPlayer private val mainPlayer: ExoPlayer
) {
    private var reverb: EnvironmentalReverb? = null

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
            reverb = EnvironmentalReverb(0, sessionId).apply { enabled = false }
            _isReady.value = true
        } catch (_: Exception) {
            reverb = null
            _isReady.value = false
        }
        apply(_currentPreset.value)
    }

    /** 应用 V3 混响预设（NONE 关闭混响）。 */
    fun apply(preset: V3ReverbPreset) {
        _currentPreset.value = preset
        val r = reverb ?: return
        try {
            if (preset == V3ReverbPreset.NONE) {
                r.enabled = false
                return
            }
            val p = paramsOf(preset)
            // 先关闭、再设参数、最后开启：避免开启后参数逐个跳变产生爆音/电流声。
            r.enabled = false
            r.setRoomLevel(p.roomLevel.toShort())
            r.setRoomHFLevel(p.roomHFLevel.toShort())
            r.setDecayTime(p.decayTime)
            r.setDecayHFRatio(p.decayHFRatio.toShort())
            r.setReflectionsLevel(p.reflectionsLevel.toShort())
            r.setReflectionsDelay(p.reflectionsDelay)
            r.setReverbLevel(p.reverbLevel.toShort())
            r.setReverbDelay(p.reverbDelay)
            r.setDiffusion(p.diffusion.toShort())
            r.setDensity(p.density.toShort())
            r.enabled = true
        } catch (_: Exception) {
            r.enabled = false
        }
    }

    private fun paramsOf(preset: V3ReverbPreset): V3Params = when (preset) {
        V3ReverbPreset.NONE -> V3Params(0, 0, 0, 1000, 0, 0, 0, 0, 1000, 1000)
        // 空间感参数整体上调（较原版更靠近 0 dB / 更长衰减），让各预设差异更明显、可闻。
        V3ReverbPreset.STUDIO -> V3Params(-1200, -1400, 250, 500, -800, 5, -1200, 15, 800, 800)
        V3ReverbPreset.CHAMBER -> V3Params(-1000, -1200, 450, 600, -700, 8, -1000, 12, 750, 850)
        V3ReverbPreset.HALL -> V3Params(-500, -700, 1500, 600, -400, 25, -700, 35, 900, 1000)
        V3ReverbPreset.CATHEDRAL -> V3Params(-250, -500, 3500, 700, -300, 40, -500, 50, 1000, 1000)
        V3ReverbPreset.PLATE -> V3Params(-700, -400, 700, 900, -500, 5, -900, 8, 1000, 1000)
    }

    private fun release() {
        try { reverb?.release() } catch (_: Exception) { }
        reverb = null
        _isReady.value = false
    }
}
