package com.asmrhelper.player

import com.asmrhelper.domain.model.Audio
import com.asmrhelper.domain.model.LoopMode

sealed interface PlayerEvent {
    data class Play(val audio: Audio, val playlist: List<Audio> = emptyList()) : PlayerEvent
    /** 电台（私人FM）播放：队列播完自动拉取新一批歌曲续播，而非循环固定几首。 */
    data class PlayRadio(val audio: Audio, val playlist: List<Audio> = emptyList()) : PlayerEvent
    data object Pause : PlayerEvent
    data object Resume : PlayerEvent
    data object Next : PlayerEvent
    data object Previous : PlayerEvent
    data class SeekTo(val positionMs: Long) : PlayerEvent
    data class SetLoopMode(val mode: LoopMode) : PlayerEvent
    data object ToggleBackground : PlayerEvent
    data class SetBackgroundAudio(val filePath: String) : PlayerEvent
    data class SetAmbientLoop(val enabled: Boolean) : PlayerEvent
    data class SetCrossfade(val durationMs: Int) : PlayerEvent
    data class FadeOut(val durationMs: Long = 3000L) : PlayerEvent
    data class FadeIn(val durationMs: Long = 3000L) : PlayerEvent
}
