package com.asmrhelper.player

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 真实左右平衡处理器：对 PCM 16-bit 立体声做线性 L/R 增益（-1=全左, 0=居中, 1=全右）。
 * 作为用户自定义 [androidx.media3.common.audio.AudioProcessor] 注入主播放器的
 * AudioSink 处理链，替换原先用 Virtualizer 强度「近似」左右平衡的伪实现。
 */
@Singleton
class StereoBalanceAudioProcessor @Inject constructor() : BaseAudioProcessor() {

    @Volatile private var leftGain = 1f
    @Volatile private var rightGain = 1f

    /** 设置左右平衡（-1=全左, 0=居中, 1=全右）；线性平衡，居中两侧均为 1.0 不改变响度。 */
    fun setBalance(value: Float) {
        val b = value.coerceIn(-1f, 1f)
        leftGain = if (b < 0f) 1f else 1f - b
        rightGain = if (b > 0f) 1f else 1f + b
    }

    override fun onConfigure(inputAudioFormat: AudioFormat): AudioFormat {
        // 仅处理 PCM 16-bit 立体声；其它格式返回 NOT_SET，处理链会自动跳过本处理器。
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT || inputAudioFormat.channelCount != 2) {
            return AudioFormat.NOT_SET
        }
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        // 每帧左右声道各 2 字节（16-bit）；只处理完整立体声帧，避免残帧下溢。
        val frames = inputBuffer.remaining() / 4
        if (frames <= 0) return
        val outputBuffer = replaceOutputBuffer(frames * 4)
        val l = leftGain
        val r = rightGain
        repeat(frames) {
            val leftSample = (inputBuffer.short * l).toInt().coerceIn(-32768, 32767)
            val rightSample = (inputBuffer.short * r).toInt().coerceIn(-32768, 32767)
            outputBuffer.putShort(leftSample.toShort())
            outputBuffer.putShort(rightSample.toShort())
        }
        outputBuffer.flip()
    }
}
