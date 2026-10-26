package com.asmrhelper.player

import android.content.Context
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink.DefaultAudioProcessorChain

/**
 * 自定义 RenderersFactory：把 [StereoBalanceAudioProcessor] 注入默认 AudioSink 的
 * 音频处理链，使「左右平衡」真正作用于 PCM 声道增益。仅用于主播放器。
 */
class BalanceRenderersFactory(
    context: Context,
    private val balanceProcessor: StereoBalanceAudioProcessor
) : DefaultRenderersFactory(context) {

    override fun buildAudioSink(
        context: Context,
        enableFloatOutput: Boolean,
        enableAudioTrackPlaybackParams: Boolean
    ): AudioSink {
        return DefaultAudioSink.Builder(context)
            .setAudioProcessorChain(DefaultAudioProcessorChain(balanceProcessor))
            .setEnableFloatOutput(enableFloatOutput)
            .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
            .build()
    }
}
