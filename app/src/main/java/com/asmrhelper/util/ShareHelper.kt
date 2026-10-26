package com.asmrhelper.util

import android.content.Context
import android.content.Intent
import com.asmrhelper.R
import com.asmrhelper.domain.model.Audio

/**
 * 分享当前歌曲 / 视频音频的辅助工具。
 * - 在线歌曲/视频按平台拼出官方链接（B 站视频、网易云、QQ、YouTube）；
 * - 本地文件无链接，仅分享「歌名 - 歌手」文本。
 */
object ShareHelper {

    /** 在线歌曲/视频的分享链接；本地文件返回 null。 */
    fun linkFor(audio: Audio): String? {
        val id = audio.sourceSongId?.takeIf { it.isNotBlank() } ?: return null
        return when (audio.sourceId) {
            "bilibili" -> "https://www.bilibili.com/video/$id"
            "youtube" -> "https://www.youtube.com/watch?v=$id"
            "netease" -> "https://music.163.com/#/song?id=$id"
            "qq" -> "https://y.qq.com/n/ryqq/songDetail/$id"
            else -> null
        }
    }

    /** 分享文案：有链接时附带链接，否则仅歌名与歌手。 */
    fun textFor(audio: Audio): String {
        val name = buildString {
            append(audio.title)
            if (audio.artist.isNotBlank()) append(" - ").append(audio.artist)
        }
        val link = linkFor(audio)
        return if (link != null) "$name\n$link" else name
    }

    /** 唤起系统分享面板。 */
    fun share(context: Context, audio: Audio) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, textFor(audio))
        }
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.menu_share)))
    }
}
