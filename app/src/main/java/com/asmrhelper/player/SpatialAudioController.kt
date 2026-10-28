package com.asmrhelper.player

import android.media.audiofx.Virtualizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sin

enum class SpatialMode {
    OFF,
    D3,
    SWEEP,
    CIRCLE,
    WIDE
}

@Singleton
class SpatialAudioController @Inject constructor(
    private val balanceProcessor: StereoBalanceAudioProcessor
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var virtualizer: Virtualizer? = null
    private var animJob: Job? = null
    private var fadeJob: Job? = null

    /** 当前已应用强度，用于关闭时渐变归零，避免高增益直接禁用造成爆音。 */
    @Volatile private var lastStrength: Short = 0

    /** 环绕动画速度倍率（0.5~3.0） */
    @Volatile private var surroundSpeed: Float = 1f

    /** 左右平衡 (-1=全左, 0=居中, 1=全右) —— 由 StereoBalanceAudioProcessor 真实作用于 L/R 增益 */
    private val _balance = MutableStateFlow(0f)
    val balanceValue: StateFlow<Float> = _balance.asStateFlow()

    @Volatile var currentMode: SpatialMode = SpatialMode.OFF
        private set

    @Volatile var isActive: Boolean = false
        private set

    fun attach(audioSessionId: Int) {
        release()
        if (audioSessionId == 0) return

        try {
            virtualizer = Virtualizer(0, audioSessionId)
            // 重新应用当前模式强度（此时 virtualizer 已就绪）
            applyModeStrength()
            isActive = currentMode != SpatialMode.OFF
        } catch (_: Exception) {
            // Audio effects not supported on this device
            virtualizer = null
            isActive = false
        }
    }

    fun setMode(mode: SpatialMode) {
        currentMode = mode
        animJob?.cancel()
        fadeJob?.cancel()

        if (mode == SpatialMode.OFF) {
            isActive = false
            val v = virtualizer
            if (v != null) {
                fadeJob = scope.launch { fadeOutAndDisable(v) }
            }
            return
        }
        // 若 virtualizer 尚未创建（尚未 attach），这里先不置 isActive=true，
        // 让 ViewModel 的 attach 守卫能触发 attach()；attach() 完成后会
        // 通过 applyModeStrength() 应用 currentMode。
        if (virtualizer == null) return
        applyModeStrength()
        isActive = true
    }

    /** 将 currentMode 的强度应用到 virtualizer（virtualizer 非空时有效）。 */
    private fun applyModeStrength() {
        val v = virtualizer ?: return
        when (currentMode) {
            SpatialMode.OFF -> v.enabled = false
            SpatialMode.D3 -> {
                v.enabled = true
                applyStrength(v, 300)
            }
            SpatialMode.WIDE -> {
                v.enabled = true
                applyStrength(v, 800)
            }
            SpatialMode.SWEEP, SpatialMode.CIRCLE -> {
                v.enabled = true
                applyStrength(v, 400)
                startPanAnimation(currentMode == SpatialMode.CIRCLE)
            }
        }
    }

    /** 应用强度并记录当前值，供关闭时渐变归零。 */
    private fun applyStrength(v: Virtualizer, strength: Int) {
        val s = strength.coerceIn(0, 1000).toShort()
        lastStrength = s
        try { v.setStrength(s) } catch (_: Exception) { }
    }

    /** 关闭前把强度渐变到 0 再禁用，避免高增益（如「扩展」800）直接关闭产生爆音。 */
    private suspend fun fadeOutAndDisable(v: Virtualizer) {
        val start = lastStrength.toInt()
        val steps = 10
        repeat(steps) { i ->
            val s = (start * (steps - 1 - i) / steps).coerceIn(0, 1000)
            applyStrength(v, s)
            delay(16L)
        }
        try { v.enabled = false } catch (_: Exception) { }
    }

    /** 设置左右平衡 (-1.0 = 全左, 0 = 居中, 1 = 全右)。
     *  由注入主播放器的 StereoBalanceAudioProcessor 真实作用于左右声道增益。 */
    fun setBalance(value: Float) {
        _balance.value = value.coerceIn(-1f, 1f)
        balanceProcessor.setBalance(_balance.value)
    }

    /** 设置声源距离（Virtualizer 强度 0~1000） */
    fun setDistance(strength: Int) {
        virtualizer?.let { applyStrength(it, strength) }
    }

    /** 设置环绕速度倍率 */
    fun setSurroundSpeed(speed: Float) {
        surroundSpeed = speed.coerceIn(0.5f, 3f)
        // 若正在环绕动画，重启以应用新速度
        if (currentMode == SpatialMode.CIRCLE && isActive) {
            animJob?.cancel()
            startPanAnimation(true)
        }
    }

    private fun startPanAnimation(circular: Boolean) {
        animJob = scope.launch {
            var t = 0f
            while (isActive) {
                if (circular) {
                    val depth = (sin(t * 1.7f) * 0.5f + 0.5f).toFloat()
                    virtualizer?.let { applyStrength(it, (400 + depth * 500).toInt()) }
                }
                t += 0.05f * surroundSpeed
                delay((30L / surroundSpeed).toLong().coerceAtLeast(10L))
            }
        }
    }

    fun release() {
        animJob?.cancel()
        animJob = null
        // 不再 cancel scope：这是单例，scope 必须跨 attach/release 周期存活，
        // 否则后续 startPanAnimation 的 scope.launch 会落到已取消的 scope 上，
        // 导致扫掠/环绕动画静默失效。
        virtualizer?.let {
            try { it.setStrength(0.toShort()); it.enabled = false; it.release() } catch (_: Exception) { }
        }
        virtualizer = null
        isActive = false
        // 保留 currentMode，使重新 attach 时能恢复用户已选的模式
    }
}
