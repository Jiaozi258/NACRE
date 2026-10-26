package com.asmrhelper.player

import android.media.audiofx.BassBoost
import android.media.audiofx.DynamicsProcessing
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.PresetReverb
import android.os.Build
import androidx.media3.common.Player
import androidx.annotation.StringRes
import androidx.media3.exoplayer.ExoPlayer
import com.asmrhelper.R
import com.asmrhelper.di.MainPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** 场景效果预设 */
enum class SceneEffect(@StringRes val labelRes: Int) {
    NONE(R.string.scene_effect_none),
    BLANKET(R.string.scene_effect_blanket),   // 闷声包裹感
    DISTORTED(R.string.scene_effect_distorted), // 黑胶失真感
    CONCERT(R.string.scene_effect_concert),   // 大厅混响
    BATHROOM(R.string.scene_effect_bathroom), // 小房间混响
    THEATER(R.string.scene_effect_theater),   // 中型厅混响
}

/**
 * 场景效果控制器：基于 Android 音频特效 API 合成场景预设。
 * 使用 PresetReverb（混响）、BassBoost（低音增强）、LoudnessEnhancer（响度）。
 */
@Singleton
class SceneEffectsController @Inject constructor(
    @MainPlayer private val mainPlayer: ExoPlayer
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var initJob: Job? = null

    private var reverb: PresetReverb? = null
    private var bassBoost: BassBoost? = null
    private var loudness: LoudnessEnhancer? = null
    private var dynamics: DynamicsProcessing? = null

    // 阈值模式状态：mode 0=响度（LoudnessEnhancer），1=阈值（DynamicsProcessing 限幅 + 噪声门）。
    private var thresholdMode = 0
    private var maxThresholdDb = -10
    private var minThresholdDb = -40

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _currentScene = MutableStateFlow(SceneEffect.NONE)
    val currentScene: StateFlow<SceneEffect> = _currentScene.asStateFlow()

    private val _loudnessGain = MutableStateFlow(0)
    val loudnessGain: StateFlow<Int> = _loudnessGain.asStateFlow() // dB * 100

    init {
        // 用 Player.Listener 监听 audioSessionId 变化作为挂载时机，而不是只在
        // STATE_READY 挂一次：audioSessionId 由底层 AudioTrack 运行时产生，可能与
        // STATE_READY 回调竞态（那一刻 sessionId 仍是 0），导致音效对象永久挂不上、
        // 场景/响度全程静默失效。onAudioSessionIdChanged 在 sessionId 真正可用时必然触发。
        mainPlayer.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                if (audioSessionId > 0) attachEffects() else releaseEffects()
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY && !_isReady.value) {
                    attachEffects()
                }
            }
        })
        if (mainPlayer.audioSessionId > 0) attachEffects()
    }

    private fun attachEffects() {
        val sessionId = mainPlayer.audioSessionId
        if (sessionId <= 0) return
        // sessionId 变化时旧对象已失效，先释放再重建
        releaseEffects()
        try {
            reverb = PresetReverb(0, sessionId).apply {
                enabled = false
                preset = PresetReverb.PRESET_NONE
            }
        } catch (_: Exception) { reverb = null }
        try {
            bassBoost = BassBoost(0, sessionId).apply { enabled = false }
        } catch (_: Exception) { bassBoost = null }
        try {
            loudness = LoudnessEnhancer(sessionId).apply {
                enabled = false
                setTargetGain(0)
            }
        } catch (_: Exception) { loudness = null }
        try {
            dynamics = createDynamics(sessionId)
        } catch (_: Exception) { dynamics = null }
        _isReady.value = true
        // 迟到挂载后必须把用户已选的场景/响度/阈值重新应用一次，否则选择会丢失
        applyScene(_currentScene.value)
        applyDynamics()
    }

    /** 应用场景预设（只控制混响/低音；响度由 [setLoudnessGain] 独立控制） */
    fun applyScene(scene: SceneEffect) {
        _currentScene.value = scene
        // 先关闭、再设参数、最后开启：避免「开启后再跳参数」在切换瞬间产生爆音/电流声。
        reverb?.let { it.enabled = false }
        bassBoost?.let { it.enabled = false }
        when (scene) {
            SceneEffect.NONE -> {
                // 保持关闭即可
            }
            SceneEffect.BLANKET -> {
                // 被窝声：低音增强 + 小房间混响（闷声包裹感）
                bassBoost?.apply {
                    setStrength(700.toShort())
                    enabled = true
                }
                reverb?.apply {
                    preset = PresetReverb.PRESET_SMALLROOM
                    enabled = true
                }
            }
            SceneEffect.DISTORTED -> {
                // 失真声：低音增强 + 无混响（颗粒感）
                bassBoost?.apply {
                    setStrength(500.toShort())
                    enabled = true
                }
                reverb?.let { it.enabled = false }
            }
            SceneEffect.CONCERT -> {
                // 演唱会：大厅混响
                reverb?.apply {
                    preset = PresetReverb.PRESET_LARGEHALL
                    enabled = true
                }
                bassBoost?.apply {
                    setStrength(300.toShort())
                    enabled = true
                }
            }
            SceneEffect.BATHROOM -> {
                // 浴室：小房间混响
                reverb?.apply {
                    preset = PresetReverb.PRESET_SMALLROOM
                    enabled = true
                }
                bassBoost?.let { it.enabled = false }
            }
            SceneEffect.THEATER -> {
                // 影院：中型厅混响
                reverb?.apply {
                    preset = PresetReverb.PRESET_MEDIUMHALL
                    enabled = true
                }
                bassBoost?.let { it.enabled = false }
            }
        }
        applyLoudness()
    }

    /** 响度作为独立功能：只要用户设过增益就作用于 LoudnessEnhancer，与场景无关。 */
    fun setLoudnessGain(gainDbX100: Int) {
        val clamped = gainDbX100.coerceIn(0, 1000)
        _loudnessGain.value = clamped
        applyLoudness()
    }

    private fun applyLoudness() {
        val gain = _loudnessGain.value
        loudness?.apply {
            // 阈值模式与响度模式互斥：阈值模式下关闭 LoudnessEnhancer。
            if (gain > 0 && thresholdMode == 0) {
                // 先设目标增益再开启，避免开启后增益跳变产生爆音
                setTargetGain(gain)
                enabled = true
            } else {
                enabled = false
            }
        }
    }

    /** 设置音量阈值模式：0=响度, 1=阈值（限幅器 + 噪声门）。 */
    fun setThresholdMode(mode: Int) {
        thresholdMode = if (mode == 1) 1 else 0
        applyDynamics()
        applyLoudness()
    }

    /** 设置最大阈值（限幅器阈值，dB）：高于该电平的峰值被压限。 */
    fun setMaxThreshold(db: Int) {
        maxThresholdDb = db.coerceIn(-60, 0)
        applyDynamics()
    }

    /** 设置最小阈值（噪声门，dB）：低于该电平的信号被衰减为底噪抑制。 */
    fun setMinThreshold(db: Int) {
        minThresholdDb = db.coerceIn(-80, -1)
        applyDynamics()
    }

    /** 创建 DynamicsProcessing（API 28+）。不支持时返回 null，阈值模式退化为响度模式。 */
    private fun createDynamics(sessionId: Int): DynamicsProcessing? {
        if (Build.VERSION.SDK_INT < 28) return null
        return try {
            // 初始 config 声明启用 MBC（噪声门）+ Limiter（限幅）两段；
            // 具体参数由 applyDynamics() 用运行时 setter 实时更新，避免拖动滑块时重建特效。
            val config = DynamicsProcessing.Config.Builder(
                DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
                2,          // channelCount（立体声）
                false, 0,   // preEqInUse, preEqBandCount
                true, 1,    // mbcInUse, mbcBandCount
                false, 0,   // postEqInUse, postEqBandCount
                true        // limiterInUse
            )
                .setMbcAllChannelsTo(DynamicsProcessing.Mbc(true, false, 1))
                .setLimiterAllChannelsTo(
                    DynamicsProcessing.Limiter(true, false, 0, 1f, 100f, 2f, 0f, 0f)
                )
                .build()
            DynamicsProcessing(0, sessionId, config).apply { enabled = false }
        } catch (_: Exception) { null }
    }

    /** 将当前阈值参数通过运行时 setter 应用到 DynamicsProcessing（阈值模式下启用）。 */
    private fun applyDynamics() {
        val dp = dynamics ?: return
        try {
            if (thresholdMode == 1) {
                // 噪声门（最小阈值）：单段 MBC 扩展器
                dp.setMbcAllChannelsTo(DynamicsProcessing.Mbc(true, true, 1))
                dp.setMbcBandAllChannelsTo(
                    0,
                    DynamicsProcessing.MbcBand(
                        true,                     // enabled
                        20000f,                   // cutoffFrequency：单段覆盖全频段
                        5f, 200f,                 // attack/release (ms)
                        1f,                       // ratio（不压缩）
                        minThresholdDb.toFloat(), // threshold (dB)
                        0f,                       // knee width
                        minThresholdDb.toFloat(), // noise gate threshold (dB)
                        50f,                      // expander ratio（强扩展 = 噪声门）
                        0f, 0f                    // pre/post gain
                    )
                )
                // 限幅器（最大阈值）：高压缩比 + 快速起音
                dp.setLimiterAllChannelsTo(
                    DynamicsProcessing.Limiter(
                        true, true,
                        0,                         // link group
                        0.1f, 100f,                // attack/release (ms)
                        20f,                       // ratio
                        maxThresholdDb.toFloat(),  // threshold (dB)
                        0f                         // post gain
                    )
                )
                dp.enabled = true
            } else {
                dp.enabled = false
            }
        } catch (_: Exception) {
            try { dp.enabled = false } catch (_: Exception) { }
        }
    }

    private fun releaseEffects() {
        try { reverb?.release() } catch (_: Exception) { }
        try { bassBoost?.release() } catch (_: Exception) { }
        try { loudness?.release() } catch (_: Exception) { }
        try { dynamics?.release() } catch (_: Exception) { }
        reverb = null
        bassBoost = null
        loudness = null
        dynamics = null
        _isReady.value = false
    }

    fun release() {
        initJob?.cancel()
        initJob = null
        scope.cancel()
        releaseEffects()
    }
}
